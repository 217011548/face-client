package com.tl.face.manager

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.*
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.TextView
import java.util.*


class InputTextManager private constructor(view: View, alpha: Boolean) : TextWatcher {

    companion object {

        fun with(activity: Activity): Builder {
            return Builder(activity)
        }
    }


    private val view: View

    private val alpha: Boolean


    private var viewSet: MutableList<TextView> = mutableListOf()


    private var listener: OnInputTextListener? = null

    init {
        this.view = view
        this.alpha = alpha
    }

    /**
     * Add TextView
     *
     * @param views
     */
    fun addViews(views: MutableList<TextView>) {
        viewSet.addAll(views)
        for (view: TextView in views) {
            view.addTextChangedListener(this)
        }


        notifyChanged()
    }

    /**
     * Add TextView
     *
     * @param views
     */
    fun addViews(vararg views: TextView) {
        for (view: TextView in views) {

            if (!viewSet.contains(view)) {
                view.addTextChangedListener(this)
                viewSet.add(view)
            }
        }

        notifyChanged()
    }


    fun removeViews(vararg views: TextView) {
        if (viewSet.isEmpty()) {
            return
        }
        for (view: TextView in views) {
            view.removeTextChangedListener(this)
            viewSet.remove(view)
        }

        notifyChanged()
    }


    fun removeAllViews() {
        for (view: TextView in viewSet) {
            view.removeTextChangedListener(this)
        }
        viewSet.clear()
    }


    fun setListener(listener: OnInputTextListener?) {
        this.listener = listener
    }

    /**
     * [TextWatcher]
     */
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable?) {
        notifyChanged()
    }


    fun notifyChanged() {

        for (view: TextView in viewSet) {
            if (("" == view.text.toString())) {
                setEnabled(false)
                return
            }
        }

        listener.let {
            if (it == null) {
                setEnabled(true)
                return
            }
            setEnabled(it.onInputChange(this))
        }
    }


    fun setEnabled(enabled: Boolean) {
        if (enabled == view.isEnabled) {
            return
        }
        if (enabled) {

            view.isEnabled = true
            if (alpha) {

                view.alpha = 1f
            }
        } else {

            view.isEnabled = false
            if (alpha) {

                view.alpha = 0.5f
            }
        }
    }

    class Builder constructor(private val activity: Activity) {


        private var view: View? = null


        private var alpha: Boolean = false


        private val viewSet: MutableList<TextView> = ArrayList()


        private var listener: OnInputTextListener? = null

        fun addView(view: TextView?): Builder = apply {
            if (view != null) {
                viewSet.add(view)
            }
        }

        fun setMain(view: View): Builder = apply {
            this.view = view
        }

        fun setAlpha(alpha: Boolean): Builder = apply {
            this.alpha = alpha
        }

        fun setListener(listener: OnInputTextListener?): Builder = apply {
            this.listener = listener
        }

        fun build(): InputTextManager {
            if (view == null) {
                throw IllegalArgumentException("are you ok?")
            }
            val helper = InputTextManager(view!!, alpha)
            helper.addViews(viewSet)
            helper.setListener(listener)
            TextInputLifecycle.register(activity, helper)
            return helper
        }
    }

    private class TextInputLifecycle private constructor(
        private var activity: Activity?,
        private var textHelper: InputTextManager?
    ) : ActivityLifecycleCallbacks {

        companion object {

            fun register(activity: Activity, helper: InputTextManager?) {
                val lifecycle = TextInputLifecycle(activity, helper)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    activity.registerActivityLifecycleCallbacks(lifecycle)
                } else {
                    activity.application.registerActivityLifecycleCallbacks(lifecycle)
                }
            }
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

        override fun onActivityStarted(activity: Activity) {}

        override fun onActivityResumed(activity: Activity) {}

        override fun onActivityPaused(activity: Activity) {}

        override fun onActivityStopped(activity: Activity) {}

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {
            if (this.activity !== activity) {
                return
            }
            textHelper?.removeAllViews()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                this.activity?.unregisterActivityLifecycleCallbacks(this)
            } else {
                this.activity?.application?.unregisterActivityLifecycleCallbacks(this)
            }
            textHelper = null
            this.activity = null
        }
    }


    interface OnInputTextListener {

        /**
         *
         * @return
         */
        fun onInputChange(manager: InputTextManager?): Boolean
    }
}