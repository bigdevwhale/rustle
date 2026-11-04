package app.rustle.ui.art

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The furniture that takes turns in the nook's places: on the wall (a map, a mirror), on the sill (a music
 * box, a snow globe), and in the two front corners (an armchair, a gramophone, a pouf; a fern, a radio, an
 * aquarium). The picture, the sprout and the spyglass live in [Nook] itself.
 */
internal object Furniture {
    private val INK = rgb(0x1B140E)
    private val WHITE = rgb(0xFFFBEA)
    private val WOOD = ramp(rgb(0x8A5430))
    private val BRASS = ramp(rgb(0xD8A848))
    private val RED = ramp(rgb(0xC0443A))
    private val CREAM = ramp(rgb(0xEADCB8))
    private val GLASS = ramp(rgb(0x9CC8D0))
    private val LEAF = ramp(rgb(0x5AA050))
    private val STEEL = ramp(rgb(0x9AA2B0))
    private val VELVET = ramp(rgb(0x6A3A8A))
    private val TEAL = ramp(rgb(0x2E8A8A))

    /** The wall place: 30×24 at (60, 50). */
    fun wall(b: Buf, key: String, tick: Int) {
        val x = 60; val y = 50
        when (key) {
            "map" -> {
                b.shadow(x + 15, y + 13, 16, 13, .35f)
                val paper = ramp(rgb(0xE8D8A8))
                b.rect(x, y, 30, 24, paper.b); b.rect(x, y, 30, 1, paper.l); b.rect(x + 29, y, 1, 24, paper.d); b.rect(x, y + 23, 30, 1, paper.dd)
                for (j in 0 until 24) for (i in 0 until 30) if (dith(x + i, y + j, .08)) b.p(x + i, y + j, paper.d)
                // The phone's districts as a little chart: a port, a cellar, a bridge, a gorge, a dome.
                b.line(x + 4, y + 18, x + 12, y + 14, RED.b); b.line(x + 12, y + 14, x + 18, y + 17, RED.b); b.line(x + 18, y + 17, x + 24, y + 9, RED.b); b.line(x + 24, y + 9, x + 21, y + 4, RED.b)
                for ((px, py) in listOf(x + 4 to y + 18, x + 12 to y + 14, x + 18 to y + 17, x + 24 to y + 9, x + 21 to y + 4)) { b.rect(px - 1, py - 1, 3, 3, INK); b.p(px, py, BRASS.hi) }
                b.ring(x + 2, y + 2, 26, 20, paper.d)
                for (k in 0 until 4) b.rect(x + 3 + k * 6, y + 21, 3, 1, paper.dd)
                for ((px, py) in listOf(x + 1 to y + 1, x + 28 to y + 1)) { b.p(px, py, RED.l); b.p(px, py + 1, RED.d) }
            }
            "mirror" -> {
                val cx = x + 15; val cy = y + 12
                b.shadow(cx + 1, cy + 2, 13, 13, .35f)
                b.ell(cx, cy, 12, 12, BRASS.d); b.ell(cx - .5, cy - .5, 11, 11, BRASS.b); b.ell(cx, cy, 9, 9, mix(GLASS.b, rgb(0x203040), .2f))
                b.ell(cx, cy, 8, 8, GLASS.b)
                for (k in 0 until 5) b.p(cx - 5 + k, cy - 2 - k, WHITE, .7f)
                for (k in 0 until 3) b.p(cx - 2 + k, cy + 1 - k, WHITE, .4f)
                for (a in 0 until 360 step 30) { val rad = a * PI / 180; b.p(cx + (cos(rad) * 11).roundToInt(), cy + (sin(rad) * 11).roundToInt(), BRASS.hi) }
                b.line(cx, cy - 12, cx - 4, cy - 17, INK); b.line(cx, cy - 12, cx + 4, cy - 17, INK)
            }
        }
    }

    /** The music box on the right of the sill; the lid goes up and down with the tune. */
    fun musicbox(b: Buf, x: Int, sill: Int, tick: Int) {
        val y = sill - 8
        b.crate(x - 1, y, 13, 8, 3, VELVET)
        b.rect(x + 3, y + 4, 5, 1, BRASS.b); b.p(x + 5, y + 5, BRASS.d)
        val open = Art.still || (tick / 60) % 2 == 0
        if (open) {
            b.rect(x - 1, y - 7, 13, 2, VELVET.l); b.rect(x - 1, y - 5, 1, 5, VELVET.d)
            b.rect(x + 3, y - 3, 1, 3, BRASS.l); b.ell(x + 3, y - 4, 1, 1, rgb(0xF0A0C0))
            if (!Art.still) for (k in 0 until 2) { val f = (tick + k * 25) % 50; b.p(x + 8 + f / 8, y - 3 - f / 4, WHITE, 1f - f / 50f); b.p(x + 9 + f / 8, y - 4 - f / 4, WHITE, 1f - f / 50f) }
        } else b.rect(x - 1, y - 2, 13, 2, VELVET.l)
    }

    /** A snow globe on the left of the sill. */
    fun snowglobe(b: Buf, x: Int, sill: Int, tick: Int) {
        val cx = x + 3; val cy = sill - 10
        b.crate(cx - 6, sill - 4, 13, 4, 2, WOOD)
        b.ell(cx, cy, 6, 6, mix(GLASS.b, rgb(0x6A9AC8), .3f))
        b.rect(cx - 2, cy + 1, 5, 4, CREAM.l); for (i in 0 until 3) b.rect(cx - 2 + i, cy - 1 + i, 5 - 2 * i, 1, RED.b)
        b.rect(cx + 3, cy, 1, 5, LEAF.b); b.p(cx + 3, cy - 1, LEAF.l)
        for (k in 0 until 6) { val f = if (Art.still) k * 9 else (tick / 2 + k * 9) % 12; b.p(cx - 4 + k * 2 - (if (k % 2 == 0) 1 else 0), cy - 5 + f, WHITE) }
        for (k in 0 until 4) b.p(cx - 4 + k, cy - 4 - k / 2, WHITE, .7f)
    }

    /** The front-left corner, close to us: 40×30 resting on y = 212. */
    fun frontLeft(b: Buf, key: String, tick: Int) {
        val x = 4; val base = 212
        when (key) {
            "armchair" -> {
                b.shadow(x + 20, base + 1, 20, 3, .55f)
                // A tufted back with a rounded top, rolled arms, a plump seat cushion and turned legs.
                b.block(x + 6, base - 34, 28, 22, VELVET, 7)
                for (k in 0 until 3) for (j in 0 until 2) { val bx = x + 12 + k * 8; val by = base - 28 + j * 7; b.p(bx, by, VELVET.dd); b.p(bx + 1, by, BRASS.l); b.line(bx - 3, by - 2, bx, by, VELVET.d) }
                for (side in listOf(0, 1)) {
                    val ax = if (side == 0) x + 1 else x + 30
                    b.cylV(ax, base - 20, 9, 14, VELVET); b.ell(ax + 4, base - 20, 4, 3, VELVET.l); b.ell(ax + 4, base - 20, 2, 1, VELVET.b)
                }
                b.crate(x + 9, base - 14, 22, 8, 4, VELVET); b.rect(x + 11, base - 13, 18, 1, VELVET.hi)
                for (lx in listOf(x + 4, x + 33)) { b.rect(lx, base - 6, 3, 6, WOOD.b); b.p(lx, base - 6, WOOD.l); b.rect(lx - 1, base - 1, 5, 1, WOOD.dd) }
                b.ell(x + 26, base - 16, 5, 3, RED.b); b.ell(x + 25, base - 17, 3, 1, RED.l); b.p(x + 26, base - 16, RED.d)
            }
            "gramophone" -> {
                b.shadow(x + 18, base + 1, 16, 3, .55f)
                b.crate(x + 4, base - 12, 26, 12, 4, WOOD)
                b.ell(x + 17, base - 13, 10, 2, rgb(0x1A1A20)); b.ell(x + 17, base - 13, 2, 1, RED.b)
                b.line(x + 26, base - 13, x + 22, base - 20, BRASS.d)
                // The horn: a widening brass trumpet.
                for (i in 0 until 16) {
                    val px = x + 22 - i; val py = base - 20 - i
                    val rr = 1 + i * i / 26
                    b.ell(px, py, rr, rr, if (i % 4 == 0) BRASS.l else BRASS.b)
                }
                b.ell(x + 7, base - 35, 9, 9, BRASS.d); b.ell(x + 7, base - 35, 7, 7, rgb(0x2A1A10)); b.p(x + 2, base - 40, BRASS.hi)
                val spin = if (Art.still) 0 else (tick / 3) % 4
                b.p(x + 11 + listOf(0, 3, 0, -3)[spin], base - 13, WHITE)
            }
            "pouf" -> {
                b.shadow(x + 20, base + 1, 16, 3, .55f)
                b.ell(x + 20, base - 8, 15, 9, RED.b); b.ell(x + 18, base - 11, 11, 5, RED.l); b.ell(x + 22, base - 4, 13, 4, RED.d)
                for (k in 0 until 6) { val a = k * PI / 3; b.line(x + 20, base - 9, x + 20 + (cos(a) * 12).roundToInt(), base - 9 + (sin(a) * 7).roundToInt(), RED.d) }
                for ((dx, dy, c) in listOf(Triple(14, -16, STEEL.l), Triple(20, -18, BRASS.l), Triple(26, -15, rgb(0xF0F0F0)))) { b.line(x + dx, base + dy, x + dx + 1, base + dy + 6, STEEL.b); b.ell(x + dx, base + dy, 1, 1, c) }
                b.rect(x + 8, base - 1, 24, 1, RED.dd)
            }
        }
    }

    /** The front-right corner, close to us: 40×30 resting on y = 212. */
    fun frontRight(b: Buf, key: String, tick: Int) {
        val x = 150; val base = 212
        when (key) {
            "fern" -> {
                b.shadow(x + 20, base + 1, 14, 3, .55f)
                b.cylV(x + 12, base - 12, 16, 12, STEEL, true); b.cylH(x + 10, base - 14, 20, 3, STEEL)
                val sway = if (Art.still) 0.0 else sin(tick / 35.0) * 1.5
                for (f in 0 until 7) {
                    val a = -PI / 2 + (f - 3) * .38
                    val len = 20 + (f % 3) * 4
                    for (i in 0 until len) {
                        val k = i / len.toDouble()
                        val px = x + 20 + cos(a) * i + sway * k * k * 3
                        val py = base - 14 + sin(a) * i + k * k * 9
                        b.p(px.roundToInt(), py.roundToInt(), LEAF.d)
                        if (i % 2 == 0 && i > 2) { b.p((px - sin(a) * 2).roundToInt(), (py + cos(a) * 2).roundToInt(), LEAF.b); b.p((px + sin(a) * 2).roundToInt(), (py - cos(a) * 2).roundToInt(), LEAF.l) }
                    }
                }
            }
            "radio" -> {
                b.shadow(x + 20, base + 1, 18, 3, .55f)
                b.crate(x + 3, base - 22, 34, 22, 4, WOOD)
                b.ell(x + 13, base - 9, 6, 6, CREAM.d); for (j in -5..5 step 2) b.rect(x + 8, base - 9 + j, 11, 1, WOOD.dd)
                b.rect(x + 23, base - 15, 11, 5, rgb(0xF0D890)); b.rect(x + 23, base - 15, 11, 1, rgb(0xFFF4C0))
                val needle = if (Art.still) 4 else ((sin(tick / 40.0) + 1) * 4.5).roundToInt()
                b.rect(x + 24 + needle, base - 15, 1, 5, RED.b)
                for (k in 0 until 2) { b.ell(x + 26 + k * 6, base - 6, 2, 2, BRASS.b); b.p(x + 25 + k * 6, base - 7, BRASS.hi) }
                b.line(x + 32, base - 22, x + 38, base - 34, STEEL.b); b.p(x + 38, base - 34, RED.l)
            }
            "aquarium" -> {
                b.shadow(x + 20, base + 1, 16, 3, .55f)
                b.ell(x + 20, base - 13, 15, 13, mix(GLASS.b, rgb(0x2A6A9A), .35f))
                for (y in base - 26..base - 1) for (xx in x + 5..x + 35) {
                    val dx = xx - (x + 20.0); val dy = y - (base - 13.0)
                    if (dx * dx / 225 + dy * dy / 169 <= 1 && y > base - 20) b.p(xx, y, mix(rgb(0x3A8AC8), rgb(0x1A4A7A), (y - (base - 20)) / 20f))
                }
                b.ell(x + 20, base - 2, 12, 2, rgb(0xC8A060))
                for (k in 0 until 3) { b.rect(x + 12 + k * 7, base - 9 - k * 2, 1, 6 + k * 2, LEAF.b); b.p(x + 13 + k * 7, base - 7 - k * 2, LEAF.l) }
                val fx = if (Art.still) x + 20 else x + 20 + (sin(tick / 25.0) * 8).roundToInt()
                val dir = if (Art.still || cos(tick / 25.0) > 0) 1 else -1
                b.ell(fx, base - 13, 3, 2, rgb(0xF07A40)); b.p(fx - 3 * dir, base - 13, rgb(0xC85A30)); b.p(fx - 4 * dir, base - 14, rgb(0xC85A30)); b.p(fx - 4 * dir, base - 12, rgb(0xC85A30)); b.p(fx + dir, base - 14, INK)
                if (!Art.still) { val f = tick % 40; b.p(fx + 2 * dir, base - 16 - f / 5, WHITE, 1f - f / 40f) }
                for (a in 205..250 step 4) { val rad = a * PI / 180; b.p(x + 20 + (cos(rad) * 12).roundToInt(), base - 13 + (sin(rad) * 10).roundToInt(), WHITE, .6f) }
                b.ell(x + 20, base - 26, 8, 1, BRASS.b)
            }
        }
    }
}
