package com.islab.rootbeer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.islab.rootbeer.presentation.screen.RootDetectionScreen
import com.islab.rootbeer.ui.theme.RootDetectionTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RootDetectionTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RootDetectionScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
