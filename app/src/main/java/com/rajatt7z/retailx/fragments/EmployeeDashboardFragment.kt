package com.rajatt7z.retailx.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.rajatt7z.retailx.R
import com.rajatt7z.retailx.databinding.FragmentEmployeeDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EmployeeDashboardFragment : Fragment() {

    private var _binding: FragmentEmployeeDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmployeeDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (_binding == null) return@addOnSuccessListener
                    if (document != null && document.exists()) {
                        val name = document.getString("name") ?: "Employee"
                        // Handle greeting
                        val calendar = java.util.Calendar.getInstance()
                        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                        val greeting = when (hour) {
                            in 6..11 -> "Good Morning"
                            in 12..17 -> "Good Afternoon"
                            in 18..23 -> "Good Evening"
                            else -> "Good Night"
                        }
                        binding.tvWelcome.text = "$greeting\n$name !"

                        val profileImageUrl = document.getString("profileImageUrl")
                        if (!profileImageUrl.isNullOrEmpty()) {
                            coil.ImageLoader(requireContext()).enqueue(
                                coil.request.ImageRequest.Builder(requireContext())
                                    .data(profileImageUrl)
                                    .target(binding.ivProfile)
                                    .placeholder(com.rajatt7z.retailx.R.drawable.round_account_circle_24)
                                    .error(com.rajatt7z.retailx.R.drawable.round_account_circle_24)
                                    .build()
                            )
                        }
                        // Role & Permission Logic
                        val role = document.getString("role") ?: ""
                        setupDashboardForRole(role)
                    }
                }
        }

        binding.ivProfile.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_profile)
        }
    }

    private fun setupDashboardForRole(role: String) {
        // Hide all buttons initially
        binding.btnViewProducts.visibility = View.GONE
        binding.btnManageProducts.visibility = View.GONE
        binding.btnViewOrders.visibility = View.GONE
        binding.btnManageOrders.visibility = View.GONE
        binding.btnCreateOrder.visibility = View.GONE
        binding.btnSalesPerformance.visibility = View.GONE

        when (role) {
            "Store Manager" -> {
                findNavController().navigate(R.id.action_employeeDashboardFragment_to_storeManagerFragment)
            }
            "Inventory Manager" -> {
                findNavController().navigate(R.id.action_employeeDashboardFragment_to_inventoryManagerFragment)
            }
            "Sales Executive" -> {
                findNavController().navigate(R.id.action_employeeDashboardFragment_to_salesExecutiveFragment)
            }
            else -> {
                // Fallback for Unknown Role
                android.widget.Toast.makeText(context, "Unknown Role: $role", android.widget.Toast.LENGTH_SHORT).show()
                binding.btnViewProducts.visibility = View.VISIBLE
                binding.btnViewProducts.setOnClickListener { navigateToProductList(canEdit = false) }
            }
        }
    }
    
    private fun navigateToProductList(canEdit: Boolean) {
        val action = EmployeeDashboardFragmentDirections.actionEmployeeDashboardFragmentToProductListFragment(canEdit)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
