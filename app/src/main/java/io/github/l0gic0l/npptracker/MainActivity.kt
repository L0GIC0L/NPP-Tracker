package io.github.l0gic0l.npptracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.l0gic0l.npptracker.ui.NppApp
import io.github.l0gic0l.npptracker.ui.theme.NppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NppTheme {
                NppApp()
            }
        }
    }
}
