package com.tl.face.face

import android.content.Context
import android.graphics.Bitmap
import com.arcsoft.face.*
import com.arcsoft.face.enums.DetectFaceOrientPriority
import com.arcsoft.face.enums.DetectMode
import com.arcsoft.face.enums.ExtractType
import com.arcsoft.imageutil.ArcSoftImageFormat
import com.arcsoft.imageutil.ArcSoftImageUtil
import com.arcsoft.imageutil.ArcSoftImageUtilError
import com.tl.face.configuration.FaceFeatureDataBean
import com.tl.face.face.model.CompareResult
import com.tl.face.utils.LogUtil
import java.util.*

/**
 * It is used for face comparison and feature code generation
 * Can be encapsulated into a singleton pattern by itself
 *
 * @author  ShenBen
 * @date    2021/02/24 14:08
 * @email   714081644@qq.com
 */
class FaceServer {
    companion object {
        private const val TAG = "FaceServer->"
    }

    private val faceEngine = FaceEngine()

    /**
     * Init the faceENgine
     * @param context context
     * @param faceOrient detect angle，single angle，not support[DetectFaceOrientPriority.ASF_OP_ALL_OUT]
     * [DetectFaceOrientPriority.ASF_OP_0_ONLY]
     * [DetectFaceOrientPriority.ASF_OP_90_ONLY]
     * [DetectFaceOrientPriority.ASF_OP_180_ONLY]
     * [DetectFaceOrientPriority.ASF_OP_270_ONLY]
     */
    fun init(
        context: Context,
        faceOrient: DetectFaceOrientPriority = DetectFaceOrientPriority.ASF_OP_0_ONLY
    ) {
        val orientPriority =
            if (faceOrient == DetectFaceOrientPriority.ASF_OP_ALL_OUT) {
                DetectFaceOrientPriority.ASF_OP_0_ONLY
            } else {
                faceOrient
            }
        val result = faceEngine.init(
            context,
            DetectMode.ASF_DETECT_MODE_IMAGE,
            orientPriority,
            1,
            FaceEngine.ASF_FACE_DETECT or FaceEngine.ASF_MASK_DETECT or FaceEngine.ASF_FACE_RECOGNITION
        )
        LogUtil.i("${TAG}FaceEngine Init:$result")
    }

    /**
     * Compare faces 1:N
     *
     * @param faceFeature Compare faceFeature
     * @param features waiting compare features list
     *
     * @return null:engine error；return maximum[features] data
     */
    fun compareFaceFeature(
        faceFeature: FaceFeature,
        features: List<FaceFeatureDataBean>
    ): CompareResult? {
        if (features.isEmpty()) {
            return null
        }
        val tempFaceFeature = FaceFeature()
        val faceSimilar = FaceSimilar()
        var maxSimilar = 0f
        var maxSimilarIndex = -1

        synchronized(faceEngine) {
            features.forEachIndexed { index, bean ->
                tempFaceFeature.featureData = bean.feature
                val result =
                    faceEngine.compareFaceFeature(faceFeature, tempFaceFeature, faceSimilar)
                if (result == ErrorInfo.MOK) {
                    if (faceSimilar.score > maxSimilar) {
                        maxSimilar = faceSimilar.score
                        maxSimilarIndex = index
                    }
                } else {
                    LogUtil.e("${TAG}compareFaceFeature-errorCode: $result")
                }
            }
        }
        if (maxSimilarIndex != -1) {
            return CompareResult(features[maxSimilarIndex], maxSimilar)
        }
        return null
    }

    /**
     * Compare two sets of signatures 1:1
     * @return return similarity
     */
    fun compareFaceFeature(feature1: ByteArray, feature2: ByteArray): Float {
        val faceFeature1 = FaceFeature(feature1)
        val faceFeature2 = FaceFeature(feature2)
        val similar = FaceSimilar()
        synchronized(faceEngine) {
            val result = faceEngine.compareFaceFeature(faceFeature1, faceFeature2, similar)
            if (result != ErrorInfo.MOK) {
                LogUtil.e("${TAG}compareFaceFeature-errorCode: $result")
            }
        }
        return similar.score
    }

    /**
     * Extract feature code through Bitmap, extract only one face
     * Preferably run in child thread
     *
     * @param bitmap
     * @return Feature code
     */
    fun extractFaceFeature(bitmap: Bitmap?): ByteArray? {
        if (bitmap == null) {
            return null
        }
        var feature: ByteArray? = null
        val alignedBitmap = ArcSoftImageUtil.getAlignedBitmap(bitmap, true)
        val imageData = ArcSoftImageUtil.createImageData(
            alignedBitmap.width,
            alignedBitmap.height,
            ArcSoftImageFormat.BGR24
        )
        val code = ArcSoftImageUtil.bitmapToImageData(
            alignedBitmap,
            imageData,
            ArcSoftImageFormat.BGR24
        )
        if (code == ArcSoftImageUtilError.CODE_SUCCESS) {
            feature = extractFaceFeature(
                imageData, alignedBitmap.width,
                alignedBitmap.height,
                FaceEngine.CP_PAF_BGR24
            )
        } else {
            LogUtil.e("${TAG}extractFaceFeature-bitmapToImageData: $code")
        }
        return feature
    }

    /**
     * The camera preview data extracts the face feature code, and only one face is extracted
     * Preferably run in child thread
     *
     * @param nv21 camera preview data
     * @param width camera preview width
     * @param height camera preview height
     */
    fun extractFaceFeature(nv21: ByteArray, width: Int, height: Int): ByteArray? {
        return extractFaceFeature(nv21, width, height, FaceEngine.CP_PAF_NV21)
    }

    /**
     * destroy to release resource
     */
    fun destroy() {
        synchronized(faceEngine) {
            val result = faceEngine.unInit()
            LogUtil.w("${TAG}destroy-faceEngine.unInit:$result")
        }
    }

    private fun extractFaceFeature(
        byteArray: ByteArray,
        width: Int,
        height: Int,
        format: Int
    ): ByteArray? {
        var feature: ByteArray? = null
        synchronized(faceEngine) {
            val faceInfoList: List<FaceInfo> = ArrayList()
            //Face Detection
            val detectFaceResult = faceEngine.detectFaces(
                byteArray,
                width,
                height,
                format,
                faceInfoList
            )
            if (detectFaceResult == ErrorInfo.MOK && faceInfoList.isNotEmpty()) {
                //Mask recognition
                val detectMaskResult = faceEngine.process(
                    byteArray,
                    width,
                    height,
                    format,
                    faceInfoList,
                    FaceEngine.ASF_MASK_DETECT
                )
                val maskList = mutableListOf<MaskInfo>()
                if (detectMaskResult == ErrorInfo.MOK) {
                    val maskResult = faceEngine.getMask(maskList)
                    if (maskResult == ErrorInfo.MOK) {
                        if (maskList.size == faceInfoList.size) {
                            val faceFeature = FaceFeature()
                            val extractResult = faceEngine.extractFaceFeature(
                                byteArray,
                                width,
                                height,
                                format,
                                faceInfoList[0],
                                ExtractType.REGISTER,
                                maskList[0].mask,
                                faceFeature
                            )
                            if (extractResult == ErrorInfo.MOK) {
                                feature = faceFeature.featureData
                            } else {
                                LogUtil.e("${TAG}extractFaceFeature-extractFaceFeature: $extractResult")
                            }
                        } else {
                            LogUtil.e("${TAG}extractFaceFeature-getMask: maskList.size(${maskList.size}) != faceInfoList.size(${faceInfoList.size})")
                        }
                    } else {
                        LogUtil.e("${TAG}extractFaceFeature-getMask: $maskResult")
                    }
                } else {
                    LogUtil.e("${TAG}extractFaceFeature-detectMaskResult: $detectMaskResult")
                }
            } else {
                LogUtil.e("${TAG}extractFaceFeature-detectFaces: ${detectFaceResult}, faceInfoList.size: ${faceInfoList.size}")
            }
        }
        return feature
    }
}