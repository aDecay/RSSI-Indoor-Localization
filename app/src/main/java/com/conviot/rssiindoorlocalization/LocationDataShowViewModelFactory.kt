package com.conviot.rssiindoorlocalization

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.conviot.rssiindoorlocalization.data.RssiDatabase

class LocationDataShowViewModelFactory(
    private val db: RssiDatabase,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if(modelClass.isAssignableFrom(LocationDataShowViewModel::class.java)) {
            return LocationDataShowViewModel(db, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}