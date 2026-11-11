package app.rustle.ui.art

import app.rustle.domain.Marks
import app.rustle.domain.Pose
import app.rustle.domain.Species
import app.rustle.domain.Trait
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
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
 * The creature, 48×44: his kind's drawn sprite (see [Sprites]) sitting or curled up asleep, in the owner's
 * colours and pattern, with the face of the moment painted over the drawn eyes, what his life has grown on
 * him, and what he wears. [squash] is −1..1: flattened on landing, stretched in a jump. [step] hops him a
 * pixel while walking.
 */
object Critter {
    const val W = 48
    const val H = 44

    private val INK = rgb(0x1A1018)
    private val WHITE = rgb(0xFFFBEA)
    private val PINK = rgb(0xF29AA0)
    private val SWEAT = rgb(0x8AD0F0)
    private val PLAID_A = rgb(0xC0403A)
    private val PLAID_B = rgb(0xF0E2C0)

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

    fun draw(look: Look, pose: Pose, tick: Int, petted: Boolean = false, lying: Boolean = false, squash: Double = 0.0, step: Int = 0): Buf {
        val out = Buf(W, H)
        val t = look.traits
        val sp = look.species
        val lie = lying || pose == Pose.SLEEP
        val still = Art.still
        val kind = Sprites.of(sp)
        val s = if (lie) kind.lie else kind.sit
        val fig = Buf(W, H).also { it.copyFrom(Sprites.tinted(sp, lie, look.body, look.accent, look.marks)) }
        val body = look.body or Buf.OPAQUE
        val line = mix(body, INK, .8f)
        val cx = (s.eyes[0].cx + s.eyes[1].cx) / 2
        val crown = if (lie) s.crownAt(cx.roundToInt()) else kind.crown

        // ---- What his life has grown on him, and the face of the moment. ----
        if (Trait.WANDERER in t) fig.under(line) { for ((dx, dy) in listOf(0 to -1, 1 to -2, 2 to -3, 3 to -3, 1 to -1)) p(cx.toInt() + dx, crown + dy, Sprites.lit(body, 50)) }
        if (Trait.OWL in t && sp != Species.OWL) earTufts(fig, s, cx, body, line)
        if (Trait.CUDDLY in t) fluff(fig, s, body, line)
        if (!lie) face(fig, s, look, pose, tick, petted, still)

        // ---- Breathing, squash and stretch, a round belly: the figure scaled about its feet. ----
        val breath = if (still) 0.0 else sin(tick / 22.0) * (if (lie) .05 else .025)
        val sq = squash + breath
        val fw = s.right - s.left + 1; val fh = s.bottom - s.top + 1
        val nw = (fw * (1 + sq * .5)).roundToInt() + (if (Trait.GOURMET in t) 2 else 0) - (if (pose == Pose.HUNGRY) 1 else 0)
        val nh = (fh * (1 - sq)).roundToInt().coerceAtLeast(4)
        val hop = if (step != 0) 1 else 0
        val mid = (s.left + s.right + 1) / 2.0
        val foot = s.bottom + 1 - hop
        fun mx(x: Double) = mid + (x - mid) * nw / fw
        fun my(y: Double) = foot - (s.bottom + 1 - y) * nh / fh
        val shown = Buf(W, H)
        if (nw == fw && nh == fh) shown.blit(fig, 0, -hop)
        else {
            val crop = Buf(fw, fh); crop.blit(fig, -s.left, -s.top)
            shown.blitScaled(crop, (mid - nw / 2.0).roundToInt(), foot - nh, nw, nh)
        }
        val fx = mx(cx)
        val top = my(crown.toDouble())
        val ry = nh / 2.0
        val cy = my(kind.neck.toDouble()) - ry * .42
        val rx = min(nw / 2.0, 15.0)

        // ---- Behind him: the cape and the backpack. ----
        if ("cape" in look.wear && !lie) {
            val cape = ramp(rgb(0x8A3A6A))
            val from = (cy - ry * .45).toInt()
            for (y in from until foot - 1) {
                val spread = ((y - from) * .8).toInt() + 2 + (if (!still) (sin(tick / 8.0 + y / 4.0) * 1).roundToInt() else 0)
                for (x in (fx - rx * .55 - spread).toInt()..(fx + rx * .55 + spread).toInt()) out.p(x, y, if (x < fx - rx * .55 - spread + 2) cape.l else if (x > fx + rx * .4 + spread) cape.d else cape.b)
            }
        }
        if ("backpack" in look.wear && !lie) {
            val bag = ramp(rgb(0x8A5A30))
            val bx = (fx - rx - 4).roundToInt().coerceAtLeast(0); val by = (cy - 5).toInt()
            out.block(bx, by, 11, 15, bag, 2)
            out.rect(bx + 1, by + 6, 9, 3, bag.d); out.rect(bx + 1, by + 6, 9, 1, bag.l); out.p(bx + 5, by + 7, rgb(0xE8B850))
            out.ring(bx, by, 11, 15, bag.dd)
        }
        out.blit(shown, 0, 0)

        // ---- In front: the neck, the blanket, the eyes, the hat. ----
        Wear.neck(out, look, pose, fx, cy, rx, ry, lie, shown, tick)
        if (pose == Pose.COLD && !lie) {
            val from = my(s.eyes.maxOf { it.y1 } + 2.0).toInt()
            for (y in from until H) for (x in 0 until W) if (shown.filled(x, y) && y > from + (abs(x - fx) / 3.5).toInt()) {
                val c = if (((x / 3) + (y / 3)) % 2 == 0) PLAID_A else PLAID_B
                out.p(x, y, if ((x + y) % 5 == 0) mix(c, INK, .2f) else c)
            }
            out.line((fx - 12).toInt(), from + 5, fx.toInt(), from + 1, mix(PLAID_A, INK, .4f))
        }
        if (!lie) {
            val ey = my(s.eyes[0].cy).roundToInt()
            Wear.eyes(out, look, pose, s.eyes.map { (mx(it.cx) - 1.5).roundToInt() }, ey)
            if (pose == Pose.HOT) {
                val x = mx(s.eyes[1].x1 + 2.0).roundToInt(); val y = my(s.eyes[1].y0 - 3.0).roundToInt()
                out.p(x, y - 1, SWEAT); out.rect(x - 1, y, 3, 2, SWEAT); out.p(x, y + 2, mix(SWEAT, WHITE, .5f))
            }
        }
        Wear.head(out, look, pose, fx, top, false, tick)
        if (pose == Pose.COLD && !still && tick % 6 < 3) return out.shifted(1)
        return out
    }

    /** Draws [fn] on a scratch layer, rings it with [line] and slips it under what is here. */
    private inline fun Buf.under(line: Int, fn: Buf.() -> Unit) {
        val b = Buf(w, h)
        b.fn()
        b.outline(line)
        b.blit(this, 0, 0)
        copyFrom(b)
    }

    /** A night owl's tufts: a dark wisp from the tip of each ear. */
    private fun earTufts(fig: Buf, s: Sprite, cx: Double, body: Int, line: Int) {
        val c = mix(body, INK, .45f)
        fig.under(line) {
            for (side in listOf(-1, 1)) {
                val xs = if (side < 0) s.left until (cx - 2).toInt() else (cx + 2).toInt()..s.right
                val tip = xs.minByOrNull { s.crownAt(it) } ?: continue
                val y = s.crownAt(tip)
                p(tip, y - 1, c); p(tip + side, y - 2, c); p(tip + side, y - 3, c)
            }
        }
    }

    /** Extra fluff: little tufts sticking out of his flanks. */
    private fun fluff(fig: Buf, s: Sprite, body: Int, line: Int) {
        val c = Sprites.lit(body, 20)
        fig.under(line) {
            for (y in (s.top + (s.bottom - s.top) / 3) until s.bottom - 3 step 4) for (side in listOf(-1, 1)) {
                var x = if (side < 0) 0 else W - 1
                while (x in 0 until W && !s.filled(x, y)) x -= side
                if (x !in 0 until W) continue
                p(x + side, y, c); p(x + side, y + 1, c); p(x + side * 2, y, c)
            }
        }
    }

    /** The colour round an eye, to paint it shut with. */
    private fun skin(fig: Buf, s: Sprite, e: Box): Int {
        val seen = HashMap<Int, Int>()
        for (y in e.y0 - 2 until e.y1 + 2) for (x in e.x0 - 2 until e.x1 + 2) {
            if (x in e.x0 until e.x1 && y in e.y0 until e.y1) continue
            val k = s.kind(x, y)
            // The ring right next to the eye counts most.
            val near = x in e.x0 - 1..e.x1 && y in e.y0 - 1..e.y1
            if (k == 1 || k == 2) seen.merge(fig.get(x, y), if (near) 3 else 1, Int::plus)
        }
        return seen.maxByOrNull { it.value }?.key ?: fig.get(e.x0 - 1, e.y0 - 1)
    }

    private fun face(fig: Buf, s: Sprite, look: Look, pose: Pose, tick: Int, petted: Boolean, still: Boolean) {
        val t = look.traits
        val ink = mix(look.body or Buf.OPAQUE, INK, .85f)
        val bp = if (still) -1 else tick % 170
        val lid = when (bp) { 0, 1, 6, 7 -> 1; 2, 3, 4, 5 -> 2; else -> 0 }
        val happy = petted || pose == Pose.WARM || (pose == Pose.SUN && "goggles" !in look.wear)
        for ((k, e) in s.eyes.withIndex()) {
            val sk = skin(fig, s, e)
            val w = e.x1 - e.x0
            val ex = e.cx.roundToInt(); val ey = e.cy.roundToInt()
            val half = max(2, w / 2)
            fun shut(rows: IntRange = e.y0 - 1..e.y1) { for (y in rows) for (x in e.x0 - 1..e.x1) if (s.kind(x, y) == 3) fig.p(x, y, sk) }
            when {
                pose == Pose.DIZZY -> {
                    shut()
                    fig.ring(ex - 2, ey - 2, 5, 5, ink); fig.p(ex, ey, ink)
                    val st = listOf(0 to 0, 2 to 0, 4 to 0, 4 to 2, 4 to 4, 2 to 4, 0 to 4, 0 to 2)[(tick / 5) % 8]
                    fig.p(ex - 2 + st.first, ey - 2 + st.second, WHITE)
                }
                happy -> {
                    shut()
                    fig.line(ex - half, ey + 1, ex, ey - 1, ink); fig.line(ex, ey - 1, ex + half, ey + 1, ink)
                }
                lid == 2 -> {
                    shut()
                    fig.p(ex - half, ey, ink); fig.rect(ex - half + 1, ey + 1, 2 * half - 1, 1, ink); fig.p(ex + half, ey, ink)
                }
                lid == 1 -> {
                    shut(e.y0 - 1 until ey)
                    for (x in e.x0 until e.x1) if (s.kind(x, ey) == 3) fig.p(x, ey, ink)
                }
            }
            if (pose == Pose.HUNGRY) {
                // Worried brows, raised towards the middle of the face.
                val inner = if (k == 0) 1 else -1
                val ox = if (k == 0) e.x0 else e.x1 - 1
                fig.line(ox, e.y0 - 2, ox + inner * (w - 1), e.y0 - 3, ink)
            }
            if (Trait.SLEEPYHEAD in t || pose == Pose.WARM || pose == Pose.HOT || petted) {
                val outward = if (k == 0) -1 else 1
                for (j in 0..1) for (i in -1..w) {
                    val x = e.x0 + i + outward; val y = e.y1 + 1 + j
                    val kk = s.kind(x, y)
                    if ((kk == 1 || kk == 2) && dith(x, y, .55)) fig.p(x, y, PINK)
                }
            }
        }
    }

    private fun Buf.shifted(dx: Int): Buf {
        val o = Buf(w, h)
        o.blit(this, dx, 0)
        return o
    }
}
