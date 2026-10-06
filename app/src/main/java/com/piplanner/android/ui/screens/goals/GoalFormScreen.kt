package com.piplanner.android.ui.screens.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.piplanner.android.PiPlannerApplication
import com.piplanner.android.domain.model.Goal
import com.piplanner.android.ui.components.*
import com.piplanner.android.ui.theme.*
import com.piplanner.android.util.CalculationUtils
import com.piplanner.android.util.CurrencyFormatter
import com.piplanner.android.util.DateUtils
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalFormScreen(
    goalId: String?,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    val goalRepository = PiPlannerApplication.instance.goalRepository
    val scope = rememberCoroutineScope()
    
    var name by remember { mutableStateOf("") }
    var targetAmountText by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf(LocalDate.now().plusYears(1)) }
    var inflationRate by remember { mutableDoubleStateOf(7.0) }
    var sharePercentage by remember { mutableIntStateOf(50) }
    var savedSoFar by remember { mutableLongStateOf(0L) }
    var showInflationDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf<String?>(null) }
    
    val targetAmount = targetAmountText.filter { it.isDigit() }.toLongOrNull() ?: 0L
    val targetWithInflation = remember(targetAmount, inflationRate, startDate, endDate) {
        CalculationUtils.calculateInflationAdjustedTarget(targetAmount, inflationRate, startDate, endDate)
    }
    val needsPerMonth = remember(targetWithInflation, savedSoFar, startDate, endDate) {
        CalculationUtils.calculateMonthlyNeeded(targetWithInflation, savedSoFar, startDate, endDate)
    }
    
    val isValid = name.isNotBlank() && targetAmount > 0 && endDate.isAfter(startDate)
    
    LaunchedEffect(goalId) {
        goalId?.let { id ->
            goalRepository.getGoalById(id)?.let { goal ->
                name = goal.name
                targetAmountText = goal.targetAmount.toString()
                startDate = goal.startDate
                endDate = goal.endDate
                inflationRate = goal.inflationRate
                sharePercentage = goal.sharePercentage
                savedSoFar = goal.savedAmount
            }
        }
    }
    
    if (showInflationDialog) {
        InflationDialog(
            currentRate = inflationRate,
            onRateChange = { inflationRate = it },
            onDismiss = { showInflationDialog = false }
        )
    }
    
    showDatePicker?.let { pickerType ->
        DatePickerDialog(
            initialDate = if (pickerType == "start") startDate else endDate,
            onDateSelected = { date ->
                if (pickerType == "start") {
                    startDate = date
                } else {
                    endDate = date
                }
                showDatePicker = null
            },
            onDismiss = { showDatePicker = null }
        )
    }
    
    Scaffold(
        topBar = {
            PiPlannerTopBar(
                title = if (goalId != null) "Edit goal" else "New goal",
                onBackClick = onBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Goal name") },
                    placeholder = { Text("e.g., Car, Emergency fund, Holiday") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NavyPrimary,
                        unfocusedBorderColor = CardBorder
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                
                OutlinedTextField(
                    value = if (targetAmount > 0) CurrencyFormatter.formatIndianNumber(targetAmount) else "",
                    onValueChange = { newValue ->
                        targetAmountText = newValue.filter { it.isDigit() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Target amount") },
                    leadingIcon = { Text("₹", style = MaterialTheme.typography.titleMedium) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NavyPrimary,
                        unfocusedBorderColor = CardBorder
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Start date",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        DateField(
                            date = startDate,
                            onClick = { showDatePicker = "start" }
                        )
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "End date",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        DateField(
                            date = endDate,
                            onClick = { showDatePicker = "end" }
                        )
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showInflationDialog = true },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Inflation rate",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "${inflationRate.toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = NavyPrimary
                    )
                }
                
                Divider()
                
                if (targetAmount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Target with inflation",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = CurrencyFormatter.formatIndianRupees(targetWithInflation),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Needs/month",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = CurrencyFormatter.formatIndianRupees(needsPerMonth),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface)
                    .padding(24.dp)
            ) {
                PrimaryButton(
                    text = "Save goal",
                    onClick = {
                        scope.launch {
                            val goal = Goal(
                                id = goalId ?: UUID.randomUUID().toString(),
                                name = name,
                                targetAmount = targetAmount,
                                targetWithInflation = targetWithInflation,
                                savedAmount = savedSoFar,
                                startDate = startDate,
                                endDate = endDate,
                                inflationRate = inflationRate,
                                sharePercentage = sharePercentage
                            )
                            
                            if (goalId != null) {
                                goalRepository.updateGoal(goal)
                            } else {
                                goalRepository.insertGoal(goal)
                            }
                            onSave()
                        }
                    },
                    enabled = isValid
                )
            }
        }
    }
}

@Composable
private fun DateField(
    date: LocalDate,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = DateUtils.formatMonthYear(date),
                style = MaterialTheme.typography.bodyMedium
            )
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toEpochDay() * 24 * 60 * 60 * 1000
    )
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                        onDateSelected(date)
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun InflationDialog(
    currentRate: Double,
    onRateChange: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var rate by remember { mutableDoubleStateOf(currentRate) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Inflation rate") },
        text = {
            Column {
                Text(
                    text = "At ${rate.toInt()}% inflation, your target will be adjusted to maintain purchasing power over time.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { if (rate > 0) rate -= 1 }
                    ) {
                        Text(
                            text = "−",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                    
                    Text(
                        text = "${rate.toInt()}%",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    
                    IconButton(
                        onClick = { if (rate < 20) rate += 1 }
                    ) {
                        Text(
                            text = "+",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onRateChange(rate)
                    onDismiss()
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
