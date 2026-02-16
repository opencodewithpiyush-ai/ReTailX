package com.rajatt7z.retailx.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajatt7z.retailx.models.Order
import com.rajatt7z.retailx.repository.OrderRepository
import com.rajatt7z.retailx.utils.Resource
import kotlinx.coroutines.launch

class OrderViewModel : ViewModel() {

    private val repository = OrderRepository()
    
    private val _orders = MutableLiveData<Resource<List<Order>>>()
    val orders: LiveData<Resource<List<Order>>> = _orders

    init {
        fetchOrders()
    }

    fun fetchOrders() {
        viewModelScope.launch {
            _orders.postValue(Resource.Loading())
            try {
                val result = repository.getAllOrders()
                _orders.postValue(Resource.Success(result))
            } catch (e: Exception) {
                _orders.postValue(Resource.Error(e.message ?: "Failed to fetch orders"))
            }
        }
    }
}
