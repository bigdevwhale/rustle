package app.rustle.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.rustle.R
import app.rustle.container
import app.rustle.domain.Find
import app.rustle.domain.Pose
import app.rustle.ui.Home
import app.rustle.ui.Nav
import app.rustle.ui.art.Art
import app.rustle.ui.art.Buf
import app.rustle.ui.art.Critter
import app.rustle.ui.art.Look
import app.rustle.ui.art.PixelCanvas
import app.rustle.ui.art.rect
import app.rustle.ui.art.rgb
import app.rustle.ui.components.FindIcon
import app.rustle.ui.components.Kind
import app.rustle.ui.components.PixelButton
import app.rustle.ui.theme.C
import app.rustle.ui.theme.T
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The weekly spring-clean (#1): he rubs the screen glass from the inside, dust puffing off the cloth,
 * until it sparkles. The cloth is used up only when he has finished, so leaving early costs nothing.
 */
@Composable
fun CleanScene(h: Home, nav: Nav) {
    val c = LocalContext.current.container
    var done by remember { mutableStateOf(false) }
    var found by remember { mutableStateOf<Find?>(null) }
    LaunchedEffect(Unit) {
        if (Art.still) return@LaunchedEffect
        delay(Wipe.TICKS * Art.FRAME_MS + 400)
        found = c.clean()
        done = true
    }
    BackHandler { nav.close() }
    Column(
        Modifier.fillMaxSize().background(C.night).clickable(remember { MutableInteractionSource() }, null) {}.statusBarsPadding().navigationBarsPadding().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        PixelCanvas(Wipe.W, Wipe.H, Modifier.fillMaxWidth()) { b, t -> Wipe.draw(b, t, h.look, done) }
        Text(stringResource(if (done) R.string.cl_done_t else R.string.cl_title), style = T.Title.copy(color = C.copper), textAlign = TextAlign.Center)
        Text(stringResource(if (done) R.string.cl_done else R.string.cl_sub, h.name), style = T.Body.copy(color = C.paperD), textAlign = TextAlign.Center)
        if (done) {
            found?.let { f ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FindIcon(f)
                    Text(stringResource(R.string.cl_found, f.name.of(h.ru)), style = T.Body.copy(color = C.paperD))
                }
            }
            PixelButton(stringResource(R.string.cl_ok), nav::close, Modifier.fillMaxWidth())
        } else PixelButton(stringResource(R.string.cancel), nav::close, Modifier.fillMaxWidth(), kind = Kind.PALE, small = true)
    }
}

/** The screen glass seen from inside: grime on the right, shine on the left, and him in between with a cloth. */
object Wipe {
    const val W = 128
    const val H = 88
    /** How long the wiping takes, in art ticks (about six seconds). */
    const val TICKS = 200
    private const val FLOOR = 78

    private val BEZEL = rgb(0x2C2E36)
    private val GLASS_TOP = rgb(0xB8D8E8)
    private val GLASS_BOTTOM = rgb(0x7EA8C0)
    private val GRIME = rgb(0x8A7E68)
    private val PRINT = rgb(0x5E574A)
    private val SHINE = rgb(0xFFFBEA)
    private val PLANK = rgb(0x7A4A2A)

    fun draw(b: Buf, tick: Int, look: Look, done: Boolean) {
        b.clear()
        val p = when { done -> 1f; Art.still -> .55f; else -> (tick / TICKS.toFloat()).coerceIn(0f, 1f) }
        // The pane, lighter at the top.
        b.rect(0, 0, W, FLOOR + 2, BEZEL)
        for (y in 3 until FLOOR) {
            val t = (y - 3f) / (FLOOR - 3)
            b.rect(3, y, W - 6, 1, app.rustle.ui.art.mix(GLASS_TOP, GLASS_BOTTOM, t))
        }
        val edge = 3 + ((W - 6) * p).roundToInt()
        // Clean glass: two slanted streaks of light.
        for (k in listOf(18, 30, 76, 84)) for (y in 3 until FLOOR) {
            val x = k + (FLOOR - y) / 2
            if (x in 3 until edge) b.p(x, y, SHINE, if (k == 30 || k == 84) .25f else .4f)
        }
        // Dirty glass: a film, fingerprints, specks.
        for (y in 3 until FLOOR) for (x in maxOf(edge, 3) until W - 3) b.p(x, y, GRIME, .42f + .06f * ((x * 7 + y * 13) % 5) / 4f)
        for ((fx, fy) in listOf(96 to 22, 112 to 48, 70 to 40, 40 to 18, 54 to 58)) if (fx > edge + 4) print(b, fx, fy)
        for (i in 0 until 26) {
            val x = 5 + (i * 47) % (W - 10); val y = 5 + (i * 29) % (FLOOR - 10)
            if (x > edge) b.p(x, y, PRINT, .7f)
        }
        // The sill he stands on.
        b.rect(0, FLOOR, W, H - FLOOR, PLANK); b.rect(0, FLOOR, W, 1, rgb(0xA8703C)); b.rect(0, H - 2, W, 2, rgb(0x4A2A16))

        if (done) {
            b.blit(Critter.draw(look, Pose.SUN, tick), (W - Critter.W) / 2, FLOOR - 42)
            for ((sx, sy, r) in listOf(Triple(20, 14, 5), Triple(104, 20, 4), Triple(64, 9, 3), Triple(36, 44, 3), Triple(96, 52, 4)))
                sparkle(b, sx, sy, if (Art.still) r else (r - 1 + (sin(tick / 5.0 + sx) * 1.5).roundToInt()).coerceAtLeast(1))
            return
        }
        // Him, just behind the line he is wiping, with the cloth going up and down in front.
        val x = (edge - 40).coerceIn(-6, W - Critter.W)
        val y = FLOOR - 42
        b.blit(Critter.draw(look, Pose.IDLE, tick), x, y)
        val rub = if (Art.still) 2 else (sin(tick / 2.6) * 7).roundToInt()
        val cx = x + 40; val cy = y + 12 + rub
        cloth(b, cx, cy)
        // Puffs of dust coming off the cloth and drifting up and to the right.
        for (i in 0 until 4) {
            val age = if (Art.still) i * 7 + 4 else ((tick + i * 9) % 36)
            val px = cx + 6 + age / 3 + i * 2
            val py = cy - 2 - age / 2 + i
            val a = (1f - age / 36f) * .75f
            val r = 1 + age / 12
            puff(b, px, py, r, a)
        }
        // One glint on the part already clean.
        if (edge > 30) sparkle(b, (edge - 22).coerceAtLeast(12), 12, if (Art.still) 4 else 2 + ((tick / 6) % 3))
    }

    private fun print(b: Buf, x: Int, y: Int) {
        for (r in 1..4) for (a in 0 until 24) {
            val ang = a / 24.0 * Math.PI * 2
            b.p(x + (Math.cos(ang) * r * 1.3).roundToInt(), y + (Math.sin(ang) * r).roundToInt(), PRINT, .38f)
        }
    }

    /** The cloth: a scrap stitched from coloured threads, grey on the side that rubs. */
    private fun cloth(b: Buf, x: Int, y: Int) {
        b.rect(x, y, 10, 8, rgb(0xD24A3A))
        for (i in 0 until 10 step 3) b.rect(x + i, y, 1, 8, rgb(0x4A72C8))
        b.rect(x, y + 3, 10, 1, rgb(0xF0C050))
        b.rect(x + 9, y, 1, 8, rgb(0x9A9890))
        b.rect(x - 1, y - 1, 12, 1, rgb(0x1B140E)); b.rect(x - 1, y + 8, 12, 1, rgb(0x1B140E))
        b.rect(x - 1, y, 1, 8, rgb(0x1B140E)); b.rect(x + 10, y, 1, 8, rgb(0x1B140E))
    }

    private fun puff(b: Buf, cx: Int, cy: Int, r: Int, a: Float) {
        for (j in -r..r) for (i in -r..r) if (i * i + j * j <= r * r + r) b.p(cx + i, cy + j, rgb(0xC8C2B4), a)
        b.p(cx - r / 2, cy - r / 2, rgb(0xEEE8DA), a)
    }

    private fun sparkle(b: Buf, x: Int, y: Int, r: Int) {
        for (i in -r..r) { val a = 1f - kotlin.math.abs(i) / (r + 1f); b.p(x + i, y, SHINE, a); b.p(x, y + i, SHINE, a) }
        b.p(x - 1, y - 1, SHINE, .5f); b.p(x + 1, y + 1, SHINE, .5f); b.p(x + 1, y - 1, SHINE, .5f); b.p(x - 1, y + 1, SHINE, .5f)
        b.p(x, y, rgb(0xFFFFFF))
    }
}
