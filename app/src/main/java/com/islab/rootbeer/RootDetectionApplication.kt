package com.islab.rootbeer

import android.app.Application
import android.util.Log

/**
 * App 自訂 Application。
 * 目前僅確保 Manifest 參照的類別存在，避免啟動時 ClassNotFoundException。
 * 之後可在這裡初始化：Firebase / Play Integrity helper / Crashlytics / 日誌系統。
 */
class RootDetectionApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("RootDetectionApp", "Application onCreate - initialized")
        // TODO: 未來可加入 FirebaseApp.initializeApp(this) 等初始化程式
    }
}
