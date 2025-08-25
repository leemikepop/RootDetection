package com.islab.rootbeer.detection

import android.content.Context
import com.scottyab.rootbeer.RootBeer

/**
 * 單一職責：包裝 RootBeer 的基本檢測，回傳較乾淨的資料物件。
 * 之後可以在這裡加上：
 * - Firebase Analytics 紀錄
 * - Play Integrity API token 校驗前/後的行為
 * - 其它自定義檢測
 */
class RootChecker(private val context: Context) {

    fun runBasicCheck(): RootCheckResult {
        val rootBeer = RootBeer(context)

        // 依照官方 sample 顯式列出檢測項目，避免使用反射 (kotlin-reflect) 帶來的額外體積與潛在錯誤。
        val subChecks = listOf(
            RootSubCheck("detectRootManagementApps", "Root 管理 App", rootBeer.detectRootManagementApps()),
            RootSubCheck("detectPotentiallyDangerousApps", "潛在危險 App", rootBeer.detectPotentiallyDangerousApps()),
            RootSubCheck("detectRootCloakingApps", "Root 隱藏/偽裝 App", rootBeer.detectRootCloakingApps()),
            RootSubCheck("detectTestKeys", "Build 標籤含 test-keys", rootBeer.detectTestKeys()),
            RootSubCheck("checkForBusyBoxBinary", "busybox binary 存在", rootBeer.checkForBusyBoxBinary()),
            RootSubCheck("checkForSuBinary", "su binary 存在", rootBeer.checkForSuBinary()),
            RootSubCheck("checkSuExists", "su 指令存在 (第二次檢查)", rootBeer.checkSuExists()),
            RootSubCheck("checkForRWPaths", "關鍵系統路徑可寫", rootBeer.checkForRWPaths()),
            RootSubCheck("checkForDangerousProps", "危險系統屬性", rootBeer.checkForDangerousProps()),
            RootSubCheck("checkForRootNative", "Native Root 檢測", rootBeer.checkForRootNative()),
            RootSubCheck("checkForMagiskBinary", "Magisk binary 存在", rootBeer.checkForMagiskBinary()),
        )

        val triggered = subChecks.filter { it.detected }.map { it.description }

        // RootBeer 原有的主判斷布林值 (isRooted)。過去還有 isRootedWithoutBusyBoxCheck 已被標記 deprecated，改由我們的細項結果自行判斷。
        val rootedAggregate = rootBeer.isRooted

        return RootCheckResult(
            rooted = rootedAggregate || triggered.isNotEmpty(),
            triggeredChecks = triggered,
            subChecks = subChecks
        )
    }
}
