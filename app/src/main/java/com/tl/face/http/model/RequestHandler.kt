package com.tl.face.http.model

import android.app.*
import android.content.*
import android.net.ConnectivityManager
import android.net.NetworkInfo
import androidx.lifecycle.LifecycleOwner
import com.google.gson.JsonSyntaxException
import com.tl.face.R
import com.hjq.gson.factory.GsonFactory
import com.hjq.http.EasyLog
import com.hjq.http.config.IRequestApi
import com.hjq.http.config.IRequestHandler
import com.hjq.http.exception.*
import com.tencent.mmkv.MMKV
import okhttp3.Headers
import okhttp3.Response
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.Type
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class RequestHandler constructor(private val application: Application) : IRequestHandler {

    private val mmkv: MMKV = MMKV.mmkvWithID("http_cache_id")

    @Throws(Exception::class)
    override fun requestSucceed(lifecycle: LifecycleOwner, api: IRequestApi, response: Response, type: Type): Any? {
        if ((Response::class.java == type)) {
            return response
        }
        if (!response.isSuccessful) {

            throw ResponseException(
                application.getString(R.string.http_response_error) + "，responseCode：" + response.code() + "，message：" + response.message(),
                response
            )
        }
        if ((Headers::class.java == type)) {
            return response.headers()
        }
        val body: ResponseBody = response.body() ?: return null
        if ((InputStream::class.java == type)) {
            return body.byteStream()
        }

        val text: String
        try {
            text = body.string()
        } catch (e: IOException) {

            throw DataException(application.getString(R.string.http_data_explain_error), e)
        }


        EasyLog.json(text)
        if ((String::class.java == type)) {
            return text
        }

        if ((JSONObject::class.java == type)) {
            try {

                return JSONObject(text)
            } catch (e: JSONException) {
                throw DataException(application.getString(R.string.http_data_explain_error), e)
            }
        }

        if ((JSONArray::class.java == type)) {
            try {

                return JSONArray(text)
            } catch (e: JSONException) {
                throw DataException(application.getString(R.string.http_data_explain_error), e)
            }
        }

        val result: Any?
        try {
            result = GsonFactory.getSingletonGson().fromJson(text, type)
        } catch (e: JsonSyntaxException) {

            throw DataException(application.getString(R.string.http_data_explain_error), e)
        }

        if (result is HttpData<*>) {
            val model: HttpData<*> = result
            if (model.isRequestSucceed()) {
                // success
                return result
            }
            if (model.isTokenFailure()) {

                throw TokenException(application.getString(R.string.http_token_error))
            }
            throw ResultException(model.getMessage(), model)
        }
        return result
    }

    override fun requestFail(lifecycle: LifecycleOwner, api: IRequestApi, e: Exception): Exception {

        if (e is HttpException) {
            if (e is TokenException) {
            }
            return e
        }
        if (e is SocketTimeoutException) {
            return TimeoutException(application.getString(R.string.http_server_out_time), e)
        }
        if (e is UnknownHostException) {
            val info: NetworkInfo? = (application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo
            // check network connection
            if (info == null || !info.isConnected) {

                return NetworkException(application.getString(R.string.http_network_error), e)
            }


            return ServerException(application.getString(R.string.http_server_error), e)
        }
        if (e is IOException) {

            return CancelException("", e)
        }
        return HttpException(e.message, e)
    }

    override fun readCache(lifecycle: LifecycleOwner, api: IRequestApi, type: Type): Any? {
        val cacheKey: String? = GsonFactory.getSingletonGson().toJson(api)
        val cacheValue: String? = mmkv.getString(cacheKey, null)
        if ((cacheValue == null) || ("" == cacheValue) || ("{}" == cacheValue)) {
            return null
        }
        EasyLog.print("---------- cacheKey ----------")
        EasyLog.json(cacheKey)
        EasyLog.print("---------- cacheValue ----------")
        EasyLog.json(cacheValue)
        return GsonFactory.getSingletonGson().fromJson(cacheValue, type)
    }

    override fun writeCache(lifecycle: LifecycleOwner, api: IRequestApi, response: Response, result: Any?): Boolean {
        val cacheKey: String? = GsonFactory.getSingletonGson().toJson(api)
        val cacheValue: String? = GsonFactory.getSingletonGson().toJson(result)
        if ((cacheValue == null) || ("" == cacheValue) || ("{}" == cacheValue)) {
            return false
        }
        EasyLog.print("---------- cacheKey ----------")
        EasyLog.json(cacheKey)
        EasyLog.print("---------- cacheValue ----------")
        EasyLog.json(cacheValue)
        return mmkv.putString(cacheKey, cacheValue).commit()
    }
}