package com.rajatt7z.retailx.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.rajatt7z.retailx.adapters.SimpleProductAdapter
import com.rajatt7z.retailx.adapters.SimpleProductItem
import com.rajatt7z.retailx.databinding.FragmentTopSellingBinding
import com.rajatt7z.retailx.repository.OrderRepository
import kotlinx.coroutines.launch

class TopSellingFragment : Fragment() {

    private var _binding: FragmentTopSellingBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: SimpleProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTopSellingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeRefresh()
        
        // Configure empty state
        binding.emptyState.tvEmptyTitle.text = "No Sales Data"
        binding.emptyState.tvEmptySubtitle.text = "Top selling products will appear here once orders are placed"
        
        showShimmer()
        loadTopSellingProducts()
    }

    private fun setupRecyclerView() {
        adapter = SimpleProductAdapter(emptyList())
        binding.rvTopSelling.layoutManager = LinearLayoutManager(context)
        binding.rvTopSelling.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadTopSellingProducts()
        }
    }

    private fun showShimmer() {
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.shimmerViewContainer.startShimmer()
        binding.rvTopSelling.visibility = View.GONE
        binding.emptyState.emptyStateContainer.visibility = View.GONE
    }

    private fun hideShimmer() {
        binding.shimmerViewContainer.stopShimmer()
        binding.shimmerViewContainer.visibility = View.GONE
    }

    private fun loadTopSellingProducts() {
        lifecycleScope.launch {
            try {
                val orderRepo = OrderRepository()
                val orders = orderRepo.getAllOrders()

                // Group by product name and sum quantities & revenue
                val productStats = orders.groupBy { it.productName }
                    .map { (name, productOrders) ->
                        Triple(
                            name,
                            productOrders.sumOf { it.quantity },
                            productOrders.sumOf { it.totalPrice }
                        )
                    }
                    .sortedByDescending { it.third } // Sort by revenue
                    .take(10)

                val items = productStats.mapIndexed { index, (name, qty, revenue) ->
                    SimpleProductItem(
                        name = name,
                        subtitle = "$qty units sold",
                        value = String.format("$%,.2f", revenue),
                        rank = index + 1
                    )
                }

                if (_binding != null) {
                    hideShimmer()
                    binding.swipeRefreshLayout.isRefreshing = false
                    adapter.updateList(items)

                    if (items.isEmpty()) {
                        binding.rvTopSelling.visibility = View.GONE
                        binding.emptyState.emptyStateContainer.visibility = View.VISIBLE
                    } else {
                        binding.rvTopSelling.visibility = View.VISIBLE
                        binding.emptyState.emptyStateContainer.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("TopSellingFragment", "Failed to load data", e)
                if (_binding != null) {
                    hideShimmer()
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
