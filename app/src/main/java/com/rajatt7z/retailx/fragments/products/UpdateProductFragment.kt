package com.rajatt7z.retailx.fragments.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.rajatt7z.retailx.databinding.FragmentUpdateProductBinding
import com.rajatt7z.retailx.models.Product
import com.rajatt7z.retailx.repository.ProductRepository
import kotlinx.coroutines.launch

@dagger.hilt.android.AndroidEntryPoint
class UpdateProductFragment : Fragment() {

    private var _binding: FragmentUpdateProductBinding? = null
    private val binding get() = _binding!!
    private val repository = ProductRepository()
    private val args: UpdateProductFragmentArgs by navArgs()
    private lateinit var currentProduct: Product
    
    @javax.inject.Inject
    lateinit var geminiHelper: com.rajatt7z.retailx.utils.GeminiHelper

    private lateinit var imageAdapter: com.rajatt7z.retailx.adapters.EditableImageAdapter
    private val selectedImages = mutableListOf<com.rajatt7z.retailx.adapters.EditableImage>()

    private val pickImageLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
        uri?.let {
            val image = com.rajatt7z.retailx.adapters.EditableImage.Local(it)
            imageAdapter.addImage(image)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpdateProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        loadProduct(args.productId)

        binding.btnAddImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnUpdate.setOnClickListener {
            updateProduct()
        }
        
        binding.btnGenerateAI.setOnClickListener {
            generateDescription()
        }
    }

    private fun setupRecyclerView() {
        imageAdapter = com.rajatt7z.retailx.adapters.EditableImageAdapter(selectedImages) { image ->
            imageAdapter.removeImage(image)
        }
        binding.rvEventListeners.adapter = imageAdapter
    }

    private fun loadProduct(id: String) {
        lifecycleScope.launch {
            val product = repository.getProduct(id)
            if (product != null) {
                currentProduct = product
                populateFields(product)
            } else {
                Toast.makeText(context, "Product not found", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }
    }

    private fun populateFields(product: Product) {
        binding.etProductName.setText(product.name)
        binding.etProductDescription.setText(product.description)
        binding.etProductPrice.setText(product.price.toString())
        binding.etProductStock.setText(product.stock.toString())
        binding.etProductCategory.setText(product.category)

        selectedImages.clear()
        if (product.imageUrls.isNotEmpty()) {
            selectedImages.addAll(product.imageUrls.map { com.rajatt7z.retailx.adapters.EditableImage.Remote(it) })
            imageAdapter.notifyDataSetChanged()
        }
    }
    
    private fun generateDescription() {
        val productName = binding.etProductName.text.toString()
        val category = binding.etProductCategory.text.toString()

        if (productName.isBlank()) {
            binding.etProductName.error = "Enter product name first"
            return
        }

        lifecycleScope.launch {
            try {
                binding.btnGenerateAI.isEnabled = false
                geminiHelper.generateProductDescription(productName, category).collect { description ->
                     binding.etProductDescription.setText(description)
                }
            } catch (e: Exception) {
                android.util.Log.e("UpdateProductFragment", "AI Generation Error", e)
                Toast.makeText(requireContext(), "Error generating description: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnGenerateAI.isEnabled = true
            }
        }
    }

    private fun updateProduct() {
        if (!::currentProduct.isInitialized) return

        val name = binding.etProductName.text.toString()
        val description = binding.etProductDescription.text.toString()
        val price = binding.etProductPrice.text.toString().toDoubleOrNull() ?: 0.0
        val stock = binding.etProductStock.text.toString().toIntOrNull() ?: 0
        val category = binding.etProductCategory.text.toString()

        if (name.isEmpty() || description.isEmpty() || category.isEmpty()) {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnUpdate.isEnabled = false

        lifecycleScope.launch {
            try {
                // Handle Images
                val finalImageUrls = mutableListOf<String>()
                val currentImages = imageAdapter.getImages()

                for (image in currentImages) {
                    when (image) {
                        is com.rajatt7z.retailx.adapters.EditableImage.Remote -> {
                            finalImageUrls.add(image.url)
                        }
                        is com.rajatt7z.retailx.adapters.EditableImage.Local -> {
                            val url = repository.uploadImage(requireContext(), image.uri)
                            finalImageUrls.add(url)
                        }
                    }
                }

                currentProduct.name = name
                currentProduct.description = description
                currentProduct.price = price
                currentProduct.stock = stock
                currentProduct.category = category
                currentProduct.imageUrls = finalImageUrls

                repository.updateProduct(currentProduct)
                Toast.makeText(context, "Product updated", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } catch (e: Exception) {
                Toast.makeText(context, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnUpdate.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
