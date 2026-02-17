package com.rajatt7z.retailx.fragments

import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.rajatt7z.retailx.R
import com.rajatt7z.retailx.databinding.FragmentDashboardOverviewBinding
import com.rajatt7z.retailx.viewmodel.AuthViewModel
import com.rajatt7z.retailx.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DashboardOverviewFragment : Fragment() {

    private var _binding: FragmentDashboardOverviewBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var productRepo: com.rajatt7z.retailx.repository.ProductRepository
    
    @Inject
    lateinit var orderRepo: com.rajatt7z.retailx.repository.OrderRepository
    
    @Inject
    lateinit var authRepo: com.rajatt7z.retailx.repository.AuthRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupNavigation()
        setupListeners()
        loadUserData()
        setupObservers()
    }

    private fun setupNavigation() {
        binding.btnViewSales.setOnClickListener {
            val bundle = android.os.Bundle().apply {
                putBoolean("showAllOrders", true)
            }
            findNavController().navigate(R.id.action_overview_to_sales, bundle)
        }
        binding.btnLowStock.setOnClickListener {
            findNavController().navigate(R.id.action_overview_to_lowStock)
        }
        binding.btnRecentOrders.setOnClickListener {
            findNavController().navigate(R.id.action_overview_to_recentOrders)
        }
        binding.btnTopSelling.setOnClickListener {
            findNavController().navigate(R.id.action_overview_to_topSelling)
        }
        binding.btnManageProducts.setOnClickListener {
            findNavController().navigate(R.id.action_overview_to_productList)
        }
        
        // New Card Click Listeners
        binding.cardSales.setOnClickListener {
            findNavController().navigate(R.id.action_overview_to_salesOverview)
        }
        binding.cardProducts.setOnClickListener {
            findNavController().navigate(R.id.action_overview_to_products)
        }
        binding.cardEmployees.setOnClickListener {
             findNavController().navigate(R.id.action_overview_to_employeeGrid)
        }
        binding.cardOrders.setOnClickListener {
            findNavController().navigate(R.id.action_overview_to_orders)
        }
    }



    private fun setupListeners() {
        binding.btnAddEmployee.setOnClickListener {
            findNavController().navigate(R.id.action_overview_to_employeeList)
        }
        binding.ivAdminProfile.setOnClickListener {
            // Navigate to Admin Settings
            findNavController().navigate(R.id.action_overview_to_settings)
        }
    }

    private fun loadUserData() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            viewModel.fetchUserDetails(currentUser.uid)
        }
        
        // Show loading placeholders
        binding.tvTotalSales.text = "..."
        binding.tvTotalProducts.text = "..."
        binding.tvTotalEmployees.text = "..."
        binding.tvTotalOrders.text = "..."
        
        // Fetch real stats from Firestore
        lifecycleScope.launch {
            try {
                // Repositories are injected
                
                val products = productRepo.getAllProducts()
                val orders = orderRepo.getAllOrders()
                val employeesResult = authRepo.getEmployees()
                
                if (_binding != null) {
                    binding.tvTotalProducts.text = products.size.toString()
                    binding.tvTotalOrders.text = orders.size.toString()
                    
                    val totalSales = orders.sumOf { it.totalPrice }
                    binding.tvTotalSales.text = String.format("$%,.2f", totalSales)
                    
                    if (employeesResult is Resource.Success) {
                        binding.tvTotalEmployees.text = (employeesResult.data?.size ?: 0).toString()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DashboardOverview", "Failed to load stats", e)
            }
        }
    }
    
    private fun setupObservers() {
        viewModel.userDetails.observe(viewLifecycleOwner) { resource ->
            if (resource is Resource.Success) {
                 val data = resource.data
                 val businessName = data?.get("businessName") as? String ?: "Admin"
                 binding.tvGreeting.text = "Welcome To $businessName"
                 
                 val profileImageUrl = data?.get("profileImageUrl") as? String
                 if (!profileImageUrl.isNullOrEmpty()) {
                    coil.ImageLoader(requireContext()).enqueue(
                        coil.request.ImageRequest.Builder(requireContext())
                            .data(profileImageUrl)
                            .target(binding.ivAdminProfile)
                            .placeholder(com.rajatt7z.retailx.R.drawable.round_account_circle_24)
                            .error(com.rajatt7z.retailx.R.drawable.round_account_circle_24)
                            .build()
                    )
                 }
            }
        }
        
         viewModel.authStatus.observe(viewLifecycleOwner) { resource ->
             when (resource) {
                is Resource.Success -> {
                    // Handled elsewhere or generic success
                }
                is Resource.Error -> {
                    val msg = resource.message ?: "Error"
                    if (!msg.contains("Login", true)) { // Ignore side-effect login errors
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }
                else -> {}
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
