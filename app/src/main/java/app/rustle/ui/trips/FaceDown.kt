package app.rustle.ui.trips

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import app.rustle.device.Device
import app.rustle.device.Phone
import app.rustle.domain.District
import app.rustle.domain.Pose
import app.rustle.ui.Home
import app.rustle.ui.Nav
import app.rustle.ui.Tab
import app.rustle.ui.art.Art
import app.rustle.ui.art.Buf
import app.rustle.ui.art.Critter
import app.rustle.ui.art.PixelCanvas
import app.rustle.ui.art.rect
import app.rustle.ui.art.rgb
import app.rustle.ui.components.Kind
import app.rustle.ui.components.PixelButton
import app.rustle.ui.theme.C
import app.rustle.ui.theme.T
import kotlinx.coroutines.delay

/** "Put the phone face down": he waits at the hatch and leaves the moment the screen meets the table. */
@Composable
fun FaceDown(h: Home, d: District, minutes: Int, phone: Phone, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    var gone by remember { mutableStateOf(false) }
    val sensor = remember { Device.hasAccelerometer(ctx) }
    fun go() {
        if (gone) return
        gone = true
        buzz(ctx)
        c.write { startTrip(d, minutes) }
    }
    LaunchedEffect(phone.faceDown) { if (phone.faceDown) go() }
    LaunchedEffect(gone) { if (gone) { delay(1600); nav.tab(Tab.HOME) } }
    BackHandler { nav.close() }
    Column(
        Modifier.fillMaxSize().background(C.night).clickable(remember { MutableInteractionSource() }, null) {}.statusBarsPadding().navigationBarsPadding().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        PixelCanvas(96, 56, Modifier.width(288.dp)) { b, t -> flip(b, t, h, gone) }
        Text(
            if (gone) stringResource(R.string.fd_gone, h.name) else stringResource(R.string.fd_title),
            style = T.Title.copy(color = C.copper), textAlign = TextAlign.Center,
        )
        if (!gone) {
            Text(stringResource(R.string.fd_sub, d.name_.of(h.ru)), style = T.Body.copy(color = C.paperD), textAlign = TextAlign.Center)
            if (!sensor) Text(stringResource(R.string.fd_nosensor), style = T.Small.copy(color = C.paperD), textAlign = TextAlign.Center)
            PixelButton(stringResource(R.string.fd_anyway), ::go, Modifier.fillMaxWidth(), kind = if (sensor) Kind.WOOD else Kind.BRASS)
            PixelButton(stringResource(R.string.cancel), nav::close, Modifier.fillMaxWidth(), kind = Kind.PALE, small = true)
        }
    }
}

private fun buzz(ctx: Context) {
    runCatching { (ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(VibrationEffect.createOneShot(60, 120)) }
}

/** A phone that turns over again and again, and him waiting by its edge with a bundle. */
private fun flip(b: Buf, t: Int, h: Home, gone: Boolean) {
    b.clear()
    val phase = if (Art.still) 0 else (t / 24) % 6
    val face = rgb(0x6A8AA8)
    val back = rgb(0xC07A40)
    val edge = rgb(0xA8B4C0)
    val (ph, col) = when (phase) { 0, 1 -> 22 to face; 2 -> 10 to face; 3 -> 2 to edge; 4 -> 10 to back; else -> 22 to back }
    val cy = 30
    b.rect(6, cy - ph / 2, 48, ph, col)
    if (ph > 4) { b.rect(6, cy - ph / 2, 48, 1, rgb(0xC8D8E8)); b.rect(6, cy + ph / 2 - 1, 48, 1, rgb(0x3A4A58)) }
    if (col == back && ph > 12) { b.rect(12, cy - 6, 6, 6, rgb(0x202428)); b.rect(13, cy - 5, 4, 4, rgb(0x5A7AA0)) }
    b.rect(2, 49, 92, 2, rgb(0x5A3A1E))
    if (!gone) b.blit(Critter.draw(h.look, Pose.IDLE, t), 60, 21)
}
