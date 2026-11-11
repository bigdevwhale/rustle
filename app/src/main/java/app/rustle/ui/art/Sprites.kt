package app.rustle.ui.art

import app.rustle.domain.Marks
import app.rustle.domain.Species
import java.io.DataInputStream
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/** An eye's box in sprite pixels, ends exclusive. */
data class Box(val x0: Int, val y0: Int, val x1: Int, val y1: Int) {
    val cx get() = (x0 + x1 - 1) / 2.0
    val cy get() = (y0 + y1 - 1) / 2.0
}

/**
 * The drawn kinds: each one sitting and curled up asleep, cut by tools/cut.py from sheets drawn with image
 * generation. Every pixel knows whether it is fur, the second colour or an eye, and how light it is, so
 * the fur takes the owner's colours and the face can blink, smile and squeeze its eyes shut.
 */
internal class Sprite(
    val px: IntArray,
    /** 0 as drawn, 1 fur, 2 second colour, 3 an eye. */
    val cls: ByteArray,
    /** How light the fur is here against its colour: −127..127, the colour itself at 0. */
    val shade: ByteArray,
    val eyes: List<Box>,
) {
    val left: Int; val right: Int; val top: Int; val bottom: Int
    init {
        var l = W; var r = -1; var t = H; var b = -1
        for (y in 0 until H) for (x in 0 until W) if (px[y * W + x] ushr 24 != 0) { if (x < l) l = x; if (x > r) r = x; if (y < t) t = y; if (y > b) b = y }
        left = l; right = r; top = t; bottom = b
    }

    fun filled(x: Int, y: Int) = x in 0 until W && y in 0 until H && px[y * W + x] ushr 24 != 0
    fun kind(x: Int, y: Int) = if (x in 0 until W && y in 0 until H) cls[y * W + x].toInt() else 0

    /** The topmost row filled in column [x]. */
    fun crownAt(x: Int) = (0 until H).firstOrNull { filled(x, it) } ?: top

    companion object { const val W = 48; const val H = 44 }
}

internal class Kind(val sit: Sprite, val lie: Sprite, val crown: Int, val neck: Int)

internal object Sprites {
    private val kinds = HashMap<Species, Kind>()

    private val faces: Map<String, List<String>> by lazy {
        (Sprites::class.java.getResourceAsStream("/pets/faces.txt")?.bufferedReader()?.use { it.readText() }.orEmpty())
            .lines().filter { it.isNotBlank() && !it.startsWith("#") }.associate { l -> l.trim().split(Regex("\\s+")).let { it[0] to it.drop(1) } }
    }

    fun of(sp: Species): Kind = synchronized(kinds) { kinds.getOrPut(sp) { load(sp) } }

    private fun load(sp: Species): Kind {
        val f = faces[sp.name] ?: error("no face for $sp")
        val n = f.map { it.toIntOrNull() }
        fun box(i: Int) = Box(n[i]!!, n[i + 1]!!, n[i + 2]!!, n[i + 3]!!)
        val stream = Sprites::class.java.getResourceAsStream("/pets/${sp.name.lowercase()}.bin") ?: error("no sprite for $sp")
        val size = Sprite.W * Sprite.H
        val (sit, lie) = DataInputStream(stream.buffered()).use { inp ->
            List(2) { k ->
                val px = IntArray(size) { inp.readInt() }
                val cls = ByteArray(size).also { inp.readFully(it) }
                val shade = ByteArray(size).also { inp.readFully(it) }
                Sprite(px, cls, shade, if (k == 0) listOf(box(0), box(4)) else listOf(box(11), box(15)))
            }
        }
        return Kind(sit, lie, n[8]!!, n[9]!!)
    }

    // ---- In the owner's colours. ----

    private data class Key(val sp: Species, val lie: Boolean, val body: Int, val accent: Int, val marks: Marks)
    private val tinted = object : LinkedHashMap<Key, Buf>(16, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, Buf>?) = size > 24
    }

    private val LIGHT = rgb(0xFFF0C8)
    private val SHADE = rgb(0x3A2A5A)
    private val WHITE = rgb(0xFFFBEA)
    private val INK = rgb(0x1A1018)

    /** A colour lit to [s]: the colour itself at 0, towards warm light above, darker and cooler below. */
    fun lit(c: Int, s: Int): Int {
        val ratio = 2.2.pow(s / 127.0)
        return if (ratio >= 1) mix(c, LIGHT, (1 - 1 / ratio).toFloat() * .9f)
        else {
            val r = (Buf.ch(c, 16) * ratio).roundToInt(); val g = (Buf.ch(c, 8) * ratio).roundToInt(); val b = (Buf.ch(c, 0) * ratio).roundToInt()
            mix(rgb(r, g, b), SHADE, ((1 - ratio) * .3).toFloat())
        }
    }

    /** His sprite with the fur in [body] and [accent] and his pattern on it; as drawn when the colours are his kind's own. */
    fun tinted(sp: Species, lie: Boolean, body: Int, accent: Int, marks: Marks): Buf = synchronized(tinted) {
        tinted.getOrPut(Key(sp, lie, body and 0xFFFFFF, accent and 0xFFFFFF, marks)) { paint(sp, lie, body, accent, marks) }
    }

    private fun paint(sp: Species, lie: Boolean, body: Int, accent: Int, marks: Marks): Buf {
        val kind = of(sp)
        val s = if (lie) kind.lie else kind.sit
        val own = (body and 0xFFFFFF) == sp.body && (accent and 0xFFFFFF) == sp.accent
        val out = Buf(Sprite.W, Sprite.H)
        val b = rgb(body.toLong() and 0xFFFFFF); val a = rgb(accent.toLong() and 0xFFFFFF)
        for (i in s.px.indices) {
            val c = s.px[i]
            if (c ushr 24 == 0) continue
            out.d[i] = when (s.cls[i].toInt()) {
                1 -> if (own) c else lit(b, s.shade[i].toInt())
                2 -> if (own) c else lit(a, s.shade[i].toInt())
                else -> c
            }
        }
        if (!(lie && marks == Marks.SOCKS)) pattern(out, s, marks, b, a)
        return out
    }

    /** Spots, tabby stripes, a patch round one eye, a mask across both, white socks: over the fur only. */
    private fun pattern(out: Buf, s: Sprite, marks: Marks, body: Int, accent: Int) {
        if (marks == Marks.NONE) return
        val dark = mix(body, INK, .42f)
        val fur = { x: Int, y: Int -> s.kind(x, y) == 1 }
        val furry = { x: Int, y: Int -> s.kind(x, y) == 1 || s.kind(x, y) == 2 }
        fun over(x: Int, y: Int, c: Int) { val i = y * Sprite.W + x; out.d[i] = lit(c, s.shade[i].toInt() / 2) }
        val cx = (s.eyes[0].cx + s.eyes[1].cx) / 2
        val cy = (s.top + s.bottom) / 2.0
        val rx = (s.right - s.left) / 2.0; val ry = (s.bottom - s.top) / 2.0
        when (marks) {
            Marks.SPOTS -> for ((dx, dy, r) in listOf(Triple(-.55, -.3, 2), Triple(.5, -.45, 1), Triple(.65, .15, 2), Triple(-.7, .25, 1), Triple(.2, -.7, 1), Triple(-.25, .55, 1))) {
                val x0 = (cx + dx * rx).roundToInt(); val y0 = (cy + dy * ry).roundToInt()
                for (j in -r..r) for (i in -r..r) if (i * i + j * j <= r * r + 1 && fur(x0 + i, y0 + j)) over(x0 + i, y0 + j, dark)
            }
            Marks.STRIPES -> {
                val top = s.eyes.minOf { it.y0 }
                val x0 = cx.roundToInt()
                // Three on the brow, above the eyes, the middle one longest.
                for (k in -1..1) {
                    val crown = (s.crownAt(x0 + k * 3)..top).firstOrNull { fur(x0 + k * 3, it) } ?: continue
                    for (j in 0 until (if (k == 0) 5 else 3)) if (crown + 1 + j < top - 1 && fur(x0 + k * 3, crown + 1 + j)) over(x0 + k * 3, crown + 1 + j, dark)
                }
                // Bands down the flanks.
                for (y in s.top until s.bottom) {
                    if ((y - s.top) % 5 > 1) continue
                    for (side in listOf(-1, 1)) {
                        var x = if (side < 0) s.left else s.right
                        while (x in 0 until Sprite.W && !fur(x, y)) x -= side
                        for (k in 0 until 3) if (fur(x - side * k, y)) over(x - side * k, y, dark)
                    }
                }
            }
            Marks.PATCH -> {
                val e = s.eyes[1]
                for (y in 0 until Sprite.H) for (x in 0 until Sprite.W) {
                    val dx = (x - e.cx) / ((e.x1 - e.x0) / 2.0 + 2.5); val dy = (y - e.cy) / ((e.y1 - e.y0) / 2.0 + 2.5)
                    if (dx * dx + dy * dy + sin(x * 1.7) * .08 <= 1 && furry(x, y)) over(x, y, dark)
                }
            }
            Marks.MASK -> {
                val y0 = s.eyes.minOf { it.y0 } - 1; val y1 = s.eyes.maxOf { it.y1 }
                val x0 = s.eyes[0].x0 - 3; val x1 = s.eyes[1].x1 + 2
                for (y in y0..y1) for (x in x0..x1) if (furry(x, y) && abs(x - cx) >= 1.5) over(x, y, dark)
            }
            Marks.SOCKS -> {
                val sock = mix(WHITE, accent, .25f)
                for (y in s.bottom - 4..s.bottom) for (x in 0 until Sprite.W) if (furry(x, y)) over(x, y, sock)
            }
            Marks.NONE -> Unit
        }
    }
}
