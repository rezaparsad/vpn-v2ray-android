package com.vpnload.vpn.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.tencent.mmkv.MMKV
import com.vpnload.vpn.AppConfig
import com.vpnload.vpn.R
import com.vpnload.vpn.util.MmkvManager


class NewUpdateActivity : AppCompatActivity() {

    private val settingsStorage by lazy { MMKV.mmkvWithID(MmkvManager.ID_SETTING, MMKV.MULTI_PROCESS_MODE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_update)

        settingsStorage.encode(AppConfig.TIME_SHOW_NEW_UPDATE, System.currentTimeMillis().toString())

        val updateStatus = settingsStorage.decodeString(AppConfig.UPDATE_STATUS)
        val urlUpdate = settingsStorage.decodeString(AppConfig.UPDATE_URL, "https:google.com")
        val messageUpdate = settingsStorage.decodeString(AppConfig.UPDATE_MESSAGE, "update")

        val buttonUpdate: Button = findViewById(R.id.button_new_update)
        val textViewMessage: TextView = findViewById(R.id.message_new_update)
        val textViewLater: TextView = findViewById(R.id.later_new_update)

        textViewMessage.text = messageUpdate

        buttonUpdate.setOnClickListener {
            val urlIntent = Intent(Intent.ACTION_VIEW, Uri.parse(urlUpdate))
            startActivity(urlIntent)
        }

        if (updateStatus == "necessary") {
            textViewLater.visibility = View.GONE
        } else {
            textViewLater.setOnClickListener {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
}