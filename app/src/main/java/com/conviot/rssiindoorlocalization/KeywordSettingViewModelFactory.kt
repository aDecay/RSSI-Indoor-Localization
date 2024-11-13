package com.conviot.rssiindoorlocalization

import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.conviot.rssiindoorlocalization.datastore.UserPreferences

class KeywordSettingViewModelFactory(private val dataStore : DataStore<UserPreferences>) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if(modelClass.isAssignableFrom(KeywordSettingViewModel::class.java)) {
            return KeywordSettingViewModel(dataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}