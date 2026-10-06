package com.piplanner.android.data

import com.piplanner.android.domain.model.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class GrokStubService {
    
    private val dateFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    private var isAvailable = true
    
    fun setAvailability(available: Boolean) {
        isAvailable = available
    }
    
    fun parseGoalInput(input: String): GrokResponse {
        if (!isAvailable) {
            return GrokResponse(
                type = GrokResponseType.UNAVAILABLE,
                message = "Grok is temporarily unavailable. Please use the form to enter your goals."
            )
        }
        
        val lowerInput = input.lowercase()
        
        val proposedGoals = mutableListOf<ProposedGoal>()
        
        if (lowerInput.contains("car") || lowerInput.contains("vehicle")) {
            proposedGoals.add(
                ProposedGoal(
                    name = "Car",
                    targetAmount = 500000,
                    endDate = LocalDate.now().plusYears(2).format(dateFormatter),
                    suggestedShare = 60
                )
            )
        }
        
        if (lowerInput.contains("emergency") || lowerInput.contains("rainy day")) {
            proposedGoals.add(
                ProposedGoal(
                    name = "Emergency",
                    targetAmount = 100000,
                    endDate = LocalDate.now().plusYears(1).format(dateFormatter),
                    suggestedShare = 40
                )
            )
        }
        
        if (lowerInput.contains("holiday") || lowerInput.contains("vacation") || lowerInput.contains("trip")) {
            proposedGoals.add(
                ProposedGoal(
                    name = "Holiday",
                    targetAmount = 150000,
                    endDate = LocalDate.now().plusMonths(8).format(dateFormatter),
                    suggestedShare = if (proposedGoals.isEmpty()) 50 else 30
                )
            )
        }
        
        if (lowerInput.contains("laptop") || lowerInput.contains("computer") || lowerInput.contains("phone")) {
            proposedGoals.add(
                ProposedGoal(
                    name = if (lowerInput.contains("laptop")) "Laptop" else "Electronics",
                    targetAmount = 80000,
                    endDate = LocalDate.now().plusMonths(6).format(dateFormatter),
                    suggestedShare = if (proposedGoals.isEmpty()) 50 else 25
                )
            )
        }
        
        if (lowerInput.contains("wedding") || lowerInput.contains("marriage")) {
            proposedGoals.add(
                ProposedGoal(
                    name = "Wedding",
                    targetAmount = 1000000,
                    endDate = LocalDate.now().plusYears(3).format(dateFormatter),
                    suggestedShare = 70
                )
            )
        }
        
        if (lowerInput.contains("house") || lowerInput.contains("home") || lowerInput.contains("apartment")) {
            proposedGoals.add(
                ProposedGoal(
                    name = "Home Down Payment",
                    targetAmount = 2000000,
                    endDate = LocalDate.now().plusYears(5).format(dateFormatter),
                    suggestedShare = 80
                )
            )
        }
        
        if (proposedGoals.isEmpty()) {
            val amount = extractAmount(input) ?: 100000L
            val targetDate = extractDate(input) ?: LocalDate.now().plusYears(1).format(dateFormatter)
            val goalName = extractGoalName(input) ?: "My Goal"
            
            proposedGoals.add(
                ProposedGoal(
                    name = goalName,
                    targetAmount = amount,
                    endDate = targetDate,
                    suggestedShare = 100
                )
            )
        }
        
        redistributeShares(proposedGoals)
        
        return GrokResponse(
            type = GrokResponseType.GOALS_PROPOSAL,
            proposedGoals = proposedGoals
        )
    }
    
    fun handleAskQuery(query: String, goals: List<Goal>): GrokResponse {
        if (!isAvailable) {
            return GrokResponse(
                type = GrokResponseType.UNAVAILABLE,
                message = "Grok is temporarily unavailable."
            )
        }
        
        val lowerQuery = query.lowercase()
        
        if (lowerQuery.contains("transfer") || lowerQuery.contains("move")) {
            val fromGoal = goals.maxByOrNull { it.savedAmount }
            val toGoal = goals.minByOrNull { it.progress }
            
            if (fromGoal != null && toGoal != null && fromGoal.id != toGoal.id) {
                val suggestedAmount = minOf(fromGoal.savedAmount / 4, toGoal.remaining / 2)
                    .coerceAtLeast(1000)
                
                return GrokResponse(
                    type = GrokResponseType.TRANSFER_PROPOSAL,
                    message = "Move ₹${suggestedAmount} from ${fromGoal.name} to ${toGoal.name}",
                    transferSuggestion = TransferSuggestion(
                        fromGoalId = fromGoal.id,
                        toGoalId = toGoal.id,
                        amount = suggestedAmount,
                        reason = "${toGoal.name} is behind and needs more funds"
                    )
                )
            }
        }
        
        if (lowerQuery.contains("status") || lowerQuery.contains("how") || lowerQuery.contains("progress")) {
            val onTrackCount = goals.count { it.isOnTrack }
            val behindCount = goals.size - onTrackCount
            val totalSaved = goals.sumOf { it.savedAmount }
            val totalTarget = goals.sumOf { it.targetWithInflation }
            
            return GrokResponse(
                type = GrokResponseType.PLAIN_ANSWER,
                message = "You have ${goals.size} goals. $onTrackCount are on track, $behindCount need attention. " +
                        "Total saved: ₹${com.piplanner.android.util.CurrencyFormatter.formatIndianNumber(totalSaved)} " +
                        "of ₹${com.piplanner.android.util.CurrencyFormatter.formatIndianNumber(totalTarget)}."
            )
        }
        
        if (lowerQuery.contains("add") || lowerQuery.contains("new goal") || lowerQuery.contains("create")) {
            return parseGoalInput(query)
        }
        
        return GrokResponse(
            type = GrokResponseType.PLAIN_ANSWER,
            message = "I can help you manage your savings goals. Try asking about transfers between goals, " +
                    "your progress status, or creating new goals."
        )
    }
    
    private fun extractAmount(input: String): Long? {
        val patterns = listOf(
            Regex("₹?([0-9,]+(?:\\.[0-9]+)?\\s*(?:lakh|lac|L))", RegexOption.IGNORE_CASE),
            Regex("₹?([0-9,]+(?:\\.[0-9]+)?\\s*(?:crore|cr))", RegexOption.IGNORE_CASE),
            Regex("₹?([0-9,]+(?:\\.[0-9]+)?\\s*k)", RegexOption.IGNORE_CASE),
            Regex("₹?([0-9,]+(?:\\.[0-9]+)?)")
        )
        
        for (pattern in patterns) {
            val match = pattern.find(input)
            if (match != null) {
                val valueStr = match.groupValues[1].replace(",", "").replace("₹", "")
                val multiplier = when {
                    valueStr.lowercase().contains("lakh") || valueStr.lowercase().contains("lac") || valueStr.lowercase().endsWith("l") -> 100000
                    valueStr.lowercase().contains("crore") || valueStr.lowercase().endsWith("cr") -> 10000000
                    valueStr.lowercase().endsWith("k") -> 1000
                    else -> 1
                }
                
                val numericPart = valueStr.replace(Regex("[^0-9.]"), "")
                val value = numericPart.toDoubleOrNull() ?: continue
                return (value * multiplier).toLong()
            }
        }
        
        return null
    }
    
    private fun extractDate(input: String): String? {
        val months = mapOf(
            "january" to 1, "jan" to 1,
            "february" to 2, "feb" to 2,
            "march" to 3, "mar" to 3,
            "april" to 4, "apr" to 4,
            "may" to 5,
            "june" to 6, "jun" to 6,
            "july" to 7, "jul" to 7,
            "august" to 8, "aug" to 8,
            "september" to 9, "sep" to 9, "sept" to 9,
            "october" to 10, "oct" to 10,
            "november" to 11, "nov" to 11,
            "december" to 12, "dec" to 12
        )
        
        val yearPattern = Regex("(20[2-9][0-9])")
        val yearMatch = yearPattern.find(input)
        
        for ((monthName, monthNum) in months) {
            if (input.lowercase().contains(monthName)) {
                val year = yearMatch?.groupValues?.get(1)?.toIntOrNull() ?: LocalDate.now().year + 1
                return LocalDate.of(year, monthNum, 1).format(dateFormatter)
            }
        }
        
        return null
    }
    
    private fun extractGoalName(input: String): String? {
        val words = input.split(" ").filter { it.length > 3 }
        val stopWords = setOf("want", "need", "save", "saving", "goal", "money", "rupees", "for", "the", "and", "with")
        
        val candidates = words.filter { !stopWords.contains(it.lowercase()) }
        return candidates.firstOrNull()?.replaceFirstChar { it.uppercase() }
    }
    
    private fun redistributeShares(goals: MutableList<ProposedGoal>) {
        if (goals.isEmpty()) return
        
        val totalShare = goals.sumOf { it.suggestedShare }
        if (totalShare == 100) return
        
        val factor = 100.0 / totalShare
        var remaining = 100
        
        for (i in 0 until goals.size - 1) {
            val newShare = (goals[i].suggestedShare * factor).toInt()
            goals[i] = goals[i].copy(suggestedShare = newShare)
            remaining -= newShare
        }
        
        goals[goals.size - 1] = goals[goals.size - 1].copy(suggestedShare = remaining)
    }
}
