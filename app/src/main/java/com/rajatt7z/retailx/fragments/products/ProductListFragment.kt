package com.rajatt7z.retailx.fragments.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

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
    private lateinit var searchAdapter: ProductAdapter
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
        val canEdit = arguments?.getBoolean("canEdit") ?: false
        
        binding.fabAddProduct.visibility = if (canEdit) View.VISIBLE else View.GONE
        
        adapter = ProductAdapter(emptyList()) { product ->
             if (canEdit) {
                 if (product.isDraft) {
                     // Navigate to AddProductFragment with draft data for editing
                     val bundle = android.os.Bundle().apply {
                         putString("draft_name", product.name)
                         putString("draft_description", product.description)
                         putDouble("draft_price", product.price)
                         putInt("draft_stock", product.stock)
                         putString("draft_category", product.category)
                         putBoolean("is_draft_edit", true)
                     }
                     findNavController().navigate(R.id.action_productListFragment_to_addProductFragment, bundle)
                 } else {
                     val action = ProductListFragmentDirections.actionProductListFragmentToProductDetailsFragment(product.id)
                     findNavController().navigate(action)
                 }
             } else {
                 // View Only Mode - Maybe open details but read-only? 
                 // For now, let's open details as it usually just shows info.
                 val action = ProductListFragmentDirections.actionProductListFragmentToProductDetailsFragment(product.id)
                 findNavController().navigate(action)
             }
        }
        binding.recyclerViewProducts.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewProducts.adapter = adapter
        
        // Setup Search Results RecyclerView
        searchAdapter = ProductAdapter(emptyList()) { product ->
             // Reusing the same click logic
             if (canEdit) {
                 if (product.isDraft) {
                     val bundle = android.os.Bundle().apply {
                         putString("draft_name", product.name)
                         putString("draft_description", product.description)
                         putDouble("draft_price", product.price)
                         putInt("draft_stock", product.stock)
                         putString("draft_category", product.category)
                         putBoolean("is_draft_edit", true)
                     }
                     findNavController().navigate(R.id.action_productListFragment_to_addProductFragment, bundle)
                 } else {
                     val action = ProductListFragmentDirections.actionProductListFragmentToProductDetailsFragment(product.id)
                     findNavController().navigate(action)
                 }
             } else {
                 val action = ProductListFragmentDirections.actionProductListFragmentToProductDetailsFragment(product.id)
                 findNavController().navigate(action)
             }
             // Close search view on click if needed, or let navigation handle it
             binding.searchView.hide()
        }
        binding.recyclerViewSearchResults.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewSearchResults.adapter = searchAdapter
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
                // Parallel fetching if possible, but sequential is fine
                allProducts = repository.getAllProducts()

                // Fetch drafts
                val drafts = AppDatabase.getDatabase(requireContext()).draftProductDao().getAllDrafts()
                draftProducts = drafts.map { it.toProduct() }

                // Check if binding is still alive before updating UI
                if (_binding != null) {
                    binding.shimmerViewContainer.stopShimmer()
                    binding.shimmerViewContainer.visibility = View.GONE
                    filterList()
                }
            } catch (e: Exception) {
                // Log error
                if (_binding != null) {
                    binding.shimmerViewContainer.stopShimmer()
                    binding.shimmerViewContainer.visibility = View.GONE
                }
                // Ideally show an error state
            }
        }
    }

    private fun setupSearch() {
        binding.fabSearch.setOnClickListener {
            binding.searchView.show()
        }
        
        binding.searchView.editText.addTextChangedListener { editable ->
            currentSearchQuery = editable?.toString() ?: ""
            filterList()
        }
        
        binding.searchView.addTransitionListener { searchView, previousState, newState ->
             if (newState == com.google.android.material.search.SearchView.TransitionState.SHOWN) {
                 // Initialize search results
                 filterList()
             }
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

        // Update main list adapter regardless (in case search view closes)
        adapter.updateList(filtered)
        
        // Update search results adapter
        searchAdapter.updateList(filtered)

        // Show empty state if list is empty (handle visibility for both views)
        // Note: SearchView handles its own empty state UI if needed, but for now we rely on the list.
        if (filtered.isEmpty()) {
            binding.recyclerViewProducts.visibility = View.GONE
            // If in search mode, maybe show empty state inside search view? 
            // We can toggle visibility of recyclerViewSearchResults but currently no empty view inside.
        } else {
            binding.recyclerViewProducts.visibility = View.VISIBLE
        }
        
        // Main empty state logic
        if (filtered.isEmpty() && !binding.searchView.isShowing) {
             binding.tvEmptyState.visibility = View.VISIBLE
        } else {
             binding.tvEmptyState.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
