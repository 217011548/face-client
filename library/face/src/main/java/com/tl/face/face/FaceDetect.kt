package com.tl.face.face

import android.content.Context
import com.arcsoft.face.ErrorInfo
import com.arcsoft.face.FaceEngine
import com.arcsoft.face.FaceInfo
import com.arcsoft.face.enums.DetectFaceOrientPriority
import com.arcsoft.face.enums.DetectMode
import com.tl.face.callback.FaceDetectCallback
import com.tl.face.constant.FaceConstant
import com.tl.face.utils.LogUtil
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 *
 * @author  ShenBen
 * @date    2021/03/25 18:50
 * @email   714081644@qq.com
 */
class FaceDetect {
  companion object {
    private const val TAG = "FaceDetect-->"
  }

  /**
   * Face Detect info
   */
  private val faceInfoList: MutableList<FaceInfo> = mutableListOf()

  /**
   * VIDEO mode detection
   */
  private val detectFaceEngine: FaceEngine = FaceEngine()

  /**
   * Detect Face Executor
   */
  private val detectFaceExecutor: ExecutorService by lazy {
    ThreadPoolExecutor(
      1, 1, 0, TimeUnit.MILLISECONDS, LinkedBlockingQueue()
    ) { r ->
      val t = Thread(r)
      t.name = "face-detect-thread-" + t.id
      t
    }
  }

  /**
   * Check anybody
   *
   * true:have
   * false:no one
   */
  private var isAnybody = false

  /**
   * detected face number
   */
  private var detectFaceNum = 0

  private var mFaceDetectCallback: FaceDetectCallback? = null

  fun init(
    context: Context,
    enableImageQuality: Boolean = false,
    detectFaceMaxNum: Int,
    detectFaceOrient: DetectFaceOrientPriority = DetectFaceOrientPriority.ASF_OP_0_ONLY
  ) {

    var mask = FaceEngine.ASF_FACE_DETECT
    if (enableImageQuality) {
      mask = mask or FaceEngine.ASF_IMAGEQUALITY
    }

    val result = detectFaceEngine.init(
      context,
      DetectMode.ASF_DETECT_MODE_VIDEO,
      DetectFaceOrientPriority.valueOf(detectFaceOrient.name),
      detectFaceMaxNum,
      mask
    )

    LogUtil.i("${TAG}faceEngine active:$result")
  }

  fun setFaceDetectCallback(callback: FaceDetectCallback?) {
    this.mFaceDetectCallback = callback
  }

  /**
   * Preview Frame
   */
  fun onPreviewFrame(
    rgbNV21: ByteArray,
    previewWidth: Int,
    previewHeight: Int
  ) {
    detectFaceExecutor.execute {
      faceInfoList.clear()
      //detect result
      val result = detectFaceEngine.detectFaces(
        rgbNV21,
        previewWidth,
        previewHeight,
        FaceEngine.CP_PAF_NV21,
        faceInfoList
      )
      if (result != ErrorInfo.MOK) {
        LogUtil.w(
          "${TAG}destroy-onPreviewFrame.detectFaces:$result,msg:${
            FaceConstant.getFaceErrorMsg(
              result
            )
          }"
        )
        return@execute
      }
      anybody(faceInfoList)
    }
  }

  inline fun setFaceDetectCallback(
    crossinline someone: () -> Unit = {},
    crossinline nobody: () -> Unit = {},
    crossinline notInTheArea: () -> Unit = {},
    crossinline detectFaceNum: (num: Int, faceIds: List<Int>) -> Unit = { _, _ -> },
    crossinline detectionFace: (nav21: ByteArray, maskInfo: Int, width: Int, height: Int) -> Unit = { _, _, _, _ -> },
  ) {
    setFaceDetectCallback(object : FaceDetectCallback {
      override fun someone() = someone()

      override fun nobody() = nobody()

      override fun notInTheArea() = notInTheArea()

      override fun detectFaceNum(num: Int, faceIds: List<Int>) = detectFaceNum(num, faceIds)

      override fun detectionFace(nv21: ByteArray, maskInfo: Int, width: Int, height: Int) = detectionFace(nv21, maskInfo, width, height)
    })
  }

  /**
   * destroy to release resource
   */
  fun destroy() {
    if (detectFaceExecutor.isShutdown.not()) {
      detectFaceExecutor.shutdownNow()
    }
    synchronized(detectFaceEngine) {
      val result = detectFaceEngine.unInit()
      LogUtil.w("${TAG}destroy-detectFaceEngine.unInit:$result")
    }
    mFaceDetectCallback = null
  }

  /**
   * determine whether someone
   */
  private fun anybody(faceInfoList: List<FaceInfo>) {
    //If faceInfoList is empty mean no one
    val anybody = faceInfoList.isNotEmpty()
    if (isAnybody != anybody) {
      if (anybody) {
        mFaceDetectCallback?.someone()
      } else {
        mFaceDetectCallback?.nobody()
      }
      isAnybody = anybody
    }
    val num = faceInfoList.size
    if (detectFaceNum != num) {
      val faceIds: ArrayList<Int> = ArrayList(faceInfoList.size)
      faceInfoList.forEach { faceIds.add(it.faceId) }
      mFaceDetectCallback?.detectFaceNum(num, faceIds)
      detectFaceNum = num
    }
  }
}