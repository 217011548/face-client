package com.tl.face.widget

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.appbar.CollapsingToolbarLayout


class XCollapsingToolbarLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    CollapsingToolbarLayout(context, attrs, defStyleAttr) {


    private var listener: OnScrimsListener? = null


    private var scrimsShownStatus: Boolean = false

    override fun setScrimsShown(shown: Boolean, animate: Boolean) {
        super.setScrimsShown(shown, true)

        if (scrimsShownStatus == shown) {
            return
        }

        scrimsShownStatus = shown
        listener?.onScrimsStateChange(this, scrimsShownStatus)
    }


    fun isScrimsShown(): Boolean {
        return scrimsShownStatus
    }

    fun setOnScrimsListener(listener: OnScrimsListener?) {
        this.listener = listener
    }


    interface OnScrimsListener {

        fun onScrimsStateChange(layout: XCollapsingToolbarLayout?, shown: Boolean)
    }
}