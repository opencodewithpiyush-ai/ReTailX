package com.rajatt7z.retailx.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajatt7z.retailx.models.LoginLog
import com.rajatt7z.retailx.repository.AuthRepository
import com.rajatt7z.retailx.utils.Resource
import kotlinx.coroutines.launch

class LogsViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _logs = MutableLiveData<Resource<List<LoginLog>>>()
    val logs: LiveData<Resource<List<LoginLog>>> = _logs

    fun fetchLoginLogs() {
        _logs.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.getLoginLogs()
            _logs.value = result
        }
    }
}
