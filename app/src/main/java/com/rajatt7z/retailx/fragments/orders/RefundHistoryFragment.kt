package com.rajatt7z.retailx.fragments.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rajatt7z.retailx.adapters.RefundHistoryAdapter
import com.rajatt7z.retailx.databinding.FragmentRefundHistoryBinding
import com.rajatt7z.retailx.repository.BillRepository
import com.rajatt7z.retailx.repository.RefundRepository
import com.rajatt7z.retailx.utils.PdfGenerator
import kotlinx.coroutines.launch

class RefundHistoryFragment : Fragment() {

    private var _binding: FragmentRefundHistoryBinding? = null
    private val binding get() = _binding!!
    private val refundRepository = RefundRepository()
    private val billRepository = BillRepository()
    private lateinit var adapter: RefundHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRefundHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        setupRecyclerView()
        setupSwipeRefresh()

        binding.emptyState.tvEmptyTitle.text = "No Refunds Yet"
        binding.emptyState.tvEmptySubtitle.text = "Processed refunds will appear here"

        showShimmer()
        loadRefunds()
    }

    private fun setupRecyclerView() {
        adapter = RefundHistoryAdapter(emptyList()) { refund ->
            // On click: regenerate refund PDF
            lifecycleScope.launch {
                val bill = billRepository.getBillById(refund.originalBillId)
                if (bill != null) {
                    PdfGenerator.generateRefundPdf(requireContext(), refund, bill)
                } else {
                    Toast.makeText(context, "Original bill not found", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.rvRefundHistory.layoutManager = LinearLayoutManager(context)
        binding.rvRefundHistory.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadRefunds()
        }
    }

    private fun showShimmer() {
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.shimmerViewContainer.startShimmer()
        binding.rvRefundHistory.visibility = View.GONE
        binding.emptyState.emptyStateContainer.visibility = View.GONE
    }

    private fun hideShimmer() {
        binding.shimmerViewContainer.stopShimmer()
        binding.shimmerViewContainer.visibility = View.GONE
    }

    private fun loadRefunds() {
        lifecycleScope.launch {
            val refunds = refundRepository.getAllRefunds()

            if (_binding != null) {
                hideShimmer()
                binding.swipeRefreshLayout.isRefreshing = false

                if (refunds.isEmpty()) {
                    binding.rvRefundHistory.visibility = View.GONE
                    binding.emptyState.emptyStateContainer.visibility = View.VISIBLE
                } else {
                    adapter.updateList(refunds)
                    binding.rvRefundHistory.visibility = View.VISIBLE
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
