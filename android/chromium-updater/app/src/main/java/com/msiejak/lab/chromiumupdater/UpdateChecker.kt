package com.msiejak.lab.chromiumupdater

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley

class UpdateChecker {

    fun check(context: Context, onResult: (result: UpdateCheckerResult) -> Unit) {
        val queue = Volley.newRequestQueue(context)

        val sharedPreferences = context.getSharedPreferences("shared_prefs", MODE_PRIVATE)
        val installedVersion = sharedPreferences.getLong("build", 0)

        val request = JsonObjectRequest(VERSION_URL, null, { response ->
            try {
                val remoteVersion = response.getString("content").toLong()
                Log.i(TAG, "Remote: $remoteVersion | Installed: $installedVersion")

                val isNewVersion = remoteVersion > installedVersion
                val lastModified = response.getString("last-modified")

                onResult(UpdateCheckerResult.Success(isNewVersion, remoteVersion, lastModified))
            } catch (e: Exception) {
                onResult(UpdateCheckerResult.Error(ErrorType.PARSING))
            }
        }, {
            onResult(UpdateCheckerResult.Error(ErrorType.NETWORK))
        }).apply {
            setShouldCache(false)
            setShouldRetryConnectionErrors(true)
        }

        queue.add(request)
        queue.start()
    }

    fun getLatestVersion(context: Context, onResult: (result: UpdateCheckerResult) -> Unit) {
        val queue = Volley.newRequestQueue(context)
        val request = JsonObjectRequest(VERSION_URL, null, { response ->
            try {
                val remoteVersion = response.getString("content").toLong()
                Log.i(TAG, "Remote: $remoteVersion")
                onResult(UpdateCheckerResult.Success(false, remoteVersion, "null"))
            } catch (e: Exception) {
                onResult(UpdateCheckerResult.Error(ErrorType.PARSING))
            }
        }, {
            onResult(UpdateCheckerResult.Error(ErrorType.NETWORK))
        }).apply {
            setShouldCache(false)
            setShouldRetryConnectionErrors(true)
        }

        queue.add(request)
        queue.start()
    }


    companion object {
        private const val TAG = "UpdateChecker"

        private const val VERSION_URL = "https://download-chromium.appspot.com/rev/Android?type=snapshots"
    }
}

sealed class UpdateCheckerResult {
    data class Success(val isNewVersion: Boolean, val latestVersion: Long, val lastModified: String): UpdateCheckerResult()
    data class Error(val type: ErrorType): UpdateCheckerResult()
}

enum class ErrorType {
    PARSING, NETWORK
}