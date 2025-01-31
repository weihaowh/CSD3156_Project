package com.example.personalexpensetracker

import android.content.Context
import kotlinx.serialization.json.*
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.Serializable

@Serializable
data class Expense(
    val category: String,
    val amount: Double,
    val description: String? = null,
    val imageUri: String? = null,
    val dateTime: String = getCurrentDateTime() // Automatically assigns date and time
)

// Function to get the current date and time
fun getCurrentDateTime(): String {
    val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy HH:mm:ss")
    return LocalDateTime.now().format(formatter)
}

object ExpenseDataManager {
    private const val FILE_NAME = "expenses.json"

    fun saveExpenses(context: Context, expenses: List<Expense>) {
        val json = Json.encodeToString(expenses)
        val file = File(context.filesDir, FILE_NAME)
        file.writeText(json)
    }

    fun loadExpenses(context: Context): MutableList<Expense> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return mutableListOf()

        return try {
            val json = file.readText()
            Json.decodeFromString(json)
        } catch (e: Exception) {
            e.printStackTrace()
            mutableListOf()
        }
    }

    fun deleteExpense(context: Context, expenses: MutableList<Expense>, index: Int) {
        expenses.removeAt(index)
        saveExpenses(context, expenses)
    }
}


