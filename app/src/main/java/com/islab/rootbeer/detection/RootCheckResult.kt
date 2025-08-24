package com.islab.rootbeer.detection

/**
 * 封裝 Root 檢測結果，便於之後串接 Firebase/Play Integrity 再擴充欄位。
 * subChecks: 逐項檢測結果（反射取得 RootBeer 各個 check* 方法）
 */
data class RootCheckResult(
    val rooted: Boolean,
    val triggeredChecks: List<String> = emptyList(),
    val subChecks: List<RootSubCheck> = emptyList(),
    val timestampMs: Long = System.currentTimeMillis(),
)

/** 單一子檢測結果 */
data class RootSubCheck(
    val key: String,
    val description: String,
    val detected: Boolean,
)
