package com.tl.face.callback

import com.tl.face.constant.FaceErrorType

/**
 * 人脸识别时异常回调
 *
 * @author  ShenBen
 * @date    2021/03/03 14:22
 * @email   714081644@qq.com
 */
interface OnErrorCallback {
    /**
     * 异常回调
     * 运行在子线程
     */
    fun onError(type: FaceErrorType, errorCode: Int, errorMessage: String)
}