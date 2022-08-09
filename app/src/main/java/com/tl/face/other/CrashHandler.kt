package com.tl.face.other

import android.app.*
import android.content.*
import android.os.Process
import com.tl.face.ui.activity.CrashActivity
import com.tl.face.ui.activity.RestartActivity


class CrashHandler private constructor(private val application: Application) :
    Thread.UncaughtExceptionHandler {

    companion object {


        private const val CRASH_FILE_NAME: String = "crash_file"


        private const val KEY_CRASH_TIME: String = "key_crash_time"


        fun register(application: Application) {
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(application))
        }
    }

    private val nextHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()

    init {
        if ((javaClass.name == nextHandler?.javaClass?.name)) {

            throw IllegalStateException("are you ok?")
        }
    }

    @Suppress("ApplySharedPref")
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        val sharedPreferences: SharedPreferences = application.getSharedPreferences(
            CRASH_FILE_NAME, Context.MODE_PRIVATE)
        val currentCrashTime: Long = System.currentTimeMillis()
        val lastCrashTime: Long = sharedPreferences.getLong(KEY_CRASH_TIME, 0)

        sharedPreferences.edit().putLong(KEY_CRASH_TIME, currentCrashTime).commit()


        val deadlyCrash: Boolean = currentCrashTime - lastCrashTime < 1000 * 60 * 5
        if (AppConfig.isDebug()) {
            CrashActivity.start(application, throwable)
        } else {
            if (!deadlyCrash) {

                RestartActivity.start(application)
            }
        }


        if (nextHandler != null && !nextHandler.javaClass.name
                .startsWith("com.android.internal.os")) {
            nextHandler.uncaughtException(thread, throwable)
        }


        Process.killProcess(Process.myPid())
        System.exit(10)
    }
}