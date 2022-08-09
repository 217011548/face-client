package com.tl.face.app

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.view.*
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.SnackbarContentLayout
import com.gyf.immersionbar.ImmersionBar
import com.hjq.bar.TitleBar
import com.tl.base.BaseActivity
import com.tl.base.BaseDialog
import com.tl.face.R
import com.tl.face.action.TitleBarAction
import com.tl.face.action.ToastAction
import com.tl.face.http.model.HttpData
import com.tl.face.ui.dialog.WaitDialog
import com.hjq.http.listener.OnHttpListener
import okhttp3.Call


abstract class AppActivity : BaseActivity(),
  ToastAction, TitleBarAction, OnHttpListener<Any> {


  private var titleBar: TitleBar? = null


  private var immersionBar: ImmersionBar? = null


  private var dialog: BaseDialog? = null


  private var dialogCount: Int = 0


  open fun isShowDialog(): Boolean {
    return dialog != null && dialog!!.isShowing
  }

  open fun showDialog() {
    if (isFinishing || isDestroyed) {
      return
    }
    dialogCount++
    postDelayed(Runnable {
      if ((dialogCount <= 0) || isFinishing || isDestroyed) {
        return@Runnable
      }
      if (dialog == null) {
        dialog = WaitDialog.Builder(this)
          .setCancelable(false)
          .create()
      }
      if (!dialog!!.isShowing) {
        dialog!!.show()
      }
    }, 300)
  }


  open fun hideDialog() {
    if (isFinishing || isDestroyed) {
      return
    }
    if (dialogCount > 0) {
      dialogCount--
    }
    if ((dialogCount != 0) || (dialog == null) || !dialog!!.isShowing) {
      return
    }
    dialog?.dismiss()
  }

  override fun initLayout() {
    super.initLayout()

    val titleBar = getTitleBar()
    titleBar?.setOnTitleBarListener(this)


    if (isStatusBarEnabled()) {
      getStatusBarConfig().init()


      if (titleBar != null) {
        ImmersionBar.setTitleBar(this, titleBar)
      }
    }
  }


  protected open fun isStatusBarEnabled(): Boolean {
    return true
  }


  open fun isStatusBarDarkFont(): Boolean {
    return true
  }


  open fun getStatusBarConfig(): ImmersionBar {
    if (immersionBar == null) {
      immersionBar = createStatusBarConfig()
    }
    return immersionBar!!
  }

  protected open fun createStatusBarConfig(): ImmersionBar {
    return ImmersionBar.with(this)
      .statusBarDarkFont(isStatusBarDarkFont())
      .navigationBarColor(R.color.white)
      .autoDarkModeEnable(true, 0.2f)
  }


  override fun setTitle(@StringRes id: Int) {
    title = getString(id)
  }


  override fun setTitle(title: CharSequence?) {
    super<BaseActivity>.setTitle(title)
    getTitleBar()?.title = title
  }

  override fun getTitleBar(): TitleBar? {
    if (titleBar == null) {
      titleBar = obtainTitleBar(getContentView())
    }
    return titleBar
  }

  override fun onLeftClick(view: View) {
    onBackPressed()
  }

  override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
    super.startActivityForResult(intent, requestCode, options)
    overridePendingTransition(R.anim.right_in_activity, R.anim.right_out_activity)
  }

  override fun finish() {
    super.finish()
    overridePendingTransition(R.anim.left_in_activity, R.anim.left_out_activity)
  }

  /**
   * [OnHttpListener]
   */
  override fun onStart(call: Call) {
    showDialog()
  }

  override fun onSucceed(result: Any) {
    if (result is HttpData<*>) {
      toast(result.getMessage())
    }
  }

  override fun onFail(e: Exception) {
    toast(e.message)
  }

  override fun onEnd(call: Call) {
    hideDialog()
  }

  override fun onDestroy() {
    super.onDestroy()
    if (isShowDialog()) {
      hideDialog()
    }
    dialog = null
  }


  open fun showSnackTip(
    s: String?,
    action: String? = null,
    onClickListener: View.OnClickListener? = null
  ): Snackbar? {
    this.getContentView()?.rootView?.let {
      val snackBar = Snackbar.make(it, s!!, Snackbar.LENGTH_SHORT)
      enableSnackBarShowMultiLines(snackBar, 50)
      snackBar.setAction(action, onClickListener)
      if (Looper.myLooper() == Looper.getMainLooper()) {
        snackBar.show()
      } else {
        post { snackBar.show() }
      }
      return snackBar
    } ?: return null

  }

  @SuppressLint("RestrictedApi")
  private fun enableSnackBarShowMultiLines(snackBar: Snackbar, maxLines: Int) {
    val contentLayout = (snackBar.view as ViewGroup).getChildAt(0) as SnackbarContentLayout
    val tv = contentLayout.messageView
    tv.setTextColor(Color.WHITE)
    tv.maxLines = maxLines
  }
}