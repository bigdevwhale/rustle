package app.rustle.ui.art

import app.rustle.domain.Find
import app.rustle.domain.Shape
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A 24×24 picture of each find, drawn from its shape and colour with real volume: domes and cylinders
 * lit per pixel from the top left in five dithered tones, glints on metal and glass, a soft shadow under
 * each thing. Unseen finds are a dark silhouette.
 */
object Items {
    const val S = 24
    private val INK = rgb(0x1A1018)
    private val WHITE = rgb(0xFFFBEA)
    private val cache = HashMap<String, Buf>()

    fun of(f: Find, seen: Boolean = true): Buf = cache.getOrPut(f.key + seen) {
        val b = shape(f.shape, f.color)
        if (!seen) b.silhouette(rgb(0x0E241C))
        b.outline(if (seen) mix(f.color, INK, .82f) else rgb(0x285A46))
        if (seen) b.floorShadow()
        b
    }

    private fun tones(c: Int) = Critter.tones(c)

    /** Fills an ellipse lit as a dome (a sphere squashed to rx, ry). */
    private fun Buf.dome(cx: Double, cy: Double, rx: Double, ry: Double, c: Int, bias: Double = 0.0) {
        val t = tones(c)
        for (y in 0 until h) for (x in 0 until w) {
            val dx = x + .5 - cx; val dy = y + .5 - cy
            if (dx * dx / (rx * rx) + dy * dy / (ry * ry) > 1) continue
            p(x, y, t.at(Critter.shadeAt(dx, dy, rx, ry) + bias, x, y))
        }
    }

    /** A vertical cylinder lit across its width, with a glint line. */
    private fun Buf.tube(x0: Int, y0: Int, w: Int, h: Int, c: Int, glint: Boolean = true) {
        val t = tones(c)
        for (i in 0 until w) {
            val u = (i + .5) / w * 2 - 1
            val v = Critter.shadeAt(u * 10, 0.0, 10.0, 10.0)
            for (j in 0 until h) p(x0 + i, y0 + j, t.at(v, x0 + i, y0 + j))
        }
        if (glint && w >= 4) rect(x0 + (w * .28).toInt(), y0 + 1, 1, h - 2, ramp(c).hi)
    }

    /** A flat ellipse ring (a rim seen at an angle): dark on the far side, light on the near one. */
    private fun Buf.rim(cx: Double, cy: Double, rx: Double, ry: Double, far: Int, near: Int) {
        for (a in 0 until 360 step 3) {
            val rad = a * PI / 180
            p((cx + cos(rad) * rx).roundToInt(), (cy + sin(rad) * ry).roundToInt(), if (sin(rad) < 0) far else near)
        }
    }

    /** Kept for the shapes' own calls; the shadow itself is added once, after the outline. */
    @Suppress("UNUSED_PARAMETER") private fun Buf.shadowUnder(cx: Int, cy: Int, rx: Int) = Unit

    /** A soft oval under the lowest row of the thing. */
    private fun Buf.floorShadow() {
        val bottom = (h - 1 downTo 0).firstOrNull { y -> (0 until w).any { filled(it, y) } } ?: return
        val xs = (0 until w).filter { filled(it, bottom) }
        if (bottom + 1 >= h || xs.isEmpty()) return
        val cx = (xs.first() + xs.last()) / 2.0; val rx = (xs.last() - xs.first()) / 2.0 + 2
        for (x in 0 until w) { val d = abs(x + .5 - cx) / rx; if (d < 1 && !filled(x, bottom + 1)) p(x, bottom + 1, rgb(0x140C22), .28f * (1 - d * d).toFloat()) }
    }

    private fun Buf.sparkle(x: Int, y: Int) { p(x, y, WHITE); p(x - 1, y, WHITE, .5f); p(x + 1, y, WHITE, .5f); p(x, y - 1, WHITE, .5f); p(x, y + 1, WHITE, .5f) }

    fun shape(s: Shape, c: Int): Buf {
        val b = Buf(S, S)
        val r = ramp(c)
        when (s) {
            Shape.BUTTON -> {
                b.shadowUnder(12, 21, 8)
                b.dome(12.0, 11.5, 9.5, 9.0, c)
                // A sunken centre inside a raised rim.
                for (y in 0 until S) for (x in 0 until S) {
                    val dx = x + .5 - 12; val dy = y + .5 - 11.5
                    val d = sqrt(dx * dx + dy * dy)
                    if (d < 6.2) b.p(x, y, tones(c).at(.67 - Critter.shadeAt(dx, dy, 7.0, 7.0) * .35, x, y))
                    else if (d < 7.0) b.p(x, y, if (dx + dy < 0) r.d else r.l)
                }
                for ((hx, hy) in listOf(9 to 9, 14 to 9, 9 to 14, 14 to 14)) { b.rect(hx, hy, 2, 2, r.dd); b.p(hx, hy, INK) }
                val thread = rgb(0xF0E8D8)
                b.line(10, 10, 15, 15, thread); b.line(15, 10, 10, 15, thread); b.p(12, 12, mix(thread, INK, .2f))
                b.p(6, 6, WHITE); b.p(7, 5, WHITE, .7f); b.p(5, 8, WHITE, .5f)
            }
            Shape.SCREW -> {
                b.shadowUnder(12, 22, 5)
                b.tube(9, 9, 7, 12, c)
                for (y in 10..19 step 2) { b.line(9, y + 1, 15, y, r.l); b.p(15, y + 1, r.dd); b.p(9, y + 2, r.d) }
                for (i in 0 until 3) b.rect(11 + i / 2, 21 + i, 3 - i, 1, r.d)
                b.dome(12.0, 7.0, 7.5, 4.5, c)
                b.rect(5, 8, 15, 2, r.d); b.rect(5, 10, 15, 1, r.dd)
                b.rect(9, 6, 7, 1, r.dd); b.rect(12, 4, 1, 5, r.dd); b.p(9, 5, r.hi); b.p(8, 6, WHITE)
            }
            Shape.BEAD -> {
                b.shadowUnder(12, 21, 7)
                b.dome(12.0, 12.0, 8.5, 8.5, c, .05)
                // Light passing through: a bright crescent opposite the glint.
                for (a in 20..110 step 5) { val rad = a * PI / 180; b.p((12 + cos(rad) * 6).roundToInt(), (12 + sin(rad) * 6).roundToInt(), mix(r.l, WHITE, .3f)) }
                b.ell(12, 5, 2, 1, r.dd); b.p(12, 5, INK); b.p(11, 5, INK)
                b.rect(7, 7, 3, 2, WHITE); b.p(10, 7, WHITE, .6f); b.p(7, 9, WHITE, .6f); b.p(16, 16, WHITE, .5f)
            }
            Shape.SPRING -> {
                b.shadowUnder(12, 22, 7)
                // Each turn is a tilted ellipse; the back half darker, the front half lit.
                for (pass in 0..1) for (k in 0 until 5) {
                    val cy = 5.0 + k * 3.4
                    for (a in 0 until 360 step 3) {
                        val rad = a * PI / 180
                        val front = sin(rad) > 0
                        if ((pass == 0) == front) continue // back halves first, then the front ones over them
                        val x = (12 + cos(rad) * 7.0).roundToInt(); val y = (cy + sin(rad) * 2.2 + (a / 360.0) * 3.4).roundToInt()
                        val col = if (!front) r.dd else if (cos(rad) < -.35) r.hi else if (cos(rad) < .35) r.l else r.b
                        b.p(x, y, col); b.p(x, y + 1, if (front) r.d else mix(r.dd, INK, .3f))
                    }
                }
                b.line(5, 5, 2, 2, r.l); b.line(19, 22, 22, 22, r.b)
            }
            Shape.COIN -> {
                b.shadowUnder(12, 22, 8)
                // The edge first, then the face.
                b.dome(12.0, 13.0, 9.5, 8.5, mix(c, INK, .3f))
                for (i in 3..21 step 2) b.p(i, 13 + (sqrt(max(0.0, 1 - ((i - 12) / 9.5) * ((i - 12) / 9.5))) * 8.5).toInt(), r.dd)
                b.dome(12.0, 11.5, 9.5, 8.5, c)
                b.rim(12.0, 11.5, 7.5, 6.6, r.d, r.l)
                // An embossed star.
                for (y in 0 until S) for (x in 0 until S) {
                    val dx = x + .5 - 12; val dy = y + .5 - 11.5
                    val a = atan2(dy, dx); val rr = 2.0 + 2.6 * ((cos(a * 5 + PI / 2) + 1) / 2)
                    if (dx * dx + dy * dy <= rr * rr) b.p(x, y, if (dx + dy < -1) r.hi else if (dx + dy > 1.5) r.d else r.l)
                }
                b.p(6, 6, WHITE); b.p(7, 5, WHITE, .6f)
            }
            Shape.KEY -> {
                b.shadowUnder(12, 21, 9)
                // An ornate bow: three lobes round a ring, then the shaft and the bit.
                for ((lx, ly) in listOf(6.0 to 7.0, 3.5 to 11.0, 6.0 to 15.0)) b.dome(lx, ly, 3.2, 3.2, c)
                b.dome(7.5, 11.0, 4.5, 4.5, c)
                b.clearAt(6, 10, 3, 3); b.p(5, 11, r.dd); b.p(7, 13, r.dd); b.p(9, 10, r.dd)
                b.tube(11, 10, 11, 3, c, false)
                for (i in 0 until 11) { b.p(11 + i, 10, r.l); b.p(11 + i, 12, r.d) }
                b.rect(18, 13, 2, 4, r.b); b.rect(20, 13, 2, 3, r.b); b.p(18, 16, r.d); b.p(20, 15, r.d); b.rect(15, 13, 2, 2, r.b)
                b.p(4, 6, WHITE); b.p(13, 10, WHITE); b.p(14, 10, r.hi)
            }
            Shape.THREAD -> {
                val wood = rgb(0xC89A60)
                b.shadowUnder(12, 22, 8)
                b.tube(6, 5, 12, 14, c, false)
                for (y in 5 until 19) for (x in 6 until 18) if ((x + y * 2) % 4 == 0) b.p(x, y, mix(b.get(x, y), INK, .18f))
                for (y in 6 until 19 step 3) b.line(6, y, 17, y + 1, r.l)
                b.dome(12.0, 4.0, 7.5, 2.5, wood); b.rim(12.0, 4.0, 7.0, 2.0, ramp(wood).d, ramp(wood).l); b.ell(12, 4, 2, 1, ramp(wood).dd)
                b.dome(12.0, 19.5, 7.5, 2.5, wood); b.rect(5, 19, 15, 2, ramp(wood).d)
                // A loose end curling away.
                b.line(17, 12, 20, 14, r.b); b.line(20, 14, 21, 17, r.b); b.p(20, 18, r.l)
            }
            Shape.PIN -> {
                b.shadowUnder(13, 22, 9)
                for (i in 0 until 14) { b.p(8 + i, 8 + i, rgb(0xD8DEE8)); b.p(9 + i, 8 + i, rgb(0x7A808A)) }
                b.p(21, 21, rgb(0x5A6068)); b.p(22, 22, rgb(0x4A5058))
                b.dome(6.5, 6.5, 5.0, 5.0, c)
                b.rect(4, 4, 2, 2, WHITE); b.p(6, 3, WHITE, .6f)
            }
            Shape.LED -> {
                b.shadowUnder(12, 23, 7)
                // Legs: one long, one short and bent.
                b.rect(9, 17, 1, 7, rgb(0xC8D0D8)); b.rect(15, 17, 1, 4, rgb(0xC8D0D8)); b.line(15, 21, 17, 23, rgb(0xC8D0D8))
                b.tube(5, 14, 14, 3, mix(c, rgb(0x8A8A94), .45f))
                // The epoxy body: a dome on a cylinder, see-through, with the die inside.
                for (y in 2 until 15) for (x in 6 until 18) {
                    val dx = x + .5 - 12; val dy = y + .5 - 8
                    if (y < 8 && dx * dx + dy * dy > 36) continue
                    b.p(x, y, tones(c).at(Critter.shadeAt(dx, if (y < 8) dy else 0.0, 6.5, 6.5) * .8 + .15, x, y))
                }
                b.rect(10, 9, 2, 5, mix(c, INK, .5f)); b.rect(13, 10, 2, 4, mix(c, INK, .5f)); b.line(11, 9, 14, 10, mix(c, WHITE, .6f))
                b.rect(8, 3, 1, 9, WHITE); b.p(9, 3, mix(WHITE, c, .4f)); b.p(16, 12, mix(WHITE, c, .5f))
            }
            Shape.FEATHER -> {
                b.shadowUnder(12, 22, 9)
                for (y in 0 until S) for (x in 0 until S) {
                    val u = ((x + .5 - 12) - (y + .5 - 12)) / 1.414
                    val v = ((x + .5 - 12) + (y + .5 - 12)) / 1.414
                    val half = 4.4 * (1 - ((u - 1.5) / 11.0) * ((u - 1.5) / 11.0)).coerceAtLeast(0.0)
                    if (u < -8.5 || abs(v) > half) continue
                    // Splits in the vane, barbs, a downy end near the quill.
                    if (abs(v) > 1.6 && ((u * 1.3 + (if (v > 0) 1.5 else 0.0)) % 5.0 + 5.0) % 5.0 < .8) continue
                    val barb = ((u * 2 + abs(v)) % 2.0 + 2.0) % 2.0 < 1
                    val base = if (v < -1) r.l else if (v > 2) r.d else r.b
                    b.p(x, y, if (u < -5 && (x + y) % 2 == 0) mix(base, WHITE, .4f) else if (barb) base else mix(base, r.dd, .25f))
                }
                b.line(3, 21, 19, 5, mix(c, INK, .45f)); b.line(3, 21, 6, 18, rgb(0xF0E8D8)); b.p(10, 13, r.hi); b.p(14, 9, r.hi)
            }
            Shape.SCRAP -> {
                b.shadowUnder(12, 21, 9)
                val t = tones(c)
                for (y in 3 until 20) for (x in 3 until 21) {
                    val torn = (y == 19 && (x * 7) % 5 < 2) || (x == 20 && (y * 3) % 4 == 0)
                    if (torn || (x > 16 && y < 7 && x - 16 > 7 - y)) continue
                    b.p(x, y, t.at(.72 - (x - 3) / 60.0 - (y - 3) / 80.0, x, y))
                }
                // A folded corner, handwriting, a wax mark.
                for (i in 0 until 4) b.rect(17 + i, 3 + i, 4 - i, 1, r.d)
                b.line(17, 3, 20, 6, r.dd)
                for (y in 7..15 step 3) b.rect(5, y, if (y == 13) 7 else 11, 1, mix(c, rgb(0x2A3A7A), .55f))
                for (x in 6..14 step 4) b.p(x, 8, mix(c, rgb(0x2A3A7A), .3f))
                b.ell(15, 15, 2, 2, mix(c, rgb(0xC85A3A), .55f)); b.p(15, 15, mix(c, rgb(0xC85A3A), .8f))
            }
            Shape.GEM -> {
                b.shadowUnder(12, 22, 7)
                val pts = listOf(12 to 2, 20 to 8, 18 to 17, 12 to 22, 6 to 17, 4 to 8)
                for (y in 0 until S) for (x in 0 until S) {
                    var inside = true
                    for (k in pts.indices) { val (x1, y1) = pts[k]; val (x2, y2) = pts[(k + 1) % pts.size]; if ((x2 - x1) * (y - y1) - (y2 - y1) * (x - x1) < 0) { inside = false; break } }
                    if (!inside) continue
                    // Facets: a table on top, crown facets round it, pavilion facets below.
                    val col = when {
                        y < 8 && abs(x - 12) < 4 -> r.hi
                        y < 8 -> if (x < 12) r.l else r.b
                        y < 14 -> if (x < 9) r.l else if (x > 15) r.d else r.b
                        else -> if (x < 12) r.d else r.dd
                    }
                    b.p(x, y, col)
                }
                b.line(4, 8, 20, 8, r.hi); b.line(8, 8, 12, 22, r.d); b.line(16, 8, 12, 22, r.d); b.line(12, 2, 8, 8, r.l)
                b.sparkle(8, 5); b.p(16, 12, WHITE, .7f)
            }
            Shape.RING -> {
                b.shadowUnder(12, 22, 8)
                // The band seen at an angle: outer and inner ellipses, the back of it darker.
                for (y in 0 until S) for (x in 0 until S) {
                    val dx = (x + .5 - 12) / 8.5; val dy = (y + .5 - 15) / 6.0
                    val ix = (x + .5 - 12) / 6.0; val iy = (y + .5 - 14.2) / 3.8
                    if (dx * dx + dy * dy <= 1 && ix * ix + iy * iy > 1) b.p(x, y, if (dy < -.3) r.d else if (dx < -.3) r.hi else if (dx < .3) r.l else r.b)
                }
                // A stone in prongs on top.
                b.rect(10, 7, 5, 3, r.b); b.p(10, 6, r.l); b.p(14, 6, r.d)
                b.dome(12.0, 5.0, 3.5, 3.5, rgb(0xA8E0F8)); b.sparkle(11, 4)
            }
            Shape.MAGNET -> {
                b.shadowUnder(12, 22, 9)
                for (y in 0 until S) for (x in 0 until S) {
                    val dx = x + .5 - 12; val dy = y + .5 - 10
                    val d = sqrt(dx * dx + dy * dy)
                    val arch = y < 10 && d in 4.0..9.5
                    val legs = y in 10..20 && abs(dx) in 4.0..9.5
                    if (!arch && !legs) continue
                    val across = if (arch) (d - 4) / 5.5 else (abs(dx) - 4) / 5.5
                    val v = Critter.shadeAt((across * 2 - 1) * (if (dx < 0) -1 else 1) * 8, if (arch) dy else 0.0, 9.0, 9.0)
                    b.p(x, y, tones(c).at(v, x, y))
                }
                for (side in listOf(-1, 1)) { val px = 12 + side * 7 - (if (side < 0) 2 else 3); b.rect(px, 17, 6, 4, rgb(0xD8DCE4)); b.rect(px, 17, 6, 1, WHITE); b.rect(px, 20, 6, 1, rgb(0x8A9098)) }
                b.p(5, 4, WHITE); b.p(6, 3, WHITE, .6f)
            }
            Shape.STAR -> {
                b.shadowUnder(12, 22, 8)
                for (y in 0 until S) for (x in 0 until S) {
                    val dx = x + .5 - 12; val dy = y + .5 - 12
                    val a = atan2(dy, dx)
                    val k = (cos(a * 5 + PI / 2) + 1) / 2
                    val rr = 4.5 + 5.5 * k * k
                    if (dx * dx + dy * dy > rr * rr) continue
                    // Each arm has a lit side and a shaded side, like a pressed sequin.
                    val arm = ((a + PI / 2) / (2 * PI / 5) % 1 + 1) % 1
                    b.p(x, y, if (arm < .5) (if (dx + dy < 0) r.hi else r.l) else (if (dx + dy < 0) r.b else r.d))
                }
                b.ell(12, 12, 1, 1, r.dd)
                b.sparkle(18, 5); b.p(9, 8, WHITE)
            }
            Shape.CRUMB -> {
                b.shadowUnder(12, 21, 10)
                // A rusk: a toasted crust round a porous crumb.
                for (y in 5 until 20) for (x in 2 until 22) {
                    val top = 7 - (sin((x - 2) / 20.0 * PI) * 3).toInt()
                    if (y < top) continue
                    val edge = y == top || y >= 18 || x <= 2 || x >= 21
                    b.p(x, y, if (edge) (if (y < 10) r.l else r.d) else mix(r.l, WHITE, .25f))
                }
                for (k in 0 until 14) { val px = 4 + (k * 7) % 16; val py = 9 + (k * 5) % 8; b.p(px, py, r.b); b.p(px + 1, py, mix(r.b, INK, .2f)) }
                b.rect(3, 18, 18, 2, r.dd)
                b.p(6, 6, WHITE, .6f); b.p(1, 19, r.b); b.p(22, 18, r.d); b.p(20, 21, r.l)
            }
            Shape.RAISIN -> {
                b.shadowUnder(12, 21, 8)
                b.dome(12.0, 12.5, 8.5, 7.5, c, -.05)
                // Wrinkles: dark folds with a lit lip.
                for ((x0, y0, x1, y1) in listOf(listOf(5, 10, 18, 8), listOf(6, 14, 19, 13), listOf(9, 6, 11, 18), listOf(15, 6, 14, 17))) {
                    b.line(x0, y0, x1, y1, r.dd); b.line(x0, y0 - 1, x1, y1 - 1, mix(r.l, r.b, .3f))
                }
                b.rect(11, 3, 2, 3, rgb(0x6A8A3A)); b.p(12, 2, rgb(0x8AAA4A))
                b.p(7, 8, r.hi); b.p(8, 7, WHITE, .5f)
            }
            Shape.SUGAR -> {
                b.shadowUnder(12, 22, 9)
                val t = ramp(mix(c, rgb(0xF8F8F4), .5f))
                // Three faces of a cube: top, left, right.
                for (y in 0 until S) for (x in 0 until S) {
                    val topFace = y in 3..10 && abs(x - 12) <= (y - 3) * 1.25 + 1 && abs(x - 12) <= (10 - y) * 1.25 + 1
                    val left = x in 3..12 && y >= 7 + (x - 3) / 2 && y <= 17 + (x - 3) / 2
                    val right = x in 12..21 && y >= 11 - (x - 12) / 2 && y <= 21 - (x - 12) / 2
                    val col = when { topFace -> t.hi; left -> t.b; right -> t.d; else -> continue }
                    b.p(x, y, if ((x * 5 + y * 3) % 7 == 0) mix(col, WHITE, .5f) else col)
                }
                b.sparkle(16, 6); b.p(6, 13, WHITE); b.p(17, 16, WHITE, .7f)
            }
            Shape.SEED -> {
                b.shadowUnder(12, 22, 6)
                for (y in 0 until S) for (x in 0 until S) {
                    val dx = (x + .5 - 12) / 5.5; val dy = (y + .5 - 12) / 9.5
                    val tip = y < 6 && abs(x + .5 - 12) > (y - 1) * .9
                    if (dx * dx + dy * dy > 1 || tip) continue
                    val v = Critter.shadeAt(dx * 5.5, dy * 9.5, 5.5, 9.5)
                    val stripe = abs(x - 12) in listOf(1, 4) && y > 4
                    b.p(x, y, if (stripe) mix(rgb(0xE8E8E0), r.b, (1 - v).toFloat() * .5f) else tones(c).at(v, x, y))
                }
                b.p(10, 6, WHITE, .7f)
            }
            Shape.HONEY -> {
                b.shadowUnder(12, 22, 7)
                for (y in 0 until S) for (x in 0 until S) {
                    val dx = x + .5 - 12; val dy = y + .5 - 14.5
                    val inBall = dx * dx + dy * dy <= 49
                    val inTip = y in 2..10 && abs(dx) <= (y - 1.5) * .72
                    if (!inBall && !inTip) continue
                    // Honey glows from inside: lighter at the bottom, away from the glint.
                    val glow = (dy / 7).coerceIn(-1.0, 1.0)
                    val v = Critter.shadeAt(dx, if (inBall) dy else 0.0, 7.5, 7.5) * .6 + .25 + glow * .15
                    b.p(x, y, tones(c).at(v, x, y))
                }
                b.ell(9, 13, 1, 3, WHITE); b.p(10, 10, WHITE, .7f); b.p(15, 18, mix(c, WHITE, .5f))
            }
        }
        return b
    }
}
