package com.example.upworksheetsproject

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.upworksheetsproject.data.MainRepository
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val repo: MainRepository
): ViewModel() {

    private val isVerified: MutableLiveData<Boolean> = MutableLiveData(false)

    private val _companyData: MutableLiveData<Map<String, List<String>>> = MutableLiveData()
    val companyData: LiveData<Map<String, List<String>>>
        get() = _companyData

    fun getOtp() {

    }

    fun getCompanyData() = viewModelScope.launch {
        _companyData.value = repo.getCompanyData()
    }

    fun isVerified(): Boolean {
        return isVerified.value!!
    }

    fun setIsVerified(boolean: Boolean) {
        isVerified.value = boolean
    }
}