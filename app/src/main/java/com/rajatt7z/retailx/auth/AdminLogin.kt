package com.rajatt7z.retailx.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.rajatt7z.retailx.AdminDashboardActivity
import com.rajatt7z.retailx.R
import com.rajatt7z.retailx.databinding.ActivityAdminLoginBinding
import com.rajatt7z.retailx.utils.Resource
import com.rajatt7z.retailx.viewmodel.AuthViewModel

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AdminLogin : com.rajatt7z.retailx.utils.BaseActivity() {

    private lateinit var binding: ActivityAdminLoginBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAdminLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Clear errors when user starts typing
        binding.etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.tilEmail.error = null
        }

        binding.etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.tilPassword.error = null
        }

        binding.btnLogin.setOnClickListener {
            if (!com.rajatt7z.retailx.utils.NetworkUtils.isInternetAvailable(this)) {
                Snackbar.make(binding.root, "No internet connection", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (!validateInput(email, password)) {
                return@setOnClickListener
            }

            viewModel.loginUser(email, password, "admin")
        }

        binding.tvRegisterLink.setOnClickListener {
            startActivity(Intent(this, AdminReg::class.java))
        }

        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                viewModel.sendResetEmail(email)
            } else {
                showForgotPasswordDialog()
            }
        }

        observeViewModel()
    }

    private fun showForgotPasswordDialog() {
        // Create a programmatic TextInputLayout and TextInputEditText for a Material look
        val context = this
        val textInputLayout = com.google.android.material.textfield.TextInputLayout(context).apply {
            hint = "Email Address"
            boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
            setPadding(0, 0, 0, 0)
            val params = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(60, 0, 60, 0)
            layoutParams = params
            
            // Add start icon
            setStartIconDrawable(com.rajatt7z.retailx.R.drawable.rounded_lock_24)
        }

        val input = com.google.android.material.textfield.TextInputEditText(textInputLayout.context).apply {
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setSingleLine()
        }
        textInputLayout.addView(input)

        val container = android.widget.FrameLayout(context).apply {
            addView(textInputLayout)
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
            .setIcon(com.rajatt7z.retailx.R.drawable.rounded_lock_24)
            .setTitle("Forgot Password?")
            .setMessage("Enter your registered email address. We will send you a link to reset your password. \n\nCheck Spam Folder Too , It Should Be There...\n")
            .setView(container)
            .setPositiveButton("Send Link") { _, _ ->
                val email = input.text.toString().trim()
                if (email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    viewModel.sendResetEmail(email)
                } else {
                    Snackbar.make(binding.root, "Please enter a valid email", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun validateInput(email: String, password: String): Boolean {
        // Clear previous errors
        binding.tilEmail.error = null
        binding.tilPassword.error = null

        var isValid = true

        when {
            email.isEmpty() -> {
                binding.tilEmail.error = "Email is required"
                binding.etEmail.requestFocus()
                isValid = false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.tilEmail.error = "Please enter a valid email address"
                binding.etEmail.requestFocus()
                isValid = false
            }
        }

        when {
            password.isEmpty() -> {
                binding.tilPassword.error = "Password is required"
                if (isValid) binding.etPassword.requestFocus()
                isValid = false
            }
            password.length < 6 -> {
                binding.tilPassword.error = "Password must be at least 6 characters"
                if (isValid) binding.etPassword.requestFocus()
                isValid = false
            }
        }

        return isValid
    }

    private fun observeViewModel() {
        viewModel.authStatus.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnLogin.isEnabled = false
                    // Clear any previous errors
                    binding.tilEmail.error = null
                    binding.tilPassword.error = null
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    Snackbar.make(binding.root, resource.data ?: "Login successful", Snackbar.LENGTH_SHORT).show()

                    // Clear security state on new login
                    val securityPrefs = com.rajatt7z.retailx.utils.SecurityPreferences(this)
                    securityPrefs.isLocked = false
                    securityPrefs.resetFailedAttempts()
                    securityPrefs.recordActivity()
                    securityPrefs.userName = "Admin"
                    securityPrefs.userRole = "admin"

                    val intent = Intent(this, AdminDashboardActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true

                    // Show error in appropriate field or as Snackbar for general errors
                    val errorMessage = resource.message ?: "Login failed"
                    if (errorMessage.contains("email", ignoreCase = true)) {
                        binding.tilEmail.error = errorMessage
                        binding.etEmail.requestFocus()
                    } else if (errorMessage.contains("password", ignoreCase = true)) {
                        binding.tilPassword.error = errorMessage
                        binding.etPassword.requestFocus()
                    } else {
                        Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }

        
        viewModel.resetPasswordStatus.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                   binding.progressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Snackbar.make(binding.root, "Password reset email sent. Check your inbox.", Snackbar.LENGTH_LONG).show()
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Snackbar.make(binding.root, resource.message ?: "Failed to send reset email", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
}