package com.piplanner.android.ui.navigation

sealed class NavRoutes(val route: String) {
    
    object Welcome : NavRoutes("welcome")
    object Accounts : NavRoutes("accounts")
    object Consent : NavRoutes("consent")
    object BalanceAuto : NavRoutes("balance_auto")
    object UpdateBalance : NavRoutes("update_balance")
    object ManualBalance : NavRoutes("manual_balance")
    object UpiPin : NavRoutes("upi_pin")
    object BalanceFetched : NavRoutes("balance_fetched")
    object GoalChat : NavRoutes("goal_chat")
    object GoalForm : NavRoutes("goal_form?goalId={goalId}") {
        fun createRoute(goalId: String? = null) = if (goalId != null) "goal_form?goalId=$goalId" else "goal_form"
    }
    object InflationPopup : NavRoutes("inflation_popup")
    object OpeningSplit : NavRoutes("opening_split")
    
    object MainShell : NavRoutes("main")
    object GoalsTab : NavRoutes("goals")
    object HistoryTab : NavRoutes("history")
    object AskTab : NavRoutes("ask")
    object Settings : NavRoutes("settings")
    
    object GoalDetail : NavRoutes("goal_detail/{goalId}") {
        fun createRoute(goalId: String) = "goal_detail/$goalId"
    }
    object Transfer : NavRoutes("transfer?fromGoalId={fromGoalId}&toGoalId={toGoalId}") {
        fun createRoute(fromGoalId: String? = null, toGoalId: String? = null): String {
            val params = mutableListOf<String>()
            fromGoalId?.let { params.add("fromGoalId=$it") }
            toGoalId?.let { params.add("toGoalId=$it") }
            return if (params.isNotEmpty()) "transfer?${params.joinToString("&")}" else "transfer"
        }
    }
    object Withdrawal : NavRoutes("withdrawal")
    object SyncBalance : NavRoutes("sync_balance")
    object AssignCredit : NavRoutes("assign_credit/{entryId}") {
        fun createRoute(entryId: String) = "assign_credit/$entryId"
    }
    object StandingSplit : NavRoutes("standing_split")
    object DeleteGoal : NavRoutes("delete_goal/{goalId}") {
        fun createRoute(goalId: String) = "delete_goal/$goalId"
    }
}
