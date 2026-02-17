package com.rajatt7z.retailx.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.rajatt7z.retailx.databinding.FragmentSmartReorderBinding
import com.rajatt7z.retailx.repository.ProductRepository
import com.rajatt7z.retailx.utils.GeminiHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmartReorderFragment : Fragment() {

    private var _binding: FragmentSmartReorderBinding? = null
    private val binding get() = _binding!!
    
    // Manual mismatch - ProductRepository is not consistent on Hilt vs Manual.
    // Using manual instantiation for now as ProductViewModel does.
    // Ideally should be injected, but Repo needs @Inject constructor and Module.
    // I know I fixed Repo to have @Inject, so I can try injection or manual.
    // Sticking to manual to be safe as per other fragments for now, or just migrate.
    // Wait, I fixed ProductRepository to have @Inject. So I can use @Inject.
    // But I'll stick to manual for consistency with existing Product fragments for now to avoid scoping issues unless I refactor everything.
    private val repository = ProductRepository()

    @Inject
    lateinit var geminiHelper: GeminiHelper

    private val adapter = com.rajatt7z.retailx.adapters.ProductAdapter(emptyList()) { 
        Toast.makeText(requireContext(), "Selected ${it.name}", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSmartReorderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        loadData()
    }

    private fun setupRecyclerView() {
        binding.rvReorderItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReorderItems.adapter = adapter
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                // Fetch all products
                val allProducts = repository.getAllProducts()
                // Filter low stock (< 10)
                val lowStockProducts = allProducts.filter { it.stock < 10 }
                
                adapter.updateList(lowStockProducts)
                
                // Prepare data for AI
                val inventorySummary = lowStockProducts.joinToString("\n") { 
                    "${it.name}: ${it.stock} units currently (Price: $${it.price})" 
                }
                
                // If no low stock items, maybe send a few random ones or just say healthy
                val dataToSend = if (inventorySummary.isNotBlank()) inventorySummary else "All items have > 10 stock."
                
                analyzeWithAI(dataToSend)
                
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading inventory", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
                binding.tvAiAnalysis.text = "Error analyzing data."
            }
        }
    }

    private fun analyzeWithAI(data: String) {
        lifecycleScope.launch {
            try {
                geminiHelper.provideReorderSuggestions(data).collect { analysis ->
                    binding.tvAiAnalysis.text = analysis
                    binding.progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                binding.tvAiAnalysis.text = "AI Analysis unavailable."
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
