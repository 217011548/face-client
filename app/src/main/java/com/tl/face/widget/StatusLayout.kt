package com.tl.face.widget

import android.content.*
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.tl.face.R

/**
 * Status Layout
 */
class StatusLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0, defStyleRes: Int = 0) :
    FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    /** main Layout */
    private var mainLayout: ViewGroup? = null


    private var lottieView: LottieAnimationView? = null


    private var textView: TextView? = null


    private var retryView: TextView? = null


    private var listener: OnRetryListener? = null

    /**
     * Show
     */
    fun show() {
        if (mainLayout == null) {

            initLayout()
        }
        if (isShow()) {
            return
        }
        retryView!!.visibility = if (listener == null) INVISIBLE else VISIBLE

        mainLayout!!.visibility = VISIBLE
    }

    /**
     * Hide
     */
    fun hide() {
        if (mainLayout == null || !isShow()) {
            return
        }

        mainLayout!!.visibility = INVISIBLE
    }


    fun isShow(): Boolean {
        return mainLayout != null && mainLayout?.visibility == VISIBLE
    }


    fun setIcon(@DrawableRes id: Int) {
        setIcon(ContextCompat.getDrawable(context, id))
    }

    fun setIcon(drawable: Drawable?) {
        lottieView?.apply {
            if (isAnimating) {
                cancelAnimation()
            }
            setImageDrawable(drawable)
        }
    }


    fun setAnimResource(@RawRes id: Int) {
        lottieView?.apply {
            setAnimation(id)
            if (!isAnimating) {
                playAnimation()
            }
        }
    }


    fun setHint(@StringRes id: Int) {
        setHint(resources.getString(id))
    }

    fun setHint(text: CharSequence?) {
        textView?.text = text ?: ""
    }


    private fun initLayout() {
        mainLayout = LayoutInflater.from(context).inflate(R.layout.widget_status_layout, this, false) as ViewGroup
        lottieView = mainLayout!!.findViewById(R.id.iv_status_icon)
        textView = mainLayout!!.findViewById(R.id.iv_status_text)
        retryView = mainLayout!!.findViewById(R.id.iv_status_retry)
        if (mainLayout!!.background == null) {
            val typedArray: TypedArray = context.obtainStyledAttributes(intArrayOf(android.R.attr.windowBackground))
            mainLayout!!.background = typedArray.getDrawable(0)
            mainLayout!!.isClickable = true
            typedArray.recycle()
        }
        retryView!!.setOnClickListener(clickWrapper)
        addView(mainLayout)
    }


    fun setOnRetryListener(listener: OnRetryListener?) {
        this.listener = listener
        if (isShow()) {
            retryView!!.visibility = if (this.listener == null) INVISIBLE else VISIBLE
        }
    }


    private val clickWrapper: OnClickListener = OnClickListener { listener?.onRetry(this@StatusLayout) }


    interface OnRetryListener {


        fun onRetry(layout: StatusLayout)
    }
}