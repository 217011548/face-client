package com.tl.face.face.model

import com.tl.face.configuration.FaceFeatureDataBean

/**
 *
 * @author  ShenBen
 * @date    2021/03/02 12:06
 * @email   714081644@qq.com
 */
data class CompareResult(val bean: FaceFeatureDataBean, val similar: Float)