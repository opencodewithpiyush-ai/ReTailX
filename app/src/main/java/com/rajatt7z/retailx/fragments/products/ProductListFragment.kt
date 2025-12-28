package com.rajatt7z.retailx.fragments.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.rajatt7z.retailx.R
import com.rajatt7z.retailx.adapters.ProductAdapter
import com.rajatt7z.retailx.database.AppDatabase
import com.rajatt7z.retailx.databinding.FragmentProductListBinding
import com.rajatt7z.retailx.models.Product
import com.rajatt7z.retailx.repository.ProductRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class ProductListFragment : Fragment() {

    private var _binding: FragmentProductListBinding? = null
    private val binding get() = _binding!!
    private val repository = ProductRepository()
    private lateinit var adapter: ProductAdapter
    private var allProducts = listOf<Product>()
    private var draftProducts = listOf<Product>()
    private var currentTab = 0
    private var currentSearchQuery = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupTabs()
        loadProducts()
        setupSearch()

        binding.fabAddProduct.setOnClickListener {
            findNavController().navigate(R.id.action_productListFragment_to_addProductFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload products/drafts when returning to this fragment
        loadProducts()
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapter(emptyList()) { product ->
             if (product.isDraft) {
                 android.widget.Toast.makeText(context, "Editing draft is not implemented yet", android.widget.Toast.LENGTH_SHORT).show()
                 // Future: navigate to AddProductFragment with product details
             } else {
                 val action = ProductListFragmentDirections.actionProductListFragmentToProductDetailsFragment(product.id)
                 findNavController().navigate(action)
             }
        }
        binding.recyclerViewProducts.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewProducts.adapter = adapter
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                filterList()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadProducts() {
        // Show shimmer and hide RecyclerView while loading
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.shimmerViewContainer.startShimmer()
        binding.recyclerViewProducts.visibility = View.GONE

        lifecycleScope.launch {
            try {
                //Fake Delay
                delay(3000)

                // Parallel fetching if possible, but sequential is fine
                allProducts = repository.getAllProducts()

                // Fetch drafts
                val drafts = AppDatabase.getDatabase(requireContext()).draftProductDao().getAllDrafts()
                draftProducts = drafts.map { it.toProduct() }

                // Hide shimmer and stop animation
                binding.shimmerViewContainer.stopShimmer()
                binding.shimmerViewContainer.visibility = View.GONE
                
                filterList()
            } catch (e: Exception) {
                // Log error
                binding.shimmerViewContainer.stopShimmer()
                binding.shimmerViewContainer.visibility = View.GONE
                // Ideally show an error state
            }
        }
    }

    private fun setupSearch() {
        binding.searchBar.addTextChangedListener { editable ->
            currentSearchQuery = editable?.toString() ?: ""
            filterList()
        }
    }

    private fun filterList() {
        val sourceList = if (currentTab == 0) allProducts else draftProducts

        val filtered = if (currentSearchQuery.isEmpty()) {
            sourceList
        } else {
            sourceList.filter {
                it.name.contains(currentSearchQuery, ignoreCase = true) ||
                        it.description.contains(currentSearchQuery, ignoreCase = true)
            }
        }

        adapter.updateList(filtered)

        // Show empty state if list is empty
        if (filtered.isEmpty()) {
            binding.recyclerViewProducts.visibility = View.GONE
            binding.tvEmptyState.visibility = View.VISIBLE
        } else {
            binding.recyclerViewProducts.visibility = View.VISIBLE
            binding.tvEmptyState.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
