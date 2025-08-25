package com.islab.rootbeer.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.islab.rootbeer.detection.RootCheckResult
import com.islab.rootbeer.detection.RootChecker
import com.islab.rootbeer.integrity.PlayIntegrityHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 整合 Root 檢測 + Play Integrity token 取得的 ViewModel。
 * 後續可在這裡加入：server 驗證、風險評級、快取策略、重放保護邏輯。
 */
class RootSecurityViewModel(app: Application) : AndroidViewModel(app) {

    private val rootChecker by lazy { RootChecker(app.applicationContext) }
    private val integrityHelper by lazy { PlayIntegrityHelper(app.applicationContext) }
    private val apiClient by lazy { com.islab.rootbeer.integrity.ApiClient() }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var rootJob: Job? = null
    private var integrityJob: Job? = null

    fun runRootChecks() {
        rootJob?.cancel()
        rootJob = viewModelScope.launch {
            _uiState.update { it.copy(rootLoading = true, rootError = null) }
            try {
                val result = rootChecker.runBasicCheck()
                _uiState.update { it.copy(rootLoading = false, rootResult = result) }
            } catch (e: Exception) {
                _uiState.update { it.copy(rootLoading = false, rootError = e.message ?: e.toString()) }
            }
        }
    }

    /** 簡化：取 nonce -> 取得 token -> server 解碼後回傳完整 JSON */
    fun decodeIntegrity(packageName: String) {
        integrityJob?.cancel()
        integrityJob = viewModelScope.launch {
            _uiState.update { it.copy(integrityLoading = true, integrityError = null, integrityToken = null, decoded = null) }
            val nonceResp = apiClient.fetchNonce().getOrElse { e ->
                _uiState.update { it.copy(integrityLoading = false, integrityError = "nonce: ${e.message}") }
                return@launch
            }
            val token = integrityHelper.requestIntegrityToken(nonceResp.nonce).getOrElse { e ->
                _uiState.update { it.copy(integrityLoading = false, integrityError = "token: ${e.message}") }
                return@launch
            }
            _uiState.update { it.copy(integrityToken = token) }
            val decoded = apiClient.decodeIntegrity(packageName, token).getOrElse { e ->
                _uiState.update { it.copy(integrityLoading = false, integrityError = "decode: ${e.message}") }
                return@launch
            }
            if (decoded.error != null) {
                _uiState.update { it.copy(integrityLoading = false, integrityError = decoded.error) }
            } else {
                _uiState.update { it.copy(integrityLoading = false, decoded = decoded.rawJson?.toString(2)) }
            }
        }
    }

    data class UiState(
        val rootLoading: Boolean = false,
        val rootResult: RootCheckResult? = null,
        val rootError: String? = null,
        val integrityLoading: Boolean = false,
        val integrityToken: String? = null,
        val integrityError: String? = null,
        val decoded: String? = null,
    )
}

private inline fun <T> MutableStateFlow<T>.update(block: (T) -> T) { this.value = block(this.value) }

// Play Integrity 需要 nonce 長度 (解碼後位元組數) 介於 16~500 bytes，建議使用安全隨機來源。
// 這裡使用 32 bytes -> Base64 URL 安全編碼後大約 43 字元，足夠且不過長。
// generateSecureNonce 已由 server 端提供，前端不再自行產生避免重放/預測
