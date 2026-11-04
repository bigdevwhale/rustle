package app.rustle.ui.art

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * Form-making helpers: things with volume rather than flat boxes. Light always comes from the top
 * left; edges facing it get the light tone, the far edges the shade, and a one-pixel glint marks
 * shiny surfaces.
 */

/** An upright cylinder (a pipe, a barrel, a pole): banded light across its width. */
fun Buf.cylV(x: Int, y: Int, w: Int, h: Int, r: Ramp, glint: Boolean = true) {
    for (i in 0 until w) {
        val t = (i + .5f) / w
        val c = when { t < .12f -> r.d; t < .38f -> r.l; t < .72f -> r.b; t < .9f -> r.d; else -> r.dd }
        rect(x + i, y, 1, h, c)
    }
    if (glint && w >= 4) rect(x + (w * .25f).toInt(), y, 1, h, r.hi)
}

/** A lying cylinder (a beam, a log): banded light down its height. */
fun Buf.cylH(x: Int, y: Int, w: Int, h: Int, r: Ramp) {
    for (j in 0 until h) {
        val t = (j + .5f) / h
        rect(x, y + j, w, 1, when { t < .2f -> r.l; t < .55f -> r.b; t < .85f -> r.d; else -> r.dd })
    }
}

/** A solid block with rounded corners: light top and left rims, shaded bottom and right. */
fun Buf.block(x: Int, y: Int, w: Int, h: Int, r: Ramp, round: Int = 1) {
    // Top corners are cut by [round] pixels, bottom corners by one; nothing is drawn there, so what is behind shows.
    fun cut(i: Int, j: Int): Boolean {
        val fromTop = j; val fromSide = minOf(i, w - 1 - i)
        return (fromTop < round && fromSide < round - fromTop) || (j == h - 1 && fromSide == 0 && round > 0)
    }
    for (j in 0 until h) for (i in 0 until w) {
        if (cut(i, j)) continue
        val c = when {
            j == h - 1 -> r.dd
            i == w - 1 -> r.d
            j == 0 || i == 0 || cut(i, j - 1) || cut(i - 1, j) -> r.l
            else -> r.b
        }
        p(x + i, y + j, c)
    }
    p(x + 1 + round / 2, y + 1 + round / 2, r.hi)
}

/** A box seen slightly from above: a lit top face of [top] pixels and a front face. */
fun Buf.crate(x: Int, y: Int, w: Int, h: Int, top: Int, r: Ramp) {
    rect(x + 1, y, w - 1, top, r.l); rect(x, y + top - 1, w, 1, r.hi)
    rect(x, y + top, w, h - top, r.b)
    rect(x, y + top, 1, h - top, mix(r.b, r.l, .5f)); rect(x + w - 1, y + top, 1, h - top, r.d); rect(x, y + h - 1, w, 1, r.dd)
}

/** A rivet: a light dot with a dark one under it. */
fun Buf.rivet(x: Int, y: Int, r: Ramp) { p(x, y, r.hi); p(x + 1, y + 1, r.dd); p(x + 1, y, r.l) }

/** A soft contact shadow on the floor under something. */
fun Buf.shadow(cx: Int, cy: Int, rx: Int, ry: Int, strength: Float = .5f) {
    val sh = rgb(0x140C22)
    for (y in -ry..ry) for (x in -rx..rx) {
        val d = (x * x).toFloat() / (rx * rx) + (y * y).toFloat() / (ry * ry)
        if (d <= 1f) p(cx + x, cy + y, sh, strength * (1f - d * .6f))
    }
}

/** Wood grain: wavy darker strands along a plank. */
fun Buf.grain(x: Int, y: Int, w: Int, h: Int, c: Int, seed: Int) {
    for (j in 0 until h) for (i in 0 until w) {
        val v = sin((i + seed * 7) / 5.0 + j * 1.7 + sin(i / 11.0 + seed) * 2)
        if (v > .82 && (i + j + seed) % 2 == 0) p(x + i, y + j, c)
    }
}

/** A fabric check, for blankets and curtains. */
fun Buf.check(x: Int, y: Int, w: Int, h: Int, a: Int, b: Int, cell: Int = 2) {
    for (j in 0 until h) for (i in 0 until w) {
        val u = (i / cell) % 2; val v = (j / cell) % 2
        p(x + i, y + j, if (u == 1 && v == 1) mix(a, rgb(0x140C22), .25f) else if (u == 1 || v == 1) a else b)
    }
}

/** A thin hanging line that sags between two points. */
fun Buf.sag(x0: Int, y0: Int, x1: Int, y1: Int, droop: Int, c: Int): List<Pair<Int, Int>> {
    val pts = mutableListOf<Pair<Int, Int>>()
    for (x in x0..x1) {
        val t = (x - x0).toDouble() / (x1 - x0)
        val y = (y0 + (y1 - y0) * t + droop * 4 * t * (1 - t)).roundToInt()
        p(x, y, c); pts += x to y
    }
    return pts
}

/** A small tiny spark of light that fades with [a]. */
fun Buf.glint(x: Int, y: Int, a: Float = 1f) { p(x, y, rgb(0xFFFBEA), a); p(x - 1, y, rgb(0xFFF0C8), a * .4f); p(x + 1, y, rgb(0xFFF0C8), a * .4f); p(x, y - 1, rgb(0xFFF0C8), a * .4f); p(x, y + 1, rgb(0xFFF0C8), a * .4f) }

fun near(a: Int, b: Int, by: Int) = abs(a - b) <= by

/** A rectangle laid over what is there at strength [a]. */
fun Buf.rect(x: Int, y: Int, w: Int, h: Int, c: Int, a: Float) { for (j in y until y + h) for (i in x until x + w) p(i, j, c, a) }
