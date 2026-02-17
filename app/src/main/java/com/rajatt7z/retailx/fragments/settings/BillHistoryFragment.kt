package com.rajatt7z.retailx.fragments.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.rajatt7z.retailx.adapters.BillHistoryAdapter
import com.rajatt7z.retailx.databinding.FragmentBillHistoryBinding
import com.rajatt7z.retailx.models.Bill
import com.rajatt7z.retailx.repository.BillRepository
import com.rajatt7z.retailx.utils.PdfGenerator
import kotlinx.coroutines.launch

class BillHistoryFragment : Fragment() {

    private var _binding: FragmentBillHistoryBinding? = null
    private val binding get() = _binding!!
    private val billRepository = BillRepository()
    private lateinit var adapter: BillHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBillHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        setupRecyclerView()
        setupSwipeRefresh()
        
        // Configure empty state
        binding.emptyState.tvEmptyTitle.text = "No Bills Yet"
        binding.emptyState.tvEmptySubtitle.text = "Your billing history will appear here"
        
        showShimmer()
        loadBills()
    }

    private fun setupRecyclerView() {
        adapter = BillHistoryAdapter(emptyList()) { bill ->
            // On Click: Generate/Open PDF
            PdfGenerator.generateBillPdf(requireContext(), bill)
        }
        binding.rvBillHistory.layoutManager = LinearLayoutManager(context)
        binding.rvBillHistory.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadBills()
        }
    }

    private fun showShimmer() {
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.shimmerViewContainer.startShimmer()
        binding.rvBillHistory.visibility = View.GONE
        binding.emptyState.emptyStateContainer.visibility = View.GONE
    }

    private fun hideShimmer() {
        binding.shimmerViewContainer.stopShimmer()
        binding.shimmerViewContainer.visibility = View.GONE
    }

    private fun loadBills() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        lifecycleScope.launch {
            val bills = billRepository.getBillsByEmployee(userId)
            
            if (_binding != null) {
                hideShimmer()
                binding.swipeRefreshLayout.isRefreshing = false
                
                if (bills.isEmpty()) {
                    binding.rvBillHistory.visibility = View.GONE
                    binding.emptyState.emptyStateContainer.visibility = View.VISIBLE
                } else {
                    adapter.updateList(bills)
                    binding.rvBillHistory.visibility = View.VISIBLE
                    binding.emptyState.emptyStateContainer.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
