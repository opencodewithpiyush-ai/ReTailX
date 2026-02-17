package com.rajatt7z.retailx.fragments.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.rajatt7z.retailx.R
import com.rajatt7z.retailx.databinding.FragmentSecuritySettingsBinding
import com.rajatt7z.retailx.utils.BiometricHelper
import com.rajatt7z.retailx.utils.SecurityPreferences

class SecuritySettingsFragment : Fragment() {

    private var _binding: FragmentSecuritySettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var securityPrefs: SecurityPreferences
    private lateinit var biometricHelper: BiometricHelper

    private val timeoutOptions = listOf("Never", "1 minute", "5 minutes", "15 minutes", "30 minutes")
    private val timeoutValues = listOf(0, 1, 5, 15, 30)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecuritySettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        securityPrefs = SecurityPreferences(requireContext())
        biometricHelper = BiometricHelper(requireActivity())

        setupBiometricToggle()
        setupPinToggle()
        setupTimeoutDropdown()
        updateSecurityStatus()
    }

    private fun setupBiometricToggle() {
        val canUseBiometric = biometricHelper.canAuthenticate()
        binding.switchBiometric.isEnabled = canUseBiometric
        binding.switchBiometric.isChecked = securityPrefs.isBiometricEnabled && canUseBiometric

        if (!canUseBiometric) {
            binding.tvBiometricStatus.text = "Biometric not available on this device"
        }

        binding.switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Verify biometric works before enabling
                biometricHelper.authenticate(
                    title = "Verify Biometric",
                    subtitle = "Confirm your identity to enable biometric login",
                    negativeButtonText = "Cancel",
                    onSuccess = {
                        securityPrefs.isBiometricEnabled = true
                        updateSecurityStatus()
                        Toast.makeText(context, "Biometric login enabled", Toast.LENGTH_SHORT).show()
                    },
                    onError = {
                        binding.switchBiometric.isChecked = false
                    },
                    onFailed = {
                        binding.switchBiometric.isChecked = false
                        Toast.makeText(context, "Biometric verification failed", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                securityPrefs.isBiometricEnabled = false
                updateSecurityStatus()
            }
        }
    }

    private fun setupPinToggle() {
        binding.switchPin.isChecked = securityPrefs.isPinEnabled
        binding.btnChangePin.visibility = if (securityPrefs.isPinEnabled) View.VISIBLE else View.GONE

        binding.switchPin.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showSetPinDialog()
            } else {
                // Verify current PIN before removing
                if (securityPrefs.isPinEnabled) {
                    showVerifyPinDialog(
                        onSuccess = {
                            securityPrefs.removePin()
                            binding.btnChangePin.visibility = View.GONE
                            updateSecurityStatus()
                            Toast.makeText(context, "PIN removed", Toast.LENGTH_SHORT).show()
                        },
                        onCancel = {
                            binding.switchPin.isChecked = true
                        }
                    )
                }
            }
        }

        binding.btnChangePin.setOnClickListener {
            showVerifyPinDialog(
                onSuccess = { showSetPinDialog() }
            )
        }
    }

    private fun showSetPinDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_set_pin, null)
        val etPin = dialogView.findViewById<TextInputEditText>(R.id.etNewPin)
        val etConfirm = dialogView.findViewById<TextInputEditText>(R.id.etConfirmPin)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Set PIN")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val pin = etPin.text.toString()
                val confirm = etConfirm.text.toString()

                when {
                    pin.length != 4 -> {
                        Toast.makeText(context, "PIN must be 4 digits", Toast.LENGTH_SHORT).show()
                        binding.switchPin.isChecked = securityPrefs.isPinEnabled
                    }
                    pin != confirm -> {
                        Toast.makeText(context, "PINs don't match", Toast.LENGTH_SHORT).show()
                        binding.switchPin.isChecked = securityPrefs.isPinEnabled
                    }
                    else -> {
                        securityPrefs.setPin(pin)
                        binding.btnChangePin.visibility = View.VISIBLE
                        updateSecurityStatus()
                        Toast.makeText(context, "PIN set successfully", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                binding.switchPin.isChecked = securityPrefs.isPinEnabled
            }
            .setOnCancelListener {
                binding.switchPin.isChecked = securityPrefs.isPinEnabled
            }
            .show()
    }

    private fun showVerifyPinDialog(onSuccess: () -> Unit, onCancel: (() -> Unit)? = null) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_verify_pin, null)
        val etPin = dialogView.findViewById<TextInputEditText>(R.id.etCurrentPin)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Enter Current PIN")
            .setView(dialogView)
            .setPositiveButton("Verify") { _, _ ->
                val pin = etPin.text.toString()
                if (securityPrefs.verifyPin(pin)) {
                    onSuccess()
                } else {
                    Toast.makeText(context, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                    onCancel?.invoke()
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                onCancel?.invoke()
            }
            .setOnCancelListener {
                onCancel?.invoke()
            }
            .show()
    }

    private fun setupTimeoutDropdown() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, timeoutOptions)
        binding.actvTimeout.setAdapter(adapter)

        // Set current value
        val currentTimeout = securityPrefs.sessionTimeoutMinutes
        val index = timeoutValues.indexOf(currentTimeout)
        if (index >= 0) {
            binding.actvTimeout.setText(timeoutOptions[index], false)
        } else {
            binding.actvTimeout.setText(timeoutOptions[0], false)
        }

        binding.actvTimeout.setOnItemClickListener { _, _, position, _ ->
            securityPrefs.sessionTimeoutMinutes = timeoutValues[position]
            updateSecurityStatus()
        }
    }

    private fun updateSecurityStatus() {
        val parts = mutableListOf<String>()

        if (securityPrefs.isBiometricEnabled) {
            parts.add("✓ Biometric login enabled")
        } else {
            parts.add("✗ Biometric login disabled")
        }

        if (securityPrefs.isPinEnabled) {
            parts.add("✓ PIN login enabled")
        } else {
            parts.add("✗ PIN login disabled")
        }

        val timeout = securityPrefs.sessionTimeoutMinutes
        if (timeout > 0) {
            parts.add("✓ Session timeout: ${timeout}min")
        } else {
            parts.add("✗ Session timeout: Never")
        }

        binding.tvSecurityStatus.text = parts.joinToString("\n")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
