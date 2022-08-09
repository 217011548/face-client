package com.tl.face.http.api

import com.hjq.http.config.IRequestApi
import com.hjq.http.config.IRequestType
import com.hjq.http.model.BodyType
import java.io.File

/**
 * Upload Image
 */
class UpdateImageApi : IRequestApi, IRequestType {

    override fun getApi(): String {
        return "api/upload/files"
    }

    private var file: File? = null

    fun setImage(image: File?): UpdateImageApi = apply {
        this.file = image
    }

    override fun getType(): BodyType {
        return BodyType.FORM
    }
}