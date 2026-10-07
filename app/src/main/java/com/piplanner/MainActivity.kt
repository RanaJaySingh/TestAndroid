package com.piplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.piplanner.ui.navigation.PiPlannerNavHost
import com.piplanner.ui.theme.PiPlannerTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Compose host. First-run / post-reset → Welcome (PIP-36); CTA → Accounts (PIP-38).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PiPlannerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PiPlannerNavHost()
                }
            }
        }
    }
}
