package com.rajatt7z.retailx.fragments.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rajatt7z.retailx.R
import com.rajatt7z.retailx.adapters.ReturnItemAdapter
import com.rajatt7z.retailx.adapters.ReturnSelection
import com.rajatt7z.retailx.databinding.FragmentReturnRefundBinding
import com.rajatt7z.retailx.models.Bill
import com.rajatt7z.retailx.models.Refund
import com.rajatt7z.retailx.models.ReturnItem
import com.rajatt7z.retailx.models.TaxConfig
import com.rajatt7z.retailx.repository.BillRepository
import com.rajatt7z.retailx.repository.CustomerRepository
import com.rajatt7z.retailx.repository.ProductRepository
import com.rajatt7z.retailx.repository.RefundRepository
import com.rajatt7z.retailx.utils.PdfGenerator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ReturnRefundFragment : Fragment() {

    private var _binding: FragmentReturnRefundBinding? = null
    private val binding get() = _binding!!

    private val billRepository = BillRepository()
    private val refundRepository = RefundRepository()
    private val productRepository = ProductRepository()
    private val customerRepository = CustomerRepository()

    private var selectedBill: Bill? = null
    private lateinit var returnItemAdapter: ReturnItemAdapter
    private var currentEmployeeName = "Staff"
    private var taxConfig = TaxConfig()
    private var currencySymbol = "$"

    private val refundReasons = listOf(
        "Defective Product",
        "Wrong Item Delivered",
        "Customer Request",
        "Damaged in Transit",
        "Quality Issue",
        "Other"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReturnRefundBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        setupRecyclerView()
        setupReasonDropdown()
        setupListeners()
        fetchEmployeeName()
        loadTaxConfig()
    }

    private fun setupRecyclerView() {
        returnItemAdapter = ReturnItemAdapter(mutableListOf(), currencySymbol) {
            updateRefundSummary()
        }
        binding.rvReturnItems.layoutManager = LinearLayoutManager(context)
        binding.rvReturnItems.adapter = returnItemAdapter
    }

    private fun setupReasonDropdown() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, refundReasons)
        binding.actvReason.setAdapter(adapter)
    }

    private fun setupListeners() {
        binding.btnSearchBill.setOnClickListener {
            val query = binding.etSearchBill.text.toString().trim()
            if (query.isEmpty()) {
                binding.tilSearch.error = "Enter a Bill ID, customer phone or name"
                return@setOnClickListener
            }
            binding.tilSearch.error = null
            searchBill(query)
        }

        binding.btnProcessRefund.setOnClickListener {
            processRefund()
        }

        binding.btnViewHistory.setOnClickListener {
            findNavController().navigate(R.id.action_returnRefund_to_refundHistory)
        }
    }

    private fun fetchEmployeeName() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener {
                if (it.exists()) {
                    currentEmployeeName = it.getString("name") ?: "Staff"
                }
            }
    }

    private fun loadTaxConfig() {
        FirebaseFirestore.getInstance().collection("config").document("tax").get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    taxConfig = TaxConfig(
                        name = doc.getString("name") ?: "Tax",
                        rate = doc.getDouble("rate") ?: 0.0,
                        isEnabled = doc.getBoolean("isEnabled") ?: false,
                        currency = doc.getString("currency") ?: "$"
                    )
                    currencySymbol = taxConfig.currency
                    // Update adapter currency
                    returnItemAdapter = ReturnItemAdapter(mutableListOf(), currencySymbol) {
                        updateRefundSummary()
                    }
                    binding.rvReturnItems.adapter = returnItemAdapter
                }
            }
    }

    private fun searchBill(query: String) {
        binding.progressBar.visibility = View.VISIBLE
        hideBillDetails()

        lifecycleScope.launch {
            try {
                val bills = billRepository.searchBills(query)

                if (_binding == null) return@launch

                binding.progressBar.visibility = View.GONE

                if (bills.isEmpty()) {
                    Toast.makeText(context, "No bill found matching \"$query\"", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // If multiple, take the first match
                val bill = bills.first()
                
                if (bill.status == "refunded") {
                    Toast.makeText(context, "This bill has already been fully refunded", Toast.LENGTH_LONG).show()
                    showBillDetails(bill)
                    return@launch
                }

                selectedBill = bill
                showBillDetails(bill)
                showReturnItems(bill)

            } catch (e: Exception) {
                if (_binding != null) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "Search failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showBillDetails(bill: Bill) {
        binding.cardBillDetails.visibility = View.VISIBLE
        binding.tvBillId.text = "Bill ID: ${bill.id.takeLast(8).uppercase()}"
        binding.tvBillCustomer.text = "Customer: ${bill.customerName}"
        binding.tvBillPhone.text = "Phone: ${bill.customerPhone}"
        binding.tvBillDate.text = formatDate(bill.timestamp)
        binding.tvBillTotal.text = String.format("%s%.2f", currencySymbol, bill.totalAmount)

        val statusText = when (bill.status) {
            "paid" -> "Paid"
            "partially_refunded" -> "Partial Refund"
            "refunded" -> "Refunded"
            else -> "Paid"
        }
        binding.tvBillStatus.text = statusText
        binding.tvBillStatus.setTextColor(
            when (bill.status) {
                "paid" -> android.graphics.Color.parseColor("#2E7D32")
                "partially_refunded" -> android.graphics.Color.parseColor("#E65100")
                "refunded" -> android.graphics.Color.parseColor("#D32F2F")
                else -> android.graphics.Color.parseColor("#2E7D32")
            }
        )
    }

    private fun showReturnItems(bill: Bill) {
        val selections = bill.products.map { cartItem ->
            ReturnSelection(
                cartItem = cartItem,
                isSelected = false,
                returnQuantity = 0,
                maxQuantity = cartItem.quantity
            )
        }.toMutableList()

        returnItemAdapter.updateList(selections)

        binding.tvSelectItemsLabel.visibility = View.VISIBLE
        binding.rvReturnItems.visibility = View.VISIBLE
        binding.tilReason.visibility = View.VISIBLE
        binding.cardRefundSummary.visibility = View.VISIBLE
        binding.btnProcessRefund.visibility = View.VISIBLE

        updateRefundSummary()
    }

    private fun hideBillDetails() {
        binding.cardBillDetails.visibility = View.GONE
        binding.tvSelectItemsLabel.visibility = View.GONE
        binding.rvReturnItems.visibility = View.GONE
        binding.tilReason.visibility = View.GONE
        binding.cardRefundSummary.visibility = View.GONE
        binding.btnProcessRefund.visibility = View.GONE
        selectedBill = null
    }

    private fun updateRefundSummary() {
        val selectedItems = returnItemAdapter.getSelectedItems()
        val subtotal = returnItemAdapter.getTotalRefundAmount()
        val itemCount = selectedItems.sumOf { it.returnQuantity }

        binding.tvItemsCount.text = itemCount.toString()
        binding.tvRefundSubtotal.text = String.format("%s%.2f", currencySymbol, subtotal)

        if (taxConfig.isEnabled && taxConfig.rate > 0) {
            val taxReversal = subtotal * (taxConfig.rate / 100.0)
            binding.layoutTaxReversal.visibility = View.VISIBLE
            binding.tvTaxReversalLabel.text = "${taxConfig.name} Reversal (${taxConfig.rate}%)"
            binding.tvTaxReversalAmount.text = String.format("%s%.2f", currencySymbol, taxReversal)
            binding.tvTotalRefund.text = String.format("%s%.2f", currencySymbol, subtotal + taxReversal)
        } else {
            binding.layoutTaxReversal.visibility = View.GONE
            binding.tvTotalRefund.text = String.format("%s%.2f", currencySymbol, subtotal)
        }
    }

    private fun processRefund() {
        val bill = selectedBill ?: return
        val selectedItems = returnItemAdapter.getSelectedItems()

        if (selectedItems.isEmpty()) {
            Toast.makeText(context, "Please select at least one item to return", Toast.LENGTH_SHORT).show()
            return
        }

        val reason = binding.actvReason.text.toString()
        if (reason.isEmpty()) {
            binding.tilReason.error = "Please select a reason"
            return
        }
        binding.tilReason.error = null

        binding.btnProcessRefund.isEnabled = false

        val subtotal = returnItemAdapter.getTotalRefundAmount()
        val taxReversal = if (taxConfig.isEnabled) subtotal * (taxConfig.rate / 100.0) else 0.0
        val totalRefund = subtotal + taxReversal

        val returnItems = selectedItems.map { selection ->
            ReturnItem(
                productId = selection.cartItem.productId,
                productName = selection.cartItem.productName,
                quantity = selection.returnQuantity,
                unitPrice = selection.cartItem.unitPrice,
                refundAmount = selection.returnQuantity * selection.cartItem.unitPrice
            )
        }

        val refund = Refund(
            id = UUID.randomUUID().toString(),
            originalBillId = bill.id,
            returnedItems = returnItems,
            refundAmount = totalRefund,
            reason = reason,
            processedBy = FirebaseAuth.getInstance().currentUser?.uid ?: "",
            processedByName = currentEmployeeName,
            timestamp = System.currentTimeMillis(),
            status = "Processed"
        )

        lifecycleScope.launch {
            try {
                // 1. Save refund
                val success = refundRepository.saveRefund(refund)
                if (!success) {
                    Toast.makeText(context, "Failed to save refund", Toast.LENGTH_SHORT).show()
                    binding.btnProcessRefund.isEnabled = true
                    return@launch
                }

                // 2. Restore stock for returned items
                returnItems.forEach { item ->
                    try {
                        productRepository.incrementStock(item.productId, item.quantity)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // 3. Update bill status
                val allItemsReturned = selectedItems.all { it.returnQuantity == it.maxQuantity } &&
                        selectedItems.size == bill.products.size
                val newStatus = if (allItemsReturned) "refunded" else "partially_refunded"
                billRepository.updateBillStatus(bill.id, newStatus)

                // 4. Reverse customer stats if applicable
                if (bill.customerId.isNotEmpty()) {
                    try {
                        customerRepository.incrementStats(bill.customerId, -totalRefund)
                    } catch (_: Exception) {}
                }

                // 5. Generate refund PDF
                PdfGenerator.generateRefundPdf(requireContext(), refund, bill)

                if (_binding != null) {
                    Toast.makeText(context, "Refund processed successfully!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            } catch (e: Exception) {
                if (_binding != null) {
                    Toast.makeText(context, "Error processing refund: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.btnProcessRefund.isEnabled = true
                }
            }
        }
    }

    private fun formatDate(timestamp: Long): String {
        return try {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            "Unknown"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
