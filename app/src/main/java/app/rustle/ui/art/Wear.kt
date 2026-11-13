package app.rustle.ui.art

import app.rustle.domain.Pose
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** What he wears, drawn over him: hats on the crown, things round the neck, things over the eyes. */
internal object Wear {
    private val INK = rgb(0x1A1018)
    private val WHITE = rgb(0xFFFBEA)
    private val GOLD = ramp(rgb(0xE8B840))
    private val RED = ramp(rgb(0xC8403A))
    private val BLUE = ramp(rgb(0x4A6AC8))
    private val NAVY = ramp(rgb(0x2A3A7A))
    private val BLACK = ramp(rgb(0x2A2830))
    private val BROWN = ramp(rgb(0x8A5A30))
    private val GREEN = ramp(rgb(0x5AA050))
    private val PURPLE = ramp(rgb(0x7A4AA8))

    /** A hat on the crown; [top] is the top of his head. */
    fun head(out: Buf, look: Look, pose: Pose, cx: Double, top: Double, lie: Boolean, tick: Int) {
        val x = cx.roundToInt(); val y = top.roundToInt() + (if (lie) 2 else 0)
        val hat = look.wear.firstOrNull { it in HATS } ?: return
        val b = Buf(out.w, out.h)
        when (hat) {
            "cap" -> {
                for (i in 0 until 13) {
                    val w = 12 - i
                    val c = if ((i / 2) % 2 == 0) BLUE else ramp(rgb(0xE8E0D0))
                    b.rect(x - 6 + i, y + 1 - i / 2, w, 2, if (i < 2) c.l else c.b); b.p(x - 6 + i + w - 1, y + 2 - i / 2, c.d)
                }
                b.ell(x + 8, y - 6, 2, 2, WHITE); b.p(x + 7, y - 7, rgb(0xFFFFFF))
                b.rect(x - 7, y + 2, 15, 2, ramp(rgb(0xE8E0D0)).b); b.rect(x - 7, y + 3, 15, 1, ramp(rgb(0xE8E0D0)).d)
            }
            "beret" -> {
                b.ell(x + 1, y + 1, 9, 3, RED.b); b.ell(x, y, 8, 2, RED.l); b.rect(x - 8, y + 3, 18, 1, RED.d)
                b.rect(x, y - 3, 1, 2, RED.d); b.p(x - 4, y - 1, RED.hi)
            }
            "bobble" -> {
                for (j in 0 until 7) { val half = (8.5 * kotlin.math.sqrt(1 - (j / 7.0) * (j / 7.0))).toInt(); b.rect(x - half, y + 3 - j, 2 * half + 1, 1, if (j > 4) GREEN.l else GREEN.b) }
                for (i in -8..8 step 2) b.rect(x + i, y + 2, 1, 3, GREEN.d)
                b.rect(x - 9, y + 3, 19, 3, ramp(rgb(0xE8E0D0)).b); b.rect(x - 9, y + 3, 19, 1, ramp(rgb(0xE8E0D0)).l)
                b.ell(x, y - 5, 3, 3, ramp(rgb(0xE8E0D0)).b); b.p(x - 1, y - 6, WHITE)
            }
            "acorn" -> {
                for (j in 0 until 5) { val half = (9 * kotlin.math.sqrt(1 - (j / 5.0) * (j / 5.0))).toInt(); b.rect(x - half, y + 2 - j, 2 * half + 1, 1, BROWN.b) }
                for (j in 0 until 5) for (i in -8..8) if ((i + j * 2) % 4 == 0 && b.filled(x + i, y + 2 - j)) b.p(x + i, y + 2 - j, BROWN.d)
                b.rect(x - 9, y + 2, 19, 1, BROWN.dd); b.rect(x, y - 5, 1, 3, BROWN.d); b.p(x + 1, y - 5, BROWN.l)
            }
            "crown" -> {
                b.rect(x - 7, y - 1, 15, 4, GOLD.b); b.rect(x - 7, y - 1, 15, 1, GOLD.l); b.rect(x - 7, y + 2, 15, 1, GOLD.d)
                for (k in -1..1) for (j in 1..4) { val half = (4 - j + 1) / 2; b.rect(x + k * 6 - half, y - 1 - j, 2 * half + 1, 1, if (j == 1) GOLD.l else GOLD.b) }
                for (k in -1..1) b.p(x + k * 6, y - 5, GOLD.hi)
                b.p(x - 3, y, RED.l); b.p(x, y, ramp(rgb(0x4AA8E8)).l); b.p(x + 3, y, GREEN.l)
            }
            "wreath" -> {
                for (a in 0 until 360 step 20) {
                    val rad = a * PI / 180
                    val px = x + (cos(rad) * 10).roundToInt(); val py = y + 1 + (sin(rad) * 2.5).roundToInt()
                    b.p(px, py, GREEN.b)
                    if (a % 40 == 0) { val c = listOf(rgb(0xF07AA0), rgb(0xF0D050), WHITE, rgb(0x9AA8F0))[(a / 40) % 4]; b.rect(px - 1, py - 1, 2, 2, c); b.p(px, py, rgb(0xF0C040)) }
                }
            }
            "bow" -> {
                val bx = x + 7; val by = y + 1
                b.ell(bx - 3, by, 3, 2, RED.b); b.ell(bx + 3, by, 3, 2, RED.b); b.rect(bx - 1, by - 1, 3, 3, RED.d)
                b.p(bx - 4, by - 1, RED.hi); b.p(bx + 2, by - 1, RED.l)
                b.line(bx - 1, by + 2, bx - 3, by + 5, RED.b); b.line(bx + 1, by + 2, bx + 3, by + 5, RED.b)
            }
            "tophat" -> {
                b.rect(x - 8, y + 1, 17, 2, BLACK.b); b.rect(x - 8, y + 1, 17, 1, BLACK.l)
                b.rect(x - 5, y - 10, 11, 11, BLACK.b); b.rect(x - 5, y - 10, 2, 11, BLACK.l); b.rect(x + 4, y - 10, 2, 11, BLACK.dd)
                b.rect(x - 5, y - 3, 11, 2, RED.b); b.rect(x - 5, y - 10, 11, 1, BLACK.l); b.p(x - 4, y - 9, BLACK.hi)
            }
            "bandana" -> {
                for (j in 0 until 6) b.rect(x - 9 + j, y + 3 - j, 19 - 2 * j, 1, RED.b)
                b.rect(x - 10, y + 3, 21, 2, RED.d)
                for ((dx, dy) in listOf(-5 to 2, 0 to 0, 4 to 2, -2 to 3, 2 to -1)) b.p(x + dx, y + dy, WHITE)
                b.ell(x + 10, y + 5, 1, 1, RED.b); b.line(x + 11, y + 6, x + 13, y + 9, RED.b); b.line(x + 10, y + 6, x + 11, y + 10, RED.d)
            }
            "wizard" -> {
                for (j in 0 until 15) { val half = (7 - j / 2.2).toInt().coerceAtLeast(0); b.rect(x - half + j / 5, y - j, 2 * half + 1, 1, if (j % 5 == 0) PURPLE.l else PURPLE.b) }
                b.rect(x - 10, y + 1, 21, 2, PURPLE.d); b.rect(x - 10, y + 1, 21, 1, PURPLE.l)
                for ((dx, dy) in listOf(-2 to -3, 2 to -7, 0 to -11)) { b.p(x + dx, y + dy, GOLD.hi); b.p(x + dx - 1, y + dy, GOLD.b, .6f); b.p(x + dx + 1, y + dy, GOLD.b, .6f) }
            }
            "propeller" -> {
                for (j in 0 until 5) { val half = (8 * kotlin.math.sqrt(1 - (j / 5.0) * (j / 5.0))).toInt(); for (i in -half..half) b.p(x + i, y + 2 - j, if ((i + 8) / 4 % 2 == 0) RED.b else BLUE.b) }
                b.rect(x - 9, y + 2, 19, 1, BLACK.b); b.rect(x + 2, y + 2, 7, 1, BLACK.d)
                b.rect(x, y - 5, 1, 3, BLACK.b)
                val spin = if (Art.still) 0.0 else tick / 2.0
                val len = (abs(cos(spin)) * 6).roundToInt()
                b.rect(x - len, y - 6, 2 * len + 1, 1, GOLD.b); b.p(x, y - 6, GOLD.hi)
            }
        }
        b.outline(mix(INK, rgb(0x3A2A20), .3f))
        out.blit(b, 0, 0)
    }

    /** Round the neck: the scarf, a bow tie, a bell on a ribbon, beads. */
    fun neck(out: Buf, look: Look, pose: Pose, cx: Double, cy: Double, rx: Double, ry: Double, lie: Boolean, body: Buf, tick: Int, neckHalf: Double = rx) {
        if (lie || pose == Pose.SLEEP) return
        val sy = (cy + ry * .42).roundToInt()
        val x = cx.roundToInt()
        // Only the neck itself: the run of his body through the middle of the row, not a tail or a wing
        // that happens to reach the same height (#6).
        fun neckRun(y: Int, rows: Int): IntRange {
            fun on(i: Int) = (0 until rows).all { body.filled(i, y + it) }
            var l = x; var r = x
            if (!on(x)) return x..x - 1
            val half = neckHalf.roundToInt().coerceAtLeast(6)
            while (l - 1 >= 0 && x - (l - 1) <= half && on(l - 1)) l--
            while (r + 1 < out.w && (r + 1) - x <= half && on(r + 1)) r++
            return l..r
        }
        if ("cape" in look.wear) {
            // The cape's collar and clasp show in front.
            val cape = ramp(rgb(0x8A3A6A))
            for (side in listOf(-1, 1)) for (k in 0 until 5) out.rect((cx + side * (rx * .55 - k)).roundToInt() - (if (side < 0) 0 else 2), sy - 2 + k, 3, 1, if (k < 2) cape.l else cape.b)
            out.ell(x, sy + 2, 1, 1, GOLD.b); out.p(x - 1, sy + 1, GOLD.hi)
        }
        when {
            "scarf" in look.wear -> {
                for (i in neckRun(sy, 3)) { out.p(i, sy, if ((i / 2) % 3 == 0) RED.l else RED.b); out.p(i, sy + 1, RED.b); out.p(i, sy + 2, RED.d) }
                val tx = (cx - 10).toInt()
                for (k in 0 until 5) { out.rect(tx - k / 2, sy + 2 + k, 3, 1, if (k % 2 == 0) RED.b else RED.l); out.p(tx - k / 2 + 2, sy + 2 + k, RED.d) }
                out.rect(tx - 2, sy + 7, 3, 1, RED.d)
            }
            "bowtie" in look.wear -> {
                for (j in -2..2) { out.rect(x - 5, sy + j, 4 - abs(j), 1, RED.b); out.rect(x + 2 + abs(j), sy + j, 4 - abs(j), 1, RED.d) }
                out.rect(x - 1, sy - 1, 3, 3, RED.dd); out.p(x - 4, sy - 1, RED.hi)
            }
            "bell" in look.wear -> {
                for (i in neckRun(sy, 2)) out.p(i, sy, BLUE.b)
                val swing = if (Art.still) 0 else (sin(tick / 7.0) * 1.2).roundToInt()
                out.ell(x + swing, sy + 3, 2, 2, GOLD.b); out.p(x - 1 + swing, sy + 2, GOLD.hi); out.p(x + swing, sy + 5, GOLD.dd); out.rect(x - 2 + swing, sy + 4, 5, 1, GOLD.d)
            }
            "beads" in look.wear -> {
                val cols = listOf(RED.l, GOLD.l, BLUE.l, GREEN.l, WHITE)
                var k = 0
                for (i in (cx - rx * .6).toInt()..(cx + rx * .6).toInt() step 2) {
                    val dy = ((i - cx) * (i - cx) / (rx * rx) * -4 + 4).roundToInt()
                    out.rect(i, sy + dy - 2, 2, 2, cols[k % cols.size]); out.p(i, sy + dy - 2, WHITE, .6f); k++
                }
            }
        }
    }

    /** Over the eyes: goggles, round glasses or a monocle on a chain. */
    fun eyes(out: Buf, look: Look, pose: Pose, eyes: List<Int>, ey: Int) {
        val frame = rgb(0xB89048)
        when {
            "goggles" in look.wear -> {
                val gy = if (pose == Pose.SUN) ey - 3 else ey - 10
                val lens = if (pose == Pose.SUN) rgb(0x30405A) else rgb(0x7AB8E0)
                out.rect(eyes[0] - 4, gy + 2, eyes[1] - eyes[0] + 11, 1, rgb(0x6A4A2A))
                for (x0 in eyes) { out.ell(x0 + 1, gy + 2, 3, 3, frame); out.ell(x0 + 1, gy + 2, 2, 2, lens); out.p(x0, gy + 1, WHITE) }
            }
            "glasses" in look.wear -> {
                for (x0 in eyes) { out.ring(x0 - 2, ey - 4, 8, 8, BLACK.b); out.p(x0 - 1, ey - 3, WHITE, .6f) }
                out.rect(eyes[0] + 6, ey - 2, eyes[1] - eyes[0] - 8, 1, BLACK.b)
            }
            "monocle" in look.wear -> {
                val x0 = eyes[1]
                for (a in 0 until 360 step 20) { val rad = a * PI / 180; out.p(x0 + 1 + (cos(rad) * 4).roundToInt(), ey + (sin(rad) * 4).roundToInt(), GOLD.b) }
                out.p(x0 - 1, ey - 2, WHITE, .7f)
                for (k in 0 until 6) out.p(x0 + 4 + k / 2, ey + 4 + k, GOLD.d)
            }
        }
    }

    val HATS = setOf("cap", "beret", "bobble", "acorn", "crown", "wreath", "bow", "tophat", "bandana", "wizard", "propeller")
}
