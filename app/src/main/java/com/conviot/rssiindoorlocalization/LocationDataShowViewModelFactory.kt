package com.conviot.rssiindoorlocalization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.conviot.rssiindoorlocalization.data.RssiDatabase

class LocationDataShowViewModelFactory(
    private val db: RssiDatabase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if(modelClass.isAssignableFrom(LocationDataShowViewModel::class.java)) {
            return LocationDataShowViewModel(db) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}