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
import com.rajatt7z.retailx.databinding.FragmentLowStockBinding
import com.rajatt7z.retailx.repository.ProductRepository
import kotlinx.coroutines.launch

class LowStockFragment : Fragment() {

    private var _binding: FragmentLowStockBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: SimpleProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLowStockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeRefresh()
        
        // Configure empty state
        binding.emptyState.tvEmptyTitle.text = "All Stocked Up!"
        binding.emptyState.tvEmptySubtitle.text = "All products are well-stocked. No alerts at this time."
        
        showShimmer()
        loadLowStockProducts()
    }

    private fun setupRecyclerView() {
        adapter = SimpleProductAdapter(emptyList())
        binding.rvLowStock.layoutManager = LinearLayoutManager(context)
        binding.rvLowStock.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadLowStockProducts()
        }
    }

    private fun showShimmer() {
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.shimmerViewContainer.startShimmer()
        binding.rvLowStock.visibility = View.GONE
        binding.emptyState.emptyStateContainer.visibility = View.GONE
    }

    private fun hideShimmer() {
        binding.shimmerViewContainer.stopShimmer()
        binding.shimmerViewContainer.visibility = View.GONE
    }

    private fun loadLowStockProducts() {
        lifecycleScope.launch {
            try {
                val productRepo = ProductRepository()
                val products = productRepo.getAllProducts()
                    .filter { it.stock < 10 }
                    .sortedBy { it.stock } // Most critical first

                val items = products.map { product ->
                    val urgency = when {
                        product.stock == 0 -> "⚠️ Out of stock"
                        product.stock < 3 -> "🔴 Critical"
                        product.stock < 5 -> "🟠 Low"
                        else -> "🟡 Running low"
                    }
                    SimpleProductItem(
                        name = product.name,
                        subtitle = urgency,
                        value = "${product.stock} left"
                    )
                }

                if (_binding != null) {
                    hideShimmer()
                    binding.swipeRefreshLayout.isRefreshing = false
                    adapter.updateList(items)
                    
                    if (items.isEmpty()) {
                        binding.rvLowStock.visibility = View.GONE
                        binding.emptyState.emptyStateContainer.visibility = View.VISIBLE
                    } else {
                        binding.rvLowStock.visibility = View.VISIBLE
                        binding.emptyState.emptyStateContainer.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("LowStockFragment", "Failed to load data", e)
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
