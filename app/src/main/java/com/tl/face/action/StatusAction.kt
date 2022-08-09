package com.tl.face.action

import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkInfo
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.tl.face.R
import com.tl.face.widget.StatusLayout
import com.tl.face.widget.StatusLayout.OnRetryListener


interface StatusAction {


    fun getStatusLayout(): StatusLayout?

    /**
     * Display loading
     */
    fun showLoading(@RawRes id: Int = R.raw.loading) {
        getStatusLayout()?.let {
            it.show()
            it.setAnimResource(id)
            it.setHint("")
            it.setOnRetryListener(null)
        }
    }

    /**
     * Display complete
     */
    fun showComplete() {
        getStatusLayout()?.let {
            if (!it.isShow()) {
                return
            }
            it.hide()
        }
    }

    /**
     * Display empty
     */
    fun showEmpty() {
        showLayout(R.drawable.status_empty_ic, R.string.status_layout_no_data, null)
    }

    /**
     * Display Error
     */
    fun showError(listener: OnRetryListener?) {
        getStatusLayout()?.let {
            val manager: ConnectivityManager? = ContextCompat.getSystemService(it.context, ConnectivityManager::class.java)
            if (manager != null) {
                val info: NetworkInfo? = manager.activeNetworkInfo
                // Check internet connection
                if (info == null || !info.isConnected) {
                    showLayout(R.drawable.status_network_ic, R.string.status_layout_error_network, listener)
                    return
                }
            }
            showLayout(R.drawable.status_error_ic, R.string.status_layout_error_request, listener)
        }
    }

    /**
     * Show Message
     */
    fun showLayout(@DrawableRes drawableId: Int, @StringRes stringId: Int, listener: OnRetryListener?) {
        getStatusLayout()?.let {
            showLayout(ContextCompat.getDrawable(it.context, drawableId), it.context.getString(stringId), listener)
        }
    }

    fun showLayout(drawable: Drawable?, hint: CharSequence?, listener: OnRetryListener?) {
        getStatusLayout()?.let {
            it.show()
            it.setIcon(drawable)
            it.setHint(hint)
            it.setOnRetryListener(listener)
        }
    }
}