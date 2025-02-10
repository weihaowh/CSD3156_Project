package com.example.personalexpensetracker

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.os.Build
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.widget.Toast
import android.net.Uri
import java.io.File
import androidx.core.content.FileProvider
import android.app.Activity
import android.app.TimePickerDialog
import android.provider.MediaStore
import android.view.View
import androidx.core.app.ActivityCompat.startActivityForResult
import coil.compose.rememberAsyncImagePainter
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.personalexpensetracker.CategoryIcon

class MainActivity : ComponentActivity() {
    private val CAMERA_PERMISSION_CODE = 100
    private val CAMERA_REQUEST_CODE = 200
    private var capturedImageUri: Uri? = null
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private var currentNavController: NavController? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            currentNavController = navController // Store navController

            MaterialTheme {
                ExpenseApp()
            }
        }
        // Initialize the camera launcher
        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && capturedImageUri != null) {
                // Image was taken successfully, navigate to AddExpenseScreen
                Toast.makeText(this, "Image saved at: $capturedImageUri", Toast.LENGTH_LONG).show()
                currentNavController?.currentBackStackEntry?.savedStateHandle?.set("capturedImageUri", capturedImageUri.toString())
                currentNavController?.navigate(Screen.AddExpense.route)
            } else {
                // User canceled the camera, return to OverviewScreen
                currentNavController?.popBackStack(Screen.Overview.route, inclusive = false)
            }
        }
    }

    fun requestCameraPermission(context: Context, navController: NavController) {
        if (context.checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera(context, navController)
        } else {
            (context as Activity).requestPermissions(arrayOf(android.Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        }
    }

    private fun openCamera(context: Context, navController: NavController) {
        val imageUri = createImageUri(context)
        capturedImageUri = imageUri
        currentNavController = navController  // Store the navController

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        }
        //(context as Activity).startActivityForResult(intent, CAMERA_REQUEST_CODE)

        if (imageUri != null) {
            cameraLauncher.launch(imageUri)
            navController.currentBackStackEntry?.savedStateHandle?.set("capturedImageUri", imageUri.toString())
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




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(navController: NavController, expenses: MutableList<Expense>) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    // Initialize the selected month/year to the current month/year.
    var selectedMonth by remember { mutableIntStateOf(LocalDate.now().monthValue) }
    var selectedYear by remember { mutableIntStateOf(LocalDate.now().year) }

    // Launcher for image selection from the gallery remains the same...
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                Toast.makeText(context, "Selected Image: $uri", Toast.LENGTH_SHORT).show()
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.set("selectedImageUri", it.toString())
                navController.navigate(Screen.AddExpense.route)
            }
        }
    )


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
        // Compute the expense totals by category for the pie chart.
        val expenseByCategory: Map<String, Double> = expenses
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Display Total Expense
            Text(
                text = "Total Expense: \n$${expenses.sumOf { it.amount }}",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Use the new container with the header, centered pie chart, and legend.
            ExpenseChartContainer(
                expenseData = expenseByCategory,
                monthYear = "March '25", // Replace with dynamic data as needed.
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // Expense List Title
            Text(
                text = "Expense List",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Expense List
            LazyColumn(Modifier.fillMaxSize()) {
                items(expenses.size) { index ->
                    val expense = expenses[index]
                    var isExpanded by remember { mutableStateOf(false) }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { isExpanded = !isExpanded }, // Toggle expansion
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Display the category icon beside the category name
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CategoryIcon(
                                        category = expense.category,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = expense.category,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                val previewLength = 50
                                val descriptionText = expense.description ?: "No description"
                                val shouldTruncate = descriptionText.length > previewLength
                                val displayText = if (!isExpanded && shouldTruncate) {
                                    descriptionText.take(previewLength) + "..."
                                } else {
                                    descriptionText
                                }
                                Text(
                                    text = "Description: $displayText",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Amount: $${"%.2f".format(expense.amount)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Date: ${expense.dateTime}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                if (!isExpanded && shouldTruncate) {
                                    Text(
                                        text = "Tap to expand",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    if (index in expenses.indices) {
                                        navController.navigate("editExpense/$index")
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Invalid expense selection",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_menu_edit),
                                    contentDescription = "Edit",
                                    tint = Color.Blue
                                )
                            }

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

    // Bottom sheet implementation remains the same...
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
                        (context as MainActivity).requestCameraPermission(context, navController)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Camera")
                }
                Button(
                    onClick = {
                        showBottomSheet = false
                        galleryLauncher.launch("image/*")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Image Gallery")
                }
                Button(
                    onClick = {
                        showBottomSheet = false
                        navController.navigate("addExpense")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Manually")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(navController: NavController, expenses: MutableList<Expense>) {
    var category by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var imageUri by rememberSaveable { mutableStateOf<String?>(null) }
    val focusManager: FocusManager = LocalFocusManager.current
    val context = LocalContext.current

    // Retrieve selected/captured image URI
    val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
    LaunchedEffect(Unit) {
        // Set imageUri to the image captured by the camera
        savedStateHandle?.get<String>("capturedImageUri")?.let {
            imageUri = it
            savedStateHandle.remove<String>("capturedImageUri")
        }
        // Set imageUri to the image selected from the gallery
        savedStateHandle?.get<String>("selectedImageUri")?.let {
            imageUri = it
            savedStateHandle.remove<String>("selectedImageUri")
        }
    }
    savedStateHandle?.getLiveData<String>("category")?.observeForever { selectedCategory ->
        category = selectedCategory
        savedStateHandle.remove<String>("category")
    }

    // Function to extract text using OCR
    suspend fun extractTextFromImage(imageUri: Uri) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val bitmap = loadBitmapFromUri(context, imageUri)

        bitmap?.let {
            val inputImage = InputImage.fromBitmap(it, 0)
            try {
                val result = withContext(Dispatchers.IO) { recognizer.process(inputImage).await() }
                if (result.text.isNotEmpty()) {
                    description = result.text // Auto-fill the description field
                } else {
                    Toast.makeText(context, "No text found in receipt.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("OCR", "Error processing image: ${e.message}")
            }
        }
    }

    // Trigger OCR when an image is selected
    LaunchedEffect(imageUri) {
        imageUri?.let {
            extractTextFromImage(Uri.parse(it))
        }
    }

    Scaffold(
        topBar = {
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
            // Updated category selection button that includes the icon
            Button(
                onClick = { navController.navigate(Screen.SelectCategory.route) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Use the CategoryIcon composable
                    CategoryIcon(category = category, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (category.isEmpty()) "Select Category" else "$category")
                }
            }

            TextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth(),
                singleLine = true
            )
            TextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                modifier = Modifier
                    .fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Display the captured image
            imageUri?.let {
                Image(
                    painter = rememberAsyncImagePainter(it),
                    contentDescription = "Selected Receipt Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }

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
                                description = description.ifBlank { null },
                                imageUri = imageUri
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelectionScreen(navController: NavController, onCategorySelected: (String) -> Unit) {
    val categories = listOf("Food", "Transport", "Shopping", "Entertainment", "Utilities", "Other")
    Scaffold(
        topBar = {
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
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryIcon(category = category, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = category,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
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

// Function to convert URI to Bitmap
fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (e: Exception) {
        Log.e("OCR", "Failed to load image: ${e.message}")
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

    if (expenseIndex !in expenses.indices) {
        Toast.makeText(context, "Expense not found", Toast.LENGTH_SHORT).show()
        navController.popBackStack()
        return
    }

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
                        val amountValue = amount.toDoubleOrNull()
                        if (amountValue == null || amountValue <= 0) {
                            Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                        } else {
                            val updatedExpense = Expense(
                                category = category,
                                amount = amountValue,
                                description = description.ifBlank { null },
                                dateTime = "$selectedDate ($dayOfWeek) ${selectedTime.format(DateTimeFormatter.ofPattern("hh:mm a"))}"
                            )

                            // Trigger recomposition by replacing the entire list
                            expenses[expenseIndex] = updatedExpense
                            expenses.removeAt(expenseIndex)
                            expenses.add(expenseIndex, updatedExpense)

                            // Save to storage
                            ExpenseDataManager.saveExpenses(context, expenses)

                            // Clear focus & navigate back
                            focusManager.clearFocus()
                            navController.popBackStack()
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

