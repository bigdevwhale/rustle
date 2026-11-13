package app.rustle.ui.art

import app.rustle.domain.Find
import kotlin.math.abs

/**
 * The finds, 24×24: each one drawn with image generation (tools/gen.py) and shrunk by tools/art.py into the
 * "items" atlas. One not seen yet is shown as a dark shape.
 */
object Items {
    const val S = 24
    private val INK = rgb(0x1A1018)
    private val cache = HashMap<String, Buf>()

    fun of(f: Find, seen: Boolean = true): Buf = synchronized(cache) {
        cache.getOrPut(f.key + seen) {
            val b = Atlas.get("items", f.key) ?: blob(f.color)
            if (!seen) { b.silhouette(rgb(0x0E241C)); b.outline(rgb(0x285A46)) }
            else b.floorShadow()
            b
        }
    }

    /** A plain bead in the find's colour, should a picture be missing. */
    private fun blob(c: Int): Buf = Buf(S, S).also { b ->
        val r = ramp(c)
        b.ell(12, 13, 7, 7, r.b); b.ell(11, 12, 5, 5, r.l); b.p(9, 10, r.hi)
        b.outline(mix(c, INK, .82f))
    }

    /** A soft oval under the lowest row of the thing. */
    private fun Buf.floorShadow() {
        val bottom = (h - 1 downTo 0).firstOrNull { y -> (0 until w).any { filled(it, y) } } ?: return
        val xs = (0 until w).filter { filled(it, bottom) }
        if (bottom + 1 >= h || xs.isEmpty()) return
        val cx = (xs.first() + xs.last()) / 2.0; val rx = (xs.last() - xs.first()) / 2.0 + 2
        for (x in 0 until w) { val d = abs(x + .5 - cx) / rx; if (d < 1 && !filled(x, bottom + 1)) p(x, bottom + 1, rgb(0x140C22), .28f * (1 - d * d).toFloat()) }
    }
}
