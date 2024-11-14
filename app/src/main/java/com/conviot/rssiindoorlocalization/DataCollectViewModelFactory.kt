package com.conviot.rssiindoorlocalization

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.conviot.rssiindoorlocalization.ui.theme.DataCollectViewModel

class DataCollectViewModelFactory(private val context : Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if(modelClass.isAssignableFrom(DataCollectViewModel::class.java)) {
            return DataCollectViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}