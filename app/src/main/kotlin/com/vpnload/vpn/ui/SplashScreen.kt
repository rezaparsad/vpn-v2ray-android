package com.vpnload.vpn.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.tencent.mmkv.MMKV
import com.vpnload.vpn.AppConfig
import com.vpnload.vpn.BuildConfig
import com.vpnload.vpn.R
import com.vpnload.vpn.extension.toast
import com.vpnload.vpn.network.RequestResponseCallback
import com.vpnload.vpn.network.Requests
import com.vpnload.vpn.util.AngConfigManager
import com.vpnload.vpn.util.CheckInternet
import com.vpnload.vpn.util.MmkvManager
import com.vpnload.vpn.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Random


class SplashScreen : AppCompatActivity() {

    private val settingsStorage by lazy { MMKV.mmkvWithID(MmkvManager.ID_SETTING, MMKV.MULTI_PROCESS_MODE) }
    private val mainStorage by lazy { MMKV.mmkvWithID(MmkvManager.ID_MAIN, MMKV.MULTI_PROCESS_MODE) }
    private val mainViewModel: MainViewModel by viewModels()

    private lateinit var animationSplash: LottieAnimationView
    private lateinit var buttonRefresh: Button
    private var isRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.WHITE
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        settingsStorage.encode(AppConfig.PREF_LANGUAGE, "en")

        animationSplash = findViewById(R.id.splash_screen_animation)
        buttonRefresh = findViewById(R.id.refresh_button)
        buttonRefresh.setOnClickListener {
            loadPage()
        }
        mainViewModel.isRunning.observe(this) { isRunning ->
            if (isRunning) {
                this@SplashScreen.isRunning = true
                startActivity(Intent(this@SplashScreen, MainActivity::class.java))
                finish()
            } else {
                Handler().postDelayed({
                    loadPage()
                }, 300)
            }
        }
        mainViewModel.startListenBroadcast()

    }
    private fun loadPage() {
        if (isRunning) return
        if (!CheckInternet().isOnline(this)) {
            toast("Check your internet !")
            buttonRefresh.visibility = View.VISIBLE
            animationSplash.setAnimation(R.raw.splash_screen_error)
            animationSplash.loop(false)
            animationSplash.playAnimation()
        } else {
            buttonRefresh.visibility = View.GONE
            animationSplash.setAnimation(R.raw.splash_screen_load)
            animationSplash.loop(true)
            animationSplash.playAnimation()
            setData()
        }
    }
    private fun setData() {
        val timeFixData = settingsStorage?.decodeString(AppConfig.WATT_FIX_DATA)?.toLong()
        val timeLastGetData = settingsStorage?.decodeString(AppConfig.TIME_LAST_GET_DATA)?.toLong()
        if (timeFixData != null && timeLastGetData != null) {
            if (((timeFixData) + timeLastGetData) > System.currentTimeMillis()) {
                selectedRandomServer()
                checkNewUpdate()
            }else {
                getData()
            }
        }else {
            getData()
        }
    }
    private fun selectedRandomServer() {
        val serverList = MmkvManager.decodeServerList()
        val sizeServerList = serverList.size
        val rand = (0 until sizeServerList).random()
        val guid = serverList[rand]
        mainStorage?.encode(MmkvManager.KEY_SELECTED_SERVER, guid)
    }

    private fun getData() {
        Requests(this).post("http://vpnload.de/api/v1/status/", object: RequestResponseCallback {
            override fun callback(data: String) {
                val jsonData = JSONObject(data)
                val to = jsonData.getJSONObject("to")
                val appVersion = jsonData.getJSONObject("app_version")
                val time = jsonData.getJSONObject("time")
                val data = jsonData.getJSONObject("data")

                settingsStorage?.encode(AppConfig.ADMOB_STATUS, to.getString("admob_status"))
                settingsStorage?.encode(AppConfig.ADMOB_APP_ID, to.getString("admob_app_id"))
                settingsStorage?.encode(AppConfig.ADMOB_INTERSTITIAL_ID, to.getString("admob_interstitial_id"))
                settingsStorage?.encode(AppConfig.ADMOB_NATIVE_ID, to.getString("admob_native_id"))
                settingsStorage?.encode(AppConfig.ADMOB_APP_OPEN_ID, to.getString("admob_app_open_id"))

                settingsStorage?.encode(AppConfig.UPDATE_STATUS, appVersion.getString("status"))
                settingsStorage?.encode(AppConfig.UPDATE_VERSION_APP, appVersion.getString("version"))
                settingsStorage?.encode(AppConfig.UPDATE_URL, appVersion.getString("url"))
                settingsStorage?.encode(AppConfig.UPDATE_MESSAGE, appVersion.getString("message"))

                settingsStorage?.encode(AppConfig.WATT_FIX_DATA, time.getString("wait_fix_data"))
                settingsStorage?.encode(AppConfig.WATT_SEE_INTERSTITIAL, time.getString("wait_see_interstitial"))
                settingsStorage?.encode(AppConfig.WAIT_SEE_OPEN_APP, time.getString("wait_see_open_app"))
                settingsStorage?.encode(AppConfig.WAIT_NEW_UPDATE, time.getString("wait_new_update"))
                settingsStorage?.encode(AppConfig.WAIT_EXPIRE_ADS, time.getString("wait_expire_ads"))
                settingsStorage?.encode(AppConfig.WAIT_FAILED_ADS, time.getString("wait_failed_ads"))
                settingsStorage?.encode(AppConfig.WAIT_ADS, time.getString("wait_ads"))
                settingsStorage?.encode(AppConfig.WAIT_CONNECTED, time.getString("wait_connected"))
                settingsStorage?.encode(AppConfig.TIME_STOP_CONNECTED, time.getString("stop_connected"))
                settingsStorage?.encode(AppConfig.IS_EXPIRE_INTERSTITIAL, time.getString("is_expire_interstitial"))
                settingsStorage?.encode(AppConfig.IS_EXPIRE_INTERSTITIAL_CLICK_CONNECT, time.getString("is_expire_interstitial_click_connect"))

                settingsStorage?.encode(AppConfig.SHARE_APP_LINK, data.getString("share_link"))

                settingsStorage?.encode(AppConfig.TIME_LAST_GET_DATA, System.currentTimeMillis().toString())

                setServers(jsonData.getJSONArray("servers"))
                AngConfigManager.migrateLegacyConfig(this@SplashScreen)
                selectedRandomServer()
                checkNewUpdate()

            }

            override fun callbackError() {
                loadPage()
            }
        })
    }

    private fun shuffleJsonArray(array: JSONArray): JSONArray {
        // Implementing Fisher–Yates shuffle
        val rnd = Random()
        for (i in array.length() - 1 downTo 0) {
            val j = rnd.nextInt(i + 1)
            // Simple swap
            val obj = array[j]
            array.put(j, array[i])
            array.put(i, obj)
        }
        return array
    }
    private fun setServers(servers: JSONArray) {
        MmkvManager.removeAllServer()
        mainViewModel.reloadServerList()
        val servers = shuffleJsonArray(servers)
        for (i in 0 until servers.length()) {
            val jsonServer = servers.getJSONObject(i)
            importBatchConfig(jsonServer.getString("key"))
        }
    }

    private fun importBatchConfig(server: String, subid: String = "") {
        val subid2 = if(subid.isNullOrEmpty()){
            mainViewModel.subscriptionId
        }else{
            subid
        }
        val append = subid.isNullOrEmpty()

        val removedSelectedServer =
            if (!TextUtils.isEmpty(subid2) && !append) {
                MmkvManager.decodeServerConfig(
                    AngConfigManager.mainStorage?.decodeString(
                        MmkvManager.KEY_SELECTED_SERVER
                    ) ?: "")?.let {
                    if (it.subscriptionId == subid) {
                        return@let it
                    }
                    return@let null
                }
            } else {
                null
            }
        if(!append) {
            MmkvManager.removeServerViaSubid(subid)
        }

        server.lines()
            .reversed()
            .forEach {
                val resId = AngConfigManager.importConfig(it, subid, removedSelectedServer)
            }
    }

    private fun isTimeShowUpdate(): Boolean {
        var lastTimeShowNewUpdate = settingsStorage?.decodeString(AppConfig.TIME_SHOW_NEW_UPDATE, "0")?.toLong()
        val waitTimeShowNewUpdate = settingsStorage?.decodeString(AppConfig.WAIT_NEW_UPDATE)?.toLong()
        if (lastTimeShowNewUpdate == null) lastTimeShowNewUpdate = 0
        if ((lastTimeShowNewUpdate + waitTimeShowNewUpdate!!) > System.currentTimeMillis()) return true
        return false
    }

    private fun checkNewUpdate() {
        val updateVersion = settingsStorage?.decodeString(AppConfig.UPDATE_VERSION_APP, "0")?.toInt()
        val updateStatus = settingsStorage?.decodeString(AppConfig.UPDATE_STATUS)
        if (updateVersion != null) {
            if (updateVersion > BuildConfig.VERSION_CODE) {
                if (updateStatus == "necessary" || !isTimeShowUpdate()) {
                    startActivity(Intent(this@SplashScreen, NewUpdateActivity::class.java))
                    finish()
                }else {
                    startActivity(Intent(this@SplashScreen, MainActivity::class.java))
                    finish()
                }
            } else {
                startActivity(Intent(this@SplashScreen, MainActivity::class.java))
                finish()
            }
        }
    }

    private fun migrateLegacy() {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = AngConfigManager.migrateLegacyConfig(this@SplashScreen)
            if (result != null) {
                launch(Dispatchers.Main) {
                    if (result) {
                        toast(getString(R.string.migration_success))
                        mainViewModel.reloadServerList()
                    } else {
                        toast(getString(R.string.migration_fail))
                    }
                }
            }
        }
    }
}