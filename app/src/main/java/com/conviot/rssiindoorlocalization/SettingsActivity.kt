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

class SettingsActivity : ComponentActivity() {
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

    @Composable
    fun Menu(
        modifier: Modifier = Modifier,
        menuData: List<MenuData> = listOf(
            MenuData("지도 설정", MapSettingActivity::class.java),
            MenuData("키워드 설정", KeywordSettingActivity::class.java),
            MenuData("통신 설정", NetworkSettingActivity::class.java),
            MenuData("초기 위치 설정", LocationSettingActivity::class.java)
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
}
