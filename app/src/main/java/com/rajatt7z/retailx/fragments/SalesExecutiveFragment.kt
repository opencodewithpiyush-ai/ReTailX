package com.rajatt7z.retailx.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.rajatt7z.retailx.R
import com.rajatt7z.retailx.databinding.FragmentSalesExecutiveBinding
import com.rajatt7z.retailx.repository.OrderRepository
import com.rajatt7z.retailx.models.Order
import kotlinx.coroutines.launch

class SalesExecutiveFragment : Fragment() {

    private var _binding: FragmentSalesExecutiveBinding? = null
    private val binding get() = _binding!!
    private val repository = OrderRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSalesExecutiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupHeader()
        setupClickListeners()
        loadStats()
    }

    private fun setupHeader() {
        val headerView = binding.tvHeader
        val profileView = binding.ivProfile
        val roleTitle = "Sales Executive"

        // Greeting Logic
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
        headerView.text = "$greeting\n$roleTitle"

        // Profile Click
        profileView.setOnClickListener {
             findNavController().navigate(com.rajatt7z.retailx.R.id.action_salesExecutiveFragment_to_profile)
        }

        // Fetch Name
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name") ?: roleTitle
                        headerView.text = "$greeting\n$name"
                    }
                }
        }
    }

    private fun setupClickListeners() {
        binding.btnNewOrder.setOnClickListener {
             findNavController().navigate(R.id.action_salesExecutiveFragment_to_createOrderFragment)
        }

        binding.btnViewProducts.setOnClickListener {
             // View Only mode
             val action = SalesExecutiveFragmentDirections.actionSalesExecutiveFragmentToProductListFragment(false)
             findNavController().navigate(action)
        }

        binding.btnAnalytics.setOnClickListener {
             findNavController().navigate(R.id.action_salesExecutiveFragment_to_salesChartFragment)
        }
    }

    private fun loadStats() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        lifecycleScope.launch {
            try {
                // In a real app we'd need a specific query for "My Orders" or filter locally
                // repository.getOrdersByEmployee(userId) is what we need
                val orders = repository.getOrdersByEmployee(userId)
                
                binding.tvTotalOrders.text = orders.size.toString()
                
                // Calculate Total Revenue
                // Order likely has 'totalPrice' or we calculate from 'price' * 'quantity'
                // Assuming Order has 'totalPrice' (Double)
                var revenue = 0.0
                orders.forEach { 
                    revenue += it.totalPrice
                }
                
                binding.tvTotalSales.text = String.format("$%.2f", revenue)
                
            } catch (e: Exception) {
                android.util.Log.e("SalesExecFragment", "Failed to load stats", e)
                android.widget.Toast.makeText(context, "Failed to load stats", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
