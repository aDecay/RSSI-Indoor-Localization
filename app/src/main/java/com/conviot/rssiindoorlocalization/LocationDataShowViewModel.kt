package com.conviot.rssiindoorlocalization

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conviot.rssiindoorlocalization.data.RssiDatabase
import com.conviot.rssiindoorlocalization.data.entity.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LocationDataShowViewModel(
    private val db: RssiDatabase
) : ViewModel() {
    val locationList = mutableStateListOf<Location>()

    private val locationDao = db.locationDao()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            locationList.addAll(locationDao.getAll())
        }
    }
}