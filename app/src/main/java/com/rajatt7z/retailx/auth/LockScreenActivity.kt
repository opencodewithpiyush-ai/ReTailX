package com.rajatt7z.retailx.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.addCallback
import com.rajatt7z.retailx.R
import com.rajatt7z.retailx.databinding.ActivityLockScreenBinding
import com.rajatt7z.retailx.utils.BiometricHelper
import com.rajatt7z.retailx.utils.SecurityPreferences

class LockScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockScreenBinding
    private lateinit var securityPrefs: SecurityPreferences
    private lateinit var biometricHelper: BiometricHelper
    private val pinBuilder = StringBuilder()
    private val dots by lazy {
        listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4)
    }

    companion object {
        var isShowingLockScreen = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        isShowingLockScreen = true
        binding = ActivityLockScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        securityPrefs = SecurityPreferences(this)
        biometricHelper = BiometricHelper(this)

        setupGreeting()
        setupKeypad()
        setupBiometric()
        setupFullLogin()

        // Auto-trigger biometric on open if enabled
        if (securityPrefs.isBiometricEnabled && biometricHelper.canAuthenticate()) {
            triggerBiometric()
        }

        // Prevent back press
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing
            }
        })
    }

    private fun setupGreeting() {
        val name = securityPrefs.userName
        if (name.isNotEmpty()) {
            binding.tvGreeting.text = "Welcome back, $name"
        }

        // Show/hide PIN section based on config
        if (!securityPrefs.isPinEnabled) {
            binding.layoutPinDots.visibility = View.GONE
            binding.gridKeypad.visibility = View.GONE
            binding.tvSubtitle.text = "Use biometric to unlock"
        }

        // Show biometric button only if available
        if (!securityPrefs.isBiometricEnabled || !biometricHelper.canAuthenticate()) {
            binding.btnBiometric.visibility = View.INVISIBLE
        }

        // If max attempts reached, force full login
        if (securityPrefs.isMaxAttemptsReached) {
            forceFullLogin()
        }
    }

    private fun setupKeypad() {
        val buttons = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3,
            binding.btn4, binding.btn5, binding.btn6, binding.btn7,
            binding.btn8, binding.btn9
        )

        buttons.forEach { btn ->
            btn.setOnClickListener {
                if (pinBuilder.length < 4) {
                    pinBuilder.append(btn.text)
                    updateDots()
                    if (pinBuilder.length == 4) {
                        verifyPin()
                    }
                }
            }
        }

        binding.btnBackspace.setOnClickListener {
            if (pinBuilder.isNotEmpty()) {
                pinBuilder.deleteCharAt(pinBuilder.length - 1)
                updateDots()
                binding.tvError.visibility = View.GONE
            }
        }
    }

    private fun setupBiometric() {
        binding.btnBiometric.setOnClickListener {
            triggerBiometric()
        }
    }

    private fun triggerBiometric() {
        biometricHelper.authenticate(
            onSuccess = { onUnlockSuccess() },
            onError = { msg ->
                // User cancelled or pressed "Use PIN" — just stay on screen
            },
            onFailed = {
                // Biometric didn't match, stay on screen
            }
        )
    }

    private fun setupFullLogin() {
        binding.btnFullLogin.setOnClickListener {
            goToFullLogin()
        }
    }

    private fun verifyPin() {
        if (securityPrefs.verifyPin(pinBuilder.toString())) {
            onUnlockSuccess()
        } else {
            securityPrefs.failedAttempts = securityPrefs.failedAttempts + 1
            pinBuilder.clear()
            updateDots()

            if (securityPrefs.isMaxAttemptsReached) {
                forceFullLogin()
            } else {
                val remaining = 5 - securityPrefs.failedAttempts
                binding.tvError.text = "Wrong PIN. $remaining attempts remaining"
                binding.tvError.visibility = View.VISIBLE
                // Shake animation on dots
                val shake = AnimationUtils.loadAnimation(this, R.anim.shake)
                binding.layoutPinDots.startAnimation(shake)
            }
        }
    }

    private fun updateDots() {
        dots.forEachIndexed { index, dot ->
            dot.setBackgroundResource(
                if (index < pinBuilder.length) R.drawable.pin_dot_filled
                else R.drawable.pin_dot_empty
            )
        }
    }

    private fun onUnlockSuccess() {
        securityPrefs.resetFailedAttempts()
        securityPrefs.isLocked = false
        securityPrefs.recordActivity()
        isShowingLockScreen = false
        finish()
    }

    private fun forceFullLogin() {
        binding.layoutPinDots.visibility = View.GONE
        binding.gridKeypad.visibility = View.GONE
        binding.btnBiometric.visibility = View.GONE
        binding.tvSubtitle.text = "Too many failed attempts"
        binding.tvError.text = "Please login with your credentials"
        binding.tvError.visibility = View.VISIBLE
        binding.btnFullLogin.text = "Login with credentials"
    }

    private fun goToFullLogin() {
        securityPrefs.clearAll()
        securityPrefs.isLocked = false
        isShowingLockScreen = false

        val role = securityPrefs.userRole
        val loginClass = if (role == "admin") AdminLogin::class.java else EmployeeLogin::class.java
        val intent = Intent(this, loginClass)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }



    override fun onDestroy() {
        super.onDestroy()
        isShowingLockScreen = false
    }
}
