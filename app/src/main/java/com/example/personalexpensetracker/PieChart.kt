// File: ExpenseChartContainer.kt
package com.example.personalexpensetracker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

/**
 * Returns a color for the given category.
 */
fun categoryColor(category: String): Color {
    return when (category) {
        "Food" -> Color(0xFFE57373)
        "Transport" -> Color(0xFF81C784)
        "Shopping" -> Color(0xFF64B5F6)
        "Entertainment" -> Color(0xFFFFB74D)
        "Utilities" -> Color(0xFFBA68C8)
        else -> Color.Gray
    }
}
@Composable
fun MonthYearSelector(
    selectedMonth: Int,
    selectedYear: Int,
    onMonthYearChange: (month: Int, year: Int) -> Unit
) {
    // Create a row with previous button, text, and next button.
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {
            // Calculate previous month
            val currentDate = LocalDate.of(selectedYear, selectedMonth, 1)
            val previous = currentDate.minusMonths(1)
            onMonthYearChange(previous.monthValue, previous.year)
        }) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
        }

        // Display the month name and year (e.g., "March 2025")
        Text(
            text = "${Month.of(selectedMonth).getDisplayName(TextStyle.FULL, Locale.getDefault())} $selectedYear",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = {
            // Calculate next month
            val currentDate = LocalDate.of(selectedYear, selectedMonth, 1)
            val next = currentDate.plusMonths(1)
            onMonthYearChange(next.monthValue, next.year)
        }) {
            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Next Month")
        }
    }
}
/**
 * A container composable that displays:
 * - A header (month/year) at the top-left.
 * - A pie chart centered in the box.
 * - A legend at the bottom-right (only for categories with nonzero amounts).
 *
 * @param expenseData A map where the key is the category and the value is the total expense amount.
 * @param monthYear The header text (for example, "March '25") to display at the top left.
 * @param modifier Modifier for sizing and styling.
 */
@Composable
fun ExpenseChartContainer(
    expenseData: Map<String, Double>,
    monthYear: String,
    modifier: Modifier = Modifier
) {
    // Use a Box as the container.
    Box(
        modifier = modifier
            .fillMaxWidth()
            // Set a fixed height for the container (adjust as needed)
            .height(250.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Top-left header for Month/Year.
        Text(
            text = monthYear,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        )

        // Bottom-right legend.
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
        ) {
            expenseData.forEach { (category, amount) ->
                if (amount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(color = categoryColor(category), shape = CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        // Centered pie chart.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                // Set a fixed size for the pie chart so it doesn't fill the entire container.
                .size(150.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val total = expenseData.values.sum()
                if (total <= 0) return@Canvas

                var startAngle = -90f  // Start at the top.
                expenseData.forEach { (category, amount) ->
                    val sweepAngle = (amount / total * 360f)
                    drawArc(
                        color = categoryColor(category),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle.toFloat(),
                        useCenter = true,
                        topLeft = Offset(0f, 0f),
                        size = Size(size.width, size.height)
                    )
                    startAngle += sweepAngle.toFloat()
                }
            }
        }
    }
}
