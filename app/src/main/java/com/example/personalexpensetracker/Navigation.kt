package com.example.personalexpensetracker

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

sealed class Screen(val route: String) {
    object Overview : Screen("overview")
    object AddExpense : Screen("addExpense")
    object SelectCategory : Screen("selectCategory")
}

@Composable
fun ExpenseApp() {
    val context = LocalContext.current
    val expenses = remember { mutableStateListOf<Expense>() }
    val navController = rememberNavController()

    // Load expenses when the app starts
    LaunchedEffect(Unit) {
        expenses.addAll(ExpenseDataManager.loadExpenses(context))
    }

    // Pass navController to NavHost
    NavHost(navController = navController, startDestination = Screen.Overview.route) {
        composable(Screen.Overview.route) { OverviewScreen(navController, expenses) }
        composable(Screen.AddExpense.route) { AddExpenseScreen(navController, expenses) }
        composable(Screen.SelectCategory.route) {
            CategorySelectionScreen(navController) { selectedCategory ->
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("category", selectedCategory)
            }
        }
        composable("editExpense/{expenseIndex}") { backStackEntry ->
            val index = backStackEntry.arguments?.getString("expenseIndex")?.toIntOrNull()
            if (index != null && index in expenses.indices) {
                EditExpenseScreen(navController, expenses, index)
            } else {
                Toast.makeText(context, "Invalid expense", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            }
        }

    }
}
