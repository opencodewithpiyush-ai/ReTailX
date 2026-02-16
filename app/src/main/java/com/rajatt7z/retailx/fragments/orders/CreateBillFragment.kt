package com.rajatt7z.retailx.fragments.orders

import android.content.Context
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
import com.rajatt7z.retailx.adapters.CartAdapter
import com.rajatt7z.retailx.databinding.FragmentCreateBillBinding
import com.rajatt7z.retailx.models.Bill
import com.rajatt7z.retailx.models.CartItem
import com.rajatt7z.retailx.models.Product
import com.rajatt7z.retailx.repository.BillRepository
import com.rajatt7z.retailx.repository.ProductRepository
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
    
    private var allProducts = listOf<Product>()
    private var selectedProduct: Product? = null
    
    private val cartItems = mutableListOf<CartItem>()
    private lateinit var cartAdapter: CartAdapter
    
    private var currentEmployeeName = "Store Staff"

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
        setupListeners()
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
                     // The position in the adapter might match allProducts if filter is not modifying order significantly
                     // But safer to find by name if filter is active. 
                     // Default AutoComplete filter works, but careful with index.
                     // Let's find product by name from the list
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

    private fun setupListeners() {
        binding.btnAddItem.setOnClickListener {
            addItemToCart()
        }

        binding.btnGenerateBill.setOnClickListener {
            generateBill()
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

            // Create a temporary bill for preview
            val tempBill = com.rajatt7z.retailx.models.Bill(
                id = "PREVIEW",
                products = cartItems,
                customerName = customerName,
                customerPhone = customerPhone,
                totalAmount = cartItems.sumOf { it.totalPrice },
                timestamp = System.currentTimeMillis(),
                generatedByName = "Current User" // In real app, fetch user name
            )

            // Show Dialog
            val dialog = com.rajatt7z.retailx.fragments.orders.BillPreviewDialog(tempBill, {})
            dialog.show(parentFragmentManager, "BillPreviewDialog")
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

        // Add or Update
        val existingItemIndex = cartItems.indexOfFirst { it.productId == product.id }
        if (existingItemIndex != -1) {
            val existing = cartItems[existingItemIndex]
            val newQty = existing.quantity + quantity
            val newItem = existing.copy(
                quantity = newQty,
                totalPrice = newQty * existing.unitPrice
            )
            cartItems[existingItemIndex] = newItem
        } else {
            val newItem = CartItem(
                productId = product.id,
                productName = product.name,
                quantity = quantity,
                unitPrice = product.price,
                totalPrice = quantity * product.price
            )
            cartItems.add(newItem)
        }

        updateUI()
        binding.etQuantity.text?.clear()
        binding.autoCompleteProduct.text.clear()
        selectedProduct = null
    }

    private fun removeItem(item: CartItem) {
        cartItems.remove(item)
        updateUI()
    }

    private fun updateUI() {
        cartAdapter.updateList(cartItems)
        cartAdapter.notifyDataSetChanged() // Force refresh
        
        val total = cartItems.sumOf { it.totalPrice }
        binding.tvTotalAmount.text = String.format("$%.2f", total)
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
            val bill = Bill(
                id = billId,
                products = cartItems.toList(),
                customerName = customerName,
                customerPhone = customerPhone,
                totalAmount = cartItems.sumOf { it.totalPrice },
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
