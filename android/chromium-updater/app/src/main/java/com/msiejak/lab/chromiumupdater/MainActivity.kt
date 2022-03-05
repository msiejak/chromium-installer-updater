package com.msiejak.lab.chromiumupdater

import android.app.AlarmManager
import android.app.DownloadManager
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageInstaller
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.msiejak.lab.chromiumupdater.databinding.ActivityMainBinding
import com.msiejak.lab.chromiumupdater.service.UpdateNotificationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


class MainActivity : AppCompatActivity() {
    private lateinit var receiver: BroadcastReceiver
    private lateinit var binding: ActivityMainBinding
    private var currentRemote: Long = 0
    private var chromiumInstalled = false
    private var init = false

    // You didn't have to delete both of them lol

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
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
//        startService()
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
                R.id.settings -> {
                    launchSettingsActivity()
                    true
                }
                else -> true
            }
        }
//        setChromiumVersionText()
        //No I'm just not getting any error. Can I try something? Can you restart android st
    }

    private fun launchSettingsActivity() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun startService() {
//        val configuration = Configuration.Builder().apply {
//            // example: setWorkerFactory()
//            //I actually need to go no. It sounds like there is a bunch of water in our basement or something?
//            // no problem, good luck! THnaks!
//        }.build()
//
//        if(WorkManager.getInstance(this).) {
//            WorkManager.initialize(this, configuration)
//            init = true
//        }
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val periodicWorkRequest =
            PeriodicWorkRequest.Builder(UpdateNotificationService::class.java, 40, TimeUnit.MINUTES)
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
                "Retry"
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
                var ze: ZipEntry? //Let's try this. Letting you try lol. RUnning it? emulator??
                var count: Int
                val buffer = ByteArray(8192)
                try {
                    while (zis.nextEntry.also { ze = it } != null) {
                        if (ze == null) error("Zip Entry should not be null")
                        val currentEntry = ze!!

                        val file = File(targetDirectory, currentEntry.name)
                        val dir = if (currentEntry.isDirectory) file else file.parentFile
                        if (!dir.isDirectory && !dir.mkdirs()) throw FileNotFoundException(
                            "Failed to ensure directory: " +
                                    dir.absolutePath
                        )
                        if (currentEntry.isDirectory) continue
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
                    Log.e("error", "error")
                }
            } finally {
                zis.close()
                Log.d("Unzip", "Successfully unzipped archive")
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
        var session: PackageInstaller.Session? = null
        Log.d("Install", "Starting installation")
        try {
            val packageInstaller = packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL
            )
            val sessionId = packageInstaller.createSession(params)
            session = packageInstaller.openSession(sessionId)
            addApkToInstallSession("/chromium/extracted/chrome-android/apks/ChromePublic.apk", session)
            // Create an install status receiver.
            val context: Context = this@MainActivity
            val intent = Intent(context, MainActivity::class.java)
            intent.action = PACKAGE_INSTALLED_ACTION
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            val statusReceiver = pendingIntent.intentSender
            // Commit the session (this will start the installation workflow).
            session.commit(statusReceiver)
//            getSharedPreferences("shared_prefs", MODE_PRIVATE).edit().putLong("build", currentRemote)
//                .apply()
//            setChromiumVersionText()
//            reset()
            Log.d("Install", "Installation finished")
        } catch (e: IOException) {
            throw RuntimeException("Couldn't install package", e)
        } catch (e: RuntimeException) {
            session?.abandon()
            throw e
            Log.e("crash", "install: " )
        }
        Log.e("done", "install: ")
    }

    @Throws(IOException::class)
    private fun addApkToInstallSession(fileName: String, session: PackageInstaller.Session) {
        // It's recommended to pass the file size to openWrite(). Otherwise installation may fail
        // if the disk is almost full.
        session.openWrite("package", 0, -1).use { packageInSession ->
            packageInSession.write(File(externalCacheDir, fileName).readBytes())
        }
    }

    // Note: this Activity must run in singleTop launchMode for it to be able to receive the intent
    // in onNewIntent().
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val extras = intent.extras
        if (PACKAGE_INSTALLED_ACTION == intent.action) {
            val status = extras!!.getInt(PackageInstaller.EXTRA_STATUS)
            val message = extras.getString(PackageInstaller.EXTRA_STATUS_MESSAGE)
            when (status) {
                PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                    // This test app isn't privileged, so the user has to confirm the install.
                    val confirmIntent = extras[Intent.EXTRA_INTENT] as Intent?
                    startActivity(confirmIntent)
                }
                PackageInstaller.STATUS_SUCCESS -> Toast.makeText(
                    this,
                    "Install succeeded!",
                    Toast.LENGTH_SHORT
                ).show()
                PackageInstaller.STATUS_FAILURE, PackageInstaller.STATUS_FAILURE_ABORTED, PackageInstaller.STATUS_FAILURE_BLOCKED, PackageInstaller.STATUS_FAILURE_CONFLICT, PackageInstaller.STATUS_FAILURE_INCOMPATIBLE, PackageInstaller.STATUS_FAILURE_INVALID, PackageInstaller.STATUS_FAILURE_STORAGE -> Toast.makeText(
                    this,
                    "Install failed! $status, $message",
                    Toast.LENGTH_SHORT
                ).show()
                else -> Toast.makeText(
                    this, "Unrecognized status received from installer: $status",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    companion object {
        private const val PACKAGE_INSTALLED_ACTION =
            "com.example.android.apis.content.SESSION_API_PACKAGE_INSTALLED"
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

//That's unrealated. It's just saying that chromium isn't installed. Text is small. did it work?
// no

//Here's the history before I made any changes
// eah I can see that. I don't really know your app so I can't know what's causing it.
// 'm pretty sure it's caused by that.