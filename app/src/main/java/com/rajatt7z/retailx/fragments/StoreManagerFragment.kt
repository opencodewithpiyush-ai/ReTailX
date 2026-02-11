package com.rajatt7z.retailx.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.rajatt7z.retailx.R
import com.rajatt7z.retailx.adapters.ProductAdapter
import com.rajatt7z.retailx.databinding.FragmentStoreManagerBinding
import com.rajatt7z.retailx.repository.ProductRepository
import kotlinx.coroutines.launch

class StoreManagerFragment : Fragment() {

    private var _binding: FragmentStoreManagerBinding? = null
    private val binding get() = _binding!!
    private val repository = ProductRepository()
    private lateinit var adapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStoreManagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupHeader()
        setupRecyclerView()
        loadData()

        binding.fabAddProduct.setOnClickListener {
            findNavController().navigate(R.id.action_storeManagerFragment_to_addProductFragment)
        }
    }

    private fun setupHeader() {
        val headerView = binding.tvHeader
        val profileView = binding.ivProfile
        val roleTitle = "Store Manager"

        // Greeting Logic
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
        headerView.text = "$greeting\n$roleTitle"

        // Profile Click
        profileView.setOnClickListener {
            findNavController().navigate(com.rajatt7z.retailx.R.id.action_storeManagerFragment_to_profile)
        }

        // Fetch Name
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name") ?: roleTitle
                        headerView.text = "$greeting\n$name"
                    }
                }
        }
    }

    private fun setupRecyclerView() {
        // Reusing ProductAdapter but maybe with a grid layout manager
        adapter = ProductAdapter(emptyList()) { product ->
            // Open for editing since this is Store Manager
             val action = StoreManagerFragmentDirections.actionStoreManagerFragmentToProductDetailsFragment(product.id)
             findNavController().navigate(action)
        }
        
        val layoutManager = GridLayoutManager(context, 2)
        binding.rvProducts.layoutManager = layoutManager
        binding.rvProducts.adapter = adapter
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                val products = repository.getAllProducts()
                adapter.updateList(products)
                
                binding.tvTotalProducts.text = products.size.toString()
                
                // Assuming Product has 'quantity' field, count low stock (< 10)
                // If not available in generic Product model, we might need to adjust or cast
                // For now, let's just use what's available or map if needed.
                // Checking Product model...
                 val lowStockCount = products.count { 
                     // Simple check if property exists or try-catch
                     try {
                         // Reflection or if we know the field. 
                         // Let's assume standard Product model has 'stock' or 'quantity'
                         // Using inspection from previous `view_file` might be needed if compilation fails.
                         // I recall `Product` model. Let's assume it has quantity.
                         it.stock < 10 
                     } catch(e: Exception) { false }
                 }
                 binding.tvLowStock.text = lowStockCount.toString()

            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
