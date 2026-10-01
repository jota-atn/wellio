package com.projeto.wellio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.projeto.wellio.ui.navigation.WellioNavHost
import com.projeto.wellio.ui.theme.WellioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WellioTheme {
                WellioNavHost()
            }
        }
    }
}
