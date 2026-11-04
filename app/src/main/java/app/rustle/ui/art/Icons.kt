package app.rustle.ui.art

import app.rustle.domain.Shape
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Pictograms for the gauges and the action tiles, 24×24, drawn with the same light as the finds. */
object Icons {
    const val S = 24
    private val INK = rgb(0x1B140E)
    private val WHITE = rgb(0xFFFBEA)
    private val cache = HashMap<String, Buf>()

    private fun icon(key: String, draw: Buf.() -> Unit): Buf = cache.getOrPut(key) { Buf(S, S).also { it.draw(); it.outline(INK) } }

    private fun Buf.dome(cx: Double, cy: Double, rx: Double, ry: Double, c: Int, bias: Double = 0.0) {
        val t = Critter.tones(c)
        for (y in 0 until h) for (x in 0 until w) {
            val dx = x + .5 - cx; val dy = y + .5 - cy
            if (dx * dx / (rx * rx) + dy * dy / (ry * ry) > 1) continue
            p(x, y, t.at(Critter.shadeAt(dx, dy, rx, ry) + bias, x, y))
        }
    }

    /** Lights the rim of an empty gauge pip so it reads as a hollow. */
    private fun Buf.hollow() {
        for (y in 0 until S) for (x in 0 until S) if (filled(x, y) && (!filled(x - 1, y) || !filled(x, y - 1))) p(x, y, rgb(0x46564F))
    }

    /** A glossy heart; an empty one is a dark hollow. */
    fun heart(full: Boolean) = icon("heart$full") {
        val c = if (full) rgb(0xE8485A) else rgb(0x2E3A36)
        val t = Critter.tones(c)
        for (y in 0 until S) for (x in 0 until S) {
            val fx = (x + .5 - 12) / 10.0; val fy = -(y + .5 - 11) / 10.0
            val a = fx * fx + fy * fy - .55
            if (a * a * a - fx * fx * fy * fy * fy > 0) continue
            p(x, y, if (full) t.at(Critter.shadeAt(x + .5 - 12, y + .5 - 11, 11.0, 11.0), x, y) else c)
        }
        if (full) { rect(6, 6, 3, 2, WHITE); p(5, 8, WHITE, .7f); p(9, 6, WHITE, .6f); p(16, 15, mix(c, WHITE, .4f)) } else hollow()
    }

    /** A rusk for the hunger gauge; an empty one is a dark crumb outline. */
    fun crumb(full: Boolean): Buf = cache.getOrPut("crumb$full") {
        val b = Items.shape(Shape.CRUMB, rgb(0xE0A860))
        if (!full) { b.silhouette(rgb(0x2E3A36)); b.hollow() }
        b.outline(INK)
        b
    }

    /** A steaming bowl of porridge with a wooden spoon. */
    val bowl = icon("bowl") {
        val red = rgb(0xC8403A)
        for (y in 12 until 21) for (x in 2 until 22) {
            val dx = (x + .5 - 12) / 10.0; val dy = (y + .5 - 12) / 8.5
            if (dx * dx + dy * dy > 1) continue
            p(x, y, Critter.tones(red).at(Critter.shadeAt(x + .5 - 12, y + .5 - 14, 11.0, 8.0), x, y))
        }
        for (x in 5 until 19 step 4) { p(x, 16, WHITE); p(x + 1, 17, WHITE, .7f) }
        rect(7, 20, 10, 2, ramp(red).d); rect(8, 22, 8, 1, ramp(red).dd)
        dome(12.0, 12.0, 9.5, 2.5, rgb(0xE8C890))
        for ((x, y) in listOf(8 to 11, 12 to 12, 15 to 11, 10 to 13)) p(x, y, rgb(0xB88A50))
        p(13, 11, rgb(0x6A3050)); p(9, 12, rgb(0x6A3050))
        line(15, 12, 20, 3, rgb(0xA0703A)); line(16, 12, 21, 3, rgb(0xC8904A)); dome(20.5, 3.0, 1.6, 1.3, rgb(0xC8904A))
        // Two soft curls of steam.
        for (k in 0 until 2) for (j in 0 until 7) p(7 + k * 5 + (sin(j / 1.6 + k) * 1.5).roundToInt(), 9 - j, WHITE, .6f - j * .07f)
    }

    /** A bundle on a stick: a polka-dot cloth knotted at the top. */
    val bundle = icon("bundle") {
        line(2, 22, 20, 2, rgb(0x6A4020)); line(3, 22, 21, 2, rgb(0x9A6A38)); p(21, 1, rgb(0x9A6A38))
        dome(9.0, 14.0, 7.0, 6.5, rgb(0xC0403A))
        for ((x, y) in listOf(6 to 11, 10 to 10, 12 to 14, 7 to 16, 10 to 18, 4 to 14)) { p(x, y, rgb(0xF4ECD8)); p(x + 1, y, rgb(0xE0D4BC)) }
        dome(13.0, 8.0, 3.0, 2.5, rgb(0xC0403A)); line(14, 6, 17, 4, rgb(0xA0302A)); line(14, 9, 18, 9, rgb(0xA0302A)); p(18, 9, rgb(0xC0403A))
        rect(4, 9, 3, 2, WHITE); p(3, 11, WHITE, .6f)
    }

    /** A little maze on a board, him at the start and a lump of sugar at the end. */
    val maze = icon("maze") {
        val board = ramp(rgb(0x1F5A44)); val cu = ramp(rgb(0xE0954A))
        rect(1, 1, 22, 22, board.b); rect(1, 1, 22, 1, board.l); rect(1, 22, 22, 1, board.dd)
        val walls = listOf("##########", "#........#", "#.####.#.#", "#.#....#.#", "#.#.####.#", "#...#....#", "###.#.##.#", "#.....#..#", "#.###...##", "##########")
        for ((j, row) in walls.withIndex()) for ((i, ch) in row.withIndex()) if (ch == '#') {
            val x = 2 + i * 2; val y = 2 + j * 2
            rect(x, y, 2, 2, cu.b); p(x, y, cu.l); p(x + 1, y + 1, cu.d)
        }
        dome(5.0, 5.0, 1.6, 1.6, rgb(0xE8B890)); p(4, 4, WHITE)
        rect(16, 16, 3, 3, rgb(0xF8F4EC)); p(16, 16, WHITE); p(18, 18, rgb(0xC8C8C0))
    }

    /** A crescent moon with craters and two little stars. */
    val moon = icon("moon") {
        dome(11.0, 12.0, 9.0, 9.0, rgb(0xF0E2A0), .1)
        for (y in 0 until S) for (x in 0 until S) { val dx = x + .5 - 16; val dy = y + .5 - 8.5; if (dx * dx + dy * dy <= 52) clearAt(x, y) }
        for ((x, y, rr) in listOf(Triple(6, 13, 1), Triple(9, 18, 1), Triple(5, 9, 0))) { ell(x, y, rr, rr, rgb(0xC8B878)); p(x - 1, y - 1, rgb(0xFFF4C8)) }
        for ((x, y) in listOf(19 to 16, 21 to 4)) { p(x, y, WHITE); p(x - 1, y, rgb(0xF0E2A0)); p(x + 1, y, rgb(0xF0E2A0)); p(x, y - 1, rgb(0xF0E2A0)); p(x, y + 1, rgb(0xF0E2A0)) }
    }

    /** A sun with long and short rays. */
    val sun = icon("sun") {
        for (k in 0 until 12) {
            val a = k * PI / 6
            val len = if (k % 2 == 0) 11 else 9
            for (i in 8..len) p((12 + cos(a) * i).roundToInt(), (12 + sin(a) * i).roundToInt(), if (k % 2 == 0) rgb(0xFFB030) else rgb(0xFFD060))
        }
        dome(12.0, 12.0, 6.5, 6.5, rgb(0xFFC840), .1)
        rect(8, 8, 2, 2, WHITE); p(10, 8, WHITE, .6f)
    }

    /** A small leather backpack with a flap, a buckle and a rolled blanket on top: outings. */
    val pack = icon("pack") {
        val leather = rgb(0xA8642F); val strap = ramp(rgb(0x6A3A1C)); val brass = ramp(rgb(0xE0B048))
        // The rolled blanket.
        for (x in 6 until 18) { p(x, 3, rgb(0xE8DCC0)); p(x, 4, if (x % 3 == 0) rgb(0xC0403A) else rgb(0xD8CCB0)); p(x, 5, rgb(0xB8AC90)) }
        // The bag, lit like a soft box.
        dome(12.0, 13.5, 8.5, 8.5, leather)
        rect(4, 13, 16, 9, Critter.tones(leather).at(.5, 0, 0)); rect(4, 13, 2, 9, ramp(leather).l); rect(18, 13, 2, 9, ramp(leather).d); rect(4, 21, 16, 1, ramp(leather).dd)
        // The flap with a buckle.
        for (y in 7 until 14) { val inset = if (y > 11) y - 11 else 0; rect(5 + inset, y, 14 - 2 * inset, 1, if (y < 9) ramp(leather).l else ramp(leather).b) }
        rect(11, 12, 3, 3, brass.b); p(11, 12, brass.hi); p(12, 13, strap.dd)
        rect(7, 15, 1, 6, strap.b); rect(16, 15, 1, 6, strap.b)
        // A side pocket.
        rect(8, 17, 8, 3, ramp(leather).d); rect(8, 17, 8, 1, ramp(leather).b)
    }

    /** A wooden chest with brass bands and a lock: the pantry. */
    val chest = icon("chest") {
        val wood = ramp(rgb(0x9A5A30)); val brass = ramp(rgb(0xE0B048))
        // A rounded lid, then the box.
        for (y in 4 until 10) { val inset = if (y == 4) 3 else if (y == 5) 2 else 1; rect(2 + inset, y, 20 - 2 * inset, 1, if (y < 6) wood.l else wood.b) }
        rect(2, 10, 20, 11, wood.b); rect(2, 10, 20, 1, wood.dd); rect(2, 20, 20, 1, wood.dd); rect(21, 10, 1, 11, wood.d)
        for (y in 12 until 20 step 3) rect(3, y, 18, 1, wood.d)
        for (x in listOf(5, 17)) { rect(x, 4, 2, 17, brass.b); p(x, 5, brass.hi) }
        rect(10, 8, 4, 5, brass.b); rect(11, 10, 2, 2, rgb(0x2A1A10)); p(10, 8, brass.hi)
    }

    /** A hammer crossed with a needle and thread: the workshop. */
    val tools = icon("tools") {
        val wood = ramp(rgb(0xB07A40)); val steel = ramp(rgb(0xA8B0BC))
        for (i in 0 until 14) { p(5 + i, 19 - i, wood.l); p(6 + i, 19 - i, wood.b); p(6 + i, 20 - i, wood.d) }
        // The hammer head across the top of the handle.
        for (t in -5..5) for (k in -1..1) p(17 + t + k, 6 + t - k, if (t < -2) steel.l else if (t < 3) steel.b else steel.d)
        // A needle the other way, with a red thread.
        for (i in 0 until 15) p(4 + i, 4 + i, if (i < 2) steel.d else steel.l)
        p(5, 5, rgb(0x2A1A10))
        for (i in 0 until 7) p(3 + (sin(i / 1.4) * 2).roundToInt(), 6 + i * 2, rgb(0xC8403A))
    }

    /** A little house: a red roof, cream walls, a lit window, a door. */
    val home = icon("home") {
        for (i in 0 until 9) for (x in 12 - i - 2..12 + i + 1) p(x, 2 + i, if (x < 12 - i) rgb(0xE06048) else if (x > 12 + i - 1) rgb(0x8A2A20) else rgb(0xC0403A))
        rect(5, 11, 14, 10, rgb(0xEADCB8)); rect(5, 11, 14, 1, rgb(0xC8B890)); rect(18, 11, 1, 10, rgb(0xB8A880))
        rect(7, 13, 4, 4, rgb(0xFFD070)); rect(9, 13, 1, 4, rgb(0x8A5A30)); rect(7, 15, 4, 1, rgb(0x8A5A30))
        rect(13, 14, 4, 7, rgb(0x8A5A30)); p(16, 17, rgb(0xE8B850)); rect(4, 21, 16, 1, rgb(0x5A3A20))
        rect(15, 3, 2, 4, rgb(0x8A5A30))
    }
}
