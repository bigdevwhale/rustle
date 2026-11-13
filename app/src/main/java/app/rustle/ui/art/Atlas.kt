package app.rustle.ui.art

import java.io.DataInputStream

/**
 * Pictures drawn with image generation (tools/gen.py) and shrunk into sprites by tools/art.py: the finds, the
 * icons, the outing places and the nook's wall and floor. Each group is one atlas in resources/art/<group>.bin:
 * a count, then for each sprite its key, width, height and ARGB pixels. Read once; [get] hands out a copy.
 */
object Atlas {
    private val groups = HashMap<String, Map<String, Buf>>()

    private fun group(name: String): Map<String, Buf> = synchronized(groups) {
        groups.getOrPut(name) {
            val stream = Atlas::class.java.getResourceAsStream("/art/$name.bin") ?: return@getOrPut emptyMap()
            DataInputStream(stream.buffered()).use { inp ->
                List(inp.readInt()) {
                    val key = inp.readUTF()
                    val b = Buf(inp.readInt(), inp.readInt())
                    for (i in b.d.indices) b.d[i] = inp.readInt()
                    key to b
                }.toMap()
            }
        }
    }

    /** A copy of the sprite [key] of [group], or null when there is none. */
    fun get(group: String, key: String): Buf? = group(group)[key]?.let { s -> Buf(s.w, s.h).also { it.copyFrom(s) } }

    fun has(group: String, key: String) = key in group(group)
}
