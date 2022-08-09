package com.tl.face.configuration

import android.content.Context
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import com.arcsoft.face.enums.DetectFaceOrientPriority
import com.tl.face.callback.OnErrorCallback
import com.tl.face.callback.OnRecognizeCallback
import com.tl.face.view.ViewfinderView
import io.fotoapparat.configuration.CameraConfiguration

/**
 * FaceConfiguration
 *
 * @author  ShenBen
 * @date    2021/02/05 15:25
 * @email   714081644@qq.com
 */
class FaceConfiguration internal constructor(builder: Builder) {

  companion object {

    @JvmStatic
    fun builder(context: Context, recognizeCallback: OnRecognizeCallback?): Builder =
      Builder(context, recognizeCallback)

    /**
     * Default
     */
    @JvmStatic
    fun default(context: Context, recognizeCallback: OnRecognizeCallback?): FaceConfiguration =
      builder(context, recognizeCallback).build()
  }

  val context: Context = builder.context

  val recognizeCallback: OnRecognizeCallback? = builder.recognizeCallback

  /**
   * Detect Orientation
   */
  val detectFaceOrient: DetectFaceOrientPriority = builder.detectFaceOrient

  /**
   * start recognize
   */
  val enableRecognize: Boolean = builder.enableRecognize

  /**
   * livenessType
   */
  val livenessType: LivenessType = builder.livenessType

  /**
   * Set RGB RGB float，prefer 0.6f
   */
  val rgbLivenessThreshold: Float = builder.rgbLivenessThreshold

  /**
   * RGB Live face quality confidence，prefer 0.6f
   */
  val fqLivenessThreshold: Float = builder.fqLivenessThreshold

  /**
   * Set the IR infrared living threshold, the valid value range (0.0f, 1.0f), the recommended value is 0.7f
   */
  val irLivenessThreshold: Float = builder.irLivenessThreshold

  /**
   * Whether to enable image quality threshold
   */
  val enableImageQuality: Boolean = builder.enableImageQuality

  /**
   * The maximum number of faces to be detected, the value range is [1,10]
   */
  val detectFaceMaxNum: Int = builder.detectFaceMaxNum

  /**
   * Whether to recognize only the largest face
   */
  val recognizeKeepMaxFace: Boolean = builder.recognizeKeepMaxFace

  /**
   * Whether to limit the recognition area
   */
  val enableRecognizeAreaLimited: Boolean = builder.enableRecognizeAreaLimited

  /**
   * The screen ratio of the recognition area, the default is in the middle of the camera preview screen, the valid value range (0.0f, 1.0f)
   */
  val recognizeAreaLimitedRatio: Float = builder.recognizeAreaLimitedRatio

  /**
   * Related attribute detection, age, gender, 3d angle
   */
  val detectInfo: DetectInfo = builder.detectInfo

  /**
   * Color RGB camera type
   */
  val rgbCameraFcing: CameraFacing = builder.rgbCameraFcing

  /**
   * Color IR camera type
   */
  val irCameraFcing: CameraFacing = builder.irCameraFcing

  /**
   * Camera preview resolution, automatically calculated when null
   * preview size[rgbCameraFcing] [irCameraFcing]
   */
  val previewSize: PreviewSize? = builder.previewSize

  /**
   * drawFaceRect
   */
  val drawFaceRect: DrawFaceRect = builder.drawFaceRect

  /**
   * isRgbMirror
   */
  val isRgbMirror: Boolean = builder.isRgbMirror

  /**
   * isIrMirror
   */
  val isIrMirror: Boolean = builder.isIrMirror

  /**
   * Face Feature Extraction Error Retry Times
   */
  @IntRange(from = 1)
  val extractFeatureErrorRetryCount: Int = builder.extractFeatureErrorRetryCount

  /**
   * After face recognition fails, retry interval, unit: milliseconds
   */
  @IntRange(from = 1)
  val recognizeFailedRetryInterval: Long = builder.recognizeFailedRetryInterval

  /**
   * Body detection error retries
   */
  @IntRange(from = 1)
  val livenessErrorRetryCount: Int = builder.livenessErrorRetryCount

  /**
   * After the liveness detection fails, the retry interval, in milliseconds
   */
  @IntRange(from = 1)
  val livenessFailedRetryInterval: Long = builder.livenessFailedRetryInterval

  /**
   * Whether to enable face comparison, use registration mode if face comparison is not enabled, note: no mask is required; enable face comparison and use comparison mode
   * @see OnRecognizeCallback.onGetFaceFeature
   */
  val enableCompareFace: Boolean = builder.enableCompareFace

  /**
   * Whether to enable mask recognition
   */
  val enableMask: Boolean = builder.enableMask

  /**
   * face size limit
   * The recognition operation will only be performed when the face size exceeds this value.
   * Since the width and height of the current face frame are close to the same, it is approximately a square
   */
  val faceSizeLimit: Int = builder.faceSizeLimit

  /**
   * Image quality detection threshold: suitable for face recognition scenarios without masks
   */
  val imageQualityNoMaskRecognizeThreshold: Float = builder.imageQualityNoMaskRecognizeThreshold

  /**
   * Image quality detection threshold: suitable for wearing masks and face recognition scenarios
   */
  val imageQualityMaskRecognizeThreshold: Float = builder.imageQualityMaskRecognizeThreshold

  /**
   * Exception callback during face recognition
   */
  val onErrorCallback: OnErrorCallback? = builder.onErrorCallback

  class Builder internal constructor(
    internal val context: Context,
    val recognizeCallback: OnRecognizeCallback?
  ) {
    /**
     * face detection angle
     */
    internal var detectFaceOrient: DetectFaceOrientPriority =
      DetectFaceOrientPriority.ASF_OP_0_ONLY

    fun setDetectFaceOrient(detectFaceOrient: DetectFaceOrientPriority) =
      apply { this.detectFaceOrient = detectFaceOrient }

    /**
     * Does it need to be identified
     */
    internal var enableRecognize = true
    fun enableRecognize(enableRecognize: Boolean) =
      apply { this.enableRecognize = enableRecognize }

    /**
     * Type of liveness detection
     */
    internal var livenessType: LivenessType = LivenessType.NONE
    fun setLivenessType(livenessType: LivenessType) =
      apply { this.livenessType = livenessType }

    /**
     * Set the RGB visible light in vivo threshold, the valid value range (0.0f, 1.0f), the recommended value is 0.6f
     */
    internal var rgbLivenessThreshold: Float = 0.6f
    fun setRgbLivenessThreshold(
      @FloatRange(
        from = 0.0,
        to = 1.0,
        fromInclusive = false,
        toInclusive = false
      ) rgbLivenessThreshold: Float
    ) =
      apply { this.rgbLivenessThreshold = rgbLivenessThreshold }


    /**
     * Set the RGB visible light in vivo threshold, the valid value range (0.0f, 1.0f), the recommended value is 0.6f
     */
    internal var fqLivenessThreshold: Float = 0.6f
    fun setFqLivenessThreshold(
      @FloatRange(
        from = 0.0,
        to = 1.0,
        fromInclusive = false,
        toInclusive = false
      ) fqLivenessThreshold: Float
    ) =
      apply { this.fqLivenessThreshold = fqLivenessThreshold }

    /**
     * Set the IR infrared living threshold, the valid value range (0.0f, 1.0f), the recommended value is 0.7f
     */
    internal var irLivenessThreshold: Float = 0.7f
    fun setIrLivenessThreshold(
      @FloatRange(
        from = 0.0,
        to = 1.0,
        fromInclusive = false,
        toInclusive = false
      ) irLivenessThreshold: Float
    ) =
      apply { this.irLivenessThreshold = irLivenessThreshold }

    /**
     * Whether to enable image quality detection
     */
    internal var enableImageQuality = false
    fun enableImageQuality(
      enableImageQuality: Boolean
    ) =
      apply { this.enableImageQuality = enableImageQuality }

    /**
     * The maximum number of faces to be detected, the value range is [1,10]
     */
    internal var detectFaceMaxNum = 1
    fun setDetectFaceMaxNum(@IntRange(from = 1, to = 10) detectFaceMaxNum: Int) =
      apply { this.detectFaceMaxNum = detectFaceMaxNum }

    /**
     * Whether to recognize only the largest face
     */
    internal var recognizeKeepMaxFace = true
    fun recognizeKeepMaxFace(recognizeKeepMaxFace: Boolean) =
      apply { this.recognizeKeepMaxFace = recognizeKeepMaxFace }

    /**
     * Whether to limit the recognition area
     */
    internal var enableRecognizeAreaLimited = false
    fun enableRecognizeAreaLimited(enableRecognizeAreaLimited: Boolean) =
      apply { this.enableRecognizeAreaLimited = enableRecognizeAreaLimited }

    /**
     * The screen ratio of the recognition area, the default is in the middle of the camera preview screen, the valid value range (0.0f, 1.0f)
     * The restricted area is a square
     */
    internal var recognizeAreaLimitedRatio = 0.625f
    fun setRecognizeAreaLimitedRatio(
      @FloatRange(
        from = 0.0,
        to = 1.0,
        fromInclusive = false,
        toInclusive = false
      ) recognizeAreaLimitedRatio: Float
    ) =
      apply { this.recognizeAreaLimitedRatio = recognizeAreaLimitedRatio }

    /**
     * Related attribute detection, age, gender, 3d angle
     * This function is attached to [livenessType]，need to[LivenessType.RGB]
     */
    internal var detectInfo: DetectInfo = DetectInfo()
    fun setDetectInfo(detectInfo: DetectInfo) =
      apply {
        this.detectInfo = detectInfo
      }

    /**
     * Color RGB camera type
     */
    internal var rgbCameraFcing = CameraFacing.BACK
    fun setRgbCameraFcing(rgbCameraFcing: CameraFacing) =
      apply { this.rgbCameraFcing = rgbCameraFcing }

    /**
     * Color IR camera type
     */
    internal var irCameraFcing = CameraFacing.FRONT
    fun setIrCameraFcing(irCameraFcing: CameraFacing) =
      apply { this.irCameraFcing = irCameraFcing }

    /**
     * Camera preview resolution, automatically calculated when null,
     * Maybe[rgbCameraFcing] [irCameraFcing] The resolution of the preview is inconsistent, resulting in abnormal IR liveness detection
     * <p>Best to set manually</p>
     * The preview resolution is[rgbCameraFcing] [irCameraFcing]
     */
    internal var previewSize: PreviewSize? = null
    fun setPreviewSize(previewSize: PreviewSize?) = apply { this.previewSize = previewSize }

    /**
     * Face recognition frame drawing related
     */
    internal var drawFaceRect: DrawFaceRect = DrawFaceRect()
    fun setDrawFaceRect(drawFaceRect: DrawFaceRect) = apply {
      this.drawFaceRect = drawFaceRect
    }

    /**
     * Whether RGB is mirrored preview
     */
    internal var isRgbMirror = false
    fun isRgbMirror(isRgbMirror: Boolean) = apply {
      this.isRgbMirror = isRgbMirror
    }

    /**
     * Whether the IR mirrors the preview
     */
    internal var isIrMirror = false
    fun isIrMirror(isIrMirror: Boolean) = apply {
      this.isIrMirror = isIrMirror
    }

    /**
     * Face Feature Extraction Error Retry Times
     */
    @IntRange(from = 1)
    internal var extractFeatureErrorRetryCount = 3
    fun setExtractFeatureErrorRetryCount(@IntRange(from = 1) extractFeatureErrorRetryCount: Int) =
      apply {
        this.extractFeatureErrorRetryCount = extractFeatureErrorRetryCount
      }

    /**
     * After face recognition fails, retry interval, unit: milliseconds
     */
    @IntRange(from = 1)
    internal var recognizeFailedRetryInterval: Long = 1000
    fun setRecognizeFailedRetryInterval(@IntRange(from = 1) recognizeFailedRetryInterval: Long) =
      apply {
        this.recognizeFailedRetryInterval = recognizeFailedRetryInterval
      }

    /**
     * Body detection error retries
     */
    @IntRange(from = 1)
    internal var livenessErrorRetryCount = 3
    fun setLivenessErrorRetryCount(@IntRange(from = 1) livenessErrorRetryCount: Int) = apply {
      this.livenessErrorRetryCount = livenessErrorRetryCount
    }

    /**
     * After the liveness detection fails, the retry interval, in milliseconds
     */
    @IntRange(from = 1)
    internal var livenessFailedRetryInterval: Long = 1000
    fun setLivenessFailedRetryInterval(@IntRange(from = 1) livenessFailedRetryInterval: Long) =
      apply {
        this.livenessFailedRetryInterval = livenessFailedRetryInterval
      }

    /**
     * Whether to enable face matching
     */
    internal var enableCompareFace = true
    fun enableCompareFace(enableCompareFace: Boolean) =
      apply {
        this.enableCompareFace = enableCompareFace
      }

    /**
     * Whether to enable mask recognition
     */
    internal var enableMask: Boolean = false
    fun enableMask(enableMask: Boolean) = apply { this.enableMask = enableMask }

    /**
     * face size limit
     * The recognition operation will only be performed when the face size exceeds this value.
     * Since the width and height of the current face frame are close to the same, it is approximately a square
     */
    internal var faceSizeLimit: Int = 160
    fun setFaceSizeLimit(faceSizeLimit: Int) = apply { this.faceSizeLimit = faceSizeLimit }

    /**
     * Image quality detection threshold: suitable for face recognition scenarios without masks
     */
    internal var imageQualityNoMaskRecognizeThreshold: Float = 0.49f
    fun setImageQualityNoMaskRecognizeThreshold(imageQualityNoMaskRecognizeThreshold: Float) =
      apply {
        this.imageQualityNoMaskRecognizeThreshold = imageQualityNoMaskRecognizeThreshold
      }

    /**
     * Image quality detection threshold: suitable for wearing masks and face recognition scenarios
     */
    internal var imageQualityMaskRecognizeThreshold: Float = 0.29f
    fun setImageQualityMaskRecognizeThreshold(imageQualityMaskRecognizeThreshold: Float) =
      apply {
        this.imageQualityMaskRecognizeThreshold = imageQualityMaskRecognizeThreshold
      }

    /**
     * Exception callback during face recognition
     */
    internal var onErrorCallback: OnErrorCallback? = null
    fun setOnErrorCallback(onErrorCallback: OnErrorCallback?) =
      apply {
        this.onErrorCallback = onErrorCallback
      }

    fun build(): FaceConfiguration {
      if (recognizeCallback == null) {
        //If the recognition result is returned to be empty, directly force the recognition operation not to be enabled
        enableRecognize = false
      }
      return FaceConfiguration(this)
    }
  }
}