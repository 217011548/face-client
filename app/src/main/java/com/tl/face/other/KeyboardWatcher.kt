package com.tl.face.other

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.graphics.*
import android.os.*
import android.view.*
import android.view.ViewTreeObserver.OnGlobalLayoutListener


class KeyboardWatcher private constructor(private var activity: Activity) :
    OnGlobalLayoutListener, ActivityLifecycleCallbacks {

    companion object {

        fun with(activity: Activity): KeyboardWatcher {
            return KeyboardWatcher(activity)
        }
    }

    private var contentView: View = activity.findViewById(Window.ID_ANDROID_CONTENT)
    private var listeners: SoftKeyboardStateListener? = null
    private var softKeyboardOpened: Boolean = false
    private var statusBarHeight: Int = 0

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activity.registerActivityLifecycleCallbacks(this)
        } else {
            activity.application.registerActivityLifecycleCallbacks(this)
        }
        contentView.viewTreeObserver.addOnGlobalLayoutListener(this)


        val resourceId: Int = activity.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {

            statusBarHeight = activity.resources.getDimensionPixelSize(resourceId)
        }
    }

    /**
     * [ViewTreeObserver.OnGlobalLayoutListener]
     */
    override fun onGlobalLayout() {
        val r = Rect()
        //r will be populated with the coordinates of your view that area still visible.
        contentView.getWindowVisibleDisplayFrame(r)
        val heightDiff: Int = contentView.rootView.height - (r.bottom - r.top)
        if (!softKeyboardOpened && heightDiff > contentView.rootView.height / 4) {
            softKeyboardOpened = true
            if ((activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN) != WindowManager.LayoutParams.FLAG_FULLSCREEN) {
                listeners?.onSoftKeyboardOpened(heightDiff - statusBarHeight)
            } else {
                listeners?.onSoftKeyboardOpened(heightDiff)
            }
        } else if (softKeyboardOpened && heightDiff < contentView.rootView.height / 4) {
            softKeyboardOpened = false
            listeners?.onSoftKeyboardClosed()
        }
    }


    fun setListener(listener: SoftKeyboardStateListener?) {
        listeners = listener
    }

    /**
     * [ActivityLifecycleCallbacks]
     */

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        if (this.activity === activity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                this.activity.unregisterActivityLifecycleCallbacks(this)
            } else {
                this.activity.application.unregisterActivityLifecycleCallbacks(this)
            }
            contentView.viewTreeObserver.removeOnGlobalLayoutListener(this)
            listeners = null
        }
    }


    interface SoftKeyboardStateListener {

        /**
         *
         *
         * @param keyboardHeight
         */
        fun onSoftKeyboardOpened(keyboardHeight: Int)

        /**
         *
         */
        fun onSoftKeyboardClosed()
    }
}