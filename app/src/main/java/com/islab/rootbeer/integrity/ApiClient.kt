package com.islab.rootbeer.integrity

import android.util.Log
import com.islab.rootbeer.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/** 與本地 server 溝通的最小封裝 (單例 OkHttp) */
class ApiClient(private val client: OkHttpClient = sharedClient) {

    private val base = BuildConfig.BASE_API_URL.trimEnd('/')
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    data class NonceResponse(val nonceId: String, val nonce: String)
    data class DecodeResult(val rawJson: JSONObject?, val error: String? = null)

    suspend fun fetchNonce(): Result<NonceResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url("$base/nonce").build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("http ${resp.code}")
                val body = resp.body?.string() ?: error("empty body")
                val obj = JSONObject(body)
                NonceResponse(obj.getString("nonceId"), obj.getString("nonce"))
            }
        }
    }

    suspend fun decodeIntegrity(packageName: String, token: String, serviceAccountFile: String? = null): Result<DecodeResult> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("packageName", packageName)
                put("token", token)
                if (serviceAccountFile != null) put("serviceAccountFile", serviceAccountFile)
            }
            val req = Request.Builder()
                .url("$base/integrity/decode")
                .post(payload.toString().toRequestBody(jsonMedia))
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string() ?: ""
                if (!resp.isSuccessful) {
                    Log.w("ApiClient", "decode failed: ${resp.code} $body")
                    return@use DecodeResult(null, error = body.ifBlank { "http ${resp.code}" })
                }
                DecodeResult(JSONObject(body), null)
            }
        }
    }
}

private val sharedClient: OkHttpClient by lazy {
    val logger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }
    OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(logger)
        .build()
}