package com.msiejak.lab.chromiumupdater

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.DownloadManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.msiejak.lab.chromiumupdater.databinding.ActivityMainBinding
import com.msiejak.lab.chromiumupdater.service.UpdateNotificationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.*
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


class MainActivity : AppCompatActivity() {
    private lateinit var receiver: BroadcastReceiver
    private lateinit var binding: ActivityMainBinding
    private var currentRemote: Long = 0
    private var chromiumInstalled = false

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DynamicColors.applyIfAvailable(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
        if(!checkNotPermission()) {
            requestNotPermission()
        }
        val alarmManager =
            getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val pendingIntent =
            PendingIntent.getService(
                this, 1, intent,
                PendingIntent.FLAG_IMMUTABLE
            )
        if (pendingIntent != null && alarmManager != null) {
            alarmManager.cancel(pendingIntent)
        }
        startService()
        setChromiumVersionText()
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                unzip(
                    File(externalCacheDir, "/chromium/chromium.zip"),
                    File(externalCacheDir, "/chromium/extracted")
                )
            }
        }
        registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        if (chromiumInstalled) {
            binding.startButton.setOnClickListener { checkForUpdate() }
        } else {
            binding.startButton.setOnClickListener { downloadBuild() }
        }
        binding.topAppBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.refresh -> {
                    setChromiumVersionText()
                    checkForUpdate()
                    true
                }
                else -> true
            }
        }
    }

    private fun checkNotPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }else return true
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotPermission() {
        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    Toast.makeText(
                        this@MainActivity,
                        "Permission granted",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Permission denied. You will not be notified about new updates",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        MaterialAlertDialogBuilder(this@MainActivity)
            .setTitle("Permissions Needed")
            .setMessage("In order to be notified about new Chromium builds, you need to grant permission to send notifications")
            .setPositiveButton("Okay") { _, _ ->
                requestPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
            .setNegativeButton("No thanks") { _, _ ->
                Toast.makeText(
                    this@MainActivity,
                    "You will not be notified about new updates",
                    Toast.LENGTH_LONG
                ).show()
            }
            .show()
    }

    private fun startService() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val periodicWorkRequest =
            PeriodicWorkRequest.Builder(UpdateNotificationService::class.java, 15, TimeUnit.MINUTES)
                .setConstraints(constraints).build()
        WorkManager.getInstance(application).enqueueUniquePeriodicWork(
            "updateCheck",
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicWorkRequest
        )

    }

    private fun downloadBuild() {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager.activeNetworkInfo != null && connectivityManager.activeNetworkInfo!!.isConnected) {

            binding.progressIndicator.visibility = View.VISIBLE
            binding.startButton.isEnabled = false
            Toast.makeText(this, "working...", Toast.LENGTH_LONG).show()
            File(externalCacheDir?.absolutePath + "/chromium").deleteRecursively()
            File(externalCacheDir?.absolutePath + "/chromium").mkdir()
            val request =
                DownloadManager.Request(Uri.parse("https://download-chromium.appspot.com/dl/Android?type=snapshots"))
            val uri = "file://${externalCacheDir?.absolutePath}/chromium/chromium.zip".toUri()
            request.setDestinationUri(uri)
            val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
        } else {
            val builder = MaterialAlertDialogBuilder(this)
            builder.setTitle("Network Error")
            builder.setCancelable(false)
            builder.setIcon(R.drawable.ic_baseline_error_outline_24)
            builder.setPositiveButton(
                "retry"
            ) { _: DialogInterface?, _: Int ->
                downloadBuild()
            }
            builder.setMessage("You will be unable to download Chromium until you connect to the internet")
            builder.show()
        }
    }


    @Throws(IOException::class)
    private fun unzip(zipFile: File?, targetDirectory: File?) {
        val zis = ZipInputStream(
            BufferedInputStream(FileInputStream(zipFile))
        )
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                var ze: ZipEntry
                var count: Int
                val buffer = ByteArray(8192)
                try {
                    while (zis.nextEntry.also { ze = it } != null) {
                        val file = File(targetDirectory, ze.name)
                        val dir = if (ze.isDirectory) file else file.parentFile
                        if (!dir.isDirectory && !dir.mkdirs()) throw FileNotFoundException(
                            "Failed to ensure directory: " +
                                    dir.absolutePath
                        )
                        if (ze.isDirectory) continue
                        val fout = FileOutputStream(file)
                        try {
                            while (zis.read(buffer).also { count = it } != -1) fout.write(
                                buffer,
                                0,
                                count
                            )
                        } finally {
                            fout.close()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } finally {
                zis.close()
                runOnUiThread {
                    binding.progressIndicator.visibility = View.GONE
                    binding.startButton.isEnabled = true
                    install()
                }

            }
        }
    }

    private fun checkForUpdate() {
        binding.progressIndicator.visibility = View.VISIBLE

        UpdateChecker().check(this) { result ->
            // Currently, only UpdateCheckerResult.Success is used.
            if (result is UpdateCheckerResult.Success && result.isNewVersion) {
                binding.startButton.setOnClickListener { downloadBuild() }
                this.currentRemote = result.latestVersion

                binding.startButton.setText(R.string.action_update)
                binding.updateAvaliable.text = "Update Available\nNewest Version available was built at ${result.lastModified}"
            } else {
                binding.updateAvaliable.text = getString(R.string.no_update)
            }

            binding.progressIndicator.visibility = View.INVISIBLE
        }
    }

    private fun install() {
        val uri = FileProvider.getUriForFile(
            this,
            applicationContext.packageName + ".provider",
            File(externalCacheDir, "/chromium/extracted/chrome-android/apks/ChromePublic.apk")
        )
        val install = Intent(Intent.ACTION_VIEW)
        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        install.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        install.data = uri
        startActivity(install)
        getSharedPreferences("shared_prefs", MODE_PRIVATE).edit().putLong("build", currentRemote)
            .apply()
        setChromiumVersionText()
        reset()
    }

    private fun reset() {
        binding.startButton.setOnClickListener { checkForUpdate() }
        binding.updateAvaliable.text = ""
    }

    private fun setChromiumVersionText() {
        var txt = getString(R.string.not_installed)
        try {
            val packageInfo = packageManager.getPackageInfo("org.chromium.chrome", 0)
            txt = "Installed Chromium Version: ${packageInfo.versionName}"
            binding.startButton.setText(R.string.check_update)
            chromiumInstalled = true
        } catch (e: Exception) {
            e.printStackTrace()
            binding.startButton.setText(R.string.action_install)
        }
        binding.versionName.text = txt
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }
}