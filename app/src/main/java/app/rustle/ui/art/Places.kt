package app.rustle.ui.art

import app.rustle.domain.District

/**
 * A picture of each district for the outings list, 96×42, drawn with image generation (tools/gen.py) and shrunk
 * by tools/art.py into the "places" atlas; locked ones are shown in shadow.
 */
object Places {
    const val W = 96
    const val H = 42
    private val cache = HashMap<String, Buf>()

    fun of(d: District, open: Boolean): Buf = synchronized(cache) {
        cache.getOrPut(d.name + open) {
            val b = Atlas.get("places", d.name.lowercase()) ?: Buf(W, H).also { it.rect(0, 0, W, H, rgb(0x1E4A3A)) }
            b.ring(0, 0, b.w, b.h, rgb(0x1B140E))
            if (!open) for (i in b.d.indices) b.d[i] = mix(b.d[i], rgb(0x0E1A16), .72f)
            b
        }
    }
}
