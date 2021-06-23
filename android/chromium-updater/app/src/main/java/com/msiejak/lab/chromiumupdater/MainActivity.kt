package com.msiejak.lab.chromiumupdater

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.msiejak.lab.chromiumupdater.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


class MainActivity : AppCompatActivity() {
    lateinit var receiver: BroadcastReceiver
    lateinit var binding: ActivityMainBinding
    var downloaded = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setChromiumVersionText()
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                unzip(File(externalCacheDir, "/chromium/chromium.zip"), File(externalCacheDir, "/chromium/extracted"))
            }
        }
        registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        binding.startButton.setOnClickListener{ downloadBuild() }
        binding.topAppBar.setOnMenuItemClickListener() { item ->
            when (item.itemId) {
                R.id.refresh -> {
                    setChromiumVersionText()
                    true
                }else -> true
            }
        }
    }

    private fun downloadBuild() {
        binding.progressIndicator.visibility = View.VISIBLE
        Toast.makeText(this, "working...", Toast.LENGTH_LONG).show()
        File(externalCacheDir?.absolutePath + "/chromium").deleteRecursively()
        File(externalCacheDir?.absolutePath + "/chromium").mkdir()
        val request = DownloadManager.Request(Uri.parse("https://download-chromium.appspot.com/dl/Android?type=snapshots"))
        val uri = "file://${externalCacheDir?.absolutePath}/chromium/chromium.zip".toUri()
        request.setDestinationUri(uri)
        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
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
                if (checkForUpdate()) install()
                else Toast.makeText(applicationContext, "no update avaliable", Toast.LENGTH_LONG).show() }
            downloaded = true

        }
    }
    }

    private fun checkForUpdate(): Boolean {
        return true
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
    }

    private fun setChromiumVersionText() {
        var txt = getString(R.string.not_installed)
        try {
            val packageInfo = packageManager.getPackageInfo("org.chromium.chrome", 0)
            txt = "Chromium Version: ${packageInfo.versionName}"
            binding.startButton.setText(R.string.action_update)
        }catch(e: Exception){
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