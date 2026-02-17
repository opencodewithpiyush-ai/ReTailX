package com.rajatt7z.retailx.fragments.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.rajatt7z.retailx.databinding.FragmentTaxConfigBinding
import com.google.firebase.firestore.FirebaseFirestore

class TaxConfigFragment : Fragment() {

    private var _binding: FragmentTaxConfigBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val configDoc = db.collection("config").document("tax")

    private val currencies = listOf(
        "$ (USD)", "₹ (INR)", "€ (EUR)", "£ (GBP)", "¥ (JPY)",
        "A$ (AUD)", "C$ (CAD)", "CHF (Swiss Franc)", "R$ (BRL)"
    )
    private val currencySymbols = listOf("$", "₹", "€", "£", "¥", "A$", "C$", "CHF", "R$")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaxConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupCurrencyDropdown()
        loadCurrentConfig()
        
        binding.btnSaveTaxConfig.setOnClickListener {
            saveConfig()
        }
    }

    private fun setupCurrencyDropdown() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, currencies)
        binding.actvCurrency.setAdapter(adapter)
    }

    private fun loadCurrentConfig() {
        configDoc.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val name = doc.getString("name") ?: "Tax"
                val rate = doc.getDouble("rate") ?: 0.0
                val isEnabled = doc.getBoolean("isEnabled") ?: false
                val currency = doc.getString("currency") ?: "$"

                binding.switchTaxEnabled.isChecked = isEnabled
                binding.etTaxName.setText(name)
                binding.etTaxRate.setText(rate.toString())
                
                // Find matching currency display
                val idx = currencySymbols.indexOf(currency)
                if (idx >= 0) {
                    binding.actvCurrency.setText(currencies[idx], false)
                } else {
                    binding.actvCurrency.setText(currencies[0], false)
                }

                val statusText = if (isEnabled) "Enabled" else "Disabled"
                binding.tvCurrentConfig.text = "$name: $rate% ($statusText) | Currency: $currency"
            } else {
                binding.tvCurrentConfig.text = "No configuration saved yet"
                binding.actvCurrency.setText(currencies[0], false)
            }
        }.addOnFailureListener {
            binding.tvCurrentConfig.text = "Failed to load configuration"
        }
    }

    private fun saveConfig() {
        val taxName = binding.etTaxName.text.toString().trim()
        val taxRateStr = binding.etTaxRate.text.toString().trim()
        val isEnabled = binding.switchTaxEnabled.isChecked
        val selectedCurrencyText = binding.actvCurrency.text.toString()

        if (taxName.isEmpty()) {
            binding.etTaxName.error = "Required"
            return
        }

        val taxRate = taxRateStr.toDoubleOrNull()
        if (taxRate == null || taxRate < 0 || taxRate > 100) {
            binding.etTaxRate.error = "Enter 0-100"
            return
        }

        // Map display text to symbol
        val idx = currencies.indexOf(selectedCurrencyText)
        val currencySymbol = if (idx >= 0) currencySymbols[idx] else "$"

        val configData = mapOf(
            "name" to taxName,
            "rate" to taxRate,
            "isEnabled" to isEnabled,
            "currency" to currencySymbol
        )

        binding.btnSaveTaxConfig.isEnabled = false
        
        configDoc.set(configData)
            .addOnSuccessListener {
                Toast.makeText(context, "Configuration saved!", Toast.LENGTH_SHORT).show()
                binding.btnSaveTaxConfig.isEnabled = true
                loadCurrentConfig() // Refresh display
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to save", Toast.LENGTH_SHORT).show()
                binding.btnSaveTaxConfig.isEnabled = true
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
