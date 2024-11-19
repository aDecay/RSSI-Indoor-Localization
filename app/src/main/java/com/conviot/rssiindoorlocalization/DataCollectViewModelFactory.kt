package com.conviot.rssiindoorlocalization

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.conviot.rssiindoorlocalization.datastore.UserPreferences

class DataCollectViewModelFactory(
    private val context: Context,
    private val dataStore: DataStore<UserPreferences>
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if(modelClass.isAssignableFrom(DataCollectViewModel::class.java)) {
            return DataCollectViewModel(context, dataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}