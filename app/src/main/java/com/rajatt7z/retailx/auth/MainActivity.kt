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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.rajatt7z.retailx.AdminDashboardActivity
import com.rajatt7z.retailx.EmployeeDashboardActivity
import com.rajatt7z.retailx.R
import com.rajatt7z.retailx.databinding.ActivityMainBinding
import com.rajatt7z.retailx.utils.Resource
import com.rajatt7z.retailx.viewmodel.AuthViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: AuthViewModel by viewModels()

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
    }

    private fun setupViewPager() {
        val onboardingItems = listOf(
            OnboardingItem(
                title = "Inventory Control At One Place",
                description = "Manage your stock efficiently and effortlessly",
                animationRes = R.raw.inventory
            ),
            OnboardingItem(
                title = "Cloud Sync",
                description = "Real-time backup with Firebase & RoomDB",
                animationRes = R.raw.sync
            ),
            OnboardingItem(
                title = "Material 3",
                description = "Modern, expressive, and beautiful UI design",
                animationRes = R.raw.material
            ),
            OnboardingItem(
                title = "Get Started",
                description = "Login below to access your dashboard",
                animationRes = R.raw.retailx
            )
        )

        val adapter = OnboardingAdapter(onboardingItems)
        binding.viewPager.adapter = adapter
        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL

        // Attach TabLayout (Dots)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, _ ->
            tab.icon = ContextCompat.getDrawable(this, R.drawable.tab_pager_selector)
        }.attach()

        // Page Change Callback for Button Visibility
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == onboardingItems.size - 1) {
                    // Final Page: Show Buttons
                    binding.loginBusinessBtn.visibility = View.VISIBLE
                    binding.loginCustomerBtn.visibility = View.VISIBLE
                    // Optional: Fade in animation
                    binding.loginBusinessBtn.alpha = 0f
                    binding.loginCustomerBtn.alpha = 0f
                    binding.loginBusinessBtn.animate().alpha(1f).setDuration(300).start()
                    binding.loginCustomerBtn.animate().alpha(1f).setDuration(300).start()
                } else {
                    // Other Pages: Hide Buttons
                    binding.loginBusinessBtn.visibility = View.GONE
                    binding.loginCustomerBtn.visibility = View.GONE
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

        binding.loginCustomerBtn.setOnClickListener {
            startActivity(Intent(this, EmployeeLogin::class.java))
        }

        binding.loginBusinessBtn.setOnClickListener {
            startActivity(Intent(this, AdminLogin::class.java))
        }
    }

    private fun checkUserSession() {
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
                    binding.loginCustomerBtn.isEnabled = false
                    binding.loginBusinessBtn.isEnabled = false
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
                            binding.loginCustomerBtn.isEnabled = true
                            binding.loginBusinessBtn.isEnabled = true
                            Toast.makeText(this, "Unknown user type", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.loginCustomerBtn.isEnabled = true
                    binding.loginBusinessBtn.isEnabled = true
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
