package app.rustle

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import app.rustle.data.DemoSeed
import app.rustle.data.HOUR
import app.rustle.data.MIN
import app.rustle.data.State
import app.rustle.device.Phone
import app.rustle.ui.Overlay
import app.rustle.ui.RustleRoot
import app.rustle.ui.Tab
import app.rustle.ui.art.Art
import app.rustle.ui.theme.C
import app.rustle.ui.theme.Hue
import app.rustle.ui.theme.RustleTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/** The main screens over the demo house, drawn to PNGs (run with `-Prender --tests '*RenderScreens*'`), in both languages. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class ScreensBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    abstract val lang: String
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() } }

    @Before fun still() { Art.still = true; C.pal = Hue.GREEN.palette(true) }

    private fun at(hour: Int): Long = LocalDate.now().atStartOfDay(ZoneId.of("Europe/Moscow")).toInstant().toEpochMilli() + hour * HOUR + 10 * MIN

    private fun shot(name: String, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent { RustleTheme { content() } }
        rule.mainClock.advanceTimeBy(1_500)
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(500)
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun screen(name: String, tab: Tab, hour: Int = 15, overlay: Overlay? = null, phone: Phone = Phone(level = 72, charging = true), edit: (State) -> State = { it }) {
        val now = at(hour)
        val s = edit(DemoSeed.seeded(now, lang == "ru"))
        shot(name) { RustleRoot(s, now, tab, overlay, phone) }
    }

    @Test fun home() = screen("1-home", Tab.HOME, 16)
    @Test fun evening() = screen("2-home-evening", Tab.HOME, 21)
    @Test fun trips() = screen("3-trips", Tab.TRIPS)
    @Test fun shelf() = screen("4-shelf", Tab.SHELF)
    @Test fun workshop() = screen("5-workshop", Tab.WORKSHOP)
    @Test fun profile() = screen("6-profile", Tab.HOME, overlay = Overlay.Profile)
    @Test fun feats() = screen("7-feats", Tab.HOME, overlay = Overlay.Feats)
    @Test fun needs() = screen("8-needs", Tab.HOME, overlay = Overlay.Needs)
}

@Config(sdk = [34], qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderScreensRu : ScreensBase() { override val lang = "ru" }

@Config(sdk = [34], qualifiers = "en-w411dp-h891dp-xxhdpi")
class RenderScreensEn : ScreensBase() { override val lang = "en" }
