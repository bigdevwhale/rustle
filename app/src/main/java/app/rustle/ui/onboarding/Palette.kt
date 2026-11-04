package app.rustle.ui.onboarding

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.rustle.R
import app.rustle.ui.components.Flow
import app.rustle.ui.components.Kind
import app.rustle.ui.components.PixelButton
import app.rustle.ui.components.pixelBox
import app.rustle.ui.theme.C
import app.rustle.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Fur colours to choose from, each with its second colour. */
val SWATCHES = listOf(
    0xC8946A to 0xF2DCC0, // ginger
    0x9A9AA2 to 0xE4E4EA, // grey
    0xE8D8B8 to 0xFFF4E0, // cream
    0x4A4048 to 0xC8B8C0, // soot
    0x7AB8A0 to 0xE0F4E8, // mint
    0xA890C8 to 0xF0E4FA, // lilac
    0xD87A5A to 0xF8D8B8, // fox
    0x6A88B8 to 0xDCE8F8, // twilight
)

/** Picks a fur colour and a lighter second colour from a photo: the most common lively colour, kept bright enough to read on the dark board. */
object PhotoColors {
    fun from(bmp: Bitmap): Pair<Int, Int> {
        val small = Bitmap.createScaledBitmap(bmp, 48, 48, true)
        val buckets = HashMap<Int, IntArray>()
        for (y in 0 until 48) for (x in 0 until 48) {
            val c = small.getPixel(x, y)
            val r = (c shr 16) and 0xFF; val g = (c shr 8) and 0xFF; val b = c and 0xFF
            val mx = max(r, max(g, b)); val mn = min(r, min(g, b))
            // Skip near-white and near-black: backgrounds and shadows.
            if (mx < 40 || mn > 235) continue
            val key = ((r / 32) shl 6) or ((g / 32) shl 3) or (b / 32)
            val a = buckets.getOrPut(key) { IntArray(4) }
            val weight = 1 + (mx - mn) / 40
            a[0] += r * weight; a[1] += g * weight; a[2] += b * weight; a[3] += weight
        }
        val best = buckets.values.maxByOrNull { it[3] } ?: return SWATCHES[0]
        var r = best[0] / best[3]; var g = best[1] / best[3]; var b = best[2] / best[3]
        // Lift dark colours so he doesn't vanish into the board.
        val lum = (r * 3 + g * 6 + b) / 10
        if (lum < 90) { val k = 90f / max(lum, 1); r = min(255, (r * k).toInt()); g = min(255, (g * k).toInt()); b = min(255, (b * k).toInt()) }
        val body = (r shl 16) or (g shl 8) or b
        val acc = (((r + 255 * 2) / 3) shl 16) or (((g + 255 * 2) / 3) shl 8) or ((b + 255 * 2) / 3)
        return body to acc
    }

    fun load(ctx: Context, uri: Uri): Bitmap? = runCatching {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(ctx.contentResolver, uri)) { d, info, _ ->
            d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val s = max(1, max(info.size.width, info.size.height) / 256)
            d.setTargetSize(info.size.width / s, info.size.height / s)
        }
    }.getOrNull()

    fun near(a: Int, b: Int) = abs(((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)) + abs(((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)) + abs((a and 0xFF) - (b and 0xFF)) < 12
}

@Composable
fun ColourPicker(body: Int, onPick: (Int, Int) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val photo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val p = withContext(Dispatchers.Default) { PhotoColors.load(ctx, uri)?.let { PhotoColors.from(it) } }
            if (p != null) onPick(p.first, p.second)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Flow {
            for ((b, a) in SWATCHES) {
                val on = PhotoColors.near(b, body)
                Box(Modifier.size(40.dp).pixelBox(C.of(b), outline = if (on) C.copperL else C.ink).clickable { onPick(b, a) })
            }
        }
        Text(stringResource(R.string.ob_photo_hint), style = T.Small.copy(color = C.textDim))
        PixelButton(stringResource(R.string.ob_photo), { photo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, kind = Kind.WOOD, small = true)
    }
}

/** The eight kinds as small moving portraits; the chosen one is framed. */
@Composable
fun SpeciesPicker(selected: app.rustle.domain.Species, body: Int, accent: Int, marks: app.rustle.domain.Marks, ru: Boolean, onPick: (app.rustle.domain.Species) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Flow {
            for (sp in app.rustle.domain.Species.entries) {
                val on = sp == selected
                Column(
                    Modifier.pixelBox(if (on) C.copperL else C.paper, outline = if (on) C.copperD else C.ink).clickable { onPick(sp) }.padding(4.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                ) {
                    app.rustle.ui.components.Portrait(app.rustle.ui.art.Look(if (on) body else sp.body, if (on) accent else sp.accent, species = sp, marks = if (on) marks else app.rustle.domain.Marks.NONE), scale = 1.5.dp)
                    Text(sp.title.of(ru), style = T.Small.copy(color = C.ink), maxLines = 1)
                }
            }
        }
        Text(selected.note.of(ru), style = T.Small.copy(color = C.textDim))
    }
}

@Composable
fun MarksPicker(selected: app.rustle.domain.Marks, ru: Boolean, onPick: (app.rustle.domain.Marks) -> Unit) {
    Flow { for (m in app.rustle.domain.Marks.entries) app.rustle.ui.components.Chip(m.title.of(ru), m == selected, { onPick(m) }, dark = true) }
}
