package com.tl.face.ui.activity

import android.app.Activity
import android.content.*
import com.tl.face.R
import com.tl.face.app.AppActivity

class RestartActivity : AppActivity() {

  companion object {
    fun start(context: Context) {
      val intent = Intent(context, RestartActivity::class.java)
      if (context !is Activity) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
    }

    fun restart(context: Context) {
      val intent = Intent(context, MainActivity::class.java)
      if (context !is Activity) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
    }
  }

  override fun getLayoutId(): Int {
    return 0
  }

  override fun initView() {}

  override fun initData() {
    restart(this)
    finish()
    toast(R.string.common_crash_hint)
  }
}