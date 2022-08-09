package com.tl.face.ui.activity

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.util.DisplayMetrics
import android.view.View
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.gyf.immersionbar.ImmersionBar
import com.tl.face.R
import com.tl.face.aop.SingleClick
import com.tl.face.app.AppActivity
import com.tl.face.other.AppConfig
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.io.StringWriter
import java.net.InetAddress
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.min


class CrashActivity : AppActivity() {

  companion object {

    private const val INTENT_KEY_IN_THROWABLE: String = "throwable"


    private val SYSTEM_PACKAGE_PREFIX_LIST: Array<String> = arrayOf(
      "android", "com.android",
      "androidx", "com.google.android", "java", "javax", "dalvik", "kotlin"
    )


    private val CODE_REGEX: Pattern = Pattern.compile("\\(\\w+\\.\\w+:\\d+\\)")

    fun start(application: Application, throwable: Throwable?) {
      if (throwable == null) {
        return
      }
      val intent = Intent(application, CrashActivity::class.java)
      intent.putExtra(INTENT_KEY_IN_THROWABLE, throwable)
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      application.startActivity(intent)
    }
  }

  private val titleView: TextView? by lazy { findViewById(R.id.tv_crash_title) }
  private val drawerLayout: DrawerLayout? by lazy { findViewById(R.id.dl_crash_drawer) }
  private val infoView: TextView? by lazy { findViewById(R.id.tv_crash_info) }
  private val messageView: TextView? by lazy { findViewById(R.id.tv_crash_message) }
  private var stackTrace: String? = null

  override fun getLayoutId(): Int {
    return R.layout.crash_activity
  }

  override fun initView() {
    setOnClickListener(R.id.iv_crash_info, R.id.iv_crash_restart)


    ImmersionBar.setTitleBar(this, findViewById(R.id.ll_crash_bar))
    ImmersionBar.setTitleBar(this, findViewById(R.id.ll_crash_info))
  }

  override fun initData() {
    val throwable: Throwable = getSerializable(INTENT_KEY_IN_THROWABLE) ?: return
    titleView?.text = throwable.javaClass.simpleName
    val stringWriter = StringWriter()
    val printWriter = PrintWriter(stringWriter)
    throwable.printStackTrace(printWriter)
    throwable.cause?.printStackTrace(printWriter)
    stackTrace = stringWriter.toString()
    val matcher: Matcher = CODE_REGEX.matcher(stackTrace!!)
    val spannable = SpannableStringBuilder(stackTrace)
    if (spannable.isNotEmpty()) {
      while (matcher.find()) {

        val start: Int = matcher.start() + "(".length

        val end: Int = matcher.end() - ")".length

        var codeColor: Int = Color.parseColor("#999999")
        val lineIndex: Int = stackTrace!!.lastIndexOf("at ", start)
        if (lineIndex != -1) {
          val lineData: String = spannable.subSequence(lineIndex, start).toString()
          if (TextUtils.isEmpty(lineData)) {
            continue
          }

          var highlight = true
          for (packagePrefix: String? in SYSTEM_PACKAGE_PREFIX_LIST) {
            if (lineData.startsWith("at $packagePrefix")) {
              highlight = false
              break
            }
          }
          if (highlight) {
            codeColor = Color.parseColor("#287BDE")
          }
        }


        spannable.setSpan(
          ForegroundColorSpan(codeColor),
          start,
          end,
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
      }
      messageView?.text = spannable
    }
    val displayMetrics: DisplayMetrics = resources.displayMetrics
    val screenWidth: Int = displayMetrics.widthPixels
    val screenHeight: Int = displayMetrics.heightPixels
    val smallestWidth: Float = min(screenWidth, screenHeight) / displayMetrics.density
    val targetResource: String?
    when {
      displayMetrics.densityDpi > 480 -> {
        targetResource = "xxxhdpi"
      }
      displayMetrics.densityDpi > 320 -> {
        targetResource = "xxhdpi"
      }
      displayMetrics.densityDpi > 240 -> {
        targetResource = "xhdpi"
      }
      displayMetrics.densityDpi > 160 -> {
        targetResource = "hdpi"
      }
      displayMetrics.densityDpi > 120 -> {
        targetResource = "mdpi"
      }
      else -> {
        targetResource = "ldpi"
      }
    }
    val builder: StringBuilder = StringBuilder()
    builder.append("Device brand：\t").append(Build.BRAND)
      .append("\nmodel：\t").append(Build.MODEL)
      .append("\ntype：\t").append(if (isTablet()) "tab" else "phone")

    builder.append("\nmonitor width：\t").append(screenWidth).append(" x ").append(screenHeight)
      .append("\nmonitor dpi：\t").append(displayMetrics.densityDpi)
      .append("\ndensity：\t").append(displayMetrics.density)
      .append("\ntarget resource：\t").append(targetResource)
      .append("\nmin width：\t").append(smallestWidth.toInt())

    builder.append("\nAndroid verison：\t").append(Build.VERSION.RELEASE)
      .append("\nAPI version：\t").append(Build.VERSION.SDK_INT)
      .append("\nCPU firmware：\t").append(Build.SUPPORTED_ABIS[0])

    builder.append("\napp ver：\t").append(AppConfig.getVersionName())
      .append("\ncode ver：\t").append(AppConfig.getVersionCode())

    try {
      val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
      val packageInfo: PackageInfo =
        packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
      builder.append("\nfirst time install：\t")
        .append(dateFormat.format(Date(packageInfo.firstInstallTime)))
        .append("\nRecently install：\t").append(dateFormat.format(Date(packageInfo.lastUpdateTime)))
        .append("\nCrash time：\t").append(dateFormat.format(Date()))
      val permissions: MutableList<String> = mutableListOf(*packageInfo.requestedPermissions)
      if (permissions.contains(Permission.READ_EXTERNAL_STORAGE) ||
        permissions.contains(Permission.WRITE_EXTERNAL_STORAGE)
      ) {
        builder.append("\nSave permission：\t").append(
          if (XXPermissions.isGranted(this, *Permission.Group.STORAGE)) "Get" else "Not Get"
        )
      }
      if (permissions.contains(Permission.ACCESS_FINE_LOCATION) ||
        permissions.contains(Permission.ACCESS_COARSE_LOCATION)
      ) {
        builder.append("\nlocation permission：\t")
        if (XXPermissions.isGranted(
            this,
            Permission.ACCESS_FINE_LOCATION,
            Permission.ACCESS_COARSE_LOCATION
          )
        ) {
          builder.append("FINE、COARSE")
        } else {
          when {
            XXPermissions.isGranted(this, Permission.ACCESS_FINE_LOCATION) -> {
              builder.append("FINE")
            }
            XXPermissions.isGranted(this, Permission.ACCESS_COARSE_LOCATION) -> {
              builder.append("COARSE")
            }
            else -> {
              builder.append("Not Get")
            }
          }
        }
      }
      if (permissions.contains(Permission.CAMERA)) {
        builder.append("\nCamera Permission：\t")
          .append(if (XXPermissions.isGranted(this, Permission.CAMERA)) "Get" else "Not Get")
      }
      if (permissions.contains(Permission.RECORD_AUDIO)) {
        builder.append("\nVoice Permission：\t").append(
          if (XXPermissions.isGranted(this, Permission.RECORD_AUDIO)) "Get" else "Not Get"
        )
      }
      if (permissions.contains(Permission.SYSTEM_ALERT_WINDOW)) {
        builder.append("\nAlert Permission：\t").append(
          if (XXPermissions.isGranted(this, Permission.SYSTEM_ALERT_WINDOW)) "Get" else "Not Get"
        )
      }
      if (permissions.contains(Permission.REQUEST_INSTALL_PACKAGES)) {
        builder.append("\nInstallation Permission：\t").append(
          if (XXPermissions.isGranted(this, Permission.REQUEST_INSTALL_PACKAGES)) "Get" else "Not Get"
        )
      }
      if (permissions.contains(Manifest.permission.INTERNET)) {
        builder.append("\nInternet connection：\t")

        lifecycleScope.launch(Dispatchers.IO) {
          try {
            InetAddress.getByName("www.google.com")
            builder.append("normal")
          } catch (ignored: UnknownHostException) {
            builder.append("abnormal")
          }
          lifecycleScope.launch(Dispatchers.Main) {
            infoView?.text = builder
          }
        }
      } else {
        infoView?.text = builder
      }
    } catch (e: PackageManager.NameNotFoundException) {
    }
  }

  @SingleClick
  override fun onClick(view: View) {
    when (view.id) {
      R.id.iv_crash_info -> {
        drawerLayout?.openDrawer(GravityCompat.START)
      }
      R.id.iv_crash_restart -> {
        onBackPressed()
      }
    }
  }

  override fun onBackPressed() {
    // restart app
    RestartActivity.restart(this)
    finish()
  }

  override fun createStatusBarConfig(): ImmersionBar {
    return super.createStatusBarConfig()
      .navigationBarColor(R.color.white)
  }


  fun isTablet(): Boolean {
    return ((resources.configuration.screenLayout
            and Configuration.SCREENLAYOUT_SIZE_MASK)
            >= Configuration.SCREENLAYOUT_SIZE_LARGE)
  }
}