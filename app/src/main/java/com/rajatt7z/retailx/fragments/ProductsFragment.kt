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
import com.rajatt7z.retailx.R
import com.rajatt7z.retailx.adapters.ProductsAdapter
import com.rajatt7z.retailx.databinding.FragmentProductsBinding
import com.rajatt7z.retailx.utils.Resource
import com.rajatt7z.retailx.viewmodel.ProductViewModel

class ProductsFragment : Fragment() {

    private var _binding: FragmentProductsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProductViewModel by viewModels()
    private lateinit var productsAdapter: ProductsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.fabAddProduct.setOnClickListener {
             findNavController().navigate(R.id.action_productsFragment_to_addProductFragment)
        }

        setupRecyclerView()
        observeData()
    }

    private fun setupRecyclerView() {
        productsAdapter = ProductsAdapter(emptyList()) { product ->
            val bundle = Bundle().apply {
                putString("productId", product.id)
            }
            // Ensure this action exists in nav graph or reuse existing product detail navigation
            // For now assuming existing detail fragment can be used
             try {
                findNavController().navigate(R.id.action_productsFragment_to_productDetailsFragment, bundle)
            } catch (e: Exception) {
                 // Fallback or log if action not found
                 Toast.makeText(requireContext(), "Error navigating to details", Toast.LENGTH_SHORT).show()
            }
        }
        binding.rvProducts.apply {
            adapter = productsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeData() {
        viewModel.products.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    productsAdapter.updateList(resource.data ?: emptyList())
                }
                is Resource.Error -> {
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                }
                is Resource.Loading -> {
                    // Show loading indicator
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
