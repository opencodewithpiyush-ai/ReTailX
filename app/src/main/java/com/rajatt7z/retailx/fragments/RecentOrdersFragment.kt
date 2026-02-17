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
        setupSwipeRefresh()
        
        // Configure empty state
        binding.emptyState.tvEmptyTitle.text = "No Recent Orders"
        binding.emptyState.tvEmptySubtitle.text = "Orders will appear here once they are placed"
        
        showShimmer()
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

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadOrders()
        }
    }

    private fun showShimmer() {
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.shimmerViewContainer.startShimmer()
        binding.rvRecentOrders.visibility = View.GONE
        binding.emptyState.emptyStateContainer.visibility = View.GONE
    }

    private fun hideShimmer() {
        binding.shimmerViewContainer.stopShimmer()
        binding.shimmerViewContainer.visibility = View.GONE
    }

    private fun loadOrders() {
        lifecycleScope.launch {
            val orders = repository.getAllOrders()
            
            if (_binding != null) {
                hideShimmer()
                binding.swipeRefreshLayout.isRefreshing = false
                adapter.updateList(orders)
                
                if (orders.isEmpty()) {
                    binding.rvRecentOrders.visibility = View.GONE
                    binding.emptyState.emptyStateContainer.visibility = View.VISIBLE
                } else {
                    binding.rvRecentOrders.visibility = View.VISIBLE
                    binding.emptyState.emptyStateContainer.visibility = View.GONE
                }
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
