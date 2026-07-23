package com.github.helltar.anpaside

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.github.helltar.anpaside.ui.AnpasideApp
import com.github.helltar.anpaside.ui.theme.AnpasideTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AnpasideTheme {
                AnpasideApp()
            }
        }
    }
}
