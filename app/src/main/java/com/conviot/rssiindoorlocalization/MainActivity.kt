package com.conviot.rssiindoorlocalization

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.conviot.rssiindoorlocalization.ui.theme.RSSIIndoorLocalizationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RSSIIndoorLocalizationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Menu(
                        Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

data class MenuData(val text: String, val cls: Class<*>)

@Composable
fun Menu(
    modifier: Modifier = Modifier,
    menuData: List<MenuData> = listOf(
        MenuData("데이터 수집", DataCollectActivity::class.java),
        MenuData("데이터 열람", MainActivity::class.java),
        MenuData("테스트", MainActivity::class.java),
        MenuData("설정", SettingsActivity::class.java)
    )
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxSize()
    ) {
        for (data in menuData) {
            MenuIntentButton(data.text, data.cls, Modifier.width(200.dp))
        }
    }
}

@Composable
fun MenuIntentButton(text: String, cls: Class<*>, modifier: Modifier) {
    val context = LocalContext.current

    Button(
        onClick = {
            context.startActivity(Intent(context, cls))
        },
        modifier = modifier
    ) {
        Text(text = text)
    }
}

@Preview(showBackground = true)
@Composable
fun MenuPreview() {
    RSSIIndoorLocalizationTheme {
        Menu()
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RSSIIndoorLocalizationTheme {
        Greeting("Android")
    }
}