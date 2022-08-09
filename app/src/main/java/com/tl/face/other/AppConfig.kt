package com.tl.face.other

import com.tl.face.BuildConfig

object AppConfig {


    fun isDebug(): Boolean {
        return BuildConfig.DEBUG
    }


    fun getBuildType(): String {
        return BuildConfig.BUILD_TYPE
    }

    fun isLogEnable(): Boolean {
        return BuildConfig.LOG_ENABLE
    }


    fun getPackageName(): String {
        return BuildConfig.APPLICATION_ID
    }

    fun getVersionName(): String {
        return BuildConfig.VERSION_NAME
    }


    fun getVersionCode(): Int {
        return BuildConfig.VERSION_CODE
    }


    fun getHostUrl(): String {
        return BuildConfig.HOST_URL
    }
}