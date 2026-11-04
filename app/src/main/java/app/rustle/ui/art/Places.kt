package app.rustle.ui.art

import app.rustle.domain.District
import kotlin.math.sin

/** A small picture of each district for the outings list, 64×28; locked ones are drawn in shadow. */
object Places {
    const val W = 64
    const val H = 28
    private val cache = HashMap<String, Buf>()

    fun of(d: District, open: Boolean): Buf = cache.getOrPut(d.name + open) {
        val b = Buf(W, H)
        when (d) {
            District.PORT -> port(b)
            District.CELLAR -> cellar(b)
            District.BRIDGE -> bridge(b)
            District.GORGE -> gorge(b)
            District.DOME -> dome(b)
        }
        b.ring(0, 0, W, H, rgb(0x1B140E))
        if (!open) for (i in b.d.indices) { val c = b.d[i]; b.d[i] = mix(c, rgb(0x0E1A16), .72f) }
        b
    }

    /** A USB-C mouth in a metal wall, light from the street coming in. */
    private fun port(b: Buf) {
        b.rect(0, 0, W, H, rgb(0x5A606A))
        for (y in 0 until H) for (x in 0 until W) if (dith(x, y, .15)) b.p(x, y, rgb(0x4A505A))
        b.rect(16, 8, 32, 12, rgb(0x16181C)); b.rect(18, 7, 28, 1, rgb(0x16181C)); b.rect(18, 20, 28, 1, rgb(0x16181C))
        b.rect(20, 10, 24, 8, rgb(0xFFE8B0)); for (x in 22 until 42 step 3) b.rect(x, 13, 2, 2, rgb(0xC8A040))
        for (y in 18 until H) for (x in 24 until 40) if (dith(x, y, .3)) b.p(x, y, rgb(0xFFE8B0), .5f)
        b.rect(0, H - 4, W, 4, rgb(0x6A4222)); b.rect(0, H - 4, W, 1, rgb(0xE0954A))
        b.p(8, 5, rgb(0xE0B050)); b.line(6, 20, 12, 22, rgb(0xC8CED6))
    }

    /** Rows of warm battery cells in the dark, raisins drying on a string. */
    private fun cellar(b: Buf) {
        b.rect(0, 0, W, H, rgb(0x1A120C))
        for (i in 0 until 6) {
            val x = 3 + i * 10
            b.rect(x, 6, 8, 18, rgb(0x3A3E46)); b.rect(x, 6, 8, 2, rgb(0x6A707A)); b.rect(x + 1, 12, 6, 4, rgb(0x2E7A5A))
            b.rect(x + 2, 3, 4, 3, rgb(0xC8A040))
        }
        for (y in 0 until H) for (x in 0 until W) { val d = ((x - 32) * (x - 32) + (y - 16) * (y - 16)) / 900f; if (d < 1) b.p(x, y, rgb(0xFF9A40), (1 - d) * .35f) }
        b.line(0, 2, W, 4, rgb(0x8A5A30)); for (x in 6 until W step 9) b.ell(x, 4 + x / 30, 1, 1, rgb(0x6A3050))
        b.rect(0, H - 3, W, 3, rgb(0x3A2410))
    }

    /** A flat ribbon cable across a dark gap between two boards. */
    private fun bridge(b: Buf) {
        b.rect(0, 0, W, H, rgb(0x0A120E))
        b.rect(0, 0, 14, H, rgb(0x1E4A3A)); b.rect(W - 14, 0, 14, H, rgb(0x1E4A3A))
        for (y in 0 until H step 4) { b.rect(0, y, 14, 1, rgb(0x7A6A34)); b.rect(W - 14, y + 2, 14, 1, rgb(0x7A6A34)) }
        for (x in 10 until W - 10) {
            val y = 12 + (sin((x - 10) / (W - 20.0) * Math.PI) * 4).toInt()
            b.rect(x, y, 1, 3, rgb(0xC8862A)); if (x % 3 == 0) b.rect(x, y, 1, 3, rgb(0xE0A848))
        }
        b.line(10, 8, W - 10, 8, rgb(0x8A5A30)); b.p(31, 9, rgb(0xF4F4F0)); b.p(32, 10, rgb(0x9A7450))
    }

    /** The speaker: a round mesh and rings of sound. */
    private fun gorge(b: Buf) {
        b.rect(0, 0, W, H, rgb(0x24262C))
        b.ell(32, 14, 12, 12, rgb(0x16181C)); b.ell(32, 14, 10, 10, rgb(0x3A3E46))
        for (y in 4..24 step 2) for (x in 22..42 step 2) if ((x - 32) * (x - 32) + (y - 14) * (y - 14) < 90) b.p(x, y, rgb(0x16181C))
        for (r in listOf(15, 19, 23)) for (a in -40..40 step 6) {
            val rad = a * Math.PI / 180
            b.p(32 + (kotlin.math.cos(rad) * r).toInt(), 14 + (kotlin.math.sin(rad) * r).toInt(), rgb(0xE070C0), .7f)
            b.p(32 - (kotlin.math.cos(rad) * r).toInt(), 14 + (kotlin.math.sin(rad) * r).toInt(), rgb(0xE070C0), .7f)
        }
        b.p(10, 6, rgb(0xF8D050)); b.p(54, 22, rgb(0xF8D050)); b.p(50, 5, rgb(0xE070C0))
    }

    /** Under the camera glass: the night sky with a moon. */
    private fun dome(b: Buf) {
        for (y in 0 until H) b.rect(0, y, W, 1, mix(rgb(0x101830), rgb(0x3A3A70), y / H.toFloat()))
        for ((x, y) in listOf(6 to 5, 14 to 12, 22 to 4, 40 to 8, 50 to 3, 58 to 13, 30 to 15, 46 to 18)) b.p(x, y, rgb(0xFFFFFF))
        b.ell(48, 10, 5, 5, rgb(0xF4ECC8)); b.ell(50, 8, 4, 4, rgb(0x1A2040))
        b.rect(0, H - 6, W, 6, rgb(0x2A2E36)); b.ell(32, H - 6, 18, 3, rgb(0x4A525E)); b.ell(32, H - 6, 14, 2, rgb(0x5A7AA0))
    }
}
