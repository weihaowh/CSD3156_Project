// File: CategoryIcon.kt
package com.example.personalexpensetracker

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import org.tensorflow.lite.support.label.Category

@Composable
fun CategoryIcon(category: String, modifier: Modifier = Modifier) {
    // Map each category to a Material icon (adjust as needed)
    val icon: ImageVector = when (category) {
        "Food" -> Icons.Filled.Star
        "Transport" -> Icons.Filled.Place
        "Shopping" -> Icons.Filled.ShoppingCart
        "Entertainment" -> Icons.Filled.Notifications
        "Utilities" -> Icons.Filled.Home
        "Others" -> Icons.Filled.Check
        else -> Icons.Filled.Add
    }
    Icon(
        // painter = painterResource(id = iconRes) <- If want to use drawables instead
        imageVector = icon,
        contentDescription = category,
        modifier = modifier
    )
}