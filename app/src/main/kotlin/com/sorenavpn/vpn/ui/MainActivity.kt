package com.sorenavpn.vpn.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.*
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.material.navigation.NavigationView
import com.tencent.mmkv.MMKV
import com.sorenavpn.vpn.AppConfig
import com.sorenavpn.vpn.AppConfig.ANG_PACKAGE
import com.sorenavpn.vpn.BuildConfig
import com.sorenavpn.vpn.R
import com.sorenavpn.vpn.databinding.ActivityMainBinding
import com.sorenavpn.vpn.extension.toast
import com.sorenavpn.vpn.service.V2RayServiceManager
import com.sorenavpn.vpn.ui.interfaces.NativeCallback
import com.sorenavpn.vpn.util.*
import com.sorenavpn.vpn.viewmodel.MainViewModel
import kotlinx.coroutines.*
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import java.io.File
import java.io.FileOutputStream
import java.lang.Runnable
import java.util.concurrent.TimeUnit



class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: ActivityMainBinding

    private val mainStorage by lazy { MMKV.mmkvWithID(MmkvManager.ID_MAIN, MMKV.MULTI_PROCESS_MODE) }
    private val settingsStorage by lazy { MMKV.mmkvWithID(MmkvManager.ID_SETTING, MMKV.MULTI_PROCESS_MODE) }
    private val requestVpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            startV2Ray()
        }
    }
    private val mainViewModel: MainViewModel by viewModels()
    private var isConnectPrimary = false
    private var isFirstBroadCast = true
    private var isStopActivity = true
    companion object {
        var isSelectLocationTimer = false
        var isSelectLocation = false
        var isConnected = false
    }
    private lateinit var admob: Admob
    private lateinit var dialog: Dialog
    private lateinit var runnableNativeExit: Runnable
    private lateinit var runnableCheckConnect: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        title = getString(R.string.title_server)
        setSupportActionBar(binding.toolbar)
        binding.locations.setOnClickListener {
            startActivity(Intent(this, LocationActivity::class.java))
        }
        val isExpireInterstitial = settingsStorage.decodeString(AppConfig.IS_EXPIRE_INTERSTITIAL)
        if (isExpireInterstitial == "active") {
            settingsStorage.encode(AppConfig.TIME_SHOW_INTERSTITIAL, "0")
        }
        binding.connectButton.setBackgroundResource(R.drawable.background_connect_primary)
        binding.connectButton.setOnClickListener{
            if (mainViewModel.isRunning.value == true) {
                stopV2ray()
            } else if (settingsStorage?.decodeString(AppConfig.PREF_MODE) ?: "VPN" == "VPN") {
                val intent = VpnService.prepare(this)
                if (intent == null) {
                    startV2Ray()
                } else {
                    requestVpnPermission.launch(intent)
                }
            } else {
                startV2Ray()
            }
        }
        registerReceiver(mMsgReceiver, IntentFilter(AppConfig.BROADCAST_ACTION_ACTIVITY))

        val toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        binding.navView.setNavigationItemSelectedListener(this)
        binding.version.text = "v${BuildConfig.VERSION_NAME} (${SpeedtestUtil.getLibVersion()})"

        copyAssets()
        migrateLegacy()
        setViewModel()
        dialog = Dialog(this)
        dialog.setCancelable(false)

        askNotificationPermission()


        admob = Admob(this)

        val handler = Handler()
        runnableCheckConnect = Runnable {
            if (isConnected) {
                admob.showInterstitial()
                admob.showNative(findViewById(R.id.ad_unified), object: NativeCallback {
                    override fun callback() {
                    }
                })
            }else {
                handler.postDelayed(runnableCheckConnect, 100)
            }
        }
        handler.postDelayed(runnableCheckConnect, 500)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.

            } else {
                toast("Not access to notification!")
            }
        }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {

            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private fun setViewModel() {
        mainViewModel.isRunning.observe(this) { isRunning ->
            if (isRunning) {
                mainViewModel.testCurrentServerRealPing()
                showCircle()

            } else {
                hideCircle()
            }
        }
        mainViewModel.startListenBroadcast()
    }

    private fun setLocation() {
        var imageLocation: ImageView = findViewById(R.id.image_location)
        var textCountry: TextView = findViewById(R.id.country_location)
        var textSpeed: TextView = findViewById(R.id.speed_location)
        val guid = mainStorage?.decodeString(MmkvManager.KEY_SELECTED_SERVER)
        val config = guid?.let { MmkvManager.decodeServerConfig(it) }
        var modelLocation = ModelLocation()
        if (config != null) {
            modelLocation.country = config.remarks
        }
        if (guid != null) {
            modelLocation.guid = guid
        }
        imageLocation.setImageResource(modelLocation.getImage())
        textCountry.text = modelLocation.country
        textSpeed.text = modelLocation.speed
    }

    private fun copyAssets() {
        val extFolder = Utils.userAssetPath(this)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geo = arrayOf("geosite.dat", "geoip.dat")
                assets.list("")
                    ?.filter { geo.contains(it) }
                    ?.filter { !File(extFolder, it).exists() }
                    ?.forEach {
                        val target = File(extFolder, it)
                        assets.open(it).use { input ->
                            FileOutputStream(target).use { output ->
                                input.copyTo(output)
                            }
                        }
                        Log.i(ANG_PACKAGE, "Copied from apk assets folder to ${target.absolutePath}")
                    }
            } catch (e: Exception) {
                Log.e(ANG_PACKAGE, "asset copy failed", e)
            }
        }
    }

    private fun migrateLegacy() {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = AngConfigManager.migrateLegacyConfig(this@MainActivity)
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

    private fun stopV2ray() {
        if (settingsStorage.decodeString(AppConfig.IS_EXPIRE_INTERSTITIAL_CLICK_CONNECT).equals("active")) {
            settingsStorage.encode(AppConfig.TIME_SHOW_INTERSTITIAL, "0")
        }
        nativeDisconnect()
    }

    private fun startV2Ray() {
        if (settingsStorage.decodeString(AppConfig.IS_EXPIRE_INTERSTITIAL_CLICK_CONNECT).equals("active")) {
            settingsStorage.encode(AppConfig.TIME_SHOW_INTERSTITIAL, "0")
        }
        if (mainStorage?.decodeString(MmkvManager.KEY_SELECTED_SERVER).isNullOrEmpty()) {
            return
        }
        showCircle()
        isFirstBroadCast = false
        V2RayServiceManager.startV2Ray(this)
        settingsStorage.encode(AppConfig.TIME_START_CONNECT, System.currentTimeMillis().toString())

        Handler().postDelayed({
            mainViewModel.testCurrentServerRealPing()
        }, settingsStorage.decodeString(AppConfig.WAIT_ADS, "1000")?.toLong() ?: 1000)

        val ttl: Long = 1000
        Handler().postDelayed({
            nativeConnect()
        }, ttl)
    }

    private fun restartV2Ray() {
        if (settingsStorage.decodeString(AppConfig.IS_EXPIRE_INTERSTITIAL_CLICK_CONNECT).equals("active")) {
            settingsStorage.encode(AppConfig.TIME_SHOW_INTERSTITIAL, "0")
        }
        if (mainViewModel.isRunning.value == true) {
            stopV2ray()
        }
        Observable.timer(500, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                startV2Ray()
            }
    }

    public override fun onResume() {
        super.onResume()
        setLocation()
        mainViewModel.reloadServerList()
        if (isSelectLocation) {
            isSelectLocationTimer = true
            if (settingsStorage?.decodeString(AppConfig.PREF_MODE) ?: "VPN" == "VPN") {
                val intent = VpnService.prepare(this)
                if (intent == null) {
                    restartV2Ray()
                } else {
                    requestVpnPermission.launch(intent)
                }
            } else {
                restartV2Ray()
            }
            isSelectLocation = false
        }
        if (!isStopActivity && isConnected) {
            isStopActivity = true
            admob.showOpenApp()
        }
    }

    override fun onStop() {
        super.onStop()
        isStopActivity = false
    }

    public override fun onPause() {
        super.onPause()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            return true
        }else {
            nativeExit()
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onBackPressed() {
//        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
//            binding.drawerLayout.closeDrawer(GravityCompat.START)
//        } else {
//            onBackPressedDispatcher.onBackPressed()
//        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> {
                startActivity(Intent(this, SettingsActivity::class.java)
                    .putExtra("isRunning", mainViewModel.isRunning.value == true))
            }
            R.id.share_app -> {
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.type="text/plain"
                shareIntent.putExtra(
                    Intent.EXTRA_TEXT,
                    getString(R.string.text_download_link)+"\n\n" + settingsStorage.decodeString(AppConfig.SHARE_APP_LINK, "link")
                )
                startActivity(Intent.createChooser(shareIntent,getString(R.string.share_app_title)))
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun showCircle() {
        binding.connectButton.setBackgroundResource(R.drawable.background_connect_primary)
        binding.animationConnect.visibility = View.VISIBLE
        if (!isConnectPrimary) {
            isConnectPrimary = true
            binding.animationConnect.setAnimation(R.raw.loading_primary)
            binding.animationConnect.playAnimation()
        }
    }
    @SuppressLint("ResourceAsColor")
    private fun hideCircle() {
        binding.connectButton.setBackgroundResource(R.drawable.background_connect_primary)
        isConnectPrimary = false
        binding.animationConnect.visibility = View.INVISIBLE
    }

    @SuppressLint("SetTextI18n")
    private fun nativeDisconnect() {
        if (dialog.isShowing) {
            return
        }
        if (isConnected) {
            admob.showInterstitial()
        }
        val handler = Handler()
        dialog.setContentView(R.layout.dialog_disconnect)
        val progressWait: ProgressBar = dialog.findViewById(R.id.progress_load_ad_page)
        val tikAnimation: LottieAnimationView = dialog.findViewById(R.id.tik_success)
        tikAnimation.visibility = View.GONE
        progressWait.visibility = View.VISIBLE
        runnableNativeExit = Runnable {
            if (dialog.isShowing && Admob.mNativeAd !=null || isConnected) {
                val linearNativeExit: LinearLayout = dialog.findViewById(R.id.back_native)
                val adView = layoutInflater.inflate(R.layout.ad_unified_exit, null)
                admob.showNative(adView as NativeAdView, object: NativeCallback {
                    override fun callback() {
                        linearNativeExit.removeAllViews()
                        linearNativeExit.addView(adView)
                        val displayMetrics = DisplayMetrics()
                        windowManager.defaultDisplay.getMetrics(displayMetrics)
                        val height = displayMetrics.heightPixels / 10 * 4
                        val layoutParams = linearNativeExit.layoutParams
                        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                        layoutParams.height = height
                        linearNativeExit.layoutParams = layoutParams
                        adView.post(Runnable {
                            val imageView: ImageView = adView.findViewById(R.id.ad_media)
                            if (imageView.drawable == null) {
                                val layoutParams = linearNativeExit.layoutParams as LinearLayout.LayoutParams
                                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                                linearNativeExit.layoutParams = layoutParams
                            }
                        })
                        val buttonConnect: Button = dialog.findViewById(R.id.connect)
                        val descriptionTextView: TextView = dialog.findViewById(R.id.description_load_ad_page)
                        buttonConnect.text = "Disconnect!"
                        descriptionTextView.text = "Are you going disconnect?"
                        val progressWait: ProgressBar = dialog.findViewById(R.id.progress_load_ad_page)
                        progressWait.visibility = View.GONE
                    }

                })
            }else if (dialog.isShowing) {
                handler.postDelayed(runnableNativeExit, 300)
            }
        }
        handler.postDelayed(runnableNativeExit, 0)

        dialog.show()
        val buttonConnect: Button = dialog.findViewById(R.id.connect)
        val buttonCancel: Button = dialog.findViewById(R.id.cancel)
        buttonConnect.setOnClickListener {
            if (buttonConnect.text.equals("Disconnect!")) {
                Utils.stopVService(this)
                hideCircle()
                dialog.dismiss()
                isConnected = false
            }
        }
        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }
        Handler().postDelayed({
            try {
                if (Admob.mNativeAd == null) {
                    buttonConnect.text = "Disconnect!"
                    val progressWait: ProgressBar = dialog.findViewById(R.id.progress_load_ad_page)
                    val tikAnimation: LottieAnimationView = dialog.findViewById(R.id.tik_success)
                    tikAnimation.visibility = View.VISIBLE
                    progressWait.visibility = View.GONE
                }
            }catch (e: Exception) {}
        }, settingsStorage.decodeString(AppConfig.WAIT_CONNECTED, "1000")?.toLong() ?: 1000)
    }

    private fun nativeConnect() {
        if (dialog.isShowing) {
            return
        }
        if (isConnected) {
            admob.showInterstitial()
        }
        val handler = Handler()
        dialog.setContentView(R.layout.dialog_connect)
        val progressWait: ProgressBar = dialog.findViewById(R.id.progress_load_ad_page)
        val tikAnimation: LottieAnimationView = dialog.findViewById(R.id.tik_success)
        tikAnimation.visibility = View.GONE
        progressWait.visibility = View.VISIBLE
        runnableNativeExit = Runnable {
            if (dialog.isShowing && Admob.mNativeAd !=null || isConnected) {
                val linearNativeExit: LinearLayout = dialog.findViewById(R.id.back_native)
                val adView = layoutInflater.inflate(R.layout.ad_unified_exit, null)
                admob.showNative(adView as NativeAdView, object: NativeCallback {
                    @SuppressLint("SetTextI18n")
                    override fun callback() {
                        linearNativeExit.removeAllViews()
                        linearNativeExit.addView(adView)
                        val displayMetrics = DisplayMetrics()
                        windowManager.defaultDisplay.getMetrics(displayMetrics)
                        val height = displayMetrics.heightPixels / 10 * 4
                        val layoutParams = linearNativeExit.layoutParams
                        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                        layoutParams.height = height
                        linearNativeExit.layoutParams = layoutParams
                        adView.post(Runnable {
                            val imageView: ImageView = adView.findViewById(R.id.ad_media)
                            if (imageView.drawable == null) {
                                val layoutParams = linearNativeExit.layoutParams as LinearLayout.LayoutParams
                                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                                linearNativeExit.layoutParams = layoutParams
                            }
                        })
                        val buttonConnect: Button = dialog.findViewById(R.id.connect)
                        val descriptionTextView: TextView = dialog.findViewById(R.id.description_load_ad_page)
                        buttonConnect.text = "Close!"
                        descriptionTextView.text = "Connected!"
                        val progressWait: ProgressBar = dialog.findViewById(R.id.progress_load_ad_page)
                        progressWait.visibility = View.GONE
                        val tikAnimation: LottieAnimationView = dialog.findViewById(R.id.tik_success)
                        tikAnimation.visibility = View.GONE
                    }

                })
            }else if (dialog.isShowing) {
                handler.postDelayed(runnableNativeExit, 300)
            }
        }
        handler.postDelayed(runnableNativeExit, 0)

        dialog.show()
        val buttonConnect: Button = dialog.findViewById(R.id.connect)
        buttonConnect.setOnClickListener {
            if (buttonConnect.text.equals("Close!")) dialog.dismiss()
        }
        Handler().postDelayed({
            if (Admob.mNativeAd == null) {
                val descriptionTextView: TextView = dialog.findViewById(R.id.description_load_ad_page)
                buttonConnect.text = "Close!"
                descriptionTextView.text = "Connected!"
                val progressWait: ProgressBar = dialog.findViewById(R.id.progress_load_ad_page)
                val tikAnimation: LottieAnimationView = dialog.findViewById(R.id.tik_success)
                tikAnimation.visibility = View.VISIBLE
                progressWait.visibility = View.GONE
            }
        }, settingsStorage.decodeString(AppConfig.WAIT_CONNECTED, "1000")?.toLong() ?: 1000)
    }

    private fun nativeExit() {
        if (dialog.isShowing) {
            return
        }
        val handler = Handler()
        dialog.setContentView(R.layout.dialog_exit)
        runnableNativeExit = Runnable {
            if (dialog.isShowing && Admob.mNativeAd !=null || isConnected) {
                val linearNativeExit: LinearLayout = dialog.findViewById(R.id.back_native)
                val adView = layoutInflater.inflate(R.layout.ad_unified_exit, null)
                admob.showNative(adView as NativeAdView, object: NativeCallback {
                    override fun callback() {
                        linearNativeExit.removeAllViews()
                        linearNativeExit.addView(adView)
                        val displayMetrics = DisplayMetrics()
                        windowManager.defaultDisplay.getMetrics(displayMetrics)
                        val height = displayMetrics.heightPixels / 10 * 4
                        val layoutParams = linearNativeExit.layoutParams
                        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                        layoutParams.height = height
                        linearNativeExit.layoutParams = layoutParams
                        adView.post(Runnable {
                            val imageView: ImageView = adView.findViewById(R.id.ad_media)
                            if (imageView.drawable == null) {
                                val layoutParams = linearNativeExit.layoutParams as LinearLayout.LayoutParams
                                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                                linearNativeExit.layoutParams = layoutParams
                            }
                        })
                    }

                })
            }else if (dialog.isShowing) {
                handler.postDelayed(runnableNativeExit, 300)
            }
        }
        handler.postDelayed(runnableNativeExit, 0)

        dialog.show()
        val buttonBack: Button = dialog.findViewById(R.id.back_yes)
        val buttonBackCancel: Button = dialog.findViewById(R.id.back_no)
        buttonBack.setOnClickListener(View.OnClickListener { onBackPressedDispatcher.onBackPressed() })
        buttonBackCancel.setOnClickListener(View.OnClickListener { dialog.dismiss() })
    }

    private val mMsgReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.getIntExtra("key", 0)) {
                AppConfig.MSG_MEASURE_DELAY_SUCCESS -> {
                    isConnected = true

                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.share_app,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.share_app_menu -> {
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.type="text/plain"
                shareIntent.putExtra(
                    Intent.EXTRA_TEXT,
                    getString(R.string.text_download_link)+"\n\n" + settingsStorage.decodeString(AppConfig.SHARE_APP_LINK, "link")
                )
                startActivity(Intent.createChooser(shareIntent,getString(R.string.share_app_title)))
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
