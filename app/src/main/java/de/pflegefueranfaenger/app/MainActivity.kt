package de.pflegefueranfaenger.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import de.pflegefueranfaenger.app.ui.PflegeApp
import de.pflegefueranfaenger.app.ui.theme.PflegeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PflegeTheme {
                PflegeApp()
            }
        }
    }
}
