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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
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

    private fun shot(name: String, scrollTo: String? = null, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent { RustleTheme { content() } }
        rule.mainClock.advanceTimeBy(1_500)
        rule.waitForIdle()
        scrollTo?.let { rule.onNodeWithText(it, substring = true).performScrollTo(); rule.mainClock.advanceTimeBy(500) }
        rule.mainClock.advanceTimeBy(500)
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun screen(name: String, tab: Tab, hour: Int = 15, overlay: Overlay? = null, phone: Phone = Phone(level = 72, charging = true), scrollTo: String? = null, before: () -> Unit = {}, edit: (State) -> State = { it }) {
        val now = at(hour)
        val s = edit(DemoSeed.seeded(now, lang == "ru"))
        before()
        shot(name, scrollTo) { RustleRoot(s, now, tab, overlay, phone) }
    }

    @Test fun home() = screen("1-home", Tab.HOME, 16)
    @Test fun evening() = screen("2-home-evening", Tab.HOME, 21)
    @Test fun trips() = screen("3-trips", Tab.TRIPS)
    @Test fun shelf() = screen("4-shelf", Tab.SHELF)
    @Test fun workshop() = screen("5-workshop", Tab.WORKSHOP)
    @Test fun profile() = screen("6-profile", Tab.HOME, overlay = Overlay.Profile)
    @Test fun feats() = screen("7-feats", Tab.HOME, overlay = Overlay.Feats)
    @Test fun needs() = screen("8-needs", Tab.HOME, overlay = Overlay.Needs)

    // The spring-clean and the Net (#1).
    private val ru get() = lang == "ru"
    @Test fun clean() = screen("9-clean", Tab.HOME, overlay = Overlay.Clean)
    @Test fun netCard() = screen("10-net-card", Tab.HOME, 16, phone = Phone(level = 72, charging = true, wifi = true), scrollTo = if (ru) "Поход в Сеть" else "Into the Net")
    @Test fun cleanCard() = screen("10-clean-card", Tab.HOME, 16, scrollTo = if (ru) "Протереть телефон" else "Wipe the phone") { it.copy(cleanAt = 0) }
    @Test fun choresNoCloth() = screen("11-no-cloth", Tab.HOME, 16, scrollTo = if (ru) "Генеральная уборка" else "Spring-clean") { it.copy(cleanAt = 0, cloths = 0) }
    @Test fun inNet() = screen("12-in-net", Tab.HOME, 16) { it.copy(netAt = at(16) - 3 * MIN, netDay = app.rustle.domain.World.dayOf(at(16))) }
    @Test fun netNews() = screen("13-net-news", Tab.HOME, 16, before = { rule.activity.container.netBack.value = 2 })
    @Test fun clothCard() = screen("14-cloth", Tab.WORKSHOP, scrollTo = if (ru) "В запасе" else "In the drawer")
}

@Config(sdk = [34], qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderScreensRu : ScreensBase() { override val lang = "ru" }

@Config(sdk = [34], qualifiers = "en-w411dp-h891dp-xxhdpi")
class RenderScreensEn : ScreensBase() { override val lang = "en" }
