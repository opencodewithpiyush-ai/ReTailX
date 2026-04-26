package com.rajatt7z.retailx.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.rajatt7z.retailx.AdminDashboardActivity
import com.rajatt7z.retailx.EmployeeDashboardActivity
import com.rajatt7z.retailx.R
import com.rajatt7z.retailx.databinding.ActivityMainBinding
import com.rajatt7z.retailx.utils.Resource
import com.rajatt7z.retailx.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : com.rajatt7z.retailx.utils.BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: AuthViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
        } else {
            // Permission denied
             Toast.makeText(this, "Notifications are enabled for better experience", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupViewPager()
        setupButtons()
        observeViewModel()
        checkUserSession()
        
        checkNotificationPermission()
    }
    
    private fun checkNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun handleNetworkStatus(status: com.rajatt7z.retailx.utils.ConnectivityObserver.Status) {
        super.handleNetworkStatus(status)
        if (status == com.rajatt7z.retailx.utils.ConnectivityObserver.Status.Available) {
            // Auto-retry session check
            checkUserSession()
        }
    }

    private fun setupViewPager() {
        val onboardingData = listOf(
            Triple("Inventory Control At One Place", "Manage your stock efficiently and effortlessly", R.raw.inventory),
            Triple("Cloud Sync", "Real-time backup with Firebase & RoomDB", R.raw.sync),
            Triple("Material 3", "Modern, expressive, and beautiful UI design", R.raw.material),
            Triple("Get Started", "Login below to access your dashboard", R.raw.retailx)
        )

        // Initial empty adapter or with placeholders to avoid blocking
        // Better: Load compositions asynchronously and then set the adapter
        
        com.airbnb.lottie.LottieCompositionFactory.clearCache(this) // Optional: Clear cache if needed, but usually not for onboarding

        // Use lifecycleScope to load compositions asynchronously on IO thread
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val finalItems = onboardingData.map { (title, description, resId) ->
                var composition: com.airbnb.lottie.LottieComposition? = null
                try {
                    val result = com.airbnb.lottie.LottieCompositionFactory.fromRawResSync(this@MainActivity, resId)
                    composition = result.value
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                OnboardingItem(title, description, resId, composition)
            }

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                val adapter = OnboardingAdapter(finalItems)
                binding.viewPager.adapter = adapter

                // Re-attach TabLayoutMediator because adapter is set asynchronously
                TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, _ ->
                    tab.icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.tab_pager_selector)
                }.attach()
            }
        }

        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL

        // Page Change Callback for Button Visibility
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == onboardingData.size - 1) {
                    // Final Page: Show Button
                    binding.continueBtn.visibility = View.VISIBLE
                    // Optional: Fade in animation
                    binding.continueBtn.alpha = 0f
                    binding.continueBtn.animate().alpha(1f).setDuration(300).start()
                } else {
                    // Other Pages: Hide Button
                    binding.continueBtn.visibility = View.GONE
                }
            }
        })
    }

    private fun setupButtons() {
        binding.cardInfo.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("App Information")
                .setMessage(
                    "ReTailX\n" +
                            "Store Management System\n\n" +
                            "Version: 1.0.0\n" +
                            "Developed by: Rajat Kevat"
                )
                .setIcon(R.drawable.rounded_info_24)
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        binding.continueBtn.setOnClickListener {
            showLoginBottomSheet()
        }
    }

    private fun showLoginBottomSheet() {
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_login, null)
        bottomSheetDialog.setContentView(view)

        val btnContinue = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnContinue)
        val etId = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etEmail)
        val etPassword = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPassword)
        val tilPassword = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilPassword)
        val tilId = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilEmail)
        val progressBar = view.findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(R.id.sheetProgress)

        var isIdVerified = false
        var adminData: Map<String, Any>? = null

        // Observe adminDetails for ID verification
        viewModel.adminDetails.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    btnContinue.isEnabled = false
                }
                is Resource.Success -> {
                    progressBar.visibility = View.GONE
                    btnContinue.isEnabled = true
                    adminData = resource.data
                    if (adminData != null) {
                        isIdVerified = true
                        tilId.isEnabled = false // Disable ID field
                        tilPassword.visibility = View.VISIBLE // Show password field
                        btnContinue.text = "Login"
                        etPassword.requestFocus()
                    }
                }
                is Resource.Error -> {
                    progressBar.visibility = View.GONE
                    btnContinue.isEnabled = true
                    etId.error = resource.message ?: "Invalid ReTailX ID"
                }
            }
        }

        btnContinue.setOnClickListener {
            val id = etId.text.toString().trim()
            if (!isIdVerified) {
                if (id.isNotEmpty()) {
                    viewModel.fetchAdminDetails(id)
                } else {
                    etId.error = "Please enter ReTailX ID"
                }
            } else {
                val password = etPassword.text.toString().trim()
                if (password.isNotEmpty()) {
                    val correctPassword = adminData?.get("password") as? String
                    if (password == correctPassword) {
                        Toast.makeText(this, "Welcome, ${adminData?.get("name")}", Toast.LENGTH_SHORT).show()
                        bottomSheetDialog.dismiss()
                        startActivity(Intent(this, com.rajatt7z.retailx.AdminDashboardActivity::class.java))
                        finish()
                    } else {
                        etPassword.error = "Incorrect Password"
                    }
                } else {
                    etPassword.error = "Please enter password"
                }
            }
        }

        bottomSheetDialog.show()
    }

    private fun checkUserSession() {
        if (!com.rajatt7z.retailx.utils.NetworkUtils.isInternetAvailable(this)) {
            return
        }
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            viewModel.fetchUserDetails(user.uid)
        }
    }

    private fun observeViewModel() {
        viewModel.userDetails.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.continueBtn.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val data = resource.data
                    if (data != null) {
                        val userType = data["userType"] as? String
                        if (userType == "admin") {
                            startActivity(Intent(this, AdminDashboardActivity::class.java))
                            finish()
                        } else if (userType == "employee") {
                            startActivity(Intent(this, EmployeeDashboardActivity::class.java))
                            finish()
                        } else {
                            binding.continueBtn.isEnabled = true
                            Toast.makeText(this, "Unknown user type", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.continueBtn.isEnabled = true
                    // Only show session expired if we actually tried to check a session
                    if (FirebaseAuth.getInstance().currentUser != null) {
                         Toast.makeText(this, "Session expired or invalid", Toast.LENGTH_SHORT).show()
                         FirebaseAuth.getInstance().signOut()
                    }
                }
            }
        }
    }
}
