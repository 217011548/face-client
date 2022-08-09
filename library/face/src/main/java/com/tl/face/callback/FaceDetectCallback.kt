package com.tl.face.callback

/**
 * 人脸检测Callback
 * @author  ShenBen
 * @date    2021/03/26 9:39
 * @email   714081644@qq.com
 */
interface FaceDetectCallback {

  /**
   * 有人，仅在有变化时调用一次
   * <p>运行在子线程</p>
   */
  fun someone() {

  }

  /**
   * 无人，仅在有变化时调用一次
   * <p>运行在子线程</p>
   */
  fun nobody() {

  }

  /**
   * 有人，但是不在识别区域
   */
  fun notInTheArea() {

  }

  /**
   * 检测到的人脸数量
   * <p>运行在子线程</p>
   *
   * @param num 人脸数量
   * @param faceIds faceId
   */
  fun detectFaceNum(num: Int, faceIds: List<Int>) {

  }

  /**
   * 检测到人脸
   * @param nv21 画面
   * @param maskInfo 是否佩戴了口罩
   * @param width 预览数据宽度
   * @param height 预览数据高度
   */
  fun detectionFace(
    nv21: ByteArray, maskInfo: Int, width: Int,
    height: Int
  )
}