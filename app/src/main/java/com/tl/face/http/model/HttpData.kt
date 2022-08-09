package com.tl.face.http.model


open class HttpData<T> {


    private val status: Int = 0


    private val msg: String? = null


    private val data: T? = null

    fun getCode(): Int {
        return status
    }

    fun getMessage(): String? {
        return msg
    }

    fun getData(): T? {
        return data
    }


    fun isRequestSucceed(): Boolean {
        return status == 0
    }


    fun isTokenFailure(): Boolean {
        return status == 1001
    }
}