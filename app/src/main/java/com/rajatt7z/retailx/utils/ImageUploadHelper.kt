package com.rajatt7z.retailx.utils

import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import java.io.File

class ImageUploadHelper(
    private val fragment: Fragment,
    private val scope: CoroutineScope,
    private val onImageUploaded: (String) -> Unit
) {
    private var tempImageUri: Uri? = null

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
        Toast.makeText(fragment.context, "Uploading image...", Toast.LENGTH_SHORT).show()

        scope.launch(Dispatchers.IO) {
            try {
                val context = fragment.requireContext()
                val inputStream = context.contentResolver.openInputStream(uri)
                val fileBytes = inputStream?.readBytes() ?: throw Exception("Could not read image file")
                inputStream.close()

                val requestBody = okhttp3.RequestBody.create("image/jpeg".toMediaType(), fileBytes)
                val body = okhttp3.MultipartBody.Part.createFormData("file", "image.jpg", requestBody)

                val retrofit = retrofit2.Retrofit.Builder()
                    .baseUrl("https://retailxdev.onrender.com/")
                    .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                    .build()
                
                val service = retrofit.create(com.rajatt7z.retailx.network.NotificationService::class.java)
                val response = service.uploadImage(body)

                withContext(Dispatchers.Main) {
                    onImageUploaded(response.url)
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
