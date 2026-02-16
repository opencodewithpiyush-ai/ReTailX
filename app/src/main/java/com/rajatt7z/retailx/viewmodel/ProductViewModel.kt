package com.rajatt7z.retailx.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajatt7z.retailx.models.Product
import com.rajatt7z.retailx.repository.ProductRepository
import com.rajatt7z.retailx.utils.Resource
import kotlinx.coroutines.launch

class ProductViewModel : ViewModel() {

    private val repository = ProductRepository()
    
    private val _products = MutableLiveData<Resource<List<Product>>>()
    val products: LiveData<Resource<List<Product>>> = _products

    init {
        fetchProducts()
    }

    fun fetchProducts() {
        viewModelScope.launch {
            _products.postValue(Resource.Loading())
            try {
                val result = repository.getAllProducts()
                _products.postValue(Resource.Success(result))
            } catch (e: Exception) {
                _products.postValue(Resource.Error(e.message ?: "Failed to fetch products"))
            }
        }
    }
}
