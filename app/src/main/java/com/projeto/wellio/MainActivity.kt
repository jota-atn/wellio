package com.projeto.wellio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.projeto.wellio.model.HomeViewModel
import com.projeto.wellio.ui.home.HomeScreen
import com.projeto.wellio.ui.theme.WellioTheme

class MainActivity : ComponentActivity() {
    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WellioTheme {
                HomeScreen(homeViewModel)
            }
        }
    }
}
