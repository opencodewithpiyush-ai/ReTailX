package com.rajatt7z.retailx.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.rajatt7z.retailx.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EmployeeDashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_employee_dashboard, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvWelcome = view.findViewById<TextView>(R.id.tvWelcome)
        val ivProfile = view.findViewById<View>(R.id.ivProfile)
        
        // Buttons
        val btnViewProducts = view.findViewById<Button>(R.id.btnViewProducts)
        val btnManageProducts = view.findViewById<Button>(R.id.btnManageProducts)
        val btnViewOrders = view.findViewById<Button>(R.id.btnViewOrders)
        val btnManageOrders = view.findViewById<Button>(R.id.btnManageOrders)
        val btnCreateOrder = view.findViewById<Button>(R.id.btnCreateOrder)
        val btnSalesPerformance = view.findViewById<Button>(R.id.btnSalesPerformance)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
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
                        tvWelcome.text = "$greeting\n$name !"

                        // Role & Permission Logic
                        val role = document.getString("role") ?: ""
                        val permissions = document.getString("permissions") ?: "" // "Editor", "Viewer"
                        
                        setupDashboardForRole(role, permissions, 
                            btnViewProducts, btnManageProducts, 
                            btnViewOrders, btnManageOrders, 
                            btnCreateOrder, btnSalesPerformance)
                    }
                }
        }

        ivProfile.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_profile)
        }
    }

    private fun setupDashboardForRole(
        role: String, permissions: String,
        btnViewProducts: Button, btnManageProducts: Button,
        btnViewOrders: Button, btnManageOrders: Button,
        btnCreateOrder: Button, btnSalesPerformance: Button
    ) {
        val isEditor = permissions.equals("Editor", ignoreCase = true)

        // Reset visibility
        btnViewProducts.visibility = View.GONE
        btnManageProducts.visibility = View.GONE
        btnViewOrders.visibility = View.GONE
        btnManageOrders.visibility = View.GONE
        btnCreateOrder.visibility = View.GONE
        btnSalesPerformance.visibility = View.GONE

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
                // Fallback or Unknown Role
                android.widget.Toast.makeText(context, "Unknown Role: $role", android.widget.Toast.LENGTH_SHORT).show()
                // Maybe stay on this screen but show minimal options or logout?
                // For now, let's just leave the old logic for fallback or just do nothing
                 btnViewProducts.visibility = View.VISIBLE
                 btnViewProducts.setOnClickListener { navigateToProductList(canEdit = false) }
            }
        }
    }
    
    private fun navigateToProductList(canEdit: Boolean) {
        val action = EmployeeDashboardFragmentDirections.actionEmployeeDashboardFragmentToProductListFragment(canEdit)
        findNavController().navigate(action)
    }

    private fun navigateToOrders(canEdit: Boolean) {
         val action = EmployeeDashboardFragmentDirections.actionEmployeeDashboardFragmentToRecentOrdersFragment(canEdit)
         findNavController().navigate(action)
    }
}
