package com.tl.face.face

import android.content.Context
import com.arcsoft.face.ActiveFileInfo
import com.arcsoft.face.ErrorInfo
import com.arcsoft.face.FaceEngine
import com.arcsoft.face.model.ActiveDeviceInfo
import com.tl.face.callback.OnActiveCallback
import com.tl.face.callback.OnActiveDeviceInfoCallback
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.*

/**
 * Active tool
 *
 * @author ShenBen
 * @date 2020/12/15 15:19
 * @email 714081644@qq.com
 */
object FaceActive {
    private val sExecutor: ExecutorService

    init {
        sExecutor = ThreadPoolExecutor(
            1, 1,
            0L, TimeUnit.MILLISECONDS,
            LinkedBlockingQueue()
        ) { r ->
            val t = Thread(r)
            t.name = "face-active-thread-" + t.id
            t
        }
    }

    /**
     * online active
     * run in second thread
     *
     * @param context   context
     * @param activeKey activeKey
     * @param appId     appId
     * @param sdkKey    sdkKey
     * @return
     */
    @JvmStatic
    fun activeOnline(
        context: Context,
        activeKey: String,
        appId: String,
        sdkKey: String,
        callback: OnActiveCallback?
    ) {
        sExecutor.execute {
            val code = FaceEngine.activeOnline(context, activeKey, appId, sdkKey)
            val isSuccess = code == ErrorInfo.MOK || code == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED
            callback?.activeCallback(isSuccess, code)
        }
    }

    /**
     * Offline active
     * Run in secondthread
     *
     * @param context  context
     * @param filePath key store path
     * @return
     */
    @JvmStatic
    fun activeOffline(context: Context, filePath: String, callback: OnActiveCallback?) {
        sExecutor.execute {
            val code = FaceEngine.activeOffline(context, filePath)
            val isSuccess = code == ErrorInfo.MOK || code == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED
            callback?.activeCallback(isSuccess, code)
        }
    }

    /**
     * Check active status
     *
     * @param context context
     * @return Active or not
     */
    @JvmStatic
    fun isActivated(context: Context): Boolean {
        return FaceEngine.getActiveFileInfo(context, ActiveFileInfo()) == ErrorInfo.MOK
    }

    /**
     * Gen key for offline active
     * Run in secondthread
     *
     *
     *
     *
     * @param context context
     * @param saveFilePath key store path
     * @param callback result
     */
    @JvmStatic
    fun generateActiveDeviceInfo(
        context: Context,
        saveFilePath: String,
        callback: OnActiveDeviceInfoCallback?
    ) {
        sExecutor.execute {
            val deviceInfo = ActiveDeviceInfo()
            val code = FaceEngine.getActiveDeviceInfo(context, deviceInfo)
            if (code == ErrorInfo.MOK) {
                val deviceInfoStr = deviceInfo.deviceInfo
                val file = File(saveFilePath)
                val parentFile = file.parentFile
                if (parentFile != null) {
                    if (!parentFile.exists()) {
                        parentFile.mkdirs()
                    }
                    if (file.exists()) {
                        file.delete()
                    }
                    var isSuccess: Boolean
                    try {
                        FileOutputStream(file).use { fos ->
                            fos.write(deviceInfoStr.toByteArray())
                            isSuccess = true
                        }
                    } catch (e: IOException) {
                        isSuccess = false
                    }
                    callback?.deviceInfoCallback(
                        isSuccess,
                        if (isSuccess) code else -1,
                        deviceInfoStr,
                        file
                    )
                } else {
                    callback?.deviceInfoCallback(false, -1, null, null)
                }
            } else {
                callback?.deviceInfoCallback(false, code, null, null)
            }
        }
    }
}