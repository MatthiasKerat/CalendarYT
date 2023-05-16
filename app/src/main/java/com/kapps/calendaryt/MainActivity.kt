package com.kapps.calendaryt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kapps.calendaryt.ui.theme.whiteGray

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val day = remember{
                mutableStateOf(FullDate())
            }
            // A surface container using the 'background' color from the theme
            Surface(
                modifier = Modifier
                    .fillMaxSize(),
                color = MaterialTheme.colors.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                        .background(whiteGray)
                ) {
                    // example. Allow user to select 30 days before the current date
                    // and 60 days after the current date
                    FullCalendar(-30,60){
                        day.value = it
                    }
                    Text(text = "${day.value.year}/${day.value.month}/${day.value.day}")
                    day.value.hours.forEach {
                        Text(it)
                    }
                }
            }
        }
    }
}