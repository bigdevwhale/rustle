package app.rustle.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import app.rustle.container
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.rustle.domain.Catalog
import app.rustle.domain.Find
import app.rustle.domain.Pose
import app.rustle.domain.Shape
import app.rustle.ui.art.Art
import app.rustle.ui.art.Critter
import app.rustle.ui.art.Items
import app.rustle.ui.art.Look
import app.rustle.ui.art.PixelCanvas
import app.rustle.ui.theme.C
import app.rustle.ui.theme.T

@Composable
fun FindIcon(f: Find, modifier: Modifier = Modifier, seen: Boolean = true, scale: Dp = 2.dp) =
    BufImage(Items.of(f, seen), modifier, scale)

/** A shape as the workshop shows it: the plainest find of that shape. */
@Composable
fun ShapeIcon(s: Shape, modifier: Modifier = Modifier, scale: Dp = 1.dp) {
    val f = remember(s) { Catalog.finds.filter { it.shape == s }.minBy { it.rare } }
    BufImage(Items.of(f), modifier, scale)
}

/** A cost line: shape icons with have/need. */
@Composable
fun Cost(cost: Map<Shape, Int>, have: (Shape) -> Int, ru: Boolean) {
    Flow {
        for ((s, n) in cost) {
            val ok = have(s) >= n
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(end = 6.dp)) {
                ShapeIcon(s)
                Text("${s.label.of(ru)} ${minOf(have(s), n)}/$n", style = T.Small.copy(color = if (ok) C.ok else C.ember))
            }
        }
    }
}

/** Him, standing still or moving, at [scale] dp per pixel. */
@Composable
fun Portrait(look: Look, pose: Pose = Pose.IDLE, modifier: Modifier = Modifier, scale: Dp = 4.dp) {
    Box(modifier.width(scale * Critter.W)) {
        PixelCanvas(Critter.W, Critter.H, Modifier.width(scale * Critter.W)) { b, t ->
            b.clear(); b.blit(Critter.draw(look, pose, if (Art.still) 40 else t), 0, 0)
        }
    }
}

/**
 * His explanation of a screen, shown on the first visit until "Got it": a portrait, a few lines, a button.
 * [key] names the screen in the household's read tips.
 */
@Composable
fun Tip(key: String, title: String, text: String, look: Look, read: Set<String>, modifier: Modifier = Modifier) {
    if (key in read) return
    val c = androidx.compose.ui.platform.LocalContext.current.container
    Row(modifier.fillMaxWidth().sayBox(C.note).padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Portrait(look, Pose.IDLE, scale = 1.25.dp)
        androidx.compose.foundation.layout.Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = T.Name.copy(color = C.emberD))
            Text(text, style = T.Say)
            PixelButton(androidx.compose.ui.res.stringResource(app.rustle.R.string.tip_ok), { c.write { store.update { it.copy(tips = it.tips + key) } } }, small = true)
        }
    }
}
