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

    fun requestIntegrity(nonceProvider: () -> String = { "nonce-${System.currentTimeMillis()}" }) {
        integrityJob?.cancel()
        integrityJob = viewModelScope.launch {
            _uiState.update { it.copy(integrityLoading = true, integrityError = null, integrityToken = null) }
            val nonce = nonceProvider()
            val tokenResult = integrityHelper.requestIntegrityToken(nonce)
            tokenResult.onSuccess { token ->
                _uiState.update { it.copy(integrityLoading = false, integrityToken = token) }
            }.onFailure { e ->
                _uiState.update { it.copy(integrityLoading = false, integrityError = e.message ?: e.toString()) }
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
    )
}

private inline fun <T> MutableStateFlow<T>.update(block: (T) -> T) { this.value = block(this.value) }
