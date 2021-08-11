package com.msiejak.lab.chromiumupdater.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.msiejak.lab.chromiumupdater.MainActivity
import com.msiejak.lab.chromiumupdater.R
import com.msiejak.lab.chromiumupdater.UpdateChecker
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class UpdateNotificationService(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    val c = applicationContext

    private fun run() {
        Toast.makeText(c, "test", Toast.LENGTH_LONG).show()
        UpdateChecker().check(c, object : UpdateChecker.RequestResult {
            override fun onResult(
                updateAvailable: Int,
                lastModified: String,
                remoteVersion: Long,
                resultCode: Int
            ) {
                if (updateAvailable == UpdateChecker.UPDATE_AVAILABLE && resultCode == 1) {
                    val intent = Intent(applicationContext, MainActivity::class.java)
                    val pendingIntent: PendingIntent = PendingIntent.getActivity(
                        applicationContext,
                        0,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE
                    )
                    val builder = NotificationCompat.Builder(applicationContext, "0")
                        .setSmallIcon(R.drawable.ic_baseline_update_24)
                        .setContentTitle(c.getString(R.string.not_title))
                        .setContentText("An update for chromium is available")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                    createNotificationChannel()
                    with(NotificationManagerCompat.from(applicationContext)) {
                        // notificationId is a unique int for each notification that you must define
                        notify(0, builder.build())
                    }
                }
            }
        }, 1)
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = c.getString(R.string.channel_name)
            val descriptionText = c.getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("0", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                c.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun doWork(): Result {
        Looper.prepare()
        run()
        Log.e("TAG", "doWork: ")
        val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
        val now: LocalDateTime = LocalDateTime.now()
        c.getSharedPreferences("update_check_log", MODE_PRIVATE).edit()
            .putString(dtf.format(now), "checked").apply()
        return Result.success()
    }


}