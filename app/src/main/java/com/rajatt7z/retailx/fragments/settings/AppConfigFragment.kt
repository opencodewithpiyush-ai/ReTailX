package com.rajatt7z.retailx.fragments.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.rajatt7z.retailx.databinding.FragmentAppConfigBinding
import java.io.File
import androidx.core.content.edit

class AppConfigFragment : Fragment() {

    private var _binding: FragmentAppConfigBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedPreferences: SharedPreferences
    private val firestore = FirebaseFirestore.getInstance()

    // Permission Launchers
    private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        binding.switchCameraAccess.isChecked = isGranted
        if (!isGranted) openSettingsDialog("Camera")
    }
    
    // Fine Location for better accuracy
    private val requestLocationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        binding.switchLocationAccess.isChecked = isGranted
        if (!isGranted) openSettingsDialog("Location")
    }
    
    private val requestStoragePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        binding.switchStorageAccess.isChecked = isGranted
        if (!isGranted) openSettingsDialog("Storage")
    }

    // Permission mapping for Storage based on SDK
    private val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedPreferences = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE)

        setupToolbar()
        setupSpinners()
        
        // Load Data
        refreshPermissionsUI() // Load Permissions
        loadLocalSettings()    // Load Local Prefs
        loadGlobalSettings()   // Load Firestore

        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionsUI() // Refresh in case user came back from Settings
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
    }

    private fun setupSpinners() {
        val languages = listOf("English", "Spanish", "French", "German", "Hindi")
        binding.spinnerLanguage.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, languages))

        val fonts = listOf("Circular Spotify", "Circular Std", "Montserrat Regular", "Roboto Regular")
        binding.spinnerFontFamily.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, fonts))

        val databases = listOf("Room Database", "Realm", "SQLite", "Firebase Local")
        binding.spinnerDatabaseType.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, databases))
        
        // Apply Font on Selection
        binding.spinnerFontFamily.setOnItemClickListener { parent, _, position, _ ->
            val selectedFont = parent.getItemAtPosition(position).toString()
            applyAppFont(selectedFont)
        }
    }

    // --- LOAD LOGIC ---

    private fun refreshPermissionsUI() {
        binding.switchCameraAccess.isChecked = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        binding.switchLocationAccess.isChecked = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        binding.switchStorageAccess.isChecked = ContextCompat.checkSelfPermission(requireContext(), storagePermission) == PackageManager.PERMISSION_GRANTED
    }

    private fun loadLocalSettings() {
        // UI Defaults
        binding.switchDarkMode.isChecked = sharedPreferences.getBoolean("dark_mode", false)
        
        // Language: Read from System, not Prefs
        val currentLocale = AppCompatDelegate.getApplicationLocales()[0]?.language ?: "en"
        val languageName = when(currentLocale) {
            "es" -> "Spanish"
            "fr" -> "French"
            "de" -> "German"
            "hi" -> "Hindi"
            else -> "English"
        }
        binding.spinnerLanguage.setText(languageName, false)
        
        // Font Defaults
        val savedFont = sharedPreferences.getString("font_family", "Circular Spotify") ?: "Circular Spotify"
        binding.spinnerFontFamily.setText(savedFont, false)
        applyAppFont(savedFont)
        
        // Notifs (Local Prefs only for now)
        binding.switchOrderNotifs.isChecked = sharedPreferences.getBoolean("order_notifs", true)
        binding.switchPushNotifications.isChecked = sharedPreferences.getBoolean("push_notifs", true)
    }

    private fun loadGlobalSettings() {
        // App Info - Read Only from Build Config
        binding.etAppName.setText(getString(com.rajatt7z.retailx.R.string.app_name))
        binding.etAppName.isEnabled = false
        
        binding.etVersionCode.setText(com.rajatt7z.retailx.BuildConfig.VERSION_CODE.toString())
        binding.etVersionCode.isEnabled = false
        
        binding.etVersionName.setText(com.rajatt7z.retailx.BuildConfig.VERSION_NAME)
        binding.etVersionName.isEnabled = false

        binding.btnSaveConfiguration.isEnabled = false // Disable until loaded
        binding.btnSaveConfiguration.text = "Loading..."
        
        firestore.collection("settings").document("app_config")
            .get()
            .addOnSuccessListener { document ->
                if (_binding != null) {
                    if (document.exists()) {
                        binding.switchMaintenance.isChecked = document.getBoolean("maintenance_mode") ?: false
                        binding.switchNewRegistrations.isChecked = document.getBoolean("allow_new_registrations") ?: true
                        binding.spinnerDatabaseType.setText(document.getString("database_type") ?: "Room Database", false)
                    }
                    binding.btnSaveConfiguration.isEnabled = true
                    binding.btnSaveConfiguration.text = "Save Configuration"
                }
            }
            .addOnFailureListener {
                if (_binding != null) {
                    Toast.makeText(requireContext(), "Failed to load Global Config", Toast.LENGTH_SHORT).show()
                    binding.btnSaveConfiguration.isEnabled = true
                    binding.btnSaveConfiguration.text = "Save Configuration"
                }
            }
    }

    // --- SETUP LISTENERS ---

    private fun setupListeners() {
        binding.btnSaveConfiguration.setOnClickListener {
            saveGlobalConfig()
            saveLocalConfig()
        }

        binding.btnClearCache.setOnClickListener {
            clearAppCache()
        }

        binding.btnClearData.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Clear Local Data")
                .setMessage("This will clear all local preferences, cache, and the local database. Cloud data (Firestore) will NOT be affected.\n\nAre you sure?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear") { _, _ ->
                    try {
                        // Clear SharedPreferences
                        sharedPreferences.edit { clear() }
                        
                        // Clear cache
                        clearAppCache()
                        
                        // Delete Room database
                        requireContext().deleteDatabase("retailx_database")
                        
                        Toast.makeText(requireContext(), "Local data cleared! Restart the app for changes to take effect.", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Failed to clear data: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
        }

        // Real Permission Toggles
        binding.switchCameraAccess.setOnClickListener {
            if (binding.switchCameraAccess.isChecked) requestCameraPermission.launch(Manifest.permission.CAMERA)
            else openSettingsDialog("Camera") // Cannot revoke programmatically
        }

        binding.switchLocationAccess.setOnClickListener {
            if (binding.switchLocationAccess.isChecked) requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            else openSettingsDialog("Location")
        }

        binding.switchStorageAccess.setOnClickListener {
            if (binding.switchStorageAccess.isChecked) requestStoragePermission.launch(storagePermission)
            else openSettingsDialog("Storage")
        }
    }

    // --- SAVE ACTION ---

    private fun saveLocalConfig() {
        val currentScale = sharedPreferences.getFloat("font_size", 16f)
        val newScale = binding.sliderFontSize.value

        sharedPreferences.edit {
            // Dark Mode
            val isDarkMode = binding.switchDarkMode.isChecked
            if (isDarkMode != sharedPreferences.getBoolean("dark_mode", false)) {
                AppCompatDelegate.setDefaultNightMode(if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
            }
            putBoolean("dark_mode", isDarkMode)

            // Language
            val selectedLang = binding.spinnerLanguage.text.toString()
            val currentLocale = AppCompatDelegate.getApplicationLocales()[0]?.language ?: "en"
            val newCode = when (selectedLang) {
                "Spanish" -> "es"
                "French" -> "fr"
                "German" -> "de"
                "Hindi" -> "hi"
                else -> "en"
            }
            
            var needsRecreate = false

            if (newCode != currentLocale) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(newCode))
                needsRecreate = true
            }

            // Font Size
            if (newScale != currentScale) {
                 putFloat("font_size", newScale)
                 needsRecreate = true
            }
            
            // Font Family
            val newFont = binding.spinnerFontFamily.text.toString()
            val oldFont = sharedPreferences.getString("font_family", "Circular Spotify")
            if (newFont != oldFont) {
                 putString("font_family", newFont)
                 needsRecreate = true
            }

            // Other Local Prefs
            putBoolean("order_notifs", binding.switchOrderNotifs.isChecked)
            putBoolean("push_notifs", binding.switchPushNotifications.isChecked)
            
            if (needsRecreate) {
                requireActivity().recreate()
            }
        }
    }

    private fun saveGlobalConfig() {
        val globalConfig = hashMapOf(
            // Removed App Info (Read-Only)
            "maintenance_mode" to binding.switchMaintenance.isChecked,
            "allow_new_registrations" to binding.switchNewRegistrations.isChecked,
            "database_type" to binding.spinnerDatabaseType.text.toString(),
            "last_updated" to com.google.firebase.Timestamp.now()
        )

        binding.btnSaveConfiguration.text = "Saving..."
        binding.btnSaveConfiguration.isEnabled = false

        firestore.collection("settings").document("app_config")
            .set(globalConfig, SetOptions.merge())
            .addOnSuccessListener {
                if (_binding != null) {
                    binding.btnSaveConfiguration.isEnabled = true
                    binding.btnSaveConfiguration.text = "Save Configuration"
                    Toast.makeText(requireContext(), "Configuration Updated Successfully!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                if (_binding != null) {
                    binding.btnSaveConfiguration.isEnabled = true
                    binding.btnSaveConfiguration.text = "Save Configuration"
                    Toast.makeText(requireContext(), "Error saving: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // --- UTILS ---

    private fun applyAppFont(fontName: String) {
        val fontRes = when (fontName) {
            "Circular Std" -> com.rajatt7z.retailx.R.font.circular_std
            "Montserrat Regular" -> com.rajatt7z.retailx.R.font.montserrat_regular
            "Roboto Regular" -> com.rajatt7z.retailx.R.font.roboto_regular
            else -> com.rajatt7z.retailx.R.font.circular_spotify // Default
        }

        try {
            val typeface = androidx.core.content.res.ResourcesCompat.getFont(requireContext(), fontRes)
            applyTypefaceToViewGroup(binding.root, typeface)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun applyTypefaceToViewGroup(view: View, typeface: android.graphics.Typeface?) {
        if (view is android.widget.TextView) {
            view.typeface = typeface
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyTypefaceToViewGroup(view.getChildAt(i), typeface)
            }
        }
    }

    private fun clearAppCache() {
        try {
            val cacheDir = requireContext().cacheDir
            if (cacheDir.isDirectory) {
                deleteDir(cacheDir)
                Toast.makeText(requireContext(), "Cache Cleared!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.list()
            if (children != null) {
                for (i in children.indices) {
                    val success = deleteDir(File(dir, children[i]))
                    if (!success) return false
                }
            }
            return dir.delete()
        } else if (dir != null && dir.isFile) {
            return dir.delete()
        }
        return false
    }

    private fun openSettingsDialog(permissionName: String) {
        // We cannot revoke permissions, so we send user to settings
        Toast.makeText(requireContext(), "To modify $permissionName, go to Settings", Toast.LENGTH_SHORT).show()
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", requireContext().packageName, null)
        intent.data = uri
        startActivity(intent)
        
        // Reset switch until they actually change it in settings
        refreshPermissionsUI() 
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
