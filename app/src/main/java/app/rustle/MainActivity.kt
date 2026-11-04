package app.rustle

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.rustle.data.State
import app.rustle.ui.RustleRoot
import app.rustle.ui.art.Art
import app.rustle.ui.theme.RustleTheme
import app.rustle.ui.theme.C
import app.rustle.ui.theme.Hue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.SHOTS) Art.still = true
        val t = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(t), navigationBarStyle = SystemBarStyle.dark(t))
        setContent {
            val s: State? by container.store.state.collectAsStateWithLifecycle(initialValue = null)
            val seeding by container.seeding.collectAsStateWithLifecycle()
            val system = isSystemInDarkTheme()
            val dark = when (s?.theme) { "dark" -> true; "light" -> false; else -> system }
            C.pal = Hue.of(s?.hue ?: "GREEN").palette(dark)
            // Light backgrounds want dark status bar icons, and the other way round.
            LaunchedEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = if (dark) SystemBarStyle.dark(t) else SystemBarStyle.light(t, t),
                    navigationBarStyle = if (dark) SystemBarStyle.dark(t) else SystemBarStyle.light(t, t),
                )
            }
            // Wait for the state (and the demo house) so the welcome doesn't flash for someone already here.
            if (!seeding) s?.let { RustleTheme { RustleRoot(it) } }
        }
    }
}
