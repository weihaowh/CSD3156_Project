package com.example.personalexpensetracker

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import android.widget.Toast
import android.net.Uri
import java.io.File
import androidx.core.content.FileProvider
import android.app.Activity
import android.provider.MediaStore
import android.view.View
import androidx.compose.ui.text.TextStyle
import androidx.core.app.ActivityCompat.startActivityForResult
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.app.DatePickerDialog
import android.app.TimePickerDialog

class MainActivity : ComponentActivity() {
    private val CAMERA_PERMISSION_CODE = 100
    private var capturedImageUri: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ExpenseApp()
            }
        }
    }
    fun requestCameraPermission() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        }
    }

    private fun openCamera() {
        val imageUri = createImageUri(this)
        capturedImageUri = imageUri
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        }
        startActivityForResult(intent, CAMERA_PERMISSION_CODE)
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_PERMISSION_CODE && resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "Image saved at: $capturedImageUri", Toast.LENGTH_LONG).show()
        }
    }
}

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

    // Navigation between screens
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
        composable("editExpense/{index}") { backStackEntry ->
            val index = backStackEntry.arguments?.getString("index")?.toIntOrNull()
            if (index != null) {
                EditExpenseScreen(navController, expenses, index)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(navController: NavController, expenses: MutableList<Expense>) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) } // Dialog state for confirmation

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personal Expense Tracker") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("addExpense") }) {
                Text("+")
            }
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Total Expense Overview
            Text(
                text = "Total Expense: $${"%.2f".format(expenses.sumOf { it.amount })}",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Title Row with "Expense List" and "Clear All" Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Expense List",
                    style = MaterialTheme.typography.headlineSmall
                )

                // "Clear All" Button
                Button(
                    onClick = { showDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Clear All", color = Color.White)
                }
            }

            // Expense List
            LazyColumn(Modifier.fillMaxSize()) {
                items(expenses.size) { index ->
                    val expense = expenses[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Category: ${expense.category}", style = MaterialTheme.typography.bodyLarge)
                                Text("Description: ${expense.description ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                                Text("Amount: $${"%.2f".format(expense.amount)}", style = MaterialTheme.typography.bodyMedium)
                                Text("Date: ${expense.dateTime}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }

                            // Edit Button
                            IconButton(
                                onClick = { navController.navigate("editExpense/$index") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_menu_edit),
                                    contentDescription = "Edit",
                                    tint = Color.Blue
                                )
                            }

                            // Delete Button
                            IconButton(
                                onClick = {
                                    ExpenseDataManager.deleteExpense(context, expenses, index)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_menu_delete),
                                    contentDescription = "Delete",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog for "Clear All" Button
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Clear All Expenses?") },
            text = { Text("This will delete all expenses permanently. Are you sure?") },
            confirmButton = {
                Button(
                    onClick = {
                        expenses.clear()
                        ExpenseDataManager.saveExpenses(context, expenses)
                        showDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Yes, Clear All", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}


@Composable
fun AddExpenseScreen(navController: NavController, expenses: MutableList<Expense>) {
    var category by rememberSaveable { mutableStateOf("Others") }
    var description by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    val focusManager: FocusManager = LocalFocusManager.current
    val context = LocalContext.current

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    savedStateHandle?.getLiveData<String>("category")?.observeForever { selectedCategory ->
        category = selectedCategory
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Add Expense") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { navController.navigate(Screen.SelectCategory.route) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = if (category.isEmpty()) "Select Category" else "Category: $category")
            }

            TextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                singleLine = true
            )
            TextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Button(
                onClick = {
                    if (category.isEmpty()) {
                        Toast.makeText(context, "Please select a category", Toast.LENGTH_SHORT).show()
                    } else {
                        val amountValue = amount.toDoubleOrNull()
                        if (amountValue == null || amountValue <= 0) {
                            Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                        } else {
                            // Add new expense
                            val newExpense = Expense(
                                category = category,
                                amount = amountValue,
                                description = description.ifBlank { null }
                            )
                            expenses.add(newExpense)

                            // Save the updated list to file
                            ExpenseDataManager.saveExpenses(context, expenses)

                            // Clear input fields and return to previous screen
                            focusManager.clearFocus()
                            navController.popBackStack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Expense")
            }
        }
    }
}


@Composable
fun CategorySelectionScreen(navController: NavController, onCategorySelected: (String) -> Unit) {
    val categories = listOf("Food", "Transport", "Shopping", "Entertainment", "Utilities", "Other")
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Select Category") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Select a category for the expense:",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            LazyColumn {
                items(categories.size) { index ->
                    val category = categories[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                onCategorySelected(category)
                                navController.popBackStack()
                            }
                    ) {
                        Text(
                            text = category,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

private fun createImageUri(context: Context): Uri? {
    val fileName = "expense_image_${System.currentTimeMillis()}.jpg"
    val storageDir = File(context.filesDir, "images")

    if (!storageDir.exists()) {
        val dirCreated = storageDir.mkdir()
        if (!dirCreated) {
            Log.e("CreateImageUri", "Failed to create directory")
            return null
        }
    }

    val imageFile = File(storageDir, fileName)
    return try {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
    } catch (e: IllegalArgumentException) {
        Log.e("CreateImageUri", "FileProvider URI creation failed: ${e.message}")
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseScreen(navController: NavController, expenses: MutableList<Expense>, expenseIndex: Int) {
    val context = LocalContext.current
    val focusManager: FocusManager = LocalFocusManager.current

    // Load existing values
    var category by rememberSaveable { mutableStateOf(expenses[expenseIndex].category) }
    var description by rememberSaveable { mutableStateOf(expenses[expenseIndex].description ?: "") }
    var amount by rememberSaveable { mutableStateOf(expenses[expenseIndex].amount.toString()) }

    // Date & Time variables
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var selectedTime by rememberSaveable { mutableStateOf(LocalTime.now()) }

    // Automatically get the day of the week from the selected date
    val dayOfWeek = selectedDate.dayOfWeek.getDisplayName(
        java.time.format.TextStyle.FULL, Locale.getDefault()
    )

    // Date Picker Dialog
    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, day ->
            selectedDate = LocalDate.of(year, month + 1, day)
        },
        selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth
    )

    // Time Picker Dialog
    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour, minute ->
            selectedTime = LocalTime.of(hour, minute)
        },
        selectedTime.hour, selectedTime.minute, false
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Expense") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category Input
            TextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                singleLine = true
            )

            // Description Input
            TextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                singleLine = true
            )

            // Amount Input
            TextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Date Picker Button
            Button(
                onClick = { datePickerDialog.show() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "📅 Select Date: $selectedDate ($dayOfWeek)")
            }

            // Time Picker Button
            Button(
                onClick = { timePickerDialog.show() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "⏰ Select Time: ${selectedTime.format(DateTimeFormatter.ofPattern("hh:mm a"))}")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val updatedAmount = amount.toDoubleOrNull()
                        if (updatedAmount != null && updatedAmount > 0) {
                            val updatedExpense = Expense(
                                category = category,
                                amount = updatedAmount,
                                description = description.ifBlank { null },
                                dateTime = "$selectedDate ($dayOfWeek) ${selectedTime.format(DateTimeFormatter.ofPattern("hh:mm a"))}"
                            )
                            expenses[expenseIndex] = updatedExpense
                            ExpenseDataManager.saveExpenses(context, expenses)
                            focusManager.clearFocus()
                            navController.popBackStack()
                        } else {
                            Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save Changes")
                }

                Button(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}
