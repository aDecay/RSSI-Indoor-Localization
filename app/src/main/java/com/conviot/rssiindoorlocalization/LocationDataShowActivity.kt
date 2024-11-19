package com.conviot.rssiindoorlocalization

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.conviot.rssiindoorlocalization.data.RssiDatabase
import com.conviot.rssiindoorlocalization.data.entity.Location
import com.conviot.rssiindoorlocalization.ui.theme.RSSIIndoorLocalizationTheme
import java.text.SimpleDateFormat

class LocationDataShowActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RSSIIndoorLocalizationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LocationDataViewer(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    @Composable
    fun LocationDataViewer(
        locationDataShowViewModel: LocationDataShowViewModel = ViewModelProvider(
            this,
            LocationDataShowViewModelFactory(
                db = Room.databaseBuilder(
                    applicationContext,
                    RssiDatabase::class.java, "rssi_db"
                ).build()
            )
        ).get(LocationDataShowViewModel::class.java),
        modifier: Modifier = Modifier
    ) {
        Column {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                onClick = { locationDataShowViewModel.onDeleteAllClick() }
            ) {
                Text("전체 삭제")
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                onClick = {}
            ) {
                Text("내보내기")
            }

            LazyColumn {
                items(locationDataShowViewModel.locationList) { location ->
                    LocationItem(location)
                }
            }
        }
    }

    @Composable
    fun LocationItem(
        location: Location,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(2.dp, SolidColor(Color.Black)))
        ) {
            Text("시간: ${SimpleDateFormat("YYYY-MM-DD HH:mm:ss").format(location.timestamp)}")
            Text("좌표: (${location.x}, ${location.y})")
            Text("장소: ${location.label}")
        }
    }

    @Composable
    @Preview(showBackground = true)
    fun LocationDataViewerPreview() {
        RSSIIndoorLocalizationTheme {
            LocationDataViewer()
        }
    }

    @Composable
    @Preview(showBackground = true)
    fun LocationItemPreview() {
        RSSIIndoorLocalizationTheme {
            LocationItem(Location(1, 0, "장소", 0.5f, 0.5f, 1))
        }
    }
}