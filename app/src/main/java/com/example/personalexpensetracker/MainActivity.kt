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
import androidx.core.app.ActivityCompat.startActivityForResult


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

@Serializable
data class Expense(
    val category: String,
    val amount: Double,
    val description: String? = null,
    val imageUri: String? = null
)

@Composable
fun ExpenseApp() {
    val navController = rememberNavController()
    val expenses = remember { mutableStateListOf<Expense>() } // Shared state

    NavHost(navController, startDestination = "overview") {
        composable(Screen.Overview.route) { OverviewScreen(navController,expenses) }
        composable(Screen.AddExpense.route) { AddExpenseScreen(navController,expenses) }
        composable(Screen.SelectCategory.route) {
            CategorySelectionScreen(navController) { selectedCategory ->
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("category", selectedCategory)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(navController: NavController, expenses: MutableList<Expense>) {
    var showBottomSheet by remember { mutableStateOf(false) } // State to control bottom sheet visibility
    val context = LocalContext.current

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
            FloatingActionButton(onClick = { showBottomSheet = true }) {
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
                text = "Total Expense: $${expenses.sumOf { it.amount }}",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Expense List
            LazyColumn(Modifier.fillMaxSize()) {
                items(expenses.size) { index ->
                    val expense = expenses[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Category: ${expense.category}", style = MaterialTheme.typography.bodyLarge)
                            Text("Description: ${expense.description ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                            Text("Amount: $${expense.amount}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        // Bottom Sheet for FAB Options
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false }
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Add Expense",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Button(
                        onClick = {
                            showBottomSheet = false
                            (context as MainActivity).requestCameraPermission()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Camera")
                    }

                    Button(
                        onClick = {
                            showBottomSheet = false
                            navController.navigate("addExpense") // Navigate to AddExpenseScreen
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Manually")
                    }
                }
            }
        }
    }
}

@Composable
fun AddExpenseScreen(navController: NavController, expenses: MutableList<Expense>) {
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
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
                Text(
                    text = if (category.isEmpty()) "Select Category" else "Category: $category"
                )
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
                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue == null || amountValue <= 0) {
                        Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                    } else {
                        expenses.add(
                            Expense(
                                category = category,
                                amount = amountValue,
                                description = description.ifBlank { null }
                            )
                        )
                        focusManager.clearFocus()
                        navController.popBackStack()
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

