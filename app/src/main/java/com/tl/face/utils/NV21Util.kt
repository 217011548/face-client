package com.tl.face.utils

object NV21Util {

  fun rotateYUV420Degree90(data: ByteArray, imageWidth: Int, imageHeight: Int): ByteArray? {
    val yuv = ByteArray(imageWidth * imageHeight * 3 / 2)
    var i = 0
    for (x in 0 until imageWidth) {
      for (y in imageHeight - 1 downTo 0) {
        yuv[i] = data[y * imageWidth + x]
        i++
      }
    }
    i = imageWidth * imageHeight * 3 / 2 - 1
    var x = imageWidth - 1
    while (x > 0) {
      for (y in 0 until imageHeight / 2) {
        yuv[i] = data[imageWidth * imageHeight + y * imageWidth + x]
        i--
        yuv[i] = data[imageWidth * imageHeight + y * imageWidth
          + (x - 1)]
        i--
      }
      x = x - 2
    }
    return yuv
  }

  fun rotateYUV420Degree180(
    data: ByteArray,
    imageWidth: Int,
    imageHeight: Int
  ): ByteArray? {
    val yuv = ByteArray(imageWidth * imageHeight * 3 / 2)
    var i = 0
    var count = 0
    i = imageWidth * imageHeight - 1
    while (i >= 0) {
      yuv[count] = data[i]
      count++
      i--
    }
    i = imageWidth * imageHeight * 3 / 2 - 1
    i = imageWidth * imageHeight * 3 / 2 - 1
    while (i >= imageWidth
      * imageHeight
    ) {
      yuv[count++] = data[i - 1]
      yuv[count++] = data[i]
      i -= 2
    }
    return yuv
  }

  fun rotateYUV420Degree270(
    data: ByteArray, imageWidth: Int,
    imageHeight: Int
  ): ByteArray? {
    val yuv = ByteArray(imageWidth * imageHeight * 3 / 2)
    var nWidth = 0
    var nHeight = 0
    var wh = 0
    var uvHeight = 0
    if (imageWidth != nWidth || imageHeight != nHeight) {
      nWidth = imageWidth
      nHeight = imageHeight
      wh = imageWidth * imageHeight
      uvHeight = imageHeight shr 1 // uvHeight = height / 2
    }
    var k = 0
    for (i in 0 until imageWidth) {
      var nPos = 0
      for (j in 0 until imageHeight) {
        yuv[k] = data[nPos + i]
        k++
        nPos += imageWidth
      }
    }
    var i = 0
    while (i < imageWidth) {
      var nPos = wh
      for (j in 0 until uvHeight) {
        yuv[k] = data[nPos + i]
        yuv[k + 1] = data[nPos + i + 1]
        k += 2
        nPos += imageWidth
      }
      i += 2
    }
    val rotate90 = rotateYUV420Degree90(data, imageWidth, imageHeight)
    if (rotate90 != null) {
      return rotateYUV420Degree180(
        rotate90,
        imageWidth,
        imageHeight
      )
    }
    return null
  }
}