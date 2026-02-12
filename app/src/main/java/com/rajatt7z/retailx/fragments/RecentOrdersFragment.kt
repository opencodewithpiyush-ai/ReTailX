package com.rajatt7z.retailx.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.lifecycleScope
import com.rajatt7z.retailx.databinding.FragmentRecentOrdersBinding
import kotlinx.coroutines.launch

class RecentOrdersFragment : Fragment() {

    private var _binding: FragmentRecentOrdersBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecentOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val repository = com.rajatt7z.retailx.repository.OrderRepository()
    private lateinit var adapter: com.rajatt7z.retailx.adapters.OrderAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadOrders()
    }

    private fun setupRecyclerView() {
        val canEdit = arguments?.getBoolean("canEdit") ?: false
        adapter = com.rajatt7z.retailx.adapters.OrderAdapter(emptyList(), canEdit) { order ->
            if (canEdit) {
                 markOrderCompleted(order)
            }
        }
        binding.rvRecentOrders.layoutManager = LinearLayoutManager(context)
        binding.rvRecentOrders.adapter = adapter
    }

    private fun loadOrders() {
        lifecycleScope.launch {
            val orders = repository.getAllOrders()
            // In a real app we might filter by role (e.g. Sales Exec sees only their orders), 
             // but Inventory Manager sees all. Let's assume this fragment is generic.
            adapter.updateList(orders)
            
             if (orders.isEmpty()) {
                 binding.tvEmpty.visibility = View.VISIBLE
             } else {
                 binding.tvEmpty.visibility = View.GONE
             }
        }
    }
    
    private fun markOrderCompleted(order: com.rajatt7z.retailx.models.Order) {
        lifecycleScope.launch {
            val success = repository.updateOrderStatus(order.id, "Completed")
            if (success) {
                android.widget.Toast.makeText(context, "Order Completed", android.widget.Toast.LENGTH_SHORT).show()
                loadOrders()
            } else {
                android.widget.Toast.makeText(context, "Failed to update", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
