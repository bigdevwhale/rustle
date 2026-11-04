package app.rustle.ui.art

import app.rustle.domain.Marks
import app.rustle.domain.Pose
import app.rustle.domain.Species
import app.rustle.domain.Trait
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** How he looks: his kind, his two colours and pattern, what his life has grown on him, and what he wears. */
data class Look(
    val body: Int,
    val accent: Int,
    val traits: Set<Trait> = emptySet(),
    val wear: Set<String> = emptySet(),
    val species: Species = Species.MOUSE,
    val marks: Marks = Marks.NONE,
)

/**
 * The creature, 48×44: a ball of fluff lit like a sphere (five tones, dithered where they meet) with the
 * ears, tail and extras of its kind — round mouse ears, long bunny ears, pointed fox or cat ears, a bat's
 * sails, an owl's tufts and wings, a moth's feathery feelers and soft wings, a hedgehog's spines. [squash]
 * is −1..1: flattened on landing, stretched in a jump. [step] lifts one foot while walking.
 */
object Critter {
    const val W = 48
    const val H = 44

    private val INK = rgb(0x1A1018)
    private val WHITE = rgb(0xFFFBEA)
    private val PINK = rgb(0xF29AA0)
    private val PINK_D = rgb(0xC86878)
    private val SWEAT = rgb(0x8AD0F0)
    private val PLAID_A = rgb(0xC0403A)
    private val PLAID_B = rgb(0xF0E2C0)
    private val BEAK = rgb(0xE8A838)

    private const val LX = -.5
    private const val LY = -.68
    private const val LZ = .54

    internal class Tones(val hi: Int, val l: Int, val b: Int, val d: Int, val dd: Int) {
        private val cols = intArrayOf(hi, l, b, d, dd)
        private val bands = doubleArrayOf(.9, .68, .44, .2)

        /** Five bands with a dithered seam between neighbours. */
        fun at(v: Double, x: Int, y: Int): Int {
            for (k in bands.indices) {
                if (v >= bands[k] + .03) return cols[k]
                if (v >= bands[k] - .03) return if (dith(x, y, (v - bands[k] + .03) / .06)) cols[k] else cols[k + 1]
            }
            return dd
        }
    }

    internal fun tones(c: Int) = ramp(c).let { Tones(it.hi, it.l, it.b, it.d, it.dd) }

    /** Lights a point on an ellipsoid of radii rx, ry at offset dx, dy from its centre. */
    internal fun shadeAt(dx: Double, dy: Double, rx: Double, ry: Double): Double {
        val nx = dx / rx; val ny = dy / ry
        val nz = sqrt(max(0.0, 1 - nx * nx - ny * ny))
        val diffuse = max(0.0, nx * LX + ny * LY + nz * LZ)
        val rim = if (nz < .35 && nx > .2) .12 else 0.0
        return (.3 + diffuse * .78 + rim).coerceIn(0.0, 1.0)
    }

    /** Paints a mask with fur tones by a flat light from the top left, then outlines it. */
    private fun Buf.furFill(fur: Tones, line: Int, bias: Double = 0.0) {
        val minX = (0 until w).firstOrNull { x -> (0 until h).any { filled(x, it) } } ?: return
        val maxX = (w - 1 downTo 0).first { x -> (0 until h).any { filled(x, it) } }
        val minY = (0 until h).first { y -> (0 until w).any { filled(it, y) } }
        val maxY = (h - 1 downTo 0).first { y -> (0 until w).any { filled(it, y) } }
        val cx = (minX + maxX) / 2.0; val cy = (minY + maxY) / 2.0
        val rx = (maxX - minX) / 2.0 + 1; val ry = (maxY - minY) / 2.0 + 1
        for (y in 0 until h) for (x in 0 until w) if (filled(x, y)) p(x, y, fur.at(shadeAt(x + .5 - cx, y + .5 - cy, rx * 1.15, ry * 1.15) + bias, x, y))
        outline(line)
    }

    fun draw(look: Look, pose: Pose, tick: Int, petted: Boolean = false, lying: Boolean = false, squash: Double = 0.0, step: Int = 0): Buf {
        val out = Buf(W, H)
        val t = look.traits
        val sp = look.species
        val lie = lying || pose == Pose.SLEEP || pose == Pose.SUN
        val still = Art.still
        val fur = tones(look.body)
        val dark = tones(mix(look.body, INK, .35f))
        val pale = tones(mix(look.accent, look.body, .12f))
        val r = ramp(look.body)
        val line = mix(look.body, INK, .8f)

        val breath = if (still) 0.0 else sin(tick / 22.0) * (if (pose == Pose.SLEEP) .07 else .035)
        val sq = squash + breath
        val cx = 24.0
        val rx = (if (lie) 18.5 else 15.0) * (1 + sq * .5) + (if (Trait.GOURMET in t) 1.2 else 0.0) - (if (pose == Pose.HUNGRY) .8 else 0.0)
        val ry = (if (lie) 10.5 else 13.5) * (1 - sq)
        val baseY = 41.0
        val cy = baseY - ry - (if (lie) 0.0 else 1.5)
        val sway = if (still) 0.0 else sin(tick / 17.0) * .35 + (if (petted) sin(tick / 3.0) * .4 else 0.0)
        val flutter = if (still) 0.0 else sin(tick / 5.0) * (if (petted) 1.0 else .35)

        // ---- Behind the body: wings, cape, backpack, tail. ----
        if (sp == Species.MOTH) {
            for (side in listOf(-1, 1)) {
                val wing = Buf(W, H)
                val wx = cx + side * (rx * .85 + 1); val wy = cy - ry * .25
                wing.ell(wx, wy - flutter, 8, (9 + flutter).toInt(), Buf.OPAQUE)
                wing.ell(wx + side * 1.5, wy + 8.0, 5, 4, Buf.OPAQUE)
                wing.furFill(pale, line, -.05)
                wing.ell(wx + side * 2, wy - 1 - flutter, 3, 3, mix(look.body, INK, .3f)); wing.ell(wx + side * 2, wy - 1 - flutter, 1, 1, look.accent)
                wing.p((wx + side * 1).toInt(), (wy - 2 - flutter).toInt(), WHITE)
                for (k in 0 until 4) wing.p((wx - side * 3 + side * k * 2).toInt(), (wy + 5).toInt(), mix(look.body, INK, .25f))
                out.blit(wing, 0, 0)
            }
        }
        if (sp == Species.BAT && !lie) for (side in listOf(-1, 1)) {
            val wing = Buf(W, H)
            val x0 = cx + side * rx * .7
            for (y in (cy - ry * .4).toInt()..(cy + ry * .7).toInt()) {
                val k = (y - (cy - ry * .4)) / (ry * 1.1)
                val reach = 7 + 3 * sin(k * PI) + flutter * .6
                for (i in 0..reach.toInt()) wing.p((x0 + side * i).toInt(), y, Buf.OPAQUE)
            }
            // Scalloped lower edge.
            for (i in 0..10 step 3) wing.clearAt((x0 + side * (2 + i)).toInt(), (cy + ry * .7).toInt())
            wing.furFill(dark, line)
            for (f in 0 until 3) wing.line(x0.toInt(), (cy - ry * .3).toInt(), (x0 + side * (4 + f * 3)).toInt(), (cy + ry * .65).toInt(), mix(look.body, INK, .55f))
            out.blit(wing, 0, 0)
        }
        if ("cape" in look.wear && !lie) {
            val cape = ramp(rgb(0x8A3A6A))
            val top = (cy - ry * .45).toInt()
            for (y in top until (baseY - 1).toInt()) {
                val spread = ((y - top) * .8).toInt() + 2 + (if (!still) (sin(tick / 8.0 + y / 4.0) * 1).roundToInt() else 0)
                for (x in (cx - rx * .55 - spread).toInt()..(cx + rx * .55 + spread).toInt()) out.p(x, y, if (x < cx - rx * .55 - spread + 2) cape.l else if (x > cx + rx * .4 + spread) cape.d else cape.b)
            }
        }
        if ("backpack" in look.wear && !lie) {
            val bag = ramp(rgb(0x8A5A30))
            out.block(1, (cy - 5).toInt(), 11, 15, bag, 2)
            out.rect(2, (cy + 1).toInt(), 9, 3, bag.d); out.rect(2, (cy + 1).toInt(), 9, 1, bag.l); out.p(6, (cy + 2).toInt(), rgb(0xE8B850))
            out.ring(1, (cy - 5).toInt(), 11, 15, bag.dd)
        }
        if (!lie) tail(out, look, sp, cx, cy, rx, ry, sway, fur, pale, line)

        // ---- Ears, feelers, tufts. ----
        ears(out, look, sp, pose, tick, cx, cy, rx, ry, lie, fur, dark, line)

        // ---- Body. ----
        val body = Buf(W, H)
        val tuft = (if (Trait.CUDDLY in t) 1.35 else 1.0) * (if (sp == Species.HEDGEHOG) .4 else 1.0)
        for (y in 0 until H) for (x in 0 until W) {
            val dx = x + .5 - cx; val dy = y + .5 - cy
            val a = atan2(dy / ry, dx / rx)
            val fringe = if (sin(a) > .55) 0.0 else (sin(a * 9 + 1.3) * .055 + sin(a * 17 + .4) * .035 + (if (sin(a * 23) > .7) .05 else 0.0)) * tuft
            val e = (dx * dx) / (rx * rx) + (dy * dy) / (ry * ry)
            if (e <= (1 + fringe) * (1 + fringe)) body.p(x, y, fur.at(shadeAt(dx, dy, rx * 1.05, ry * 1.05), x, y))
        }
        if (sp == Species.HEDGEHOG) spines(body, cx, cy, rx, ry, dark, look)
        for (i in 0 until 14) {
            val a = i * 2.39996 + 1.0
            val rr = .4 + (i % 5) * .11
            val x = (cx + cos(a) * rx * rr).roundToInt(); val y = (cy + sin(a) * ry * rr).roundToInt()
            if (!body.filled(x, y) || !body.filled(x + 1, y + 1)) continue
            val dk = x - cx + (y - cy) * 1.2 > 0
            val c = if (dk) mix(r.b, r.d, .7f) else mix(r.b, r.l, .8f)
            body.p(x, y, c); body.p(x + (if (dk) 1 else -1), y + 1, c)
        }
        if (Trait.WANDERER in t) for ((dx, dy) in listOf(0 to 0, 1 to -1, 2 to -2, 3 to -2, 1 to 0)) body.p(cx.toInt() + dx, (cy - ry).toInt() + dy, fur.l)
        marks(body, look, cx, cy, rx, ry)
        // A pale bib, a white muzzle for the fox, a facial disc for the owl.
        val bibCy = cy + ry * .42; val bibRx = rx * .55; val bibRy = ry * .5
        for (y in 0 until H) for (x in 0 until W) {
            val dx = x + .5 - cx; val dy = y + .5 - bibCy
            val e = dx * dx / (bibRx * bibRx) + dy * dy / (bibRy * bibRy)
            val edge = sin(atan2(dy, dx) * 11) * .08
            if (e <= 1 + edge && body.filled(x, y)) body.p(x, y, pale.at(.5 + shadeAt(x + .5 - cx, y + .5 - cy, rx * 1.05, ry * 1.05) * .45, x, y))
        }
        if (sp == Species.OWL || sp == Species.FOX) {
            val fy = cy - ry * .15 + (if (lie) 2 else 0)
            for (side in listOf(-1, 1)) for (y in 0 until H) for (x in 0 until W) {
                val dx = (x + .5 - (cx + side * (if (sp == Species.OWL) 5.5 else 4.0))) / (if (sp == Species.OWL) 6.0 else 4.5)
                val dy = (y + .5 - (fy + (if (sp == Species.FOX) 3.5 else 0.0))) / (if (sp == Species.OWL) 6.0 else 3.5)
                if (dx * dx + dy * dy <= 1 && body.filled(x, y)) body.p(x, y, pale.at(.75 - dy * .2, x, y))
            }
            if (sp == Species.OWL) for (side in listOf(-1, 1)) for (a in 0 until 360 step 15) {
                val rad = a * PI / 180
                body.p((cx + side * 5.5 + cos(rad) * 6).roundToInt(), (fy + sin(rad) * 6).roundToInt(), mix(look.body, INK, .45f))
            }
        }
        body.outline(line)
        out.blit(body, 0, 0)

        // ---- Feet and paws (white in socks). ----
        val sock = look.marks == Marks.SOCKS
        val paw = if (sock) tones(mix(WHITE, look.accent, .3f)) else pale
        if (!lie) for (side in listOf(-1, 1)) {
            val fx = (cx + side * 7).roundToInt()
            val lift = if (step != 0 && (step > 0) == (side > 0)) 1 else 0
            val fy = (baseY - 1 - lift).toInt()
            if (sp == Species.OWL || sp == Species.BAT) {
                for (k in -1..1) { out.rect(fx + k * 2, fy, 1, 2, BEAK); out.p(fx + k * 2, fy + 2, mix(BEAK, INK, .5f)) }
            } else {
                out.ell(fx, fy, 4, 2, paw.b); out.rect(fx - 3, fy - 1, 7, 1, paw.l)
                out.p(fx - 4, fy, line); out.p(fx + 4, fy, line); out.rect(fx - 3, fy + 2, 7, 1, line)
                for (k in -1..1) out.p(fx + k * 2, fy - 1, mix(paw.b, PINK_D, .3f))
            }
        }
        val pawY = (cy + ry * .55).roundToInt()
        if (!lie && pose != Pose.COLD && sp != Species.OWL && sp != Species.MOTH) for (side in listOf(-1, 1)) {
            val px = (cx + side * 5.5).roundToInt()
            out.ell(px, pawY, 3, 2, paw.l); out.rect(px - 2, pawY + 2, 5, 1, mix(paw.d, line, .3f))
            out.p(px - 1, pawY + 1, PINK); out.p(px + 1, pawY + 1, PINK); out.p(px, pawY, mix(PINK, WHITE, .3f))
        }
        // The owl hugs itself with its wings.
        if (sp == Species.OWL) for (side in listOf(-1, 1)) {
            val wing = Buf(W, H)
            wing.ell(cx + side * (rx - 3.5), cy + ry * .2 - flutter * .5, 4, (ry * .62).toInt(), Buf.OPAQUE)
            wing.furFill(dark, line)
            for (k in 0 until 4) wing.p((cx + side * (rx - 3.5) - side).toInt(), (cy + ry * .1 + k * 2.5).toInt(), mix(look.body, INK, .55f))
            out.blit(wing, 0, 0)
        }

        face(out, look, pose, tick, petted, cx, cy, ry, lie, still)
        Wear.neck(out, look, pose, cx, cy, rx, ry, lie, body, tick)

        if (pose == Pose.COLD) {
            val top = (cy - ry * .15).toInt()
            for (y in top until H) for (x in 0 until W) if (body.filled(x, y) && y > top + (abs(x - cx) / 3.5).toInt()) {
                val c = if (((x / 3) + (y / 3)) % 2 == 0) PLAID_A else PLAID_B
                out.p(x, y, if ((x + y) % 5 == 0) mix(c, INK, .2f) else c)
            }
            out.line((cx - 12).toInt(), top + 5, cx.toInt(), top + 1, mix(PLAID_A, INK, .4f))
        }
        Wear.head(out, look, pose, cx, cy - ry, lie, tick)
        if (pose == Pose.COLD && !still && tick % 6 < 3) return out.shifted(1)
        return out
    }

    private fun tail(out: Buf, look: Look, sp: Species, cx: Double, cy: Double, rx: Double, ry: Double, sway: Double, fur: Tones, pale: Tones, line: Int) {
        val tail = Buf(W, H)
        when (sp) {
            Species.MOUSE, Species.FOX -> {
                val big = sp == Species.FOX
                val n = 9
                val pts = (0 until n).map { i ->
                    val k = i / (n - 1.0)
                    val px = cx + rx * .7 + (if (big) 8 else 7) * sin(k * 1.6) + sway * 4 * k * k
                    val py = cy + ry * .5 - k * (if (big) 18 else 20) + 3 * k * k
                    Triple(px, py, if (big) (5.5 - k * 1.2) else (4.2 - k * 1.8).coerceAtLeast(2.0))
                }
                for ((px, py, rad) in pts) tail.ell(px, py, rad.roundToInt(), rad.roundToInt(), Buf.OPAQUE)
                val tipY = pts.last().second
                for (y in 0 until H) for (x in 0 until W) if (tail.filled(x, y)) {
                    val v = (.85 - (x - cx - rx * .5) / 16.0 * .5 + (pts.first().second - y) / 40.0).coerceIn(.05, .95)
                    tail.p(x, y, if (y < tipY + (if (big) 6 else 4)) pale.at(v, x, y) else fur.at(v, x, y))
                }
            }
            Species.CAT -> {
                // A thin tail curling into a question mark.
                for (i in 0 until 26) {
                    val k = i / 25.0
                    val px = cx + rx * .75 + 6 * sin(k * 2.2) + sway * 3 * k
                    val py = cy + ry * .6 - k * 22
                    tail.ell(px, py, 1, 1, Buf.OPAQUE)
                }
                for (y in 0 until H) for (x in 0 until W) if (tail.filled(x, y)) tail.p(x, y, fur.at(.7 - (x - cx - rx) / 20.0, x, y))
            }
            Species.BUNNY -> {
                tail.ell(cx + rx * .85, cy + ry * .55, 4, 4, Buf.OPAQUE)
                for (y in 0 until H) for (x in 0 until W) if (tail.filled(x, y)) tail.p(x, y, pale.at(.9 - (x - cx - rx) / 10.0, x, y))
            }
            Species.OWL -> {
                for (k in -1..1) tail.line((cx + rx * .55).toInt(), (cy + ry * .7).toInt(), (cx + rx * .55 + 5 + k).toInt(), (cy + ry * .7 + 4 + k * 2).toInt(), Buf.OPAQUE)
                for (y in 0 until H) for (x in 0 until W) if (tail.filled(x, y)) tail.p(x, y, fur.d)
            }
            else -> return
        }
        tail.outline(line)
        out.blit(tail, 0, 0)
    }

    private fun ears(out: Buf, look: Look, sp: Species, pose: Pose, tick: Int, cx: Double, cy: Double, rx: Double, ry: Double, lie: Boolean, fur: Tones, dark: Tones, line: Int) {
        val twitch = !Art.still && (tick % 140) in 0..5
        val earY = cy - ry * .78 + (if (lie) 2.0 else 0.0)
        val inner = mix(look.accent, PINK, .6f)
        for (side in listOf(-1, 1)) {
            val ear = Buf(W, H)
            val tw = if (twitch && side > 0) 1.5 else 0.0
            var pinkMask: ((Int, Int) -> Boolean)? = null
            when (sp) {
                Species.MOUSE, Species.HEDGEHOG -> {
                    val er = if (sp == Species.HEDGEHOG) 4 else if (lie) 5 else 6
                    val ex = cx + side * rx * (if (sp == Species.HEDGEHOG) .5 else .62); val ey = earY + tw + (if (sp == Species.HEDGEHOG) 3 else 0)
                    ear.ell(ex, ey, er, er, Buf.OPAQUE)
                    if (Trait.OWL in look.traits) for (k in 0 until 4) ear.p((ex + side * (2 + k * .6)).roundToInt(), (ey - er - k).roundToInt(), Buf.OPAQUE)
                    pinkMask = { x, y -> val dx = (x - ex - side * .5) / (er * .55); val dy = (y - ey - .5) / (er * .62); dx * dx + dy * dy <= 1 }
                }
                Species.BUNNY -> {
                    val droop = if (lie || pose == Pose.HUNGRY || pose == Pose.COLD) 1 else 0
                    val ex = cx + side * 5.5
                    for (j in 0 until 18) {
                        val y = earY + 2 - j + tw
                        val lean = side * (j * (.18 + droop * .4))
                        val wdt = 3.4 * sin((j + 1) / 19.0 * PI).coerceAtLeast(.5) + .6
                        for (i in -wdt.toInt()..wdt.toInt()) ear.p((ex + lean + i).roundToInt(), y.roundToInt(), Buf.OPAQUE)
                    }
                    pinkMask = { x, y -> val j = earY + 2 + tw - y; val lean = side * (j * (.18 + droop * .4)); j in 2.0..15.0 && abs(x - (ex + lean)) <= 1.2 * sin((j + 1) / 19.0 * PI) }
                }
                Species.FOX, Species.CAT -> {
                    val big = sp == Species.FOX
                    val bx = cx + side * rx * .55; val by = earY + 3 + tw
                    val hgt = if (big) 12 else 10
                    for (j in 0 until hgt) {
                        val half = (hgt - j) * (if (big) .55 else .5)
                        for (i in -half.toInt()..half.toInt()) ear.p((bx + side * j * .25 + i).roundToInt(), (by - j).roundToInt(), Buf.OPAQUE)
                    }
                    pinkMask = { x, y -> val j = by - y; val half = (hgt - j) * .25; j in 1.0..(hgt - 3.0) && abs(x - (bx + side * j * .25)) <= half }
                }
                Species.BAT -> {
                    val bx = cx + side * rx * .5; val by = earY + 4 + tw
                    for (j in 0 until 15) {
                        val half = (15 - j) * .5
                        for (i in -half.toInt()..half.toInt()) ear.p((bx + side * j * .65 + i).roundToInt(), (by - j).roundToInt(), Buf.OPAQUE)
                    }
                    pinkMask = { x, y -> val j = by - y; val half = (15 - j) * .26; j in 2.0..11.0 && abs(x - (bx + side * j * .65)) <= half }
                }
                Species.OWL -> {
                    val bx = cx + side * rx * .6; val by = earY + 2 + tw
                    for (j in 0 until 7) for (i in 0..(6 - j) / 2) { ear.p((bx + side * (j * .5 + i)).roundToInt(), (by - j).roundToInt(), Buf.OPAQUE); ear.p((bx + side * (j * .5 - i)).roundToInt(), (by - j).roundToInt(), Buf.OPAQUE) }
                }
                Species.MOTH -> {
                    // A feathery feeler: a curved stalk with barbs.
                    val bx = cx + side * 4; val by = cy - ry + 2
                    for (j in 0 until 14) {
                        val x = bx + side * (j * .55 + j * j * .02); val y = by - j + tw
                        ear.p(x.roundToInt(), y.roundToInt(), Buf.OPAQUE)
                        if (j > 3 && j % 2 == 0) { ear.p((x + side).roundToInt(), (y - 1).roundToInt(), Buf.OPAQUE); ear.p((x - side).roundToInt(), (y - 1).roundToInt(), Buf.OPAQUE) }
                    }
                    for (y in 0 until H) for (x in 0 until W) if (ear.filled(x, y)) ear.p(x, y, mix(look.accent, look.body, .3f))
                    ear.outline(line); out.blit(ear, 0, 0); continue
                }
            }
            ear.furFill(if (sp == Species.BAT) dark else fur, line)
            pinkMask?.let { m -> for (y in 0 until H) for (x in 0 until W) if (ear.filled(x, y) && m(x, y)) ear.p(x, y, if ((x + y) % 4 == 0) mix(inner, WHITE, .2f) else inner) }
            if (sp == Species.FOX) for (y in 0 until H) for (x in 0 until W) if (ear.filled(x, y) && y < earY - 4) ear.p(x, y, mix(look.body, INK, .6f))
            out.blit(ear, 0, 0)
        }
    }

    /** A hedgehog's coat: dark tipped spines over the back and crown, leaving the face clear. */
    private fun spines(body: Buf, cx: Double, cy: Double, rx: Double, ry: Double, dark: Tones, look: Look) {
        val tip = mix(look.accent, WHITE, .2f)
        for (i in 0 until 34) {
            val a = PI + i * PI / 33
            val baseR = .9 + (i % 2) * .06
            val x0 = cx + cos(a) * rx * baseR; val y0 = cy + sin(a) * ry * baseR
            val len = 4.0 + (i % 3)
            val x1 = cx + cos(a) * (rx + len); val y1 = cy + sin(a) * (ry + len)
            body.line(x0.roundToInt(), y0.roundToInt(), x1.roundToInt(), y1.roundToInt(), dark.b)
            body.p(x1.roundToInt(), y1.roundToInt(), tip)
            body.p(((x0 + x1) / 2).roundToInt() + 1, ((y0 + y1) / 2).roundToInt(), dark.d)
        }
        // A cap of spines over the forehead, above the eyes.
        for (y in 0 until H) for (x in 0 until W) {
            val dx = (x + .5 - cx) / (rx * .95); val dy = (y + .5 - (cy - ry * .55)) / (ry * .45)
            if (dx * dx + dy * dy <= 1 && body.filled(x, y)) body.p(x, y, if ((x * 3 + y * 5) % 7 == 0) tip else if ((x + y) % 2 == 0) dark.b else dark.d)
        }
    }

    /** A pattern over the fur: spots, tabby stripes, a patch round one eye, a mask across both. */
    private fun marks(body: Buf, look: Look, cx: Double, cy: Double, rx: Double, ry: Double) {
        val m = mix(look.body, INK, .42f)
        val ml = mix(look.body, INK, .3f)
        fun dab(x: Double, y: Double, r: Int, c: Int) { for (j in -r..r) for (i in -r..r) if (i * i + j * j <= r * r + 1 && body.filled((x + i).toInt(), (y + j).toInt())) body.p((x + i).toInt(), (y + j).toInt(), c) }
        when (look.marks) {
            Marks.SPOTS -> for ((dx, dy, r) in listOf(Triple(-.6, -.45, 2), Triple(.55, -.55, 1), Triple(.75, .05, 2), Triple(-.8, .1, 1), Triple(.3, -.85, 1))) dab(cx + dx * rx, cy + dy * ry, r, m)
            Marks.STRIPES -> {
                for (k in -1..1) for (j in 0 until 4) body.p((cx + k * 3).toInt(), (cy - ry + 2 + j).toInt(), if (j < 3) m else ml)
                for (side in listOf(-1, 1)) for (k in 0 until 3) for (j in 0 until 4) body.p((cx + side * (rx - 2 - j * .4)).toInt(), (cy - 3 + k * 4 + j * .3).toInt(), m)
            }
            Marks.PATCH -> for (y in 0 until H) for (x in 0 until W) {
                val dx = (x + .5 - (cx + 6)) / 6.5; val dy = (y + .5 - (cy - ry * .25)) / 6.0
                if (dx * dx + dy * dy + sin(x * 1.7) * .08 <= 1 && body.filled(x, y)) body.p(x, y, m)
            }
            Marks.MASK -> for (y in 0 until H) for (x in 0 until W) {
                val dy = y + .5 - (cy - ry * .2)
                val dx = x + .5 - cx
                if (abs(dy) <= 3.2 - abs(dx) * .06 && abs(dx) in 2.0..13.0 && body.filled(x, y)) body.p(x, y, m)
            }
            else -> Unit
        }
    }

    private fun face(out: Buf, look: Look, pose: Pose, tick: Int, petted: Boolean, cx: Double, cy: Double, ry: Double, lie: Boolean, still: Boolean) {
        val t = look.traits
        val sp = look.species
        val ink = mix(look.body, INK, .85f)
        val ey = (cy - ry * .2 + (if (lie) 2 else 0)).roundToInt()
        val eyes = listOf((cx - 8).roundToInt(), (cx + 5).roundToInt())
        val glance = if (still || pose != Pose.IDLE) 0 else listOf(0, 0, -1, 0, 1, 0, 0)[(tick / 80) % 7]
        val bp = if (still) -1 else tick % 170
        val lid = when (bp) { 0, 1, 6, 7 -> 1; 2, 3, 4, 5 -> 2; else -> 0 }
        val happy = petted || pose == Pose.WARM || (pose == Pose.SUN && "goggles" !in look.wear)
        val big = pose == Pose.DARK || Trait.OWL in t || sp == Species.OWL || sp == Species.BAT
        val iris = when (sp) { Species.OWL -> rgb(0xE8B030); Species.CAT -> rgb(0x8AC860); Species.FOX -> rgb(0xC87A30); else -> mix(look.body, rgb(0x2A1A10), .7f) }
        for (x0 in eyes) {
            val x = x0 + glance
            when {
                pose == Pose.DIZZY -> {
                    val ph = (tick / 5) % 8
                    out.ring(x - 1, ey - 2, 5, 5, ink); out.p(x + 1, ey, ink)
                    val s = listOf(0 to 0, 2 to 0, 4 to 0, 4 to 2, 4 to 4, 2 to 4, 0 to 4, 0 to 2)[ph]
                    out.p(x - 1 + s.first, ey - 2 + s.second, WHITE)
                }
                pose == Pose.SLEEP || lid == 2 -> { out.p(x - 1, ey + 1, ink); out.rect(x, ey + 2, 3, 1, ink); out.p(x + 3, ey + 1, ink) }
                happy -> { out.p(x - 1, ey + 2, ink); out.p(x, ey + 1, ink); out.p(x + 1, ey, ink); out.p(x + 2, ey + 1, ink); out.p(x + 3, ey + 2, ink) }
                else -> {
                    val h = if (big) 7 else 6
                    val top = ey - h / 2
                    val coloured = sp == Species.OWL || sp == Species.CAT || sp == Species.FOX
                    for (j in 0 until h) for (i in -1..4) {
                        val dx = (i - 1.5) / 2.9; val dy = (j - (h - 1) / 2.0) / (h / 2.0)
                        if (dx * dx + dy * dy > 1.05) continue
                        val rim = dx * dx + dy * dy > .55
                        out.p(x + i, top + j, when { coloured && !rim -> iris; coloured -> INK; j >= h - 2 && abs(i - 1.5) < 2 -> mix(iris, look.accent, .3f); else -> INK })
                    }
                    if (sp == Species.CAT) out.rect(x + 1, top + 1, 1, h - 2, rgb(0x050308)) else out.rect(x + 1, top + h / 2 - 1, 2, 2, rgb(0x050308))
                    out.rect(x, top + 1, 2, 2, WHITE); out.p(x + 3, top + h - 2, mix(WHITE, iris, .3f))
                    if (lid == 1) { out.rect(x - 1, top, 6, h / 2, mix(look.body, INK, .15f)); out.rect(x - 1, top + h / 2, 6, 1, ink) }
                    if (pose == Pose.HUNGRY) {
                        val inner = if (x0 == eyes[0]) 1 else -1
                        out.p(x + 1 + inner * 2, top - 2, ink); out.p(x + 1 + inner, top - 2, ink); out.p(x + 1, top - 1, ink)
                    }
                }
            }
        }
        Wear.eyes(out, look, pose, eyes, ey)
        val nx = cx.roundToInt() - 1; val ny = ey + 4
        if (sp == Species.OWL) {
            // A small hooked beak instead of a nose and mouth.
            out.rect(nx - 1, ny - 1, 3, 2, BEAK); out.p(nx, ny + 1, BEAK); out.p(nx - 1, ny - 1, mix(BEAK, WHITE, .5f)); out.p(nx + 1, ny, mix(BEAK, INK, .4f))
        } else {
            out.rect(nx - 1, ny, 3, 1, if (sp == Species.BAT) mix(PINK, INK, .3f) else PINK); out.p(nx, ny + 1, PINK_D); out.p(nx - 1, ny, mix(PINK, WHITE, .5f))
            when {
                pose == Pose.HUNGRY -> out.rect(nx - 1, ny + 3, 3, 1, ink)
                happy -> { out.rect(nx - 2, ny + 2, 5, 2, ink); out.rect(nx - 1, ny + 3, 3, 1, PINK) }
                else -> { out.p(nx - 2, ny + 2, ink); out.p(nx - 1, ny + 3, ink); out.p(nx, ny + 2, ink); out.p(nx + 1, ny + 3, ink); out.p(nx + 2, ny + 2, ink) }
            }
            if (sp == Species.BAT) { out.p(nx - 1, ny + 3, WHITE); out.p(nx + 1, ny + 3, WHITE) }
            if (sp != Species.MOTH && sp != Species.HEDGEHOG) {
                val wh = mix(look.accent, WHITE, .55f)
                val len = if (sp == Species.CAT) 4 else 2
                for (side in listOf(-1, 1)) {
                    val sx = if (side < 0) eyes[0] - 3 else eyes[1] + 6
                    for (k in 0..len) { out.p(sx + side * k, ny + 1 - k / 2, wh, .8f - k * .1f); out.p(sx + side * k, ny + 3 + k / 3, wh, .7f - k * .1f) }
                }
            }
        }
        val blush = if (Trait.SLEEPYHEAD in t || pose == Pose.WARM || pose == Pose.HOT || petted) .9 else .45
        for ((k, x0) in eyes.withIndex()) for (j in 0..1) for (i in -1..3) if (dith(x0 + i, ny + j, blush)) out.p(x0 + i + (if (k == 0) -1 else 1), ny + j, PINK)
        if (pose == Pose.HOT) { out.p(eyes[1] + 8, ey - 6, SWEAT); out.rect(eyes[1] + 7, ey - 5, 3, 2, SWEAT); out.p(eyes[1] + 8, ey - 3, mix(SWEAT, WHITE, .5f)) }
    }

    private fun Buf.shifted(dx: Int): Buf {
        val o = Buf(w, h)
        o.blit(this, dx, 0)
        return o
    }
}
