package com.conviot.rssiindoorlocalization

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import com.conviot.rssiindoorlocalization.datastore.UserPreferences
import com.conviot.rssiindoorlocalization.ui.theme.RSSIIndoorLocalizationTheme
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class MethodSettingActivity: ComponentActivity() {
    private val dataStore by lazy { applicationContext.userPreferencesStore }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RSSIIndoorLocalizationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MethodSettingScreen(
                        dataStore,
                        Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    @Composable
    fun MethodSettingScreen(dataStore: DataStore<UserPreferences>, modifier: Modifier) {
        val scope = rememberCoroutineScope()

        var isDeadReckoning by remember { mutableStateOf(false) }
        var isWiFi by remember { mutableStateOf(false) }

        // Load initial values
        LaunchedEffect(Unit) {
            val preferences = dataStore.data.firstOrNull() ?: UserPreferences.getDefaultInstance()
            isDeadReckoning = preferences.deadReckoning
            isWiFi = preferences.wiFi
        }

        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = isDeadReckoning,
                    onCheckedChange = {
                        isDeadReckoning = it
                        scope.launch {
                            val data = dataStore.updateData { preferences ->
                                preferences.toBuilder()
                                    .setDeadReckoning(isDeadReckoning)
                                    .build()
                            }
                            if (data.port != null && data.server != null) {
                                Toast.makeText(applicationContext, "Saved", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(applicationContext, "Failed to save", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("isDeadReckoning")
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = isWiFi,
                    onCheckedChange = {
                        isWiFi = it
                        scope.launch {
                            val data = dataStore.updateData { preferences ->
                                preferences.toBuilder()
                                    .setWiFi(isWiFi)
                                    .build()
                            }
                            if (data.port != null && data.server != null) {
                                Toast.makeText(applicationContext, "Saved", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(applicationContext, "Failed to save", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("isWiFi")
            }
        }
    }
}
