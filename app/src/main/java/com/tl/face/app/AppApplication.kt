package com.tl.face.app

import android.app.Activity
import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.hjq.bar.TitleBar
import com.hjq.http.EasyConfig
import com.hjq.http.config.IRequestApi
import com.hjq.http.model.HttpHeaders
import com.hjq.http.model.HttpParams
import com.hjq.toast.ToastUtils
import com.tencent.mmkv.MMKV
import com.tl.face.R
import com.tl.face.aop.Log
import com.tl.face.http.glide.GlideApp
import com.tl.face.http.model.RequestHandler
import com.tl.face.http.model.RequestServer
import com.tl.face.manager.ActivityManager
import com.tl.face.other.*
import okhttp3.OkHttpClient
import timber.log.Timber
import java.util.concurrent.TimeUnit


class AppApplication : Application() {

  @Log("Start time")
  override fun onCreate() {
    super.onCreate()
    initSdk(this)
  }

  override fun onLowMemory() {
    super.onLowMemory()
    // clean memory
    GlideApp.get(this).onLowMemory()
  }

  override fun onTrimMemory(level: Int) {
    super.onTrimMemory(level)
    GlideApp.get(this).onTrimMemory(level)
  }

  companion object {

    /**
     * Init third party
     */
    fun initSdk(application: Application) {

      TitleBar.setDefaultStyle(TitleBarStyle())

      ToastUtils.init(application, ToastStyle())

      ToastUtils.setDebugMode(AppConfig.isDebug())

      ToastUtils.setInterceptor(ToastLogInterceptor())

      CrashHandler.register(application)

      ActivityManager.getInstance().init(application)

      MMKV.initialize(application)

      val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

      EasyConfig.with(okHttpClient)

        .setLogEnabled(AppConfig.isLogEnable())

        .setServer(RequestServer())

        .setHandler(RequestHandler(application))

        .setRetryCount(1)
        .setInterceptor { api: IRequestApi, params: HttpParams, headers: HttpHeaders ->

          headers.put("versionName", AppConfig.getVersionName())
          headers.put("versionCode", AppConfig.getVersionCode().toString())
        }
        .into()


      if (AppConfig.isLogEnable()) {
        Timber.plant(DebugLoggerTree())
      }


      val connectivityManager: ConnectivityManager? =
        ContextCompat.getSystemService(application, ConnectivityManager::class.java)
      if (connectivityManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        connectivityManager.registerDefaultNetworkCallback(object :
          ConnectivityManager.NetworkCallback() {
          override fun onLost(network: Network) {
            val topActivity: Activity? = ActivityManager.getInstance().getTopActivity()
            if (topActivity !is LifecycleOwner) {
              return
            }
            val lifecycleOwner: LifecycleOwner = topActivity
            if (lifecycleOwner.lifecycle.currentState != Lifecycle.State.RESUMED) {
              return
            }
            ToastUtils.show(R.string.common_network_error)
          }
        })
      }
    }
  }
}