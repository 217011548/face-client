package com.tl.face.other

import com.tl.face.action.ToastAction
import com.hjq.toast.ToastUtils
import com.hjq.toast.config.IToastInterceptor
import timber.log.Timber



class ToastLogInterceptor : IToastInterceptor {

    override fun intercept(text: CharSequence): Boolean {
        if (AppConfig.isLogEnable()) {

            val stackTrace: Array<StackTraceElement> = Throwable().stackTrace

            var i = 2
            while (stackTrace.size > 2 && i < stackTrace.size) {


                val lineNumber: Int = stackTrace[i].lineNumber

                val className: String = stackTrace[i].className
                if (((lineNumber <= 0) || className.startsWith(ToastUtils::class.java.name) ||
                            className.startsWith(ToastAction::class.java.name))) {
                    i++
                    continue
                }
                Timber.tag("ToastUtils")
                Timber.i("(%s:%s) %s", stackTrace[i].fileName, lineNumber, text.toString())
                break
                i++
            }
        }
        return false
    }
}