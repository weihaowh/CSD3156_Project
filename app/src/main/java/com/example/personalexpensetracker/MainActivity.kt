package com.example.personalexpensetracker

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import android.widget.Toast
import android.net.Uri
import android.app.Activity
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher

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


