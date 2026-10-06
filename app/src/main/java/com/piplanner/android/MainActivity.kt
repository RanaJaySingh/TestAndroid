package com.piplanner.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.piplanner.android.ui.navigation.NavRoutes
import com.piplanner.android.ui.navigation.PiPlannerNavHost
import com.piplanner.android.ui.theme.Background
import com.piplanner.android.ui.theme.PiPlannerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            PiPlannerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Background
                ) {
                    PiPlannerApp()
                }
            }
        }
    }
}

@Composable
fun PiPlannerApp() {
    val navController = rememberNavController()
    val preferencesDataStore = PiPlannerApplication.instance.preferencesDataStore
    
    val preferences by preferencesDataStore.userPreferences.collectAsStateWithLifecycle(
        initialValue = null
    )
    
    val startDestination = remember(preferences) {
        if (preferences?.hasCompletedSetup == true) {
            NavRoutes.MainShell.route
        } else {
            NavRoutes.Welcome.route
        }
    }
    
    if (preferences != null) {
        PiPlannerNavHost(
            navController = navController,
            startDestination = startDestination
        )
    }
}
