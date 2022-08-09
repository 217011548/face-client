package com.tl.face.manager

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import androidx.collection.ArrayMap
import timber.log.Timber
import java.util.*


class ActivityManager private constructor() : ActivityLifecycleCallbacks {

    companion object {

        @Suppress("StaticFieldLeak")
        private val activityManager: ActivityManager by lazy { ActivityManager() }

        fun getInstance(): ActivityManager {
            return activityManager
        }


        private fun getObjectTag(`object`: Any): String {

            return `object`.javaClass.name + Integer.toHexString(`object`.hashCode())
        }
    }


    private val activitySet: ArrayMap<String?, Activity?> = ArrayMap()


    private val lifecycleCallbacks: ArrayList<ApplicationLifecycleCallback> = ArrayList()


    private lateinit var application: Application


    private var topActivity: Activity? = null


    private var resumedActivity: Activity? = null

    fun init(application: Application) {
        this.application = application
        this.application.registerActivityLifecycleCallbacks(this)
    }


    fun getApplication(): Application {
        return application
    }


    fun getTopActivity(): Activity? {
        return topActivity
    }


    fun getResumedActivity(): Activity? {
        return resumedActivity
    }


    fun isForeground(): Boolean {
        return getResumedActivity() != null
    }


    fun registerApplicationLifecycleCallback(callback: ApplicationLifecycleCallback) {
        lifecycleCallbacks.add(callback)
    }


    fun unregisterApplicationLifecycleCallback(callback: ApplicationLifecycleCallback) {
        lifecycleCallbacks.remove(callback)
    }


    fun finishActivity(clazz: Class<out Activity?>?) {
        if (clazz == null) {
            return
        }
        val keys: Array<String?> = activitySet.keys.toTypedArray()
        for (key: String? in keys) {
            val activity: Activity? = activitySet[key]
            if (activity == null || activity.isFinishing) {
                continue
            }
            if ((activity.javaClass == clazz)) {
                activity.finish()
                activitySet.remove(key)
                break
            }
        }
    }


    fun finishAllActivities() {
        finishAllActivities(null as Class<out Activity?>?)
    }

    /**
     * @param classArray
     */
    @SafeVarargs
    fun finishAllActivities(vararg classArray: Class<out Activity>?) {
        val keys: Array<String?> = activitySet.keys.toTypedArray()
        for (key: String? in keys) {
            val activity: Activity? = activitySet[key]
            if (activity == null || activity.isFinishing) {
                continue
            }
            var whiteClazz = false
            for (clazz: Class<out Activity?>? in classArray) {
                if ((activity.javaClass == clazz)) {
                    whiteClazz = true
                }
            }
            if (whiteClazz) {
                continue
            }

            activity.finish()
            activitySet.remove(key)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        Timber.i("%s - onCreate", activity.javaClass.simpleName)
        if (activitySet.size == 0) {
            for (callback: ApplicationLifecycleCallback? in lifecycleCallbacks) {
                callback?.onApplicationCreate(activity)
            }
            Timber.i("%s - onApplicationCreate", activity.javaClass.simpleName)
        }
        activitySet[getObjectTag(activity)] = activity
        topActivity = activity
    }

    override fun onActivityStarted(activity: Activity) {
        Timber.i("%s - onStart", activity.javaClass.simpleName)
    }

    override fun onActivityResumed(activity: Activity) {
        Timber.i("%s - onResume", activity.javaClass.simpleName)
        if (topActivity === activity && resumedActivity == null) {
            for (callback: ApplicationLifecycleCallback in lifecycleCallbacks) {
                callback.onApplicationForeground(activity)
            }
            Timber.i("%s - onApplicationForeground", activity.javaClass.simpleName)
        }
        topActivity = activity
        resumedActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {
        Timber.i("%s - onPause", activity.javaClass.simpleName)
    }

    override fun onActivityStopped(activity: Activity) {
        Timber.i("%s - onStop", activity.javaClass.simpleName)
        if (resumedActivity === activity) {
            resumedActivity = null
        }
        if (resumedActivity == null) {
            for (callback: ApplicationLifecycleCallback in lifecycleCallbacks) {
                callback.onApplicationBackground(activity)
            }
            Timber.i("%s - onApplicationBackground", activity.javaClass.simpleName)
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        Timber.i("%s - onSaveInstanceState", activity.javaClass.simpleName)
    }

    override fun onActivityDestroyed(activity: Activity) {
        Timber.i("%s - onDestroy", activity.javaClass.simpleName)
        activitySet.remove(getObjectTag(activity))
        if (topActivity === activity) {
            topActivity = null
        }
        if (activitySet.size == 0) {
            for (callback: ApplicationLifecycleCallback in lifecycleCallbacks) {
                callback.onApplicationDestroy(activity)
            }
            Timber.i("%s - onApplicationDestroy", activity.javaClass.simpleName)
        }
    }


    interface ApplicationLifecycleCallback {


        fun onApplicationCreate(activity: Activity)


        fun onApplicationDestroy(activity: Activity)


        fun onApplicationBackground(activity: Activity)


        fun onApplicationForeground(activity: Activity)
    }
}