package com.tl.face.http.model

import com.tl.face.other.AppConfig
import com.hjq.http.config.IRequestServer
import com.hjq.http.model.BodyType


class RequestServer : IRequestServer {

    override fun getHost(): String {
        return AppConfig.getHostUrl()
    }

    override fun getPath(): String {
        return ""
    }

    override fun getType(): BodyType {
        return BodyType.FORM
    }
}