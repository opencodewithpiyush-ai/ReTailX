package com.rajatt7z.retailx.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rajatt7z.retailx.adapters.DetailedOrderAdapter
import com.rajatt7z.retailx.databinding.FragmentInventoryManagerBinding
import com.rajatt7z.retailx.repository.OrderRepository
import com.rajatt7z.retailx.repository.AuthRepository
import kotlinx.coroutines.launch

class InventoryManagerFragment : Fragment() {

    private var _binding: FragmentInventoryManagerBinding? = null
    private val binding get() = _binding!!
    private val repository = OrderRepository()
    private val authRepository = AuthRepository()
    private lateinit var adapter: DetailedOrderAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryManagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupHeader()
        setupRecyclerView()
        loadOrders()
    }

    private fun setupHeader() {
        val headerView = binding.tvHeader
        val profileView = binding.ivProfile
        val roleTitle = "Inventory Manager"

        // Greeting Logic
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
        headerView.text = "$greeting $roleTitle"

        // Profile Click
        profileView.setOnClickListener {
            findNavController().navigate(com.rajatt7z.retailx.R.id.action_inventoryManagerFragment_to_profile)
        }

        // Fetch Name
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name") ?: roleTitle
                        headerView.text = "$greeting\n$name"
                        
                        val profileImageUrl = document.getString("profileImageUrl")
                        if (!profileImageUrl.isNullOrEmpty()) {
                            coil.ImageLoader(requireContext()).enqueue(
                                coil.request.ImageRequest.Builder(requireContext())
                                    .data(profileImageUrl)
                                    .target(profileView)
                                    .placeholder(com.rajatt7z.retailx.R.drawable.round_account_circle_24)
                                    .error(com.rajatt7z.retailx.R.drawable.round_account_circle_24)
                                    .build()
                            )
                        }
                    }
                }
        }
    }

    private fun setupRecyclerView() {
        adapter = DetailedOrderAdapter(emptyList()) { order ->
            markOrderProcessed(order)
        }
        binding.rvOrders.layoutManager = LinearLayoutManager(context)
        binding.rvOrders.adapter = adapter
    }

    private fun loadOrders() {
        lifecycleScope.launch {
            try {
                val orders = repository.getAllOrders()
                val employeesResult = authRepository.getEmployees()
                
                if (employeesResult is com.rajatt7z.retailx.utils.Resource.Success) {
                    val employeeMap = employeesResult.data?.associate { it.uid to it.name } ?: emptyMap()
                    adapter.updateEmployeeMap(employeeMap)
                }
                
                adapter.updateList(orders)
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load orders", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun markOrderProcessed(order: com.rajatt7z.retailx.models.Order) {
         lifecycleScope.launch {
            val success = repository.updateOrderStatus(order.id, "Processed")
            if (success) {
                Toast.makeText(context, "Order Processed", Toast.LENGTH_SHORT).show()
                loadOrders()
            } else {
                Toast.makeText(context, "Failed to update", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
