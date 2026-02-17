package com.rajatt7z.retailx.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rajatt7z.retailx.adapters.OrdersAdapter
import com.rajatt7z.retailx.databinding.FragmentOrdersBinding
import com.rajatt7z.retailx.utils.Resource
import com.rajatt7z.retailx.viewmodel.OrderViewModel

class OrdersFragment : Fragment() {

    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OrderViewModel by viewModels()
    private lateinit var ordersAdapter: OrdersAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        setupRecyclerView()
        setupSwipeRefresh()
        showShimmer()
        observeData()
        
        // Configure empty state text
        binding.emptyState.tvEmptyTitle.text = "No Orders Yet"
        binding.emptyState.tvEmptySubtitle.text = "Orders will appear here once sales are made"
    }

    private fun setupRecyclerView() {
        ordersAdapter = OrdersAdapter(emptyList()) { order ->
             Toast.makeText(requireContext(), "Order: ${order.id}", Toast.LENGTH_SHORT).show()
        }
        binding.rvOrders.apply {
            adapter = ordersAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.fetchOrders()
        }
    }

    private fun showShimmer() {
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.shimmerViewContainer.startShimmer()
        binding.rvOrders.visibility = View.GONE
        binding.emptyState.emptyStateContainer.visibility = View.GONE
    }

    private fun hideShimmer() {
        binding.shimmerViewContainer.stopShimmer()
        binding.shimmerViewContainer.visibility = View.GONE
    }

    private fun observeData() {
        viewModel.orders.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    hideShimmer()
                    binding.swipeRefreshLayout.isRefreshing = false
                    val list = resource.data ?: emptyList()
                    ordersAdapter.updateList(list)
                    
                    if (list.isEmpty()) {
                        binding.rvOrders.visibility = View.GONE
                        binding.emptyState.emptyStateContainer.visibility = View.VISIBLE
                    } else {
                        binding.rvOrders.visibility = View.VISIBLE
                        binding.emptyState.emptyStateContainer.visibility = View.GONE
                    }
                }
                is Resource.Error -> {
                    hideShimmer()
                    binding.swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                }
                is Resource.Loading -> {
                    // Loading handled by shimmer or swipe refresh
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
