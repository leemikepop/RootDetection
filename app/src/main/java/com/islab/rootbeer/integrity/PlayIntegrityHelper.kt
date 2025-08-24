package com.islab.rootbeer.integrity

import android.content.Context
import com.google.android.play.core.integrity.IntegrityManager
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Play Integrity 取得加密 token 的最小封裝。
 * 回傳的 token 需送到你的後端，用官方 server side API 解碼並驗證。
 * 後端再回傳評估結果（如 deviceIntegrity / accountDetails 等）供前端顯示或風控。
 */
class PlayIntegrityHelper(private val context: Context) {

    private val integrityManager: IntegrityManager by lazy { IntegrityManagerFactory.create(context) }

    /**
     * 取得加密的 integrity token。
     * nonce：請使用後端生成（帶簽名/時間戳）更安全；此處僅示範。
     */
    suspend fun requestIntegrityToken(nonce: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = IntegrityTokenRequest.builder()
                .setNonce(nonce)
                .build()
            val response = integrityManager.requestIntegrityToken(request).await()
            Result.success(response.token())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
