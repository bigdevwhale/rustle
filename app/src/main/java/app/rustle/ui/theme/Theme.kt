package app.rustle.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.rustle.R

/** The colours that follow the theme: the background and what is written on it. */
@Immutable
data class Palette(
    val dark: Boolean,
    val board: Color, val boardD: Color, val boardL: Color,
    /** Text straight on the background, and quieter text. */
    val text: Color, val textDim: Color,
    /** Headings on the background, and highlighted words. */
    val title: Color, val hi: Color,
)

/** Background hues, each with a dark and a light version. */
enum class Hue(val dark: Triple<Long, Long, Long>, val light: Triple<Long, Long, Long>) {
    GREEN(Triple(0xFF173A2E, 0xFF0E241C, 0xFF285A46), Triple(0xFFDCE8DF, 0xFFC6D8CB, 0xFFEEF4EF)),
    NAVY(Triple(0xFF1A2A44, 0xFF0F1A2E, 0xFF2E4466), Triple(0xFFDCE3EE, 0xFFC6D1E2, 0xFFEEF2F8)),
    PLUM(Triple(0xFF3A1F38, 0xFF24121F, 0xFF553052), Triple(0xFFEADCE8, 0xFFDBC6D8, 0xFFF5EEF4)),
    COCOA(Triple(0xFF3A2A1E, 0xFF241811, 0xFF554030), Triple(0xFFEDE2D3, 0xFFDCCCB6, 0xFFF6F0E8)),
    GRAPHITE(Triple(0xFF2A2D33, 0xFF1A1C20, 0xFF3E424A), Triple(0xFFE3E4E7, 0xFFCFD2D8, 0xFFF1F2F4));

    fun palette(dark: Boolean): Palette {
        val (b, d, l) = if (dark) this.dark else light
        return if (dark) Palette(true, Color(b), Color(d), Color(l), Color(0xFFF3E8CF), Color(0xFFD9C7A0), Color(0xFFE0954A), Color(0xFFFFD08A))
        else Palette(false, Color(b), Color(d), Color(l), Color(0xFF1B140E), Color(0xFF5E5446), Color(0xFF9A5420), Color(0xFFA8501A))
    }

    companion object {
        fun of(name: String) = entries.firstOrNull { it.name == name } ?: GREEN
    }
}

/**
 * The app's colours. The background ones ([board], [text], [title]…) follow the chosen theme and hue;
 * cards (paper), copper, ember and ink stay the same in both themes.
 */
object C {
    var pal: Palette by mutableStateOf(Hue.GREEN.palette(true))

    val board get() = pal.board
    val boardD get() = pal.boardD
    val boardL get() = pal.boardL
    val text get() = pal.text
    val textDim get() = pal.textDim
    val title get() = pal.title
    val hi get() = pal.hi

    /** Always dark: the "turn the phone over" screen and the night behind it. */
    val night = Color(0xFF09140F)
    val ink = Color(0xFF1B140E)
    val paper = Color(0xFFF3E8CF)
    val paperD = Color(0xFFD9C7A0)
    val note = Color(0xFFFFF6E0)
    val muted = Color(0xFF7D6E58)
    val copper = Color(0xFFE0954A)
    val copperL = Color(0xFFFFD08A)
    val copperD = Color(0xFF8E5222)
    val ember = Color(0xFFD8553A)
    val emberD = Color(0xFF86281A)
    val emberL = Color(0xFFF59A7E)
    val led = Color(0xFF8BE0AE)
    val frost = Color(0xFF9CC8DA)
    /** "Done" and "enough" on paper. */
    val ok = Color(0xFF2E7A5A)

    fun of(rgb: Int) = Color(0xFF000000 or (rgb.toLong() and 0xFFFFFF))
}

/** m6x11 by Daniel Linssen with Cyrillic by Natto Games (free with attribution). */
val Pixel = FontFamily(Font(R.font.m6x11, FontWeight.Normal))

/**
 * Pixel letters where they carry character — his name, headings, buttons, numbers — and the phone's own
 * plain face for sentences, so tips and descriptions stay easy to read.
 */
object T {
    private val Plain = FontFamily.Default
    val Display = TextStyle(fontFamily = Pixel, fontSize = 32.sp, lineHeight = 31.sp, color = C.ink)
    val Title = TextStyle(fontFamily = Pixel, fontSize = 27.sp, lineHeight = 27.sp, color = C.ink)
    val Heading = TextStyle(fontFamily = Pixel, fontSize = 22.sp, lineHeight = 23.sp, color = C.ink)
    /** A short name of a thing in a card: pixel, a touch smaller than a heading. */
    val Name = TextStyle(fontFamily = Pixel, fontSize = 19.sp, lineHeight = 20.sp, color = C.ink)
    val Body = TextStyle(fontFamily = Plain, fontSize = 15.sp, lineHeight = 21.sp, color = C.ink)
    val Say = TextStyle(fontFamily = Plain, fontSize = 15.sp, lineHeight = 21.sp, color = C.ink)
    val Small = TextStyle(fontFamily = Plain, fontSize = 13.sp, lineHeight = 18.sp, color = C.muted)
    val Button = TextStyle(fontFamily = Pixel, fontSize = 19.sp, lineHeight = 19.sp)
    val Number = TextStyle(fontFamily = Pixel, fontSize = 48.sp, lineHeight = 46.sp, color = C.copper)
}

@Composable
fun RustleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.copper, onPrimary = C.ink, background = C.board, surface = C.paper, onSurface = C.ink,
            onBackground = C.text, surfaceContainer = C.paper, outline = C.ink, error = C.ember,
        ),
        typography = MaterialTheme.typography.copy(bodyLarge = T.Body, bodyMedium = T.Body, bodySmall = T.Small, labelLarge = T.Button, titleLarge = T.Title),
        content = content,
    )
}
