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
import com.rajatt7z.retailx.databinding.FragmentCreateOrderBinding
import com.rajatt7z.retailx.models.Order
import com.rajatt7z.retailx.models.Product
import com.rajatt7z.retailx.repository.OrderRepository
import com.rajatt7z.retailx.repository.ProductRepository
import com.rajatt7z.retailx.utils.BarcodeScannerDialog
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class CreateOrderFragment : Fragment() {

    private var _binding: FragmentCreateOrderBinding? = null
    private val binding get() = _binding!!
    private val productRepository = ProductRepository()
    private val orderRepository = OrderRepository()
    private var products = listOf<Product>()
    private var selectedProduct: Product? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadProducts()
        setupListeners()
    }

    private fun loadProducts() {
        lifecycleScope.launch {
            products = productRepository.getAllProducts()
            val productNames = products.map { it.name }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, productNames)
            binding.autoCompleteProduct.setAdapter(adapter)
            
            binding.autoCompleteProduct.setOnItemClickListener { _, _, position, _ ->
                val name = adapter.getItem(position).toString()
                selectedProduct = products.find { it.name == name }
            }
        }
    }

    private fun setupListeners() {
        binding.btnScanBarcode.setOnClickListener {
            val scanner = BarcodeScannerDialog { barcode ->
                onBarcodeScanned(barcode)
            }
            scanner.show(parentFragmentManager, "BarcodeScanner")
        }

        binding.btnSubmitOrder.setOnClickListener {
            val quantityStr = binding.etQuantity.text.toString()
            val customerName = binding.etCustomerName.text.toString()
            val customerPhone = binding.etCustomerPhone.text.toString()
            
            if (selectedProduct == null) {
                Toast.makeText(context, "Please select a product", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (quantityStr.isEmpty() || quantityStr.toIntOrNull() == null) {
                Toast.makeText(context, "Please enter a valid quantity", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val quantity = quantityStr.toInt()
            if (quantity <= 0) {
                 Toast.makeText(context, "Quantity must be greater than 0", Toast.LENGTH_SHORT).show()
                 return@setOnClickListener
            }

            if (quantity > selectedProduct!!.stock) {
                Toast.makeText(context, "Insufficient stock. Available: ${selectedProduct!!.stock}", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val employeeId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val order = Order(
                productId = selectedProduct!!.id,
                productName = selectedProduct!!.name,
                quantity = quantity,
                totalPrice = selectedProduct!!.price * quantity,
                soldBy = employeeId,
                status = "Pending",
                customerName = customerName.ifEmpty { "Walk-in Customer" },
                customerPhone = customerPhone
            )

            lifecycleScope.launch {
                val success = orderRepository.createOrder(order)
                if (success) {
                    try {
                        productRepository.decrementStock(selectedProduct!!.id, quantity)
                    } catch (e: Exception) {
                        android.util.Log.e("CreateOrderFragment", "Failed to update stock", e)
                    }
                    Toast.makeText(context, "Order Created Successfully", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } else {
                    Toast.makeText(context, "Failed to create order", Toast.LENGTH_SHORT).show()
                }
            }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
