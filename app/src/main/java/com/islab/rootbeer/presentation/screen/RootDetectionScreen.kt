package com.islab.rootbeer.presentation.screen

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.islab.rootbeer.presentation.viewmodel.RootSecurityViewModel

/**
 * 最簡版本 Root 檢測畫面：
 * 1. 進入畫面即執行基本 root 檢測 (RootBeer#isRooted / isRootedWithoutBusyBoxCheck)
 * 2. 顯示是否 rooted 與觸發的檢測項目數
 * 3. 提供重新檢測按鈕
 */
@Composable
fun RootDetectionScreen(modifier: Modifier = Modifier, vm: RootSecurityViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by vm.uiState.collectAsState()

    LaunchedEffect(Unit) { vm.runRootChecks() }

    var showIntegrityJson by remember { mutableStateOf(false) }
    val integrityJsonScroll = rememberScrollState()

    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Root 檢測",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(24.dp))

            val rootResult = uiState.rootResult
            val subChecks = rootResult?.subChecks.orEmpty()
            val triggeredChecks = rootResult?.triggeredChecks.orEmpty()
            val rooted = rootResult?.rooted

            when {
                uiState.rootLoading -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("正在檢測中 ...")
                }

                rooted == true -> {
                    Text(
                        text = "這台裝置可能已 Root",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("觸發的檢測項目數：${triggeredChecks.size}")
                    // if (triggeredChecks.isNotEmpty()) {
                    //     Spacer(Modifier.height(8.dp))
                    //     triggeredChecks.forEach { name ->
                    //         Text("• $name", style = MaterialTheme.typography.bodySmall)
                    //     }
                    // }
                }

                rooted == false -> {
                    Text(
                        text = "未發現 Root 迹象",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
            Button(onClick = { vm.runRootChecks() }, enabled = !uiState.rootLoading) {
                Text("重新檢測")
            }

            Spacer(Modifier.height(12.dp))
            Button(onClick = { vm.decodeIntegrity(packageName = "com.islab.rootbeer") }, enabled = !uiState.integrityLoading) {
                Text(if (uiState.integrityLoading) "驗證中..." else "Play Integrity")
            }

            // uiState.integrityToken?.let { tk ->
            //     Spacer(Modifier.height(8.dp))
            //     Text("Token (encrypted snippet):", style = MaterialTheme.typography.bodySmall)
            //     Text(tk.take(80) + if (tk.length > 80) "..." else "", style = MaterialTheme.typography.bodySmall)
            // }
            uiState.decoded?.let { json ->
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Play Integrity 驗證結果", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { showIntegrityJson = !showIntegrityJson }) {
                        Text(if (showIntegrityJson) "收合" else "展開")
                    }
                }
                if (showIntegrityJson) {
                    Spacer(Modifier.height(4.dp))
                    androidx.compose.foundation.layout.Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp, max = 240.dp)
                            .verticalScroll(integrityJsonScroll)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = json,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }
            uiState.integrityError?.let { err ->
                Spacer(Modifier.height(8.dp))
                Text("Integrity 失敗: $err", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                if ("App is not found" in err) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "可能原因: 1) 尚未從 Play 渠道安裝 (目前是 side-load) 2) 服務帳戶未授權該 App 3) 包名/Cloud project 不一致 4) 剛發布仍在 Play 處理。",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (!uiState.rootLoading && subChecks.isNotEmpty()) {
                Spacer(Modifier.height(32.dp))
                Text("檢測細項 (觸發 ${triggeredChecks.size}/${subChecks.size})", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Divider()
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.weight(1f, fill = false)) {
                    items(subChecks) { item ->
                        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            val label = if (item.detected) "✗" else "✔"
                            val tint = if (item.detected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            Text(label, color = tint)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.description)
                                if (item.description != item.key) {
                                    Text(item.key, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                        Divider()
                    }
                }
            }
        }
    }
}
