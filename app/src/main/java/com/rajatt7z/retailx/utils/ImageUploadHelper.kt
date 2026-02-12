package com.rajatt7z.retailx.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.rajatt7z.retailx.R
import com.rajatt7z.retailx.repository.ProductRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImageUploadHelper(
    private val fragment: Fragment,
    private val scope: CoroutineScope,
    private val onImageUploaded: (String) -> Unit
) {
    private var tempImageUri: Uri? = null
    private val repository = ProductRepository()

    private val pickMedia = fragment.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            uploadImage(uri)
        }
    }

    private val takePicture = fragment.registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempImageUri != null) {
            uploadImage(tempImageUri!!)
        }
    }

    private val requestCameraPermission = fragment.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(fragment.context, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    fun showImagePicker() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(fragment.requireContext())
            .setTitle("Change Profile Picture")
            .setItems(arrayOf("Choose from Gallery", "Take Photo")) { _, which ->
                when (which) {
                    0 -> pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    1 -> checkCameraPermissionAndLaunch()
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndLaunch() {
        val permission = android.Manifest.permission.CAMERA
        if (androidx.core.content.ContextCompat.checkSelfPermission(fragment.requireContext(), permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            requestCameraPermission.launch(permission)
        }
    }

    private fun launchCamera() {
        val context = fragment.requireContext()
        // Ensure cache directory exists
        val cacheDir = File(context.cacheDir, "camera_images")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        
        try {
            val tempFile = File.createTempFile("camera_img_", ".jpg", cacheDir).apply {
                // createNewFile() // createTempFile already creates it
                deleteOnExit()
            }
            tempImageUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            takePicture.launch(tempImageUri)
        } catch (e: Exception) {
            Toast.makeText(context, "Error creating temp file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadImage(uri: Uri) {
        // Show loading state if possible (optional: pass a callback for loading)
        Toast.makeText(fragment.context, "Uploading image...", Toast.LENGTH_SHORT).show()
        
        scope.launch(Dispatchers.IO) {
            try {
                val url = repository.uploadImage(fragment.requireContext(), uri)
                withContext(Dispatchers.Main) {
                    onImageUploaded(url)
                    Toast.makeText(fragment.context, "Image uploaded successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(fragment.context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
