package com.conviot.rssiindoorlocalization

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.conviot.rssiindoorlocalization.datastore.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class KeywordSettingViewModel(
    private val dataStore: DataStore<UserPreferences>
) : ViewModel() {
    private val _keywordList = mutableListOf<String>()
    val keywordList: SnapshotStateList<String> = _keywordList.toMutableStateList()

    var newKeyword by mutableStateOf("")
        private set

    init {
        keywordList.clear()
        viewModelScope.launch {
            keywordList.addAll(dataStore.data.first().keywordsList)
        }
    }

    fun updateNewKeyword(value: String) {
        newKeyword = value
    }

    fun addToKeyword() {
        keywordList.add(newKeyword)
        newKeyword = ""
        viewModelScope.launch {
            updateDataStore()
        }
    }

    fun removeKeyword(value: String) {
        keywordList.remove(value)
        viewModelScope.launch {
            updateDataStore()
        }
    }

    private suspend fun updateDataStore() {
        dataStore.updateData { userPreferences ->
            userPreferences.toBuilder()
                .clearKeywords()
                .addAllKeywords(keywordList)
                .build()
        }
    }
}
