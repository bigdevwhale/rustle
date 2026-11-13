package app.rustle.ui.art

/**
 * Pictograms for the gauges, the tabs and the action tiles, 24×24: drawn with image generation (tools/gen.py)
 * and shrunk by tools/art.py into the "icons" atlas, in the same style as the finds.
 */
object Icons {
    const val S = 24
    private val INK = rgb(0x1B140E)
    private val EMPTY = rgb(0x2E3A36)
    private val cache = HashMap<String, Buf>()

    private fun icon(key: String, group: String = "icons", empty: Boolean = false): Buf = synchronized(cache) {
        cache.getOrPut("$group/$key/$empty") {
            val b = Atlas.get(group, key) ?: Buf(S, S).also { it.ell(12, 12, 8, 8, rgb(0xC8A060)); it.outline(INK) }
            if (empty) { b.silhouette(EMPTY); b.hollow(); b.outline(INK) }
            b
        }
    }

    /** Lights the rim of an empty gauge pip so it reads as a hollow. */
    private fun Buf.hollow() {
        for (y in 0 until h) for (x in 0 until w) if (filled(x, y) && (!filled(x - 1, y) || !filled(x, y - 1))) p(x, y, rgb(0x46564F))
    }

    /** A glossy heart; an empty one is a dark hollow. */
    fun heart(full: Boolean) = icon("heart", empty = !full)

    /** A rusk for the hunger gauge; an empty one is a dark crumb. */
    fun crumb(full: Boolean) = icon("rusk", group = "items", empty = !full)

    val bowl get() = icon("bowl")
    val maze get() = icon("maze")
    val moon get() = icon("moon")
    val sun get() = icon("sun")
    val pack get() = icon("pack")
    val chest get() = icon("chest")
    val tools get() = icon("tools")
    val home get() = icon("home")
}
