package com.rajatt7z.retailx.fragments.orders

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rajatt7z.retailx.R
import com.rajatt7z.retailx.adapters.CartAdapter
import com.rajatt7z.retailx.databinding.FragmentCreateBillBinding
import com.rajatt7z.retailx.models.Bill
import com.rajatt7z.retailx.models.CartItem
import com.rajatt7z.retailx.models.Customer
import com.rajatt7z.retailx.models.Product
import com.rajatt7z.retailx.models.TaxConfig
import com.rajatt7z.retailx.repository.BillRepository
import com.rajatt7z.retailx.repository.CustomerRepository
import com.rajatt7z.retailx.repository.ProductRepository
import com.rajatt7z.retailx.utils.BarcodeScannerDialog
import com.rajatt7z.retailx.utils.PdfGenerator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.UUID

class CreateBillFragment : Fragment() {

    private var _binding: FragmentCreateBillBinding? = null
    private val binding get() = _binding!!
    
    private val productRepository = ProductRepository()
    private val billRepository = BillRepository()
    private val customerRepository = CustomerRepository()
    
    private var allProducts = listOf<Product>()
    private var selectedProduct: Product? = null
    private var selectedCustomer: Customer? = null
    
    private val cartItems = mutableListOf<CartItem>()
    private lateinit var cartAdapter: CartAdapter
    
    private var currentEmployeeName = "Store Staff"
    private var taxConfig = TaxConfig()
    private var currencySymbol = "$"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateBillBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        loadProducts()
        fetchEmployeeName()
        loadTaxConfig()
        setupListeners()
        setupCustomerLookup()

        // Default discount type to percentage
        binding.toggleDiscountType.check(R.id.btnDiscountPercent)
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(cartItems) { item ->
            removeItem(item)
        }
        binding.rvCartItems.layoutManager = LinearLayoutManager(context)
        binding.rvCartItems.adapter = cartAdapter
    }

    private fun loadProducts() {
        lifecycleScope.launch {
            try {
                allProducts = productRepository.getAllProducts()
                val productNames = allProducts.map { it.name }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, productNames)
                binding.autoCompleteProduct.setAdapter(adapter)
                
                binding.autoCompleteProduct.setOnItemClickListener { _, _, position, _ ->
                    val name = adapter.getItem(position).toString()
                    selectedProduct = allProducts.find { it.name == name }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load products", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun fetchEmployeeName() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { 
                if (it.exists()) {
                    currentEmployeeName = it.getString("name") ?: "Store Staff"
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
                    // Update fixed discount button text with currency
                    binding.btnDiscountFixed.text = currencySymbol
                    updateUI()
                }
            }
    }

    private fun setupCustomerLookup() {
        binding.etCustomerPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val phone = s?.toString() ?: ""
                if (phone.length >= 3) {
                    lookupCustomer(phone)
                }
            }
        })
    }

    private fun lookupCustomer(phone: String) {
        lifecycleScope.launch {
            try {
                val customers = customerRepository.searchByPhone(phone)
                if (customers.isNotEmpty()) {
                    selectedCustomer = customers.first()
                    binding.etCustomerName.setText(selectedCustomer?.name ?: "")
                }
            } catch (_: Exception) {}
        }
    }

    private fun setupListeners() {
        binding.btnAddItem.setOnClickListener {
            addItemToCart()
        }

        binding.btnGenerateBill.setOnClickListener {
            generateBill()
        }

        binding.btnScanBarcode.setOnClickListener {
            val scanner = BarcodeScannerDialog { barcode ->
                onBarcodeScanned(barcode)
            }
            scanner.show(parentFragmentManager, "BarcodeScanner")
        }
        
        binding.btnPreview.setOnClickListener {
            if (cartItems.isEmpty()) {
                Toast.makeText(context, "Cart is empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val customerName = binding.etCustomerName.text.toString()
            val customerPhone = binding.etCustomerPhone.text.toString()

            if (customerName.isBlank() || customerPhone.isBlank()) {
                Toast.makeText(context, "Please enter customer details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val totals = calculateTotals()
            val tempBill = Bill(
                id = "PREVIEW",
                products = cartItems,
                customerName = customerName,
                customerPhone = customerPhone,
                subtotal = totals.subtotal,
                discountAmount = totals.discount,
                taxRate = if (taxConfig.isEnabled) taxConfig.rate else 0.0,
                taxAmount = totals.tax,
                totalAmount = totals.total,
                timestamp = System.currentTimeMillis(),
                generatedByName = currentEmployeeName
            )

            val dialog = BillPreviewDialog(tempBill, {})
            dialog.show(parentFragmentManager, "BillPreviewDialog")
        }
    }

    private fun onBarcodeScanned(barcode: String) {
        lifecycleScope.launch {
            val product = productRepository.getProductByBarcode(barcode)
            if (product != null) {
                selectedProduct = product
                binding.autoCompleteProduct.setText(product.name, false)
                Toast.makeText(context, "Product found: ${product.name}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "No product found for barcode: $barcode", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addItemToCart() {
        val product = selectedProduct
        val quantityStr = binding.etQuantity.text.toString()

        if (product == null) {
            Toast.makeText(context, "Please select a product", Toast.LENGTH_SHORT).show()
            return
        }

        if (quantityStr.isEmpty() || quantityStr.toIntOrNull() == null) {
            Toast.makeText(context, "Invalid quantity", Toast.LENGTH_SHORT).show()
            return
        }

        val quantity = quantityStr.toInt()
        if (quantity <= 0) {
             Toast.makeText(context, "Quantity must be > 0", Toast.LENGTH_SHORT).show()
             return
        }

        // Check stock
        val currentInCart = cartItems.find { it.productId == product.id }?.quantity ?: 0
        if (quantity + currentInCart > product.stock) {
            Toast.makeText(context, "Insufficient stock. Available: ${product.stock}", Toast.LENGTH_SHORT).show()
            return
        }

        // Parse discount
        val discountValueStr = binding.etItemDiscount.text.toString()
        val discountValue = discountValueStr.toDoubleOrNull() ?: 0.0
        val isPercent = binding.toggleDiscountType.checkedButtonId == R.id.btnDiscountPercent
        val discountType = when {
            discountValue <= 0 -> "none"
            isPercent -> "percentage"
            else -> "fixed"
        }

        val grossTotal = quantity * product.price
        val discount = when (discountType) {
            "percentage" -> grossTotal * (discountValue / 100.0)
            "fixed" -> discountValue.coerceAtMost(grossTotal)
            else -> 0.0
        }

        // Add or Update
        val existingItemIndex = cartItems.indexOfFirst { it.productId == product.id }
        if (existingItemIndex != -1) {
            val existing = cartItems[existingItemIndex]
            val newQty = existing.quantity + quantity
            val newGross = newQty * existing.unitPrice
            val newDiscount = when (discountType) {
                "percentage" -> newGross * (discountValue / 100.0)
                "fixed" -> discountValue.coerceAtMost(newGross)
                else -> existing.discount
            }
            val newItem = existing.copy(
                quantity = newQty,
                discount = newDiscount,
                discountType = if (discountType != "none") discountType else existing.discountType,
                discountValue = if (discountType != "none") discountValue else existing.discountValue,
                totalPrice = newGross - newDiscount
            )
            cartItems[existingItemIndex] = newItem
        } else {
            val newItem = CartItem(
                productId = product.id,
                productName = product.name,
                quantity = quantity,
                unitPrice = product.price,
                discount = discount,
                discountType = discountType,
                discountValue = discountValue,
                totalPrice = grossTotal - discount
            )
            cartItems.add(newItem)
        }

        updateUI()
        binding.etQuantity.text?.clear()
        binding.etItemDiscount.text?.clear()
        binding.autoCompleteProduct.text.clear()
        selectedProduct = null
    }

    private fun removeItem(item: CartItem) {
        cartItems.remove(item)
        updateUI()
    }

    data class BillTotals(val subtotal: Double, val discount: Double, val tax: Double, val total: Double)

    private fun calculateTotals(): BillTotals {
        val subtotal = cartItems.sumOf { it.quantity * it.unitPrice }
        val discount = cartItems.sumOf { it.discount }
        val taxableAmount = subtotal - discount
        val tax = if (taxConfig.isEnabled) taxableAmount * (taxConfig.rate / 100.0) else 0.0
        val total = taxableAmount + tax
        return BillTotals(subtotal, discount, tax, total)
    }

    private fun updateUI() {
        cartAdapter.updateList(cartItems)
        cartAdapter.notifyDataSetChanged()
        
        val totals = calculateTotals()

        binding.tvSubtotal.text = String.format("%s%.2f", currencySymbol, totals.subtotal)

        if (totals.discount > 0) {
            binding.layoutDiscount.visibility = View.VISIBLE
            binding.tvDiscount.text = String.format("-%s%.2f", currencySymbol, totals.discount)
        } else {
            binding.layoutDiscount.visibility = View.GONE
        }

        if (taxConfig.isEnabled && totals.tax > 0) {
            binding.layoutTax.visibility = View.VISIBLE
            binding.tvTaxLabel.text = String.format("%s (%.1f%%)", taxConfig.name, taxConfig.rate)
            binding.tvTaxAmount.text = String.format("%s%.2f", currencySymbol, totals.tax)
        } else {
            binding.layoutTax.visibility = View.GONE
        }

        binding.tvTotalAmount.text = String.format("%s%.2f", currencySymbol, totals.total)
    }

    private fun generateBill() {
        if (cartItems.isEmpty()) {
            Toast.makeText(context, "Cart is empty", Toast.LENGTH_SHORT).show()
            return
        }

        val customerName = binding.etCustomerName.text.toString()
        val customerPhone = binding.etCustomerPhone.text.toString()

        if (customerName.isEmpty()) {
            binding.etCustomerName.error = "Required"
            return
        }

        binding.btnGenerateBill.isEnabled = false
        
        lifecycleScope.launch {
            val billId = UUID.randomUUID().toString()
            val totals = calculateTotals()

            val bill = Bill(
                id = billId,
                products = cartItems.toList(),
                customerName = customerName,
                customerPhone = customerPhone,
                customerId = selectedCustomer?.id ?: "",
                subtotal = totals.subtotal,
                discountAmount = totals.discount,
                taxRate = if (taxConfig.isEnabled) taxConfig.rate else 0.0,
                taxAmount = totals.tax,
                totalAmount = totals.total,
                timestamp = System.currentTimeMillis(),
                generatedBy = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                generatedByName = currentEmployeeName
            )

            val success = billRepository.saveBill(bill)
            
            if (success) {
                // Decrement Stock
                cartItems.forEach { item ->
                    try {
                        productRepository.decrementStock(item.productId, item.quantity)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // Update customer stats
                if (selectedCustomer != null) {
                    try {
                        customerRepository.incrementStats(selectedCustomer!!.id, totals.total)
                    } catch (_: Exception) {}
                }
                
                // Generate PDF
                PdfGenerator.generateBillPdf(requireContext(), bill)
                
                Toast.makeText(context, "Bill Generated Successfully", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                Toast.makeText(context, "Failed to generate bill", Toast.LENGTH_SHORT).show()
                binding.btnGenerateBill.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
