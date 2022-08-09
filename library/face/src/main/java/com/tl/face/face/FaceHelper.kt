package com.tl.face.face

import android.os.Handler
import android.os.Looper
import com.arcsoft.face.*
import com.arcsoft.face.enums.DetectFaceOrientPriority
import com.arcsoft.face.enums.DetectMode
import com.arcsoft.face.enums.ExtractType
import com.tl.face.callback.OnPreviewCallback
import com.tl.face.configuration.FaceConfiguration
import com.tl.face.configuration.LivenessType
import com.tl.face.constant.FaceConstant
import com.tl.face.constant.FaceErrorType
import com.tl.face.constant.RecognizeStatus
import com.tl.face.constant.RequestLivenessStatus
import com.tl.face.face.model.FacePreviewInfo
import com.tl.face.face.model.RecognizeInfo
import com.tl.face.utils.FaceRectTransformerUtil
import com.tl.face.utils.LogUtil
import java.util.concurrent.*

/**
 * Face Info control
 *
 * @author  ShenBen
 * @date    2021/02/24 14:08
 * @email   714081644@qq.com
 */
internal class FaceHelper(
  private val configuration: FaceConfiguration,
  private val onPreviewCallback: OnPreviewCallback
) {

  companion object {
    private const val TAG = "FaceHelper->"
  }

  private val mHandler = Handler(Looper.getMainLooper())

  /**
   * VIDEO mode to detect the face
   */
  private val detectFaceEngine: FaceEngine = FaceEngine()

  /**
   * Extract Face Feature Engine
   */
  private val extractFeatureEngine: FaceEngine = FaceEngine()

  /**
   * Extract Face Feature Engine Thread Queue
   */
  private val extractFeatureThreadQueue =
    LinkedBlockingQueue<Runnable>(configuration.detectFaceMaxNum)

  /**
   * Extract Face Feature Executor
   */
  private val extractFeatureExecutor: ExecutorService by lazy {
    ThreadPoolExecutor(
      configuration.detectFaceMaxNum,
      configuration.detectFaceMaxNum,
      0,
      TimeUnit.MILLISECONDS,
      extractFeatureThreadQueue
    ) { r ->
      val t = Thread(r)
      t.name = "extract-feature-thread-" + t.id
      t
    }
  }

  /**
   * IMAGE mode to preview the info
   * include the age angle or sex
   */
  private val detectInfoEngine: FaceEngine = FaceEngine()

  /**
   * detect Info ThreadQueue
   */
  private val detectInfoThreadQueue =
    LinkedBlockingQueue<Runnable>(configuration.detectFaceMaxNum)

  /**
   * detect Info Executor
   */
  private val detectInfoExecutor: ExecutorService by lazy {
    ThreadPoolExecutor(
      configuration.detectFaceMaxNum,
      configuration.detectFaceMaxNum,
      0,
      TimeUnit.MILLISECONDS,
      detectInfoThreadQueue
    ) { r ->
      val t = Thread(r)
      t.name = "liveness-thread-" + t.id
      t
    }
  }
  private var detectInfoMask = 0

  /**
   * face comparison
   */
  private val faceServer = FaceServer()

  /**
   * Record the face detect area
   * <faceId,RecognizeInfo>
   */
  private val recognizeInfoMap: MutableMap<Int, RecognizeInfo> =
    ConcurrentHashMap(configuration.detectFaceMaxNum)

  /**
   * Face Info
   */
  private val faceInfoList: MutableList<FaceInfo> = mutableListOf()

  /**
   * Is wearing Mask
   */
  private val maskInfoList: MutableList<MaskInfo> = mutableListOf()

  /**
   * Preview Info
   */
  private val facePreviewInfo: MutableList<FacePreviewInfo> = mutableListOf()

  /**
   * Save the info to temp
   */
  private val tempDetection: MutableList<FacePreviewInfo> = mutableListOf()

  /**
   * Is anyone currently
   *
   * true: have
   * false:no one
   */
  private var isAnybody = false

  /**
   * face number
   */
  private var detectFaceNum = 0

  /**
   * IR camera preview
   */
  @Volatile
  private var irNV21: ByteArray? = null

  /**
   * Pause the detection
   */
  private var needPauseDetection = false

  init {
    //init the engine
    var mask = FaceEngine.ASF_FACE_DETECT
    if (configuration.enableMask) {
      mask = mask or FaceEngine.ASF_MASK_DETECT
    }
    val result = detectFaceEngine.init(
      configuration.context,
      DetectMode.ASF_DETECT_MODE_VIDEO,
      configuration.detectFaceOrient,
      configuration.detectFaceMaxNum,
      mask
    )
    LogUtil.i("${TAG}Face engine init:$result")
  }

  init {
    if (configuration.enableRecognize) {
      //init the feature extraction engine
      val orientPriority =
        if (configuration.detectFaceOrient == DetectFaceOrientPriority.ASF_OP_ALL_OUT) {
          DetectFaceOrientPriority.ASF_OP_0_ONLY
        } else {
          configuration.detectFaceOrient
        }
      var mask = FaceEngine.ASF_FACE_RECOGNITION
      if (configuration.enableImageQuality) {
        mask = mask or FaceEngine.ASF_IMAGEQUALITY
      }
      val result = extractFeatureEngine.init(
        configuration.context,
        DetectMode.ASF_DETECT_MODE_IMAGE,
        orientPriority,
        configuration.detectFaceMaxNum,
        mask
      )
      LogUtil.i("${TAG}feature extraction engine init:$result")
    }
  }

  init {
    val detectInfo = configuration.detectInfo
    if (configuration.livenessType != LivenessType.NONE) {
      val orientPriority =
        if (configuration.detectFaceOrient == DetectFaceOrientPriority.ASF_OP_ALL_OUT) {
          DetectFaceOrientPriority.ASF_OP_0_ONLY
        } else {
          configuration.detectFaceOrient
        }
      if (configuration.livenessType == LivenessType.RGB) {
        detectInfoMask = detectInfoMask or FaceEngine.ASF_LIVENESS
        if (detectInfo.age) {
          detectInfoMask = detectInfoMask or FaceEngine.ASF_AGE
        }
        if (detectInfo.gender) {
          detectInfoMask = detectInfoMask or FaceEngine.ASF_GENDER
        }
      } else {
        detectInfoMask = detectInfoMask or FaceEngine.ASF_IR_LIVENESS
      }
      val result = detectInfoEngine.init(
        configuration.context,
        DetectMode.ASF_DETECT_MODE_IMAGE,
        orientPriority,
        configuration.detectFaceMaxNum,
        detectInfoMask
      )
      detectInfoEngine.setLivenessParam(
        LivenessParam(
          configuration.rgbLivenessThreshold,
          configuration.irLivenessThreshold,
          configuration.fqLivenessThreshold,
        )
      )
      LogUtil.i("${TAG}init Liveness detection engine:$result")
    }
  }

  init {
    faceServer.init(configuration.context, configuration.detectFaceOrient)
  }

  fun refreshIrPreviewData(irNV21: ByteArray?) {
    this.irNV21 = irNV21
  }

  /**
   * Preview info
   */
  fun onPreviewFrame(
    rgbNV21: ByteArray,
    previewWidth: Int,
    previewHeight: Int,
    canvasWidth: Int,
    canvasHeight: Int,
  ) {
    faceInfoList.clear()
    maskInfoList.clear()
    facePreviewInfo.clear()

    if (needPauseDetection) {
      println("Stop Detect")
      return
    }

    //detect result
    val result = detectFaceEngine.detectFaces(
      rgbNV21,
      previewWidth,
      previewHeight,
      FaceEngine.CP_PAF_NV21,
      faceInfoList
    )
    if (result != ErrorInfo.MOK) {
      onError(
        FaceErrorType.DETECT_FACES,
        result,
        FaceConstant.getFaceErrorMsg(result)
      )
      return
    }

    //check anyone
    anybody(faceInfoList)

    if (configuration.recognizeKeepMaxFace) {
      //keep the bigger face
      keepMaxFace(faceInfoList)
    }
    //enable detect with mask
    if (faceInfoList.isNotEmpty() && configuration.enableMask) {
      val detectMaskResult = detectFaceEngine.process(
        rgbNV21,
        previewWidth,
        previewHeight,
        FaceEngine.CP_PAF_NV21,
        faceInfoList,
        FaceEngine.ASF_MASK_DETECT
      )
      if (detectMaskResult == ErrorInfo.MOK) {
        val maskResult = detectFaceEngine.getMask(maskInfoList)
        if (maskResult != ErrorInfo.MOK) {
          onError(
            FaceErrorType.DETECT_MASK,
            detectMaskResult,
            FaceConstant.getFaceErrorMsg(maskResult)
          )
        }
      } else {
        onError(
          FaceErrorType.DETECT_MASK,
          detectMaskResult,
          FaceConstant.getFaceErrorMsg(detectMaskResult)
        )
      }
    }
    //draw the face rect
    faceInfoList.forEachIndexed { index, faceInfo ->
      val rgbAdjustRect = FaceRectTransformerUtil.adjustRect(
        previewWidth,
        previewHeight,
        canvasWidth,
        canvasHeight,
        configuration.isRgbMirror,
        faceInfo.rect,
        configuration.drawFaceRect.rgbOffsetX,
        configuration.drawFaceRect.rgbOffsetY
      )

      val previewInfo =
        FacePreviewInfo(faceInfo.faceId, faceInfo, FaceInfo(faceInfo))
      if (configuration.enableMask && maskInfoList.isNotEmpty() && maskInfoList.size == faceInfoList.size) {
        previewInfo.mask = maskInfoList[index].mask
      }
      previewInfo.rgbTransformedRect = rgbAdjustRect
      previewInfo.irTransformedRect = FaceRectTransformerUtil.rgbRectToIrRect(
        rgbAdjustRect,
        FaceConstant.DEFAULT_ZOOM_RATIO,
        configuration.drawFaceRect.irOffsetX,
        configuration.drawFaceRect.irOffsetY
      )
      previewInfo.recognizeAreaValid =
        onPreviewCallback.getRecognizeAreaRect().contains(rgbAdjustRect)
      facePreviewInfo.add(previewInfo)
    }

    if (facePreviewInfo.isNotEmpty()) {
      for (previewInfo in facePreviewInfo) {
        if (judgeFaceSize(previewInfo).not()) {
          continue
        }
        if (previewInfo.recognizeAreaValid.not()) {
          //Skip when not in the scan area
          onPreviewCallback?.notInTheArea()
          continue
        }

        if (tempDetection.isEmpty()) {
          tempDetection.add(previewInfo)
        } else {
          val info = tempDetection[tempDetection.size - 1]
          //Use the last data to judge
          if (info.faceId == previewInfo.faceId) {
            //client no change
            if (info.mask != previewInfo.mask) {
              //If not match will start to detect
              tempDetection.clear()
            }
            tempDetection.add(previewInfo)
          } else {
            tempDetection.clear()
          }
        }
        if (tempDetection.size >= 5) {
          //Will try 5 time
          onPreviewCallback?.detectionFace(
            rgbNV21,
            facePreviewInfo[0].mask,
            previewWidth,
            previewHeight
          )
          tempDetection.clear()
        }
      }
    }

    if (facePreviewInfo.isNotEmpty() && configuration.enableRecognize) {
      //Start to detect
//      doRecognize(rgbNV21, previewWidth, previewHeight, facePreviewInfo)
    }
    //clear the face info when client left
    clearLeftFace(faceInfoList)
    //start to preview
    onPreviewCallback.onPreviewFaceInfo(facePreviewInfo)
  }

  fun getRecognizeInfo(faceId: Int): RecognizeInfo {
    var recognizeInfo = recognizeInfoMap[faceId]
    if (recognizeInfo == null) {
      recognizeInfo = RecognizeInfo(faceId)
      recognizeInfoMap[faceId] = recognizeInfo
    }
    return recognizeInfo
  }

  fun destroy() {
    mHandler.removeCallbacksAndMessages(null)

    synchronized(detectFaceEngine) {
      val result = detectFaceEngine.unInit()
      LogUtil.w("${TAG}destroy-detectFaceEngine.unInit:$result")
    }

    synchronized(extractFeatureEngine) {
      val result = extractFeatureEngine.unInit()
      LogUtil.w("${TAG}destroy-extractFeatureEngine.unInit:$result")
    }
    if (extractFeatureExecutor.isShutdown.not()) {
      extractFeatureExecutor.shutdownNow()
    }
    extractFeatureThreadQueue.clear()

    synchronized(detectInfoEngine) {
      val result = detectInfoEngine.unInit()
      LogUtil.w("${TAG}destroy-detectInfoEngine.unInit:$result")
    }
    if (detectInfoExecutor.isShutdown.not()) {
      detectInfoExecutor.shutdownNow()
    }
    detectInfoThreadQueue.clear()

    faceServer.destroy()

    recognizeInfoMap.clear()
    faceInfoList.clear()
    maskInfoList.clear()
    facePreviewInfo.clear()
    irNV21 = null
  }

  fun changedDetectionStatus(pauseDetect: Boolean) {
    needPauseDetection = pauseDetect
  }

  /**
   * Determine whether someone
   */
  private fun anybody(faceInfoList: List<FaceInfo>) {
    //If faceinfo list empty mean no one
    val anybody = faceInfoList.isNotEmpty()
    if (isAnybody != anybody) {
      if (anybody) {
        onPreviewCallback.someone()
      } else {
        onPreviewCallback.nobody()
      }
      isAnybody = anybody
    }
    val num = faceInfoList.size
    if (detectFaceNum != num) {
      val faceIds: ArrayList<Int> = ArrayList(faceInfoList.size)
      faceInfoList.forEach { faceIds.add(it.faceId) }
      onPreviewCallback.detectFaceNum(num, faceIds)
      detectFaceNum = num
    }
  }

  /**
   * Keep Bigger face
   * @param list During face tracking, the face information of one frame of data
   */
  private fun keepMaxFace(list: MutableList<FaceInfo>) {
    if (list.size <= 1) {
      return
    }
    var maxFaceInfo = list[0]
    for (info in list) {
      if (info.rect.width() * info.rect.height() > maxFaceInfo.rect.width() * maxFaceInfo.rect.height()) {
        maxFaceInfo = info
      }
    }
    list.clear()
    list.add(maxFaceInfo)
  }

  /**
   * Delete a face that has left
   */
  private fun clearLeftFace(faceInfoList: List<FaceInfo>) {
    val iterator = recognizeInfoMap.entries.iterator()
    while (iterator.hasNext()) {
      val next = iterator.next()
      var contained = false
      for (faceInfo in faceInfoList) {
        if (next.key == faceInfo.faceId) {
          contained = true
          break
        }
      }
      if (contained.not()) {
        val value = next.value
        //remove first
        iterator.remove()
        //lock later
        synchronized(value.lock) {
          value.lock.notifyAll()
        }
      }
    }
  }

  /**
   * Start to recognize
   * @param rgbNV21 camera preview
   * @param width camera preview width
   * @param height camera preview height
   */
  private fun doRecognize(
    rgbNV21: ByteArray,
    width: Int,
    height: Int,
    faceInfoList: List<FacePreviewInfo>
  ) {
    for (previewInfo in faceInfoList) {
      if (judgeFaceSize(previewInfo).not()) {
        continue
      }
      if (previewInfo.recognizeAreaValid.not()) {
        //Not in the recognition area, skip
        continue
      }
      if (configuration.enableMask && previewInfo.mask == MaskInfo.UNKNOWN) {
        //Skip faces with mask value of MaskInfo.UNKNOWN
        continue
      }

      val recognizeInfo = getRecognizeInfo(previewInfo.faceId)

      if (configuration.livenessType != LivenessType.NONE
        && recognizeInfo.recognizeStatus != RecognizeStatus.SUCCEED
      ) {
        val liveness = recognizeInfo.liveness
        if (liveness != LivenessInfo.ALIVE
          && liveness != LivenessInfo.NOT_ALIVE
          && liveness != RequestLivenessStatus.ANALYZING
          && liveness != RequestLivenessStatus.FAILED
        ) {
          //Start the Liveness detection thread
          changeLiveness(previewInfo.faceId, RequestLivenessStatus.ANALYZING)
          requestFaceDetectInfo(rgbNV21, irNV21, width, height, previewInfo)
        }
      }
      if (recognizeInfo.recognizeStatus == RecognizeStatus.TO_RETRY) {
        //Start Face Feature Extraction Thread
        changeRecognizeStatus(previewInfo.faceId, RecognizeStatus.SEARCHING)
        requestFaceFeature(
          rgbNV21,
          width,
          height,
          previewInfo
        )
      }
    }
  }

  /**
   * Determine if the face size exceeds the recognizable limit
   * @return true:over continue ；false:less than threshold ignore
   */
  private fun judgeFaceSize(previewInfo: FacePreviewInfo): Boolean {
    val rect = previewInfo.faceInfoRgb.rect
    // Because the width and height of the current face frame are close to the same
    return rect.width() >= configuration.faceSizeLimit && rect.height() >= configuration.faceSizeLimit
  }

  /**
   * Error Capture
   */
  private fun onError(type: FaceErrorType, errorCode: Int, errorMessage: String) {
    LogUtil.e("${TAG}onError: FaceErrorType:${type},errorCode:${errorCode},errorMessage:${errorMessage}")
    configuration.onErrorCallback?.onError(type, errorCode, errorMessage)
  }

  private fun requestFaceDetectInfo(
    rgbNV21: ByteArray,
    irNV21: ByteArray?,
    width: Int,
    height: Int,
    info: FacePreviewInfo
  ) {
    if (detectInfoThreadQueue.remainingCapacity() <= 0) {
      LogUtil.w("requestFaceDetectInfo overrun")
      changeMsg(info.faceId, "DetectInfoThread is full.")
      return
    }
    detectInfoExecutor.execute(
      FaceDetectInfoRunnable(
        rgbNV21,
        irNV21,
        width,
        height,
        info.faceId,
        info.faceInfoRgb,
        info.faceInfoIr
      )
    )
  }

  private fun requestFaceFeature(
    rgbNV21: ByteArray,
    width: Int,
    height: Int,
    info: FacePreviewInfo
  ) {
    if (extractFeatureThreadQueue.remainingCapacity() <= 0) {
      LogUtil.w("requestFaceFeature overrun")
      changeMsg(info.faceId, "FeatureThread is full.")
      return
    }
    extractFeatureExecutor.execute(
      ExtractFeatureRunnable(
        rgbNV21,
        width,
        height,
        info.faceId,
        info.faceInfoRgb,
        info.mask
      )
    )
  }

  /**
   * request after check action
   */
  private fun onFaceLivenessInfoGet(livenessInfo: LivenessInfo?, faceId: Int, errorCode: Int) {
    if (recognizeInfoMap.containsKey(faceId).not()) {
      //The face has left, no need to deal with it
      LogUtil.w("${TAG}onFaceLivenessInfoGet-faceId:${faceId} Already left")
      return
    }
    val recognizeInfo = getRecognizeInfo(faceId)

    if (livenessInfo != null) {
      changeLiveness(faceId, livenessInfo.liveness)
      when (livenessInfo.liveness) {
        LivenessInfo.ALIVE -> {
          synchronized(recognizeInfo.lock) {
            recognizeInfo.lock.notifyAll()
          }
        }
        LivenessInfo.NOT_ALIVE -> {
          changeMsg(faceId, "NOT_ALIVE")
          retryLivenessDetectDelayed(faceId)
        }
        else -> {
          changeMsg(faceId, "LIVENESS FAILED:${livenessInfo.liveness}")
          //Continue liveness testing
          LogUtil.w(
            "${TAG}onFaceLivenessInfoGet-liveness:${livenessInfo.liveness},faceId:$faceId,${
              FaceConstant.getLivenessErrorMsg(
                livenessInfo.liveness
              )
            }"
          )
        }
      }
    } else {
      changeMsg(faceId, "ProcessFailed:${errorCode},faceId:${faceId}")
      if (recognizeInfo.increaseAndGetLivenessErrorRetryCount() > configuration.livenessErrorRetryCount) {
        //If try maximum time still fails will considered a failure
        recognizeInfo.resetLivenessErrorRetryCount()
        changeLiveness(faceId, RequestLivenessStatus.FAILED)
        synchronized(recognizeInfo.lock) {
          recognizeInfo.lock.notifyAll()
        }
        retryLivenessDetectDelayed(faceId)
      } else {
        changeLiveness(faceId, LivenessInfo.UNKNOWN)
      }
    }
  }

  /**
   * Actions after requesting facial features
   * @param faceFeature faceFeature info
   * @param faceId faceId
   */
  private fun onFaceFeatureInfoGet(
    faceFeature: FaceFeature?,
    faceId: Int,
    errorCode: Int,
    nv21: ByteArray,
    width: Int,
    height: Int,
    mask: Int,
    isImageQualityDetect: Boolean
  ) {
    if (recognizeInfoMap.containsKey(faceId).not()) {
      //The face has left, no need to deal with it
      LogUtil.w("${TAG}onFaceFeatureInfoGet-faceId:${faceId}Already left")
      return
    }
    val recognizeInfo = getRecognizeInfo(faceId)
    when {
      faceFeature == null -> {
        if (isImageQualityDetect) {
          if (errorCode == ErrorInfo.MOK) {
            changeMsg(faceId, "ImageQuality too low,faceId:${faceId}")
          } else {
            changeMsg(faceId, "ImageDetectFailed:${errorCode},faceId:${faceId}")
          }
        } else {
          changeMsg(faceId, "ExtractFailed:${errorCode},faceId:${faceId}")
        }
        if (recognizeInfo.increaseAndGetExtractFeatureErrorRetryCount() > configuration.extractFeatureErrorRetryCount) {
          //If try maximum time still fails will considered a failure
          recognizeInfo.resetExtractFeatureErrorRetryCount()
          changeRecognizeStatus(faceId, RecognizeStatus.FAILED)
          retryRecognizeDelayed(faceId)
        } else {
          changeRecognizeStatus(faceId, RecognizeStatus.TO_RETRY)
        }
      }
      configuration.livenessType == LivenessType.NONE || recognizeInfo.liveness == LivenessInfo.ALIVE -> {
        recognizeInfo.mask = mask
        searchFace(faceFeature, faceId, nv21, width, height)
      }
      recognizeInfo.liveness == RequestLivenessStatus.FAILED -> {
        //RecognizeStatus fail
        changeRecognizeStatus(faceId, RecognizeStatus.FAILED)
      }
      else -> {
        synchronized(recognizeInfo.lock) {
          try {
            recognizeInfo.lock.wait()
            //Avoid the middle of the comparison, the face left
            onFaceFeatureInfoGet(
              faceFeature, faceId, errorCode, nv21, width, height,
              mask, isImageQualityDetect
            )
          } catch (e: InterruptedException) {
            LogUtil.w("${TAG}onFaceFeatureInfoGet:app paused when waiting result")
          }
        }
      }
    }
  }

  /**
   * Search the Face
   */
  private fun searchFace(
    faceFeature: FaceFeature,
    faceId: Int,
    nv21: ByteArray,
    width: Int,
    height: Int
  ) {
    val callback = configuration.recognizeCallback ?: let {
      changeRecognizeStatus(faceId, RecognizeStatus.FAILED)
      changeMsg(faceId, "Visitor,$faceId")
      return
    }
    if (configuration.enableCompareFace.not()) {
      callback.onGetFaceFeature(
        faceId, faceFeature.featureData,
        getRecognizeInfo(faceId), nv21, width, height
      )
      changeRecognizeStatus(faceId, RecognizeStatus.SUCCEED)
      changeMsg(faceId, "Visitor,$faceId")
      return
    }
    val compareResult =
      faceServer.compareFaceFeature(faceFeature, callback.faceFeatureList()) ?: let {
        changeRecognizeStatus(faceId, RecognizeStatus.FAILED)
        changeMsg(faceId, "Visitor,$faceId")
        return
      }
    if (compareResult.similar >= callback.similarThreshold()) {
      changeRecognizeStatus(faceId, RecognizeStatus.SUCCEED)
      val msg =
        callback.onRecognized(
          compareResult.bean, compareResult.similar,
          getRecognizeInfo(faceId), nv21, width, height
        )
      changeMsg(faceId, msg)
    } else {
      retryRecognizeDelayed(faceId)
      changeMsg(faceId, "Not Pass：NOT_REGISTERED")
    }
  }

  private fun changeRecognizeStatus(faceId: Int, recognizeStatus: RecognizeStatus) {
    if (recognizeInfoMap.containsKey(faceId)) {
      getRecognizeInfo(faceId).recognizeStatus = recognizeStatus
    }
  }

  private fun changeLiveness(faceId: Int, liveness: Int) {
    if (recognizeInfoMap.containsKey(faceId)) {
      getRecognizeInfo(faceId).liveness = liveness
    }
  }

  private fun changeMsg(faceId: Int, msg: String?) {
    if (recognizeInfoMap.containsKey(faceId)) {
      getRecognizeInfo(faceId).msg = msg
    }
  }

  fun retryLivenessDetectDelayed(faceId: Int) {
    mHandler.postDelayed({
      changeMsg(faceId, "$faceId")
      changeLiveness(faceId, LivenessInfo.UNKNOWN)
    }, configuration.livenessFailedRetryInterval)
  }

  fun retryRecognizeDelayed(faceId: Int) {
    changeRecognizeStatus(faceId, RecognizeStatus.FAILED)
    mHandler.postDelayed({
      changeMsg(faceId, "$faceId")
      changeRecognizeStatus(faceId, RecognizeStatus.TO_RETRY)
    }, configuration.recognizeFailedRetryInterval)
  }


  /**
   * Face information detection thread
   * Including age, gender, 3d angle, live
   */
  inner class FaceDetectInfoRunnable(
    private val rgbNV21: ByteArray,
    private val irNV21: ByteArray?,
    private val width: Int,
    private val height: Int,
    private val faceId: Int,
    private val rgbFaceInfo: FaceInfo,
    private val irFaceInfo: FaceInfo
  ) : Runnable {

    override fun run() {
      //Liveness test results
      var livenessResult: Int
      //Liveness detection
      val livenessList: MutableList<LivenessInfo> = mutableListOf()
      synchronized(detectInfoEngine) {
        if (configuration.livenessType == LivenessType.RGB) {
          //Only takes effect during RGB liveness detection
          //Only support 4 face
          val processResult = detectInfoEngine.process(
            rgbNV21,
            width,
            height,
            FaceEngine.CP_PAF_NV21,
            listOf(rgbFaceInfo),
            detectInfoMask
          )
          if (processResult == ErrorInfo.MOK) {
            val recognizeInfo = getRecognizeInfo(faceId)
            /*if (configuration.detectInfo.age) {
              //get age
              val list: MutableList<AgeInfo> = mutableListOf()
              val ageResult = detectInfoEngine.getAge(list)
              if (ageResult == ErrorInfo.MOK && list.isNotEmpty()) {
                recognizeInfo.age = list[0].age
              } else {
                onError(
                  FaceErrorType.DETECT_AGE,
                  ageResult,
                  FaceConstant.getFaceErrorMsg(ageResult)
                )
              }
            }
            if (configuration.detectInfo.gender) {
              //get gender
              val list: MutableList<GenderInfo> = mutableListOf()
              val genderResult = detectInfoEngine.getGender(list)
              if (genderResult == ErrorInfo.MOK && list.isNotEmpty()) {
                recognizeInfo.gender = list[0].gender
              } else {
                onError(
                  FaceErrorType.DETECT_GENDER,
                  genderResult,
                  FaceConstant.getFaceErrorMsg(genderResult)
                )
              }
            }
            if (configuration.detectInfo.angle) {
              //get angle
              val list: MutableList<Face3DAngle> = mutableListOf()
              val angleResult = detectInfoEngine.getFace3DAngle(list)
              if (angleResult == ErrorInfo.MOK && list.isNotEmpty()) {
                recognizeInfo.angle = list[0]
              } else {
                onError(
                  FaceErrorType.DETECT_ANGLE,
                  angleResult,
                  FaceConstant.getFaceErrorMsg(angleResult)
                )
              }
            }*/
            livenessResult = detectInfoEngine.getLiveness(livenessList)
          } else {
            livenessResult = processResult
          }
        } else {
          //This interface only supports single-face IR liveness detection
          val processResult = detectInfoEngine.processIr(
            irNV21,
            width,
            height,
            FaceEngine.CP_PAF_NV21,
            listOf(irFaceInfo),
            detectInfoMask
          )
          livenessResult = if (processResult == ErrorInfo.MOK) {
            detectInfoEngine.getIrLiveness(livenessList)
          } else {
            processResult
          }
        }
      }
      if (livenessResult == ErrorInfo.MOK && livenessList.isNotEmpty()) {
        onFaceLivenessInfoGet(livenessList[0], faceId, livenessResult)
      } else {
        onFaceLivenessInfoGet(null, faceId, livenessResult)
        onError(
          FaceErrorType.DETECT_LIVENESS,
          livenessResult,
          FaceConstant.getFaceErrorMsg(livenessResult)
        )
      }
    }

  }

  /**
   * face feature extraction thread
   */
  inner class ExtractFeatureRunnable(
    private val rgbNV21: ByteArray,
    private val width: Int,
    private val height: Int,
    private val faceId: Int,
    private val faceInfo: FaceInfo,
    private val mask: Int
  ) : Runnable {

    override fun run() {
      if (configuration.enableImageQuality) {
        //Enable image quality detection
        val qualitySimilar = ImageQualitySimilar()
        val result: Int
        synchronized(extractFeatureEngine) {
          result = extractFeatureEngine.imageQualityDetect(
            rgbNV21,
            width,
            height,
            FaceEngine.CP_PAF_NV21,
            faceInfo,
            mask,
            qualitySimilar
          )
        }
        if (result == ErrorInfo.MOK) {
          val score = qualitySimilar.score
          val threshold = if (mask == MaskInfo.WORN) {
            configuration.imageQualityMaskRecognizeThreshold
          } else {
            configuration.imageQualityNoMaskRecognizeThreshold
          }
          if (score >= threshold) {
            extractFaceFeature()
          } else {
            onFaceFeatureInfoGet(
              null,
              faceId,
              result,
              rgbNV21,
              width,
              height,
              mask,
              true
            )
            onError(
              FaceErrorType.IMAGE_QUALITY,
              result,
              "imageQuality score too low"
            )
          }
        } else {
          onFaceFeatureInfoGet(null, faceId, result, rgbNV21, width, height, mask, true)
          onError(
            FaceErrorType.IMAGE_QUALITY,
            result,
            FaceConstant.getFaceErrorMsg(result)
          )
        }
      } else {
        extractFaceFeature()
      }
    }

    /**
     * Extract facial features
     */
    private fun extractFaceFeature() {
      val faceFeature = FaceFeature()
      val result: Int
      synchronized(extractFeatureEngine) {
        result = extractFeatureEngine.extractFaceFeature(
          rgbNV21,
          width,
          height,
          FaceEngine.CP_PAF_NV21,
          faceInfo,
          if (configuration.enableCompareFace) {
            ExtractType.RECOGNIZE
          } else {
            ExtractType.REGISTER
          },
          mask,
          faceFeature
        )
      }
      if (result == ErrorInfo.MOK) {
        onFaceFeatureInfoGet(
          faceFeature,
          faceId,
          result,
          rgbNV21,
          width,
          height,
          mask,
          false
        )
      } else {
        onFaceFeatureInfoGet(null, faceId, result, rgbNV21, width, height, mask, false)
        onError(
          FaceErrorType.EXTRACT_FEATURE,
          result,
          FaceConstant.getFaceErrorMsg(result)
        )
      }
    }
  }
}