package com.strive4it.greenmonkeys

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.strive4it.greenmonkeys.ui.theme.GreenMonkeysTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GreenMonkeysTheme {
                PlaceholderHome()
            }
        }
    }
}

/// Placeholder until the real Home screen lands (brief §5 parity checklist).
@Composable
private fun PlaceholderHome() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "The Monkeys are moving in. 🐒")
    }
}
