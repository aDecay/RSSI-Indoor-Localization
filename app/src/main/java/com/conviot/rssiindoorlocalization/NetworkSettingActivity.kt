package com.conviot.rssiindoorlocalization

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.conviot.rssiindoorlocalization.datastore.UserPreferences
import com.conviot.rssiindoorlocalization.ui.theme.RSSIIndoorLocalizationTheme
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class NetworkSettingActivity: ComponentActivity() {
    private val dataStore by lazy { applicationContext.userPreferencesStore }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RSSIIndoorLocalizationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ServerPortInputScreen(
                        dataStore,
                        Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    @Composable
    fun ServerPortInputScreen(dataStore: DataStore<UserPreferences>, modifier: Modifier) {
        val scope = rememberCoroutineScope()

        var server by remember { mutableStateOf(TextFieldValue("")) }
        var port by remember { mutableStateOf(TextFieldValue("")) }

        // Load initial values
        LaunchedEffect(Unit) {
            val preferences = dataStore.data.firstOrNull() ?: UserPreferences.getDefaultInstance()
            server = TextFieldValue(preferences.server)
            port = TextFieldValue(preferences.port)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            TextField(
                value = server,
                onValueChange = { server = it },
                label = { Text("Server Address") },
                modifier = Modifier.fillMaxWidth()
            )

            TextField(
                value = port,
                onValueChange = { port = it },
                label = { Text("Port") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
            )

            Button(
                onClick = {
                    if (server.text.isNotEmpty() && port.text.isNotEmpty()) {
                        scope.launch {
                            val data = dataStore.updateData { preferences ->
                                preferences.toBuilder()
                                    .setServer(server.text)
                                    .setPort(port.text)
                                    .build()
                            }
                            if (data.port != null && data.server != null) {
                                Toast.makeText(applicationContext, "Saved", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(applicationContext, "Failed to save", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}
