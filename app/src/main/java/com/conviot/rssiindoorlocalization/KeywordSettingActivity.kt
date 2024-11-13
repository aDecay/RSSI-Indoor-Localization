package com.conviot.rssiindoorlocalization

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.conviot.rssiindoorlocalization.ui.theme.RSSIIndoorLocalizationTheme

class KeywordSettingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RSSIIndoorLocalizationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    KeywordSetter(
                        Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    @Composable
    fun KeywordSetter(
        modifier: Modifier = Modifier,
        keywords: List<String> = listOf()
    ) {
        Column(modifier = modifier.fillMaxWidth(),) {
            KeywordAddBar()
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(keywords) { keyword ->
                    KeywordItem(keyword)
                }
            }
        }
    }

    @Composable
    fun KeywordAddBar(modifier: Modifier = Modifier) {
        var value by remember { mutableStateOf("") }

        TextField(
            value = value,
            onValueChange = { value = it },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
            },
            placeholder = {
                Text("키워드")
            },
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        )
    }

    @Composable
    fun KeywordItem(
        text: String,
        modifier: Modifier = Modifier
    ) {
        Surface(
            modifier = modifier.border(BorderStroke(2.dp, SolidColor(Color.Black)))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text(
                    text = text,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(25.dp)
                )
            }
        }
    }

    @Composable
    @Preview
    fun KeywordSetterPreview() {
        RSSIIndoorLocalizationTheme {
            KeywordSetter(keywords = listOf("키워드1", "키워드2"))
        }
    }

    @Composable
    @Preview
    fun KeywordAddBarPreview() {
        RSSIIndoorLocalizationTheme {
            KeywordAddBar()
        }
    }

    @Composable
    @Preview
    fun KeywordItemPreview() {
        RSSIIndoorLocalizationTheme {
            KeywordItem(text = "키워드 항목")
        }
    }
}