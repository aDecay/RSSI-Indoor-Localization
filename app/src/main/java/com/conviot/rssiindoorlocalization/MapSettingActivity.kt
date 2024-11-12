package com.conviot.rssiindoorlocalization

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.conviot.rssiindoorlocalization.ui.theme.RSSIIndoorLocalizationTheme

class MapSettingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RSSIIndoorLocalizationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MapSelector(
                        Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    @Composable
    fun MapSelector(modifier: Modifier = Modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(R.drawable.empty),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = modifier.fillMaxWidth()
            )
            Button({}) {
                Text(text = "이미지 선택")
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun MapSelectorPreview() {
        RSSIIndoorLocalizationTheme {
            MapSelector()
        }
    }
}