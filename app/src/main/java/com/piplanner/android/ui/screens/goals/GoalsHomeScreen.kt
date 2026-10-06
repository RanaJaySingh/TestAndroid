package com.piplanner.android.ui.screens.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piplanner.android.PiPlannerApplication
import com.piplanner.android.ui.components.*
import com.piplanner.android.ui.theme.*
import com.piplanner.android.util.DateUtils
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsHomeScreen(
    onSettingsClick: () -> Unit,
    onGoalClick: (String) -> Unit,
    onNewGoalClick: () -> Unit,
    onTransferClick: () -> Unit,
    onSyncClick: () -> Unit,
    onHistoryClick: (() -> Unit)? = null
) {
    val goalRepository = PiPlannerApplication.instance.goalRepository
    val accountRepository = PiPlannerApplication.instance.accountRepository
    val preferencesDataStore = PiPlannerApplication.instance.preferencesDataStore
    
    val goals by goalRepository.getAllGoals().collectAsStateWithLifecycle(initialValue = emptyList())
    val dedicatedAccount by accountRepository.getDedicatedSavingsAccount().collectAsStateWithLifecycle(initialValue = null)
    val preferences by preferencesDataStore.userPreferences.collectAsStateWithLifecycle(
        initialValue = com.piplanner.android.domain.model.UserPreferences()
    )
    
    val lastSyncDateTime = remember(preferences.lastSyncTimestamp) {
        preferences.lastSyncTimestamp?.let {
            LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
        }
    }
    
    val greeting = DateUtils.getGreeting()
    
    Scaffold(
        topBar = {
            PiPlannerTopBar(
                title = "PiPlanner",
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextOnPrimary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(paddingValues),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "$greeting, ${preferences.userName}",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            
            item {
                BalanceCard(
                    balance = dedicatedAccount?.balance ?: 0L,
                    lastSynced = lastSyncDateTime,
                    onSyncClick = onSyncClick,
                    onNewGoalClick = onNewGoalClick,
                    onTransferClick = onTransferClick,
                    onHistoryClick = onHistoryClick ?: {}
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Your Goals",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    if (goals.isNotEmpty()) {
                        val onTrackCount = goals.count { it.isOnTrack }
                        val behindCount = goals.size - onTrackCount
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (onTrackCount > 0) {
                                Text(
                                    text = "$onTrackCount on track",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatusGreen
                                )
                            }
                            if (behindCount > 0) {
                                Text(
                                    text = "$behindCount behind",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatusAmber
                                )
                            }
                        }
                    }
                }
            }
            
            if (goals.isEmpty()) {
                item {
                    PiPlannerCard {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No goals yet",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Create your first savings goal to get started",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            PrimaryButton(
                                text = "Create goal",
                                onClick = onNewGoalClick
                            )
                        }
                    }
                }
            } else {
                items(goals, key = { it.id }) { goal ->
                    GoalCard(
                        goal = goal,
                        onClick = { onGoalClick(goal.id) }
                    )
                }
            }
        }
    }
}
