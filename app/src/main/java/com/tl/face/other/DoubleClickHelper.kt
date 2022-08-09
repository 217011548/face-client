package com.tl.face.other

import android.os.SystemClock


object DoubleClickHelper {


    private val TIME_ARRAY: LongArray = LongArray(2)


    fun isOnDoubleClick(): Boolean {

        return isOnDoubleClick(1500)
    }


    fun isOnDoubleClick(time: Int): Boolean {
        System.arraycopy(TIME_ARRAY, 1, TIME_ARRAY, 0, TIME_ARRAY.size - 1)
        TIME_ARRAY[TIME_ARRAY.size - 1] = SystemClock.uptimeMillis()
        return TIME_ARRAY[0] >= (SystemClock.uptimeMillis() - time)
    }
}