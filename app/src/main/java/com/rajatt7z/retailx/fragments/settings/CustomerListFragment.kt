package com.rajatt7z.retailx.fragments.settings

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.rajatt7z.retailx.R
import com.rajatt7z.retailx.adapters.CustomerAdapter
import com.rajatt7z.retailx.databinding.FragmentCustomerListBinding
import com.rajatt7z.retailx.models.Customer
import com.rajatt7z.retailx.repository.CustomerRepository
import kotlinx.coroutines.launch

class CustomerListFragment : Fragment() {

    private var _binding: FragmentCustomerListBinding? = null
    private val binding get() = _binding!!
    private val customerRepository = CustomerRepository()
    private lateinit var customerAdapter: CustomerAdapter
    private var allCustomers = listOf<Customer>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomerListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        customerAdapter = CustomerAdapter(emptyList()) { customer ->
            confirmDelete(customer)
        }
        binding.rvCustomers.layoutManager = LinearLayoutManager(context)
        binding.rvCustomers.adapter = customerAdapter

        loadCustomers()
        setupSearch()
        
        binding.btnAddCustomer.setOnClickListener {
            showAddCustomerDialog()
        }
    }

    private fun loadCustomers() {
        lifecycleScope.launch {
            try {
                allCustomers = customerRepository.getAllCustomers()
                customerAdapter.updateList(allCustomers)
                updateEmptyState()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load customers", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.lowercase() ?: ""
                if (query.isEmpty()) {
                    customerAdapter.updateList(allCustomers)
                } else {
                    val filtered = allCustomers.filter {
                        it.name.lowercase().contains(query) || it.phone.contains(query)
                    }
                    customerAdapter.updateList(filtered)
                }
                updateEmptyState()
            }
        })
    }

    private fun updateEmptyState() {
        if (customerAdapter.itemCount == 0) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.rvCustomers.visibility = View.GONE
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.rvCustomers.visibility = View.VISIBLE
        }
    }

    private fun showAddCustomerDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_customer, null)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Customer")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = dialogView.findViewById<TextInputEditText>(R.id.etDialogName).text.toString().trim()
                val phone = dialogView.findViewById<TextInputEditText>(R.id.etDialogPhone).text.toString().trim()
                val email = dialogView.findViewById<TextInputEditText>(R.id.etDialogEmail).text.toString().trim()
                val address = dialogView.findViewById<TextInputEditText>(R.id.etDialogAddress).text.toString().trim()

                if (name.isEmpty()) {
                    Toast.makeText(context, "Name is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (phone.isEmpty()) {
                    Toast.makeText(context, "Phone is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    val customer = Customer(
                        name = name,
                        phone = phone,
                        email = email,
                        address = address
                    )
                    try {
                        customerRepository.addCustomer(customer)
                        Toast.makeText(context, "Customer added!", Toast.LENGTH_SHORT).show()
                        loadCustomers()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to add customer", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(customer: Customer) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Customer")
            .setMessage("Delete ${customer.name}? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        customerRepository.deleteCustomer(customer.id)
                        Toast.makeText(context, "Customer deleted", Toast.LENGTH_SHORT).show()
                        loadCustomers()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
