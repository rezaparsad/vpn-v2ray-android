package com.vpnload.vpn.network

import android.annotation.SuppressLint
import com.vpnload.vpn.R
import android.app.Activity
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.vpnload.vpn.BuildConfig
import com.vpnload.vpn.util.Crypto
import org.json.JSONObject
import java.util.Locale
import java.util.TimeZone


class Requests(context: Activity) {
    val activity: Activity
    init {
        this.activity = context
    }

    fun post(url: String, callback: RequestResponseCallback) {
        val stringRequest: StringRequest = object : StringRequest( Method.POST, url,
            Response.Listener { response ->
                callback.callback(Crypto().decrypt(response))
            },
            Response.ErrorListener { error ->
                callback.callbackError()
            }) {
            @SuppressLint("HardwareIds")
            override fun getBody(): ByteArray {
                val params = HashMap<String, String>()
                val manufacturer = Build.MANUFACTURER
                val model = Build.MODEL
                val locale = Locale.getDefault()
                val timeZone: TimeZone = TimeZone.getDefault()
                val id: String = Settings.Secure.getString(
                    activity.contentResolver,
                    Settings.Secure.ANDROID_ID
                )

                params["app_name"] = "VPNLoad"
                params["device_id"] = id
                params["device_model"] = "$manufacturer $model"
                params["system_version"] = "SDK " + Build.VERSION.SDK_INT
                params["app_version"] = BuildConfig.VERSION_NAME
                params["segment_name"] = activity.getString(R.string.segment_name)
                params["time_zone"] = timeZone.id
                params["country"] = locale.country
                params["language"] = locale.language

                return JSONObject(params as Map<*, *>?).toString().toByteArray()
            }
        }
        val requestQueue = Volley.newRequestQueue(activity)
        requestQueue.add(stringRequest)
    }
}