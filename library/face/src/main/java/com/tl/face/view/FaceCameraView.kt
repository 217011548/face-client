package com.tl.face.view

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.arcsoft.face.LivenessInfo
import com.tl.face.R
import com.tl.face.callback.OnCameraListener
import com.tl.face.callback.OnPreviewCallback
import com.tl.face.configuration.CameraFacing
import com.tl.face.configuration.FaceConfiguration
import com.tl.face.configuration.LivenessType
import com.tl.face.configuration.PreviewSize
import com.tl.face.face.FaceHelper
import com.tl.face.face.model.FacePreviewInfo
import com.tl.face.constant.RecognizeStatus
import com.tl.face.face.FaceActive
import com.tl.face.utils.LogUtil
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.parameter.Resolution
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.preview.Frame
import io.fotoapparat.preview.FrameProcessor
import io.fotoapparat.selector.*
import io.fotoapparat.view.CameraView
import com.tl.face.constant.FaceConstant
import com.tl.face.face.FaceDetect

/**
 * 人脸识别CameraView，支持预览RGB、IR摄像头预览画面
 * RGB摄像头铺满父布局
 * IR摄像头等比缩放[FaceConstant.DEFAULT_ZOOM_RATIO]排放在父布局左下角
 *
 * @author  ShenBen
 * @date    2021/02/05 15:16
 * @email   714081644@qq.com
 */
class FaceCameraView @JvmOverloads constructor(
  context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), LifecycleObserver, OnPreviewCallback {

  companion object {
    private const val TAG = "FaceCameraView->"
  }

  private val rgbCameraView: CameraView
  private lateinit var rgbCameraConfiguration: CameraConfiguration
  private lateinit var rgbFotoapparat: Fotoapparat
  private val rgbFaceRectView: FaceRectView

  private val viewfinderView: ViewfinderView

  private lateinit var mFaceConfiguration: FaceConfiguration
  private lateinit var faceHelper: FaceHelper

  private var mLifecycle: Lifecycle? = null

  /**
   * 是否启用人脸
   */
  @Volatile
  private var enableFace: Boolean = false

  /**
   * 启用黑白摄像头检测人脸，用于夜间模式
   */
  @Volatile
  private var enableIrDetectFaces: Boolean = false

  private var cameraListener: OnCameraListener? = null

  /**
   * 人脸检测
   */
  private val faceDetect = FaceDetect()

  init {
    LayoutInflater.from(context).inflate(R.layout.camera_preview, this)
    rgbFaceRectView = findViewById(R.id.rgbFaceRectView)
    viewfinderView = findViewById(R.id.viewfinderView)
    rgbCameraView = findViewById(R.id.rgbCameraView)
    rgbCameraView.setScaleType(ScaleType.CenterCrop)
  }

  fun setOnCameraListener(listener: OnCameraListener) {
    cameraListener = listener
  }

  /**
   * 必须要设置配置信息参数
   *
   * @param configuration 参数配置
   * @param autoInitFace 是否自动初始化[FaceHelper]，如果sdk未激活则不初始化[FaceHelper]
   * @param initIrDetectFace 初始化人脸检测，用于红外摄像头
   */
  fun setConfiguration(
    configuration: FaceConfiguration,
    autoInitFace: Boolean = false,
    initIrDetectFace: Boolean = false
  ) {
    rgbCameraView.isMirror(configuration.isRgbMirror)
    rgbCameraView.rotation = 180F
    rgbCameraConfiguration = rgbCameraConfiguration(configuration.previewSize)
    rgbFotoapparat = Fotoapparat(
      context,
      rgbCameraView,
      lensPosition = getCameraFacing(configuration.rgbCameraFcing),
      cameraConfiguration = rgbCameraConfiguration,
      cameraErrorCallback = {
        LogUtil.e("RGB摄像头开启出错：${it.message}")
        cameraListener?.onRgbCameraError(it)
      }
    )
    rgbFaceRectView.visibility = if (configuration.drawFaceRect.isDraw) VISIBLE else INVISIBLE
    //TODO 此处修改是否显示扫描框
    viewfinderView.visibility =
      if (configuration.enableRecognizeAreaLimited) VISIBLE else INVISIBLE
    viewfinderView.setFrameRatio(configuration.recognizeAreaLimitedRatio)
    mFaceConfiguration = configuration

    if (initIrDetectFace) {
      faceDetect.init(
        context,
        configuration.enableImageQuality,
        1,
        configuration.detectFaceOrient
      )
      faceDetect.setFaceDetectCallback(
        someone = {
          mFaceConfiguration.recognizeCallback?.someone()
        },
        nobody = {
          mFaceConfiguration.recognizeCallback?.nobody()
        },
        notInTheArea = {
          mFaceConfiguration.recognizeCallback?.notInTheArea()
        },
        detectFaceNum = { num, faceIds ->
          mFaceConfiguration.recognizeCallback?.detectFaceNum(num, faceIds)
        },
        detectionFace = { nav21, maskInfo, width, height ->
          mFaceConfiguration.recognizeCallback?.detectionFace(nav21, maskInfo, width, height)
        }
      )
    }
    if (autoInitFace) {
      enableFace = initFaceHelper()
    }
  }

  /**
   * 是否启用人脸，传入摄像头预览数据
   * 调用在[setConfiguration]之后
   */
  fun enableFace(enableFace: Boolean) {
    if (this.enableFace == enableFace) {
      return
    }
    if (enableFace) {
      if (this::faceHelper.isInitialized.not()) {
        val result = initFaceHelper()
        if (result.not()) {
          return
        }
      }
    }
    this.enableFace = enableFace
  }

  fun setLifecycleOwner(owner: LifecycleOwner?) {
    clearLifecycleObserver()
    owner?.let {
      mLifecycle = owner.lifecycle.apply { addObserver(this@FaceCameraView) }
    }
  }

  /**
   * 手动重新识别
   * @param faceId 人脸id
   */
  fun retryRecognizeDelayed(faceId: Int) {
    if (this::faceHelper.isInitialized) {
      faceHelper.retryRecognizeDelayed(faceId)
    }
  }

  /**
   * 手动重新活体检测
   * @param faceId 人脸id
   */
  fun retryLivenessDetectDelayed(faceId: Int) {
    if (this::faceHelper.isInitialized) {
      faceHelper.retryLivenessDetectDelayed(faceId)
    }
  }

  fun pauseDetectionFace(pause: Boolean) {
    faceHelper?.changedDetectionStatus(pause)
  }

  private fun initFaceHelper(): Boolean {
    return if (FaceActive.isActivated(context)) {
      destroyFace()
      faceHelper = FaceHelper(mFaceConfiguration, this)
      true
    } else {
      LogUtil.e("${TAG}initFaceHelper-人脸识别未激活")
      false
    }
  }

  private fun clearLifecycleObserver() {
    mLifecycle?.removeObserver(this)
    mLifecycle = null
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
  fun start() {
    if (this::rgbFotoapparat.isInitialized) {
      rgbFotoapparat.start()
    }
  }


  @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
  fun stop() {
    if (this::rgbFotoapparat.isInitialized) {
      rgbFotoapparat.stop()
    }
  }


  @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
  fun destroy() {
    cameraListener = null
    faceDetect.destroy()
    destroyFace()
    rgbFaceRectView.clearFaceInfo()
    clearLifecycleObserver()
  }

  private fun destroyFace() {
    if (this::faceHelper.isInitialized) {
      faceHelper.destroy()
    }
  }

  private fun getCameraFacing(facing: CameraFacing): LensPositionSelector {
    return when (facing) {
      CameraFacing.BACK -> {
        back()
      }
      CameraFacing.FRONT -> {
        front()
      }
    }
  }

  /**
   * 获取识别限制区域
   */
  override fun getRecognizeAreaRect(): Rect {
    return if (mFaceConfiguration.enableRecognizeAreaLimited) {
      viewfinderView.getFrameRect()
    } else {
      Rect(
        0,
        0,
        if (width == 0) Int.MAX_VALUE else width,
        if (height == 0) Int.MAX_VALUE else height
      )
    }
  }

  override fun onPreviewFaceInfo(previewInfoList: List<FacePreviewInfo>) {
    if (mFaceConfiguration.drawFaceRect.isDraw) {
      val rgbList = mutableListOf<FaceRectView.DrawInfo>()
      val irList = mutableListOf<FaceRectView.DrawInfo>()
      for (previewInfo in previewInfoList) {
        val recognizeInfo = faceHelper.getRecognizeInfo(previewInfo.faceId)
        var color: Int = mFaceConfiguration.drawFaceRect.unknownColor
        when {
          recognizeInfo.recognizeStatus == RecognizeStatus.SUCCEED -> {
            color = mFaceConfiguration.drawFaceRect.successColor
          }
          recognizeInfo.recognizeStatus == RecognizeStatus.FAILED
            || recognizeInfo.liveness == LivenessInfo.NOT_ALIVE -> {
            color = mFaceConfiguration.drawFaceRect.failedColor
          }

        }
        val msg = recognizeInfo.msg ?: previewInfo.faceId.toString()
        rgbList.add(
          FaceRectView.DrawInfo(
            previewInfo.rgbTransformedRect,
            recognizeInfo.gender,
            recognizeInfo.age,
            recognizeInfo.liveness,
            msg,
            color
          )
        )
        if (mFaceConfiguration.livenessType == LivenessType.IR) {
          irList.add(
            FaceRectView.DrawInfo(
              previewInfo.irTransformedRect,
              recognizeInfo.gender,
              recognizeInfo.age,
              recognizeInfo.liveness,
              null,
              color
            )
          )
        }
      }
      rgbFaceRectView.drawRealtimeFaceInfo(rgbList)
    }
  }

  override fun someone() {
    if (enableIrDetectFaces.not()) {
      mFaceConfiguration.recognizeCallback?.someone()
    }
  }

  override fun nobody() {
    if (enableIrDetectFaces.not()) {
      mFaceConfiguration.recognizeCallback?.nobody()
    }
  }

  override fun notInTheArea() {
    if (enableIrDetectFaces.not()) {
      mFaceConfiguration.recognizeCallback?.notInTheArea()
    }
  }

  override fun detectFaceNum(num: Int, faceIds: List<Int>) {
    if (enableIrDetectFaces.not()) {
      mFaceConfiguration.recognizeCallback?.detectFaceNum(num, faceIds)
    }
  }

  override fun detectionFace(nv21: ByteArray, maskInfo: Int, width: Int, height: Int) {
    if (enableIrDetectFaces.not()) {
      mFaceConfiguration.recognizeCallback?.detectionFace(nv21, maskInfo, width, height)
    }
  }

  private fun rgbCameraConfiguration(previewSize: PreviewSize?): CameraConfiguration {
    val previewResolution: ResolutionSelector = if (previewSize != null) {
      firstAvailable(
        { Resolution(previewSize.width, previewSize.height) },
        highestResolution()
      )
    } else {
      highestResolution()
    }
    return CameraConfiguration.builder()
      .previewResolution(previewResolution)
      .frameProcessor(object : FrameProcessor {
        override fun process(frame: Frame) {
          if (enableFace.not()) {
            return
          }
          faceHelper.onPreviewFrame(
            frame.image,
            frame.size.width,
            frame.size.height,
            rgbCameraView.width,
            rgbCameraView.height
          )
        }
      })
      .build()
  }
}

