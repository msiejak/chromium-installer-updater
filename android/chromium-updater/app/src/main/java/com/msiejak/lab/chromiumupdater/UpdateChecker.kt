package com.msiejak.lab.chromiumupdater

import android.content.Context
import android.util.Log
import android.view.ContentInfo
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException

class UpdateChecker {

    fun check(c: Context, callback: RequestResult, requestCode: Int) {
        val queue = Volley.newRequestQueue(c)
        val s = c.getSharedPreferences("shared_prefs", AppCompatActivity.MODE_PRIVATE)
        val currentInstalled = s.getLong("build", 0)
        val url = "https://download-chromium.appspot.com/rev/Android?type=snapshots"
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null, { response ->
            try {
                val currentRemote = response.getString("content").toLong()
                Log.i(currentRemote.toString(), "checkForUpdate: remote" )
                Log.i(currentInstalled.toString(), "checkForUpdate: installed" )
                if(currentRemote > currentInstalled) {
                    callback.onResult(true, response.getString("last-modified"),currentRemote,  requestCode)
                }
            } catch (e: Exception) {
                callback.onResult(false,"null",0, requestCode)
            }
        }) {
        //valley error
        }
        jsonObjectRequest.setShouldCache(false)
        jsonObjectRequest.setShouldRetryConnectionErrors(true)
        queue.add(jsonObjectRequest)
        queue.start()
    }

    interface RequestResult {
        fun onResult(updateAvailable: Boolean, lastModified: String, remoteVersion: Long, resultCode: Int)
    }

}