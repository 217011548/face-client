package com.tl.face.other

import android.os.Build
import timber.log.Timber.DebugTree


class DebugLoggerTree : DebugTree() {

    companion object {
        private const val MAX_TAG_LENGTH: Int = 23
    }


    override fun createStackElementTag(element: StackTraceElement): String {
        val tag: String = "(" + element.fileName + ":" + element.lineNumber + ")"

        if (tag.length <= MAX_TAG_LENGTH || Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return tag
        }
        return tag.substring(0, MAX_TAG_LENGTH)
    }
}