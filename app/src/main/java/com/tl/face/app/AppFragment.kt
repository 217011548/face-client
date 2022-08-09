package com.tl.face.app

import com.tl.base.BaseFragment
import com.tl.face.action.ToastAction
import com.tl.face.http.model.HttpData
import com.hjq.http.listener.OnHttpListener
import okhttp3.Call


abstract class AppFragment<A : AppActivity> : BaseFragment<A>(),
    ToastAction, OnHttpListener<Any> {


    open fun isShowDialog(): Boolean {
        val activity: A = getAttachActivity() ?: return false
        return activity.isShowDialog()
    }


    open fun showDialog() {
        getAttachActivity()?.showDialog()
    }


    open fun hideDialog() {
        getAttachActivity()?.hideDialog()
    }

    /**
     * [OnHttpListener]
     */
    override fun onStart(call: Call) {
        showDialog()
    }

    override fun onSucceed(result: Any) {
        if (result !is HttpData<*>) {
            return
        }
        toast(result.getMessage())
    }

    override fun onFail(e: Exception) {
        toast(e.message)
    }

    override fun onEnd(call: Call) {
        hideDialog()
    }
}