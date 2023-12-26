package com.sorenavpn.vpn.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.tencent.mmkv.MMKV
import com.sorenavpn.vpn.AppConfig
import com.sorenavpn.vpn.R
import com.sorenavpn.vpn.ui.interfaces.InterstitialCallback
import com.sorenavpn.vpn.ui.interfaces.NativeCallback
import com.sorenavpn.vpn.ui.interfaces.OpenAppCallback
import com.sorenavpn.vpn.util.MmkvManager


class Admob(activity: Activity) {

    private var activity: Activity
    private val settingsStorage by lazy { MMKV.mmkvWithID(MmkvManager.ID_SETTING, MMKV.MULTI_PROCESS_MODE) }

    private var interstitialId = ""
    private var nativeId = ""
    private var appOpenId = ""


    companion object {
        private var mInterstitialAd: InterstitialAd? = null
        var mNativeAd: NativeAd? = null
    }

    private var isWorkInterstitial = false
    private var isWorkNative = false
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingOpenAppAd = false

    private lateinit var runnableNativeWorker: Runnable
    private lateinit var runnableInterstitialAd: Runnable

    init {
        this.activity = activity
        setIds()
    }


    private fun isTimeShowInterstitial(): Boolean {
        val timeShow = settingsStorage.decodeString(AppConfig.TIME_SHOW_INTERSTITIAL)?.toLong()
        val timeRemainSee = settingsStorage.decodeString(AppConfig.WATT_SEE_INTERSTITIAL)?.toLong()
        if (timeRemainSee != null && timeShow != null) {
            if (timeShow + (timeRemainSee) > System.currentTimeMillis()) {
                return true
            }
        }
        return false
    }

    private fun isTimeShowOpenApp(): Boolean {
        val timeShow = settingsStorage.decodeString(AppConfig.TIME_SHOW_OPEN_APP)?.toLong()
        val timeRemainSee = settingsStorage.decodeString(AppConfig.WAIT_SEE_OPEN_APP)?.toLong()
        if (timeRemainSee != null && timeShow != null) {
            if (timeShow + (timeRemainSee) > System.currentTimeMillis()) {
                return true
            }
        }
        return false
    }

    private fun isExpireInterstitial(): Boolean {
        val timeLoad = settingsStorage.decodeString(AppConfig.TIME_LOAD_INTERSTITIAL)?.toLong()
        val timeExpire = settingsStorage.decodeString(AppConfig.WAIT_EXPIRE_ADS)?.toLong()
        if (timeLoad != null && timeExpire != null) {
            if (timeLoad + (timeExpire) < System.currentTimeMillis()) {
                return true
            }
        }
        return false
    }

    private fun isExpireOpenApp(): Boolean {
        val timeLoad = settingsStorage.decodeString(AppConfig.TIME_LOAD_OPEN_APP)?.toLong()
        val timeExpire = settingsStorage.decodeString(AppConfig.WAIT_EXPIRE_ADS)?.toLong()
        if (timeLoad != null && timeExpire != null) {
            if (timeLoad + (timeExpire) < System.currentTimeMillis()) {
                return true
            }
        }
        return false
    }

    private fun isExpireNative(): Boolean {
        val timeLoad = settingsStorage.decodeString(AppConfig.TIME_LOAD_NATIVE)?.toLong()
        val timeExpire = settingsStorage.decodeString(AppConfig.WAIT_EXPIRE_ADS)?.toLong()
        if (timeLoad != null && timeExpire != null) {
            if (timeLoad + (timeExpire) < System.currentTimeMillis()) {
                return true
            }
        }
        return false
    }

    private fun setIds() {
        if (settingsStorage.decodeString(AppConfig.ADMOB_STATUS) != "active") return
        val appId = settingsStorage.decodeString(AppConfig.ADMOB_APP_ID)
        interstitialId = settingsStorage.decodeString(AppConfig.ADMOB_INTERSTITIAL_ID)!!
        nativeId = settingsStorage.decodeString(AppConfig.ADMOB_NATIVE_ID)!!
        appOpenId = settingsStorage.decodeString(AppConfig.ADMOB_APP_OPEN_ID)!!
        val applicationInfo: ApplicationInfo = activity.packageManager
            .getApplicationInfo(activity.packageName, PackageManager.GET_META_DATA)
        applicationInfo.metaData.putString(
            "com.google.android.gms.ads.APPLICATION_ID",
            appId
        )
        MobileAds.initialize(activity) {}
    }

    fun showOpenApp() {
        if (settingsStorage.decodeString(AppConfig.ADMOB_STATUS) != "active" || isLoadingOpenAppAd || isTimeShowOpenApp()) {

            return
        }
        if (appOpenAd != null || isExpireOpenApp()) {
            appOpenAd!!.show(activity)
            appOpenAd = null
            settingsStorage.encode(AppConfig.TIME_SHOW_OPEN_APP, System.currentTimeMillis().toString())
            return
        }
        loadOpenApp(object: OpenAppCallback {
            override fun callback() {
                appOpenAd!!.show(activity)
                appOpenAd = null
                settingsStorage.encode(AppConfig.TIME_SHOW_OPEN_APP, System.currentTimeMillis().toString())
            }
        })
    }

    private fun loadOpenApp(callback: OpenAppCallback) {
        isLoadingOpenAppAd = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            activity, appOpenId, request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {

                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d("tag", "Ad was loaded.")
                    appOpenAd = ad
                    isLoadingOpenAppAd = false
                    callback.callback()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.d("tag", loadAdError.message)
                    isLoadingOpenAppAd = false
                    val waitFailedAd = settingsStorage.decodeString(AppConfig.WAIT_FAILED_ADS)?.toLong()
                    if (waitFailedAd != null) {
                        Handler().postDelayed({
                            showOpenApp()
                        }, waitFailedAd)
                    }
                }
            })
    }

    fun showInterstitial() {
        if (settingsStorage.decodeString(AppConfig.ADMOB_STATUS) != "active" || isTimeShowInterstitial()) {
            return
        }
        if (mInterstitialAd == null || isExpireInterstitial()) {
            loadInterstitial(object: InterstitialCallback {
                override fun callback() {
                    mInterstitialAd?.show(activity)
                    mInterstitialAd = null
                    settingsStorage.encode(AppConfig.TIME_SHOW_INTERSTITIAL, System.currentTimeMillis().toString())
                    val timeRemainSee = settingsStorage.decodeString(AppConfig.WATT_SEE_INTERSTITIAL)?.toLong()
                    if (timeRemainSee != null) {
                        val handler = Handler()
                        runnableInterstitialAd = Runnable {
                            if (MainActivity.isConnected) showInterstitial()
                            else handler.postDelayed(runnableInterstitialAd, 500)
                        }
                        handler.postDelayed(runnableInterstitialAd, 0)
                    }
                }

            })
        }else {
            mInterstitialAd?.show(activity)
            mInterstitialAd = null
            settingsStorage.encode(AppConfig.TIME_SHOW_INTERSTITIAL, System.currentTimeMillis().toString())
            val timeRemainSee = settingsStorage.decodeString(AppConfig.WATT_SEE_INTERSTITIAL)?.toLong()
            if (timeRemainSee != null) {
                val handler = Handler()
                runnableInterstitialAd = Runnable {
                    if (MainActivity.isConnected) showInterstitial()
                    else handler.postDelayed(runnableInterstitialAd, 500)
                }
                handler.postDelayed(runnableInterstitialAd, 0)
            }
        }
    }

    private fun loadInterstitial(callback: InterstitialCallback) {
        if (isWorkInterstitial) return
        isWorkInterstitial = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(activity,interstitialId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                mInterstitialAd = null
                val waitFailedAd = settingsStorage.decodeString(AppConfig.WAIT_FAILED_ADS)?.toLong()
                isWorkInterstitial = false
                if (waitFailedAd != null) {
                    Handler().postDelayed({
                        showInterstitial()
                    }, waitFailedAd)
                }
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                isWorkInterstitial = false
                mInterstitialAd = interstitialAd
                settingsStorage.encode(AppConfig.TIME_LOAD_INTERSTITIAL, System.currentTimeMillis().toString())
                callback.callback()
            }
        })

    }
    fun showNative(nativeAdView: NativeAdView, callback: NativeCallback) {
        if (settingsStorage.decodeString(AppConfig.ADMOB_STATUS) != "active") {
            return
        }

        if (mNativeAd == null || isExpireNative()) {
            loadNative(nativeAdView, object: NativeCallback {
                override fun callback() {
                    nativeAdView.visibility = View.VISIBLE
                    initializeNative(mNativeAd!!, nativeAdView)
                    callback.callback()
                }

            })
        } else {
            nativeAdView.visibility = View.VISIBLE
            initializeNative(mNativeAd!!, nativeAdView)
            callback.callback()
        }
    }

    private fun loadNative(nativeAdView: NativeAdView, callback: NativeCallback) {
        if (isWorkNative) {
            val handler = Handler()
            runnableNativeWorker = Runnable {
                if (mNativeAd == null) {
                    handler.postDelayed(runnableNativeWorker, 500)
                }else {
                    callback.callback()
                }
            }
            handler.postDelayed(runnableNativeWorker, 500)
        }else {
            isWorkNative = true
            val adLoader = AdLoader.Builder(activity, nativeId)
                .forNativeAd { ad : NativeAd ->
                    isWorkNative = false
                    mNativeAd = ad
                    settingsStorage.encode(AppConfig.TIME_LOAD_NATIVE, System.currentTimeMillis().toString())
                    callback.callback()
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        isWorkNative = false
                        val waitFailedAd = settingsStorage.decodeString(AppConfig.WAIT_FAILED_ADS)?.toLong()
                        if (waitFailedAd != null) {
                            Handler().postDelayed({
                                showNative(nativeAdView, object: NativeCallback { override fun callback() {} })
                            }, waitFailedAd)
                        }
                    }
                })
                .withNativeAdOptions(
                    NativeAdOptions.Builder()
                        .build())
                .build()
            adLoader.loadAd(AdRequest.Builder().build())
        }
    }

    @SuppressLint("CutPasteId")
    private fun initializeNative(nativeAd: NativeAd, adView: NativeAdView) {
        adView.visibility = View.VISIBLE
        val image: ImageView = adView.findViewById(R.id.ad_media)
        val icon: ImageView = adView.findViewById(R.id.ad_app_icon)
        val title: TextView = adView.findViewById(R.id.ad_headline)
        val body: TextView = adView.findViewById(R.id.ad_body)
        val callToAction: Button = adView.findViewById(R.id.ad_call_to_action)
        val headLine: TextView = adView.findViewById(R.id.ad_headline)
        val advertiser: TextView = adView.findViewById(R.id.ad_advertiser)
        adView.imageView = image
        adView.headlineView = title
        adView.bodyView = body
        adView.callToActionView = callToAction
        adView.iconView = icon
        adView.headlineView = headLine
        adView.advertiserView = advertiser

        image.setImageDrawable(nativeAd.images[0].drawable)

        if (nativeAd.headline == null)
            headLine.visibility = View.INVISIBLE
        else {
            headLine.visibility = View.VISIBLE
            headLine.text = nativeAd.headline
        }

        if (nativeAd.body == null) {
            body.visibility = View.INVISIBLE
        } else {
            body.visibility = View.VISIBLE
            body.text = nativeAd.body
        }

        if (nativeAd.callToAction == null) {
            callToAction.visibility = View.INVISIBLE
        } else {
            callToAction.visibility = View.VISIBLE
            callToAction.text = nativeAd.callToAction
        }

        if (nativeAd.icon == null) {
            icon.visibility = View.GONE
        } else {
            icon.setImageDrawable(
                nativeAd.icon!!.drawable
            )
            icon.visibility = View.VISIBLE
        }

        if (nativeAd.advertiser == null) {
            advertiser.visibility = View.INVISIBLE
        } else {
            advertiser.text = nativeAd.advertiser
            advertiser.visibility = View.VISIBLE
        }

        adView.setNativeAd(nativeAd)
    }
}