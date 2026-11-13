package app.rustle.ui.art

import app.rustle.domain.Catalog
import app.rustle.domain.Pose
import app.rustle.domain.Wx
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Everything the nook picture needs, read from the household and the phone. */
data class NookScene(
    val look: Look,
    val pose: Pose,
    val level: Int,
    val charging: Boolean,
    /** Hour of the day with a fraction, for the sky in the camera and the light in the room. */
    val hour: Float,
    val dark: Boolean,
    val sunny: Boolean,
    val dusty: Boolean,
    val built: Set<String>,
    val shelf: List<String> = emptyList(),
    val sproutDays: Int = 0,
    val pettedTick: Int = -100,
    /** The weather outside the window, if known. */
    val wx: Wx? = null,
)

/**
 * The inside of the phone as a cosy room, 192×224. The wall is a circuit board seen up close, with
 * resistors and chips standing out of it like a town far away; a ribbon cable runs under the ceiling as
 * a beam; the battery is a cast-iron stove; the camera is a round window with curtains; the processor is
 * a dresser on silver legs; capacitors stand as barrels. Tools made in the workshop hang on a peg rack,
 * so the room fills up as the game goes on. Lit by the stove, the lamp and the window.
 */
object Nook {
    const val W = 192
    const val H = 224
    const val FLOOR = 176

    private val INK = rgb(0x1B140E)
    private val WHITE = rgb(0xFFFBEA)

    private val BOARD = ramp(rgb(0x1F4C3B))
    private val COPPER = ramp(rgb(0xD8863E))
    private val BRASS = ramp(rgb(0xD8A848))
    private val IRON = ramp(rgb(0x3E4250))
    private val STEEL = ramp(rgb(0x9AA2B0))
    private val WOOD = ramp(rgb(0x8A5430))
    private val PLANK = ramp(rgb(0x7A4A2A))
    private val KAPTON = ramp(rgb(0xC88A30))
    private val SOLDER = ramp(rgb(0x1E5A44))
    private val BLUE = ramp(rgb(0x2E5AB0))
    private val RED = ramp(rgb(0xC0443A))
    private val CREAM = ramp(rgb(0xEADCB8))
    private val ENAMEL = ramp(rgb(0x3A72C8))
    private val CHIP = ramp(rgb(0x2C2E36))
    private val GLASS = ramp(rgb(0x9CC8D0))
    private val LEAF = ramp(rgb(0x5AA050))

    private const val WIN_X = 122
    private const val WIN_Y = 56
    private const val WIN_R = 24

    private var bg: Buf? = null

    /**
     * The wall, the beam and the floor never change: drawn once. They come from the room drawn with image
     * generation ("room" atlas, tools/art.py), band by band; the port hatch in the floor goes on top. Without
     * the atlas they are drawn here.
     */
    private fun background(): Buf = bg ?: Buf(W, H).also { b ->
        val bands = listOf("beam" to 0, "wall" to 15, "skirting" to FLOOR - 24, "floor" to FLOOR).map { (k, y) -> Atlas.get("room", k) to y }
        if (bands.all { it.first != null }) {
            for ((band, y) in bands) b.blit(band!!, 0, y)
            b.rect(0, FLOOR, W, 1, rgb(0x2A160C))
            hatch(b)
        } else { wall(b); beam(b); wainscot(b); floor(b) }
        bg = b
    }

    private fun wall(b: Buf) {
        for (y in 0 until FLOOR) b.rect(0, y, W, 1, mix(rgb(0x163A2E), rgb(0x22523F), y / FLOOR.toFloat()))
        for (y in 0 until FLOOR) for (x in 0 until W) if (dith(x, y, .09)) b.p(x, y, mix(BOARD.b, BOARD.d, .5f))
        // Tracks run in pairs and turn at 45°; vias are little rings.
        val tr = mix(COPPER.d, BOARD.b, .55f); val trL = mix(COPPER.b, BOARD.b, .45f)
        fun track(x0: Int, y0: Int, len: Int, turnAt: Int, dir: Int) {
            var x = x0; var y = y0
            for (i in 0 until len) {
                b.p(x, y, tr); b.p(x, y + 3, tr); b.p(x, y + 1, mix(tr, BOARD.b, .6f))
                if (i in turnAt until turnAt + 8) y += dir
                x++
            }
            for (vx in listOf(x0 - 2, x)) { b.ell(vx, y0 + 2 + (if (vx == x) y - y0 else 0), 2, 2, trL); b.p(vx, y0 + 2 + (if (vx == x) y - y0 else 0), BOARD.dd) }
        }
        track(56, 26, 44, 12, 1); track(60, 104, 56, 20, -1); track(6, 140, 40, 26, -1); track(150, 26, 36, 8, 1); track(84, 128, 36, 8, 1); track(160, 100, 26, 6, -1)
        // Components far behind, muted like a distant town.
        fun far(c: Int) = mix(c, BOARD.b, .55f)
        for ((x, y) in listOf(66 to 60, 154 to 88, 92 to 142, 168 to 40, 42 to 92, 100 to 30)) {
            b.rect(x, y, 10, 4, far(rgb(0x202228))); b.rect(x, y, 3, 4, far(STEEL.l)); b.rect(x + 7, y, 3, 4, far(STEEL.b))
            b.rect(x + 4, y + 1, 2, 2, far(rgb(0x6A5A3A))); b.p(x + 1, y, far(WHITE))
        }
        for ((x, y) in listOf(94 to 80, 176 to 132)) {
            b.ell(x, y, 6, 6, far(BLUE.d)); b.ell(x - 1, y - 1, 5, 5, far(BLUE.b)); b.ell(x - 2, y - 2, 2, 2, far(BLUE.l))
            b.rect(x - 4, y, 9, 1, far(STEEL.l)); b.rect(x, y - 4, 1, 9, far(STEEL.l))
        }
        val cx = 166; val cy = 110
        b.rect(cx, cy, 16, 16, far(CHIP.b)); b.rect(cx, cy, 16, 1, far(CHIP.l))
        for (i in 0 until 6) { b.p(cx + 2 + i * 2, cy - 1, far(STEEL.l)); b.p(cx + 2 + i * 2, cy + 16, far(STEEL.l)); b.p(cx - 1, cy + 2 + i * 2, far(STEEL.l)); b.p(cx + 16, cy + 2 + i * 2, far(STEEL.l)) }
        b.ell(cx + 3, cy + 3, 1, 1, far(STEEL.b))
        // White silkscreen marks, the board's own handwriting.
        val silk = mix(rgb(0xE8F0E8), BOARD.b, .7f)
        for ((x, y) in listOf(66 to 66, 154 to 94, 168 to 128, 100 to 36)) { b.rect(x, y, 5, 1, silk); b.p(x + 6, y, silk); b.rect(x + 8, y, 2, 1, silk) }
        b.ring(86, 72, 16, 18, silk)
        b.ring(162, 106, 24, 24, silk)
    }

    /** A ribbon cable across the ceiling with black clamps at both ends. */
    private fun beam(b: Buf) {
        b.cylH(0, 0, W, 12, KAPTON)
        for (x in 0 until W step 3) for (y in 1 until 11) b.p(x, y, mix(KAPTON.l, COPPER.l, .5f), if (y in 3..6) .7f else .45f)
        b.rect(0, 12, W, 3, rgb(0x0C2018))
        for (x in listOf(4, W - 18)) {
            b.block(x, -1, 14, 16, CHIP, 2)
            b.rect(x + 3, 4, 8, 1, STEEL.l); b.rect(x + 3, 9, 8, 1, STEEL.d); b.rivet(x + 2, 12, STEEL); b.rivet(x + 10, 12, STEEL)
        }
    }

    /** The lower wall is a board edge: gold contact fingers under a copper rail. */
    private fun wainscot(b: Buf) {
        val top = FLOOR - 24
        b.rect(0, top, W, 24, SOLDER.d)
        for (y in top until FLOOR) for (x in 0 until W) if (dith(x, y, .12)) b.p(x, y, SOLDER.dd)
        for (x in 4 until W - 3 step 8) {
            b.rect(x, top + 7, 5, 14, BRASS.b); b.rect(x, top + 7, 1, 14, BRASS.l); b.rect(x + 4, top + 7, 1, 14, BRASS.d)
            b.rect(x + 1, top + 7, 3, 1, BRASS.hi); b.rect(x, top + 20, 5, 1, BRASS.dd)
        }
        b.cylH(0, top - 2, W, 5, COPPER)
        b.rect(0, top + 3, W, 1, SOLDER.dd)
        b.rect(0, FLOOR - 2, W, 2, rgb(0x0C1A14))
    }

    /** Copper-wood planks with grain, knots and nail heads, rows widening towards us. */
    private fun floor(b: Buf) {
        b.rect(0, FLOOR, W, H - FLOOR, PLANK.b)
        var y = FLOOR; var row = 0; var h = 6
        while (y < H) {
            val base = mix(PLANK.b, PLANK.l, (.2f - row * .04f).coerceAtLeast(0f))
            b.rect(0, y, W, h, base)
            b.rect(0, y, W, 1, mix(base, PLANK.l, .55f))
            b.grain(0, y + 1, W, h - 2, PLANK.d, row)
            b.rect(0, y + h - 1, W, 1, PLANK.dd)
            val step = 38 + row * 9
            var x = (row * 23) % step
            while (x < W) {
                b.rect(x, y, 1, h - 1, PLANK.dd); b.p(x + 1, y + 1, PLANK.l)
                b.p(x + 4, y + h / 2, STEEL.d); b.p(x + 4, y + h / 2 - 1, STEEL.l)
                x += step
            }
            if (row % 2 == 1) { val kx = (row * 61) % (W - 20) + 10; b.ell(kx, y + h / 2, 2, 1, PLANK.dd); b.p(kx, y + h / 2, PLANK.d) }
            y += h; h += 1; row++
        }
        b.rect(0, FLOOR, W, 1, rgb(0x2A160C))
        hatch(b)
    }

    /** The port hatch: a brass-rimmed slot in the floor with contacts inside. */
    private fun hatch(b: Buf) {
        val hx = 70; val hy = H - 18
        b.block(hx, hy, 52, 12, BRASS, 3)
        b.rect(hx + 3, hy + 3, 46, 6, rgb(0x0E0C12)); b.rect(hx + 6, hy + 5, 40, 2, rgb(0x2A2A34))
        for (i in 0 until 10) b.rect(hx + 8 + i * 4, hy + 5, 2, 1, BRASS.d)
        for (x in listOf(hx + 1, hx + 49)) b.rivet(x, hy + 5, BRASS)
    }

    /** Where he stands (the sprite's left edge), or −1 when he is out. */
    fun spot(s: NookScene, tick: Int): Int = when {
        s.pose == Pose.AWAY -> -1
        s.pose == Pose.SLEEP && "bed" in s.built -> 140
        s.pose == Pose.SLEEP || s.pose == Pose.COLD || s.pose == Pose.WARM -> 50
        s.pose == Pose.SUN -> 96
        s.pose == Pose.IDLE && !Art.still -> 76 + (14 * sin(tick / 240.0)).roundToInt()
        else -> 74
    }

    /**
     * Whether the point (x, y) in scene pixels is on him as he stands at [tick]: his drawn pixels, with a few
     * pixels' grace round them so a finger finds him easily. Taps anywhere else in the nook leave him be.
     */
    fun onHim(s: NookScene, tick: Int, x: Float, y: Float): Boolean {
        val left = spot(s, tick)
        if (left < 0) return false
        val inBed = s.pose == Pose.SLEEP && "bed" in s.built
        val top = if (inBed) FLOOR - 58 else FLOOR - 42
        val fig = Critter.draw(s.look, s.pose, tick, lying = inBed)
        val px = (x - left).toInt(); val py = (y - top).toInt()
        val grace = 3
        for (dy in -grace..grace) for (dx in -grace..grace) if (fig.filled(px + dx, py + dy)) return true
        return false
    }

    /** Drawn from the app and from the widget; the light map and background are shared, so one at a time. */
    @Synchronized
    fun draw(b: Buf, s: NookScene, tick: Int) {
        b.copyFrom(background())
        val lights = mutableListOf<Light>()
        window(b, s, tick, lights)
        string(b)
        // [NookScene.built] holds what stands in the nook: one thing per place.
        when {
            "picture" in s.built -> picture(b)
            "map" in s.built -> Furniture.wall(b, "map", tick)
            "mirror" in s.built -> Furniture.wall(b, "mirror", tick)
        }
        shelf(b)
        if ("clock" in s.built) clock(b, s, tick)
        rack(b, s)
        if ("garland" in s.built) garland(b, s, tick, lights)
        stove(b, s, tick, lights)
        dresser(b, s)
        barrels(b)
        if ("ladder" in s.built) ladder(b)
        if ("rug" in s.built) rug(b)
        if ("bed" in s.built) bed(b)
        if ("lamp" in s.built) lamp(b, s, tick, lights)
        if (s.dusty) dust(b)

        val inBed = s.pose == Pose.SLEEP && "bed" in s.built
        val x = spot(s, tick)
        val since = tick - s.pettedTick
        val petted = since in 0..48
        if (x >= 0) {
            // Little steps while he walks about; a jump for joy when stroked, with a squash on landing.
            val moving = s.pose == Pose.IDLE && !Art.still && abs(cos(tick / 240.0)) > .2
            val (hop, squash) = when {
                since in 0..15 -> (sin(since / 15.0 * PI) * 9).roundToInt() to -(sin(since / 15.0 * PI) * .12)
                since in 16..24 -> 0 to sin((since - 15) / 9.0 * PI) * .14
                moving -> (abs(sin(tick / 6.0)) * 2).roundToInt() to 0.0
                else -> 0 to 0.0
            }
            val step = if (moving) (if ((tick / 6) % 2 == 0) 1 else -1) else 0
            val y = (if (inBed) FLOOR - 58 else FLOOR - 42) - hop
            if (!inBed) b.shadow(x + 24, FLOOR + 3, 17 - hop, 4, .6f - hop * .04f)
            b.blit(Critter.draw(s.look, s.pose, tick, petted, lying = inBed, squash = squash, step = step), x, y)
            if (inBed) blanket(b)
            if (s.pose == Pose.DARK && "lamp" !in s.built) candle(b, x + 46, tick, lights)
            if (s.pose == Pose.SLEEP) zzz(b, x + 36, y + 4, tick)
            if (s.pose == Pose.DIZZY) stars(b, x + 24, y + 8, tick)
            if (petted) heart(b, x + 21, y + 4 - since / 4)
        } else footprints(b)
        // The front corners are closest to us, so they go over him.
        listOf("armchair", "gramophone", "pouf").firstOrNull { it in s.built }?.let { Furniture.frontLeft(b, it, tick) }
        listOf("fern", "radio", "aquarium").firstOrNull { it in s.built }?.let { Furniture.frontRight(b, it, tick) }
        motes(b, s, tick)
        light(b, s, lights)
    }

    private class Light(val x: Int, val y: Int, val r: Float, val i: Float, val c: Int)

    private fun ambient(s: NookScene): Triple<Float, Float, Float> {
        val h = s.hour
        val base = when {
            h < 5f || h >= 22f -> Triple(.36f, .4f, .58f)
            h < 7.5f -> Triple(.66f, .58f, .66f)
            h < 17f -> Triple(1f, .98f, .95f)
            h < 20f -> Triple(.88f, .72f, .64f)
            else -> Triple(.52f, .5f, .64f)
        }
        val grey = s.wx != null && s.wx != Wx.CLEAR && s.wx != Wx.CLOUDY
        val dim = if (grey && h in 7.5f..17f) Triple(base.first * .82f, base.second * .84f, base.third * .9f) else base
        return if (s.dark) Triple(dim.first * .55f, dim.second * .55f, dim.third * .65f) else dim
    }

    private var mapKey: List<Any>? = null
    private val mapR = FloatArray(W * H)
    private val mapG = FloatArray(W * H)
    private val mapB = FloatArray(W * H)
    private val glowR = FloatArray(W * H)
    private val glowG = FloatArray(W * H)
    private val glowB = FloatArray(W * H)

    /** Light per pixel from the ambient and the lamps, vignette included; rebuilt only when the lights change. */
    private fun lightMap(amb: Triple<Float, Float, Float>, lights: List<Light>) {
        val key = listOf<Any>(amb) + lights.map { listOf(it.x, it.y, it.r, it.i, it.c) }
        if (key == mapKey) return
        mapKey = key
        val (ar, ag, ab) = amb
        for (y in 0 until H) for (x in 0 until W) {
            var lr = ar; var lg = ag; var lb = ab
            for (l in lights) {
                val dx = (x - l.x).toFloat(); val dy = (y - l.y).toFloat()
                val d2 = (dx * dx + dy * dy) / (l.r * l.r)
                if (d2 < 1f) {
                    val d = sqrt(d2)
                    val k = (1 - d) * (1 - d) * l.i
                    lr += k * ((l.c shr 16) and 0xFF) / 255f; lg += k * ((l.c shr 8) and 0xFF) / 255f; lb += k * (l.c and 0xFF) / 255f
                }
            }
            val ex = abs(x - W / 2f) / (W / 2f); val ey = abs(y - H / 2f) / (H / 2f)
            val v = 1f - .3f * (ex * ex + ey * ey) / 2f
            val i = y * W + x
            mapR[i] = lr * v; mapG[i] = lg * v; mapB[i] = lb * v
            glowR[i] = (lr - ar).coerceAtLeast(0f) * 34 * v; glowG[i] = (lg - ag).coerceAtLeast(0f) * 34 * v; glowB[i] = (lb - ab).coerceAtLeast(0f) * 34 * v
        }
    }

    private fun light(b: Buf, s: NookScene, lights: List<Light>) {
        lightMap(ambient(s), lights)
        val d = b.d
        for (i in d.indices) {
            val c = d[i]
            val r = (((c shr 16) and 0xFF) * mapR[i] + glowR[i]).toInt().coerceAtMost(255)
            val g = (((c shr 8) and 0xFF) * mapG[i] + glowG[i]).toInt().coerceAtMost(255)
            val bb = ((c and 0xFF) * mapB[i] + glowB[i]).toInt().coerceAtMost(255)
            d[i] = Buf.OPAQUE or (r shl 16) or (g shl 8) or bb
        }
    }

    private fun skyColor(h: Float): Int = when {
        h < 5f || h >= 22f -> rgb(0x141A38)
        h < 7f -> mix(rgb(0x141A38), rgb(0xF0A070), (h - 5f) / 2f)
        h < 9f -> mix(rgb(0xF0A070), rgb(0x7CC0EC), (h - 7f) / 2f)
        h < 18f -> rgb(0x7CC0EC)
        h < 20f -> mix(rgb(0x7CC0EC), rgb(0xE88050), (h - 18f) / 2f)
        else -> mix(rgb(0xE88050), rgb(0x141A38), (h - 20f) / 2f)
    }

    /** The camera: a round window in a bolted ring with a real sky, curtains on a brass rod, a sill with pots. */
    private fun window(b: Buf, s: NookScene, tick: Int, lights: MutableList<Light>) {
        val cx = WIN_X; val cy = WIN_Y; val r = WIN_R
        b.shadow(cx + 3, cy + 4, r + 8, r + 8, .5f)
        for (y in -r - 7..r + 7) for (x in -r - 7..r + 7) {
            val d = sqrt((x * x + y * y).toFloat())
            if (d > r + 7.4f || d <= r) continue
            val lit = (-x - y) / (d + .01f)
            val c = when {
                d > r + 6.4f -> STEEL.dd
                d < r + 1.4f -> CHIP.dd
                d > r + 3.6f && d < r + 4.6f -> STEEL.d
                lit > .5f -> STEEL.hi.takeIf { lit > .85f && d > r + 4.6f } ?: STEEL.l
                lit < -.45f -> STEEL.d
                else -> STEEL.b
            }
            b.p(cx + x, cy + y, c)
        }
        for (a in 0 until 360 step 30) { val rad = (a + 15) * PI / 180; b.rivet(cx + (cos(rad) * (r + 5.5)).roundToInt(), cy + (sin(rad) * (r + 5.5)).roundToInt(), STEEL) }
        val n = 2 * r + 1
        val v = Buf(n, n)
        val wx = s.wx
        val grey = wx != null && wx != Wx.CLEAR && wx != Wx.CLOUDY
        // A flash of lightning now and then in a storm.
        val flash = wx == Wx.STORM && !Art.still && (tick % 260) in 0..3
        val sky = when {
            flash -> rgb(0xE8ECFF)
            grey -> mix(skyColor(s.hour), if (s.hour < 6f || s.hour >= 21f) rgb(0x2A2E38) else rgb(0x9098A6), if (wx == Wx.STORM) .75f else .6f)
            else -> skyColor(s.hour)
        }
        for (y in 0 until n) v.rect(0, y, n, 1, mix(sky, rgb(0xFFF0C8), (y.toFloat() / n) * (if (grey) .1f else .25f)))
        val night = s.hour < 5.5f || s.hour >= 21.5f
        if (night && !grey) {
            for (i in 0 until 26) {
                val a = i * 2.39; val d = 2 + (i * 7) % (r - 2)
                val x = r + (cos(a) * d).toInt(); val y = r + (sin(a) * d).toInt() - 7
                val tw = Art.still || (tick / 18 + i) % 9 != 0
                if (tw) v.p(x, y, if (i % 5 == 0) WHITE else rgb(0xA8B4E0))
                if (i % 7 == 0 && tw) { v.p(x - 1, y, rgb(0x6878B0)); v.p(x + 1, y, rgb(0x6878B0)) }
            }
            v.ell(r + 9, r - 11, 5, 5, rgb(0xF4ECC8)); v.ell(r + 11, r - 13, 4, 4, sky); v.p(r + 7, r - 10, rgb(0xD8D0B0))
        } else {
            if (s.hour in 7f..18f && !grey) { v.ell(r - 10, r - 11, 6, 6, mix(sky, rgb(0xFFF4B0), .35f)); v.ell(r - 10, r - 11, 4, 4, rgb(0xFFF8D0)) }
            val drift = if (Art.still) 20.0 else (tick / 24.0) % 90
            // More clouds the worse it is; heavy grey ones when it rains.
            val clouds = when (wx) { null, Wx.CLEAR -> listOf(-18 to -5, 14 to 5, -30 to 9); Wx.CLOUDY -> listOf(-18 to -8, 14 to 0, -30 to 6, 4 to -14, 30 to 10); else -> listOf(-20 to -14, 0 to -10, 20 to -12, -10 to -4, 12 to 0, -28 to 4, 28 to 6) }
            val puff = if (grey) mix(sky, rgb(0x6A7080), .35f) else mix(sky, WHITE, .7f)
            val top = if (grey) mix(sky, rgb(0xC0C6D0), .4f) else mix(sky, WHITE, .88f)
            for ((ox, oy) in clouds) {
                val x = ((r + ox + drift + 18) % (n + 36) - 18).toInt()
                v.ell(x, r + oy, 8, 3, puff); v.ell(x + 3, r + oy - 3, 5, 3, top); v.ell(x - 4, r + oy - 1, 3, 2, mix(puff, top, .5f))
                v.rect(x - 7, r + oy + 3, 15, 1, mix(sky, rgb(0x5A6070), .3f))
            }
        }
        // Roofs across the street: gables, a chimney with smoke, lit windows in the evening.
        val roof = if (night) rgb(0x0A0E1C) else mix(sky, rgb(0x2A3050), .6f)
        val roofL = mix(roof, WHITE, .14f)
        val heights = listOf(9, 13, 13, 11, 16, 16, 10, 9, 12, 12)
        for (x in 0 until n) { val hgt = heights[(x / 5) % heights.size] + (if (x % 5 == 2) 1 else 0); v.rect(x, n - hgt, 1, hgt, roof); v.p(x, n - hgt, roofL) }
        v.rect(n - 18, n - 22, 3, 6, roof)
        if (!Art.still) for (i in 0 until 4) { val k = (tick / 6 + i * 9) % 36; v.p(n - 17 + (sin(k / 5.0) * 2).toInt(), n - 23 - k / 2, mix(sky, WHITE, .5f), .6f - k / 70f) }
        if (night || s.hour >= 18f || grey) for ((lx, ly) in listOf(5 to 4, 6 to 4, 17 to 7, 32 to 9, 33 to 9, 41 to 6)) v.p(lx, n - ly, rgb(0xFFD070))
        // Weather falling past the glass.
        when (wx) {
            Wx.RAIN, Wx.DRIZZLE, Wx.STORM -> {
                val count = when (wx) { Wx.DRIZZLE -> 10; Wx.RAIN -> 22; else -> 30 }
                val drop = mix(sky, WHITE, .7f)
                val len = if (wx == Wx.DRIZZLE) 3 else 6
                for (i in 0 until count) {
                    val f = if (Art.still) (i * 29) % (n + 12) else (tick * 2 + i * 37) % (n + 12)
                    val x0 = (i * 11 + f / 3) % (n + 6) - 3; val y0 = f - 8
                    for (k in 0 until len) v.p(x0 + k / 3, y0 + k, drop, .9f - k * .1f)
                }
                if (flash) { v.line(r + 6, 0, r + 2, 10, WHITE); v.line(r + 2, 10, r + 7, 12, WHITE); v.line(r + 7, 12, r + 3, 24, WHITE) }
            }
            Wx.SNOW -> {
                for (x in 0 until n) { val hgt = heights[(x / 5) % heights.size] + (if (x % 5 == 2) 1 else 0); v.p(x, n - hgt, WHITE); v.p(x, n - hgt + 1, mix(WHITE, roof, .4f)) }
                for (i in 0 until 26) {
                    val f = if (Art.still) (i * 9.0) % (n + 6) else (tick / 2.0 + i * 23) % (n + 6)
                    val x = ((i * 13 + sin(f / 6.0 + i) * 2.5) % n).toInt(); val y = f.toInt() - 3
                    if (i % 3 == 0) { v.p(x, y, WHITE); v.p(x - 1, y, WHITE, .7f); v.p(x + 1, y, WHITE, .7f); v.p(x, y - 1, WHITE, .7f); v.p(x, y + 1, WHITE, .7f) }
                    else { v.p(x, y, WHITE); v.p(x + 1, y, WHITE, .6f) }
                }
            }
            Wx.FOG -> for (y in 0 until n) { val k = .25f + .45f * (y.toFloat() / n); v.rect(0, y, n, 1, rgb(0xC8CCD2), k) }
            else -> Unit
        }
        for (y in 0 until n) for (x in 0 until n) { val dx = x - r; val dy = y - r; if (dx * dx + dy * dy <= r * r) b.p(cx - r + x, cy - r + y, v.d[y * n + x]) }
        // Glass: a sheen arc, a spot, a faint blue rim.
        for (a in 200..250 step 2) { val rad = a * PI / 180; b.p(cx + (cos(rad) * (r - 4)).roundToInt(), cy + (sin(rad) * (r - 4)).roundToInt(), WHITE, .55f) }
        for (a in 205..235 step 3) { val rad = a * PI / 180; b.p(cx + (cos(rad) * (r - 6)).roundToInt(), cy + (sin(rad) * (r - 6)).roundToInt(), WHITE, .3f) }
        b.glint(cx - 13, cy - 6, .6f)
        // Drops on the glass when it rains; one slides down slowly.
        if (wx == Wx.RAIN || wx == Wx.DRIZZLE || wx == Wx.STORM) {
            for ((dx, dy) in listOf(-9 to -4, 6 to -10, 11 to 4, -3 to 9, 3 to 2, -13 to 6)) { b.p(cx + dx, cy + dy, WHITE, .55f); b.p(cx + dx, cy + dy + 1, rgb(0x404860), .35f) }
            val slide = if (Art.still) 4 else (tick / 4) % 30
            b.p(cx + 8, cy - 14 + slide, WHITE, .6f); b.p(cx + 8, cy - 15 + slide, WHITE, .3f)
        }
        for (a in 0 until 360 step 4) { val rad = a * PI / 180; b.p(cx + (cos(rad) * (r - .5)).roundToInt(), cy + (sin(rad) * (r - .5)).roundToInt(), rgb(0x6A7AA8), .3f) }
        // Curtains: red check, gathered at the sides with tie-backs, swaying a little.
        val rodY = cy - r - 14
        b.cylH(cx - r - 22, rodY, 2 * r + 45, 3, BRASS)
        for (fx in listOf(cx - r - 23, cx + r + 23)) { b.ell(fx, rodY + 1, 3, 3, BRASS.b); b.p(fx - 1, rodY - 1, BRASS.hi); b.p(fx + 1, rodY + 3, BRASS.d) }
        for (k in 0 until 6) b.ring(cx - r - 18 + k * 3, rodY - 1, 3, 4, BRASS.d)
        for (k in 0 until 6) b.ring(cx + r + 4 + k * 3, rodY - 1, 3, 4, BRASS.d)
        val tieY = cy + 4
        for (side in listOf(-1, 1)) {
            for (y in rodY + 3 until cy + r + 10) {
                val swing = if (Art.still) 0.0 else sin(tick / 40.0 + y / 9.0) * (y - rodY) / 70.0
                val narrow = if (y < tieY) ((y - rodY) / (tieY - rodY.toDouble()) * 7).toInt() else (7 - (y - tieY) / 3).coerceAtLeast(2)
                val w = 17 - narrow
                val outer = cx + side * (r + 20)
                val x0 = (if (side < 0) outer else outer - w + 1) + swing.roundToInt() * -side
                for (i in 0 until w) {
                    val fold = ((i + y / 4 + (if (side < 0) 0 else 2)) % 5)
                    val c = if (((i / 3) + (y / 3)) % 2 == 0) RED.b else CREAM.d
                    val base = if (((i / 3) % 2 == 0) && ((y / 3) % 2 == 0)) mix(RED.b, INK, .25f) else c
                    b.p(x0 + i, y, when (fold) { 0 -> mix(base, RED.dd, .4f); 1 -> mix(base, WHITE, .15f); else -> base })
                }
                b.p(if (side < 0) x0 + w else x0 - 1, y, RED.dd)
                if (y == cy + r + 9) b.rect(x0, y, w, 1, RED.d)
            }
            val tx = cx + side * (r + 13)
            b.cylH(tx - 4, tieY, 9, 3, BRASS); b.p(tx - 4, tieY, BRASS.hi)
        }
        // Sill with pots.
        val sy = cy + r + 10
        b.crate(cx - r - 16, sy, 2 * r + 33, 6, 3, WOOD)
        b.grain(cx - r - 16, sy + 3, 2 * r + 33, 3, WOOD.d, 3)
        b.rect(cx - r - 16, sy + 6, 2 * r + 33, 1, WOOD.dd)
        for (dx in listOf(-r - 12, r + 12)) { b.rect(cx + dx, sy + 7, 3, 4, WOOD.d); b.p(cx + dx, sy + 7, WOOD.b); b.rect(cx + dx - 1, sy + 10, 5, 1, WOOD.dd) }
        b.shadow(cx, sy + 8, r + 12, 2, .45f)
        if ("plant" in s.built) plant(b, cx + r + 3, sy, s.sproutDays, tick)
        else if ("musicbox" in s.built) Furniture.musicbox(b, cx + r + 2, sy, tick)
        if ("telescope" in s.built) telescope(b, cx - r - 9, sy)
        else if ("snowglobe" in s.built) Furniture.snowglobe(b, cx - r - 8, sy, tick)
        else {
            val pot = ramp(rgb(0xC86A3A))
            b.cylV(cx - r - 10, sy - 9, 8, 9, pot, true); b.cylH(cx - r - 11, sy - 10, 10, 2, pot)
            for (i in 0 until 3) { b.rect(cx - r - 8 + i * 2, sy - 16 + i, 1, 6 - i, LEAF.b); b.p(cx - r - 8 + i * 2, sy - 16 + i, LEAF.l) }
            b.p(cx - r - 6, sy - 18, rgb(0xF0A0C0)); b.p(cx - r - 5, sy - 17, rgb(0xF0A0C0)); b.p(cx - r - 7, sy - 17, rgb(0xF0A0C0)); b.p(cx - r - 6, sy - 17, rgb(0xFFE070))
        }
        if (!night) lights += Light(cx, cy + 8, 74f, if (s.hour in 8f..17f) .35f else .25f, sky)
        if (s.sunny || (s.hour in 9f..16f && !s.dark)) {
            for (y in sy + 7 until FLOOR + 18) {
                val x0 = cx - 14 - (y - sy - 7) / 2
                for (x in x0 until x0 + 26) if (dith(x, y, if (s.sunny) .5 else .24)) b.p(x, y, rgb(0xFFF0C0), .2f)
            }
        }
    }

    /** A string across the wall with drying raisins, a feather and a sock: things he brought home. */
    private fun string(b: Buf) {
        val pts = b.sag(30, 22, 72, 22, 6, rgb(0x5A3A20))
        val raisin = ramp(rgb(0x6A3050))
        for ((i, pt) in pts.withIndex()) if (i % 8 == 4 && i < 30) {
            val (x, y) = pt
            b.p(x, y + 1, rgb(0x5A3A20)); b.ell(x, y + 4, 2, 2, raisin.b); b.p(x - 1, y + 3, raisin.l); b.p(x + 1, y + 5, raisin.dd)
        }
        val (fx, fy) = pts[30]
        b.line(fx, fy + 1, fx - 3, fy + 12, rgb(0x6A5040))
        for (i in 0..8) { b.p(fx - 1 - i / 3, fy + 2 + i, rgb(0xF4F0E8)); b.p(fx - i / 3, fy + 2 + i, rgb(0xC8C0B0)); if (i % 2 == 0) b.p(fx - 2 - i / 3, fy + 3 + i, rgb(0xE0D8C8)) }
        val (sx, sy) = pts[38]
        b.rect(sx - 1, sy, 3, 2, BRASS.d)
        b.rect(sx - 2, sy + 2, 5, 9, ramp(rgb(0x5A80C8)).b); b.rect(sx - 2, sy + 2, 5, 2, CREAM.l); b.rect(sx - 2, sy + 8, 8, 4, ramp(rgb(0x5A80C8)).b)
        for (k in 0 until 3) b.rect(sx - 2, sy + 5 + k * 2, 5, 1, ramp(rgb(0x5A80C8)).l)
        b.rect(sx + 3, sy + 9, 3, 3, CREAM.b); b.rect(sx - 2, sy + 11, 8, 1, ramp(rgb(0x5A80C8)).dd)
    }

    /** A little wall shelf that is always there: a jar of buttons, two books and a cactus. */
    private fun shelf(b: Buf) {
        val x = 58; val y = 96
        b.shadow(x + 22, y + 4, 23, 2, .4f)
        b.crate(x, y, 44, 4, 2, WOOD)
        for (bx in listOf(x + 4, x + 38)) { b.line(bx, y + 4, bx + 3, y + 9, WOOD.d); b.p(bx + 1, y + 4, WOOD.l) }
        // A glass jar with buttons inside.
        val jx = x + 4; val jy = y - 14
        b.cylV(jx, jy, 11, 14, GLASS, true)
        for ((dx, dy, c) in listOf(Triple(2, 10, RED.b), Triple(6, 11, BRASS.b), Triple(4, 8, BLUE.l), Triple(7, 8, CREAM.l), Triple(3, 12, rgb(0x58A050)))) { b.ell(jx + dx, jy + dy, 1, 1, c); b.p(jx + dx, jy + dy, mix(c, INK, .4f)) }
        b.rect(jx + 2, jy + 1, 1, 10, WHITE)
        b.cylH(jx - 1, jy - 3, 13, 3, RED); b.p(jx + 1, jy - 3, RED.hi)
        // Books.
        for ((i, c) in listOf(rgb(0x3A6A9A), rgb(0x9A3A4A), rgb(0x6A8A3A)).withIndex()) {
            val bx = x + 19 + i * 5; val bh = 13 - i * 2
            val rr = ramp(c)
            b.rect(bx, y - bh, 4, bh, rr.b); b.rect(bx, y - bh, 1, bh, rr.l); b.rect(bx + 3, y - bh, 1, bh, rr.d)
            b.rect(bx, y - bh + 2, 4, 1, BRASS.l); b.rect(bx, y - 3, 4, 1, BRASS.d)
        }
        // A cactus in a thimble.
        val kx = x + 36
        b.cylV(kx, y - 5, 6, 5, STEEL, true)
        b.cylV(kx + 1, y - 13, 4, 8, LEAF, false); b.rect(kx - 1, y - 11, 2, 2, LEAF.b); b.rect(kx + 5, y - 10, 2, 2, LEAF.d)
        for (yy in y - 12 until y - 5 step 2) b.p(kx + 2, yy, LEAF.hi)
        b.p(kx + 2, y - 14, rgb(0xF070A0)); b.p(kx + 3, y - 14, rgb(0xF070A0))
    }

    /** The battery is the stove: a cast-iron body, terminals for hotplates, a label, an arched firebox and a pipe. */
    private fun stove(b: Buf, s: NookScene, tick: Int, lights: MutableList<Light>) {
        val x = 5; val y = 70; val w = 46; val h = FLOOR - y
        // Pipe with joints, a damper and soot.
        b.cylV(x + 10, 14, 11, y - 14, COPPER)
        for (yy in listOf(24, 46)) { b.cylV(x + 8, yy, 15, 4, ramp(mix(COPPER.b, rgb(0x5A3014), .35f))); b.rivet(x + 11, yy + 1, COPPER) }
        b.cylH(x + 20, 34, 6, 2, IRON); b.ell(x + 26, 35, 2, 2, BRASS.b)
        for (yy in y - 10 until y) for (xx in x + 10 until x + 21) if (dith(xx, yy, (yy - (y - 10)) / 12.0)) b.p(xx, yy, mix(COPPER.dd, INK, .5f))
        b.shadow(x + w / 2 + 3, FLOOR + 2, 28, 4, .65f)
        // Body.
        b.block(x, y + 3, w, h - 6, IRON, 4)
        for (yy in y + 8 until y + h - 8) { b.p(x + 2, yy, IRON.l); if (yy % 3 == 0) b.p(x + 4, yy, mix(IRON.b, IRON.l, .45f)) }
        for (yy in y + 8 until y + h - 8 step 4) b.p(x + w - 3, yy, IRON.dd)
        // Ornamental crest and top plate with terminals: brass (+) on the left, steel (−) on the right, a kettle on one.
        b.crate(x - 3, y - 1, w + 6, 7, 3, IRON)
        for (i in 0 until 5) b.rivet(x + 2 + i * 10, y + 4, IRON)
        b.cylV(x + 23, y - 4, 7, 3, BRASS); b.rect(x + 25, y - 5, 3, 1, BRASS.hi)
        b.cylV(x + 32, y - 4, 11, 3, STEEL); b.rect(x + 34, y - 5, 7, 1, STEEL.l)
        kettle(b, x + 37, y - 4, s, tick)
        // Label band with a lightning bolt and tiny print.
        val ly = y + 14
        b.rect(x + 2, ly, w - 4, 13, SOLDER.b); b.rect(x + 2, ly, w - 4, 1, SOLDER.l); b.rect(x + 2, ly + 12, w - 4, 1, SOLDER.dd)
        b.rect(x + 2, ly + 2, w - 4, 1, BRASS.d); b.rect(x + 2, ly + 10, w - 4, 1, BRASS.d)
        for ((bx, by) in listOf(5 to 3, 4 to 4, 3 to 5, 3 to 6, 4 to 6, 5 to 6, 6 to 6, 5 to 7, 4 to 8, 3 to 9)) b.p(x + 4 + bx, ly + by, BRASS.hi)
        b.rect(x + 16, ly + 4, 17, 1, mix(CREAM.l, SOLDER.b, .2f)); b.rect(x + 16, ly + 6, 11, 1, mix(CREAM.b, SOLDER.b, .4f)); b.rect(x + 16, ly + 8, 14, 1, mix(CREAM.b, SOLDER.b, .5f))
        b.ring(x + 35, ly + 3, 7, 7, mix(CREAM.l, SOLDER.b, .3f)); b.p(x + 38, ly + 6, mix(CREAM.l, SOLDER.b, .3f))
        for (rx in listOf(x + 3, x + w - 5)) { b.rivet(rx, y + 9, IRON); b.rivet(rx, y + h - 12, IRON) }
        // Gauge: five cells in a brass bezel with a glass glint.
        val gx = x + w - 11; val gy = y + 33
        b.block(gx - 2, gy - 2, 9, 32, BRASS, 2)
        val cells = ((s.level + 19) / 20).coerceIn(0, 5)
        val lit = when { s.level <= 15 -> rgb(0xF05040); s.level <= 40 -> rgb(0xF0B040); else -> rgb(0x80E0A0) }
        val pulse = if (Art.still || !s.charging) 1f else (.55f + .45f * sin(tick / 6.0).toFloat())
        for (i in 0 until 5) {
            val cy = gy + 23 - i * 6
            val on = i < cells
            val top = s.charging && i == cells - 1
            b.rect(gx, cy, 5, 4, rgb(0x14161C))
            if (on) { b.rect(gx, cy, 5, 4, if (top) mix(rgb(0x14161C), lit, pulse) else lit); b.rect(gx, cy, 5, 1, mix(lit, WHITE, .45f)) }
        }
        b.rect(gx + 1, gy, 1, 27, WHITE.let { mix(it, rgb(0x14161C), .7f) })
        // Arched firebox with a brass frame, an open door and a grate.
        val fx = x + 6; val fy = y + 34; val fw = 23; val fh = 34
        for (yy in fy - 3 until fy + fh + 3) for (xx in fx - 3 until fx + fw + 3) {
            val inArch = archIn(xx - fx, yy - fy, fw, fh)
            val outer = archIn(xx - fx + 3, yy - fy + 3, fw + 6, fh + 6)
            if (outer && !inArch) {
                val lt = (xx - fx) < fw / 2 && (yy - fy) < fh / 2
                b.p(xx, yy, if (lt) BRASS.l else BRASS.d)
                if (archIn(xx - fx + 2, yy - fy + 2, fw + 4, fh + 4) && !archIn(xx - fx + 1, yy - fy + 1, fw + 2, fh + 2)) b.p(xx, yy, BRASS.b)
            }
        }
        b.rivet(fx + fw / 2 - 1, fy - 3, BRASS)
        val burning = s.charging || s.level > 15
        for (yy in fy until fy + fh) for (xx in fx until fx + fw) if (archIn(xx - fx, yy - fy, fw, fh)) b.p(xx, yy, mix(rgb(0x140806), rgb(0x3A1A10), (yy - fy) / fh.toFloat() * (if (burning) 1f else 0f)))
        if (burning) {
            val base = fy + fh - 5
            val tall = if (s.charging) 18 else 8
            val ph = if (Art.still) 0.0 else tick / 4.0
            for (i in 0 until fw) {
                val wob = sin(ph + i * .9) * 1.6 + sin(ph * 1.7 + i * .4) * 1.2
                val shape = sin(i / (fw - 1.0) * PI)
                val top = (base - tall * (.45 + .55 * shape) - wob).toInt().coerceAtLeast(fy + 4)
                for (yy in top..base) {
                    if (!archIn(i, yy - fy, fw, fh)) continue
                    val t = (base - yy).toFloat() / (base - top + 1)
                    val c = when { t > .8f -> rgb(0xA8301C); t > .55f -> rgb(0xE86A24); t > .3f -> rgb(0xFFA838); t > .1f -> rgb(0xFFD860); else -> rgb(0xFFF4C0) }
                    b.p(fx + i, yy, c)
                }
            }
            // A few loose tongues that lick upward.
            if (!Art.still) for (k in 0 until 3) { val f = (tick / 2 + k * 11) % 22; val tx = fx + 5 + k * 6 + (sin((tick + k * 9) / 5.0) * 1.5).roundToInt(); val ty = base - tall - 2 - f / 3; if (archIn(tx - fx, ty - fy, fw, fh)) b.p(tx, ty, rgb(0xFFC850), 1f - f / 22f) }
            // Logs and embers.
            b.cylH(fx + 1, base, 11, 4, ramp(rgb(0x6A3A1C))); b.cylH(fx + 10, base + 1, 11, 3, ramp(rgb(0x5A3018)))
            b.ell(fx + 2, base + 2, 1, 1, rgb(0xC89060)); b.ell(fx + 20, base + 2, 1, 1, rgb(0xC89060))
            for (k in 0 until 4) b.p(fx + 3 + k * 5, base + 3, if (Art.still || (tick / 8 + k) % 3 != 0) rgb(0xFF8A30) else rgb(0xFFD060))
            if (!Art.still && s.charging) for (i in 0 until 4) { val k = (tick + i * 17) % 60; b.p(fx + 6 + i * 4 + (sin((tick + i * 13) / 6.0) * 2).roundToInt(), base - 10 - k / 3, rgb(0xFFC850), 1f - k / 60f) }
            lights += Light(fx + fw / 2, base - 8, if (s.charging) 100f else 56f, if (s.charging) 1.15f else .6f, rgb(0xFF9A40))
        } else for (i in 0 until 5) b.p(fx + 3 + i * 4, fy + fh - 4, rgb(0x6A2A18))
        b.rect(fx - 1, fy + fh, fw + 2, 3, IRON.d); for (i in 0 until 6) b.rect(fx + i * 4, fy + fh, 2, 1, IRON.l)
        // An ash drawer with a pull under the firebox.
        b.crate(fx - 2, fy + fh + 4, fw + 4, 6, 2, IRON); b.cylH(fx + fw / 2 - 3, fy + fh + 6, 6, 2, BRASS)
        // The door, swung open: a thin plate with a lattice and a knob.
        for (yy in fy + 4 until fy + fh - 2) { b.p(fx - 6, yy, IRON.l); b.p(fx - 5, yy, IRON.b); b.p(fx - 4, yy, IRON.d) }
        for (yy in fy + 8 until fy + fh - 4 step 5) b.p(fx - 5, yy, IRON.dd)
        b.ell(fx - 6, fy + fh / 2, 1, 1, BRASS.b); b.p(fx - 7, fy + fh / 2 - 1, BRASS.hi)
        // Curled feet.
        for ((fxp, dir) in listOf(x + 1 to -1, x + w - 6 to 1)) { b.rect(fxp, FLOOR - 3, 5, 3, IRON.d); b.rect(fxp, FLOOR - 3, 5, 1, IRON.l); b.p(fxp + (if (dir < 0) 0 else 4), FLOOR - 1, IRON.l) }
    }

    private fun archIn(x: Int, y: Int, w: Int, h: Int): Boolean {
        if (x < 0 || x >= w || y < 0 || y >= h) return false
        val r = w / 2f
        if (y >= r) return true
        val dx = x + .5f - r; val dy = y + .5f - r
        return dx * dx + dy * dy <= r * r
    }

    /** An enamel kettle with white spots, steaming in curls while the stove is lit. */
    private fun kettle(b: Buf, x: Int, y: Int, s: NookScene, tick: Int) {
        val k = Buf(20, 18)
        k.lit(ENAMEL) { ell(10, 11, 7, 6, it); rect(4, 7, 13, 5, it) }
        k.rect(7, 4, 7, 3, ENAMEL.d); k.rect(8, 3, 5, 1, ENAMEL.b); k.ell(10, 2, 1, 1, ENAMEL.l)
        for (i in 0 until 5) { k.p(3 - i / 2, 10 - i, ENAMEL.b); k.p(4 - i / 2, 10 - i, ENAMEL.d) }
        k.p(1, 5, ENAMEL.l)
        for ((dx, dy) in listOf(6 to 9, 13 to 12, 9 to 14, 15 to 8)) { k.p(dx, dy, WHITE); k.p(dx + 1, dy, mix(WHITE, ENAMEL.b, .4f)) }
        k.p(7, 8, ENAMEL.hi); k.p(8, 8, ENAMEL.hi)
        k.outline(mix(ENAMEL.dd, INK, .4f))
        b.blit(k, x - 10, y - 17)
        b.sag(x - 5, y - 14, x + 5, y - 14, -5, rgb(0x2A2A34))
        if (s.charging && !Art.still) for (i in 0 until 4) {
            val f = (tick + i * 14) % 56
            val st = f / 2.5
            b.p((x - 13 - st / 3 + sin(st / 3 + i) * 2).roundToInt(), (y - 14 - st).roundToInt(), WHITE, .55f - f * .009f)
        }
    }

    /** Capacitors standing like barrels. */
    private fun barrels(b: Buf) {
        for ((x, h) in listOf(105 to 28, 121 to 21)) {
            val y = FLOOR - h
            b.shadow(x + 7, FLOOR + 2, 10, 2, .55f)
            b.cylV(x, y + 3, 15, h - 3, BLUE)
            b.cylV(x + 9, y + 4, 3, h - 6, STEEL, false)
            for (k in 0 until 3) b.rect(x + 10, y + 8 + k * 5, 1, 2, BLUE.d)
            for (sy in listOf(y + 6, y + h - 6)) { b.rect(x, sy, 15, 1, BLUE.dd); b.rect(x, sy + 1, 15, 1, BLUE.l) }
            b.ell(x + 7.0, y + 3.0, 7, 3, STEEL.d); b.ell(x + 7.0, y + 2.5, 7, 2, STEEL.b); b.ell(x + 6.0, y + 2.0, 4, 1, STEEL.l)
            b.line(x + 4, y + 1, x + 10, y + 4, STEEL.dd); b.line(x + 10, y + 1, x + 4, y + 4, STEEL.dd)
        }
    }

    /** The processor is a dresser: a top face, three drawers with brass pulls, silver pins, silver legs. */
    private fun dresser(b: Buf, s: NookScene) {
        val x = 141; val y = 104; val w = 46; val h = FLOOR - 10 - y
        b.shadow(x + w / 2, FLOOR + 2, 26, 4, .6f)
        for (lx in listOf(x + 3, x + w - 6)) { b.cylV(lx, y + h, 4, FLOOR - y - h, STEEL, false); b.rect(lx - 1, FLOOR - 1, 6, 1, STEEL.d) }
        b.crate(x, y, w, h, 6, CHIP)
        for (i in 0 until 7) {
            val py = y + 10 + i * 7
            b.rect(x - 3, py, 3, 4, STEEL.b); b.p(x - 3, py, STEEL.hi); b.rect(x - 3, py + 3, 3, 1, STEEL.d)
            b.rect(x + w, py, 3, 4, STEEL.d); b.p(x + w + 2, py + 3, STEEL.dd)
        }
        for (d in 0 until 3) {
            val dy = y + 9 + d * 17
            b.rect(x + 4, dy, w - 8, 14, CHIP.d); b.rect(x + 4, dy, w - 8, 1, CHIP.l); b.rect(x + 4, dy, 1, 14, mix(CHIP.d, CHIP.l, .5f)); b.rect(x + 4, dy + 13, w - 8, 1, CHIP.dd)
            b.cylH(x + w / 2 - 5, dy + 5, 10, 4, BRASS); b.p(x + w / 2 - 5, dy + 5, BRASS.hi); b.rivet(x + w / 2 - 7, dy + 6, BRASS); b.rivet(x + w / 2 + 5, dy + 6, BRASS)
            b.rect(x + 7, dy + 3, 4, 1, mix(CHIP.l, CHIP.b, .5f))
        }
        b.ell(x + 7, y + 3, 2, 1, CHIP.hi)
        if ("shelf" in s.built) {
            b.crate(x - 4, y - 4, w + 8, 5, 2, WOOD); b.grain(x - 4, y - 2, w + 8, 3, WOOD.d, 7)
            // Shown smaller than in the pantry, so they sit on the dresser like the little things they are.
            s.shelf.take(3).forEachIndexed { i, k -> Catalog.find(k)?.let { b.blitScaled(Items.of(it), x + 1 + i * 15, y - 4 - 14, 14, 14) } }
        } else {
            // A candle in a saucer and a tin box.
            b.ell(x + 10, y - 1, 6, 1, CREAM.d); b.cylV(x + 8, y - 12, 5, 11, CREAM, true); b.rect(x + 10, y - 14, 1, 2, INK)
            b.crate(x + 24, y - 8, 12, 8, 3, ramp(rgb(0xC8603A))); b.rect(x + 27, y - 5, 6, 1, BRASS.l)
        }
    }

    /** A peg rack on the wall: the tools he has made hang here, empty pegs for the rest. */
    private fun rack(b: Buf, s: NookScene) {
        val x = 58; val y = 118
        b.shadow(x + 21, y + 4, 22, 2, .35f)
        b.cylH(x, y, 42, 5, WOOD); b.grain(x, y + 1, 42, 3, WOOD.d, 2)
        for (i in 0 until 3) { val px = x + 6 + i * 14; b.cylV(px, y + 5, 3, 4, WOOD, false); b.p(px, y + 8, WOOD.dd) }
        if ("lantern" in s.built) {
            val lx = x + 7
            b.line(lx, y + 8, lx, y + 11, rgb(0x2A2A34))
            b.block(lx - 5, y + 12, 11, 15, BRASS, 2)
            b.rect(lx - 3, y + 15, 7, 9, rgb(0xFFE080)); b.rect(lx - 3, y + 15, 7, 1, rgb(0xFFF4C0)); b.rect(lx, y + 15, 1, 9, BRASS.d); b.p(lx - 2, y + 16, WHITE)
            b.rect(lx - 4, y + 27, 9, 2, BRASS.d)
        }
        if ("rope" in s.built) {
            val rx = x + 21
            val rope = ramp(rgb(0xC8A060))
            for (i in 0 until 6) b.ring(rx - 6 + i / 2, y + 11 + i, 13 - i, 11 - i, if (i % 2 == 0) rope.b else rope.d)
            for (k in 0 until 10) b.p(rx - 5 + k, y + 11 + (k % 3), rope.l)
            b.line(rx + 5, y + 19, rx + 6, y + 27, rope.b); b.p(rx + 6, y + 27, rope.l)
        }
        if ("earmuffs" in s.built) {
            val ex = x + 35
            b.sag(ex - 6, y + 12, ex + 6, y + 12, -6, STEEL.b); b.sag(ex - 6, y + 13, ex + 6, y + 13, -6, STEEL.d)
            for (dx in listOf(-6, 6)) { b.ell(ex + dx, y + 15, 3, 4, RED.b); b.p(ex + dx - 1, y + 13, RED.l); b.p(ex + dx + 1, y + 18, RED.d); b.ring(ex + dx - 3, y + 11, 7, 9, RED.dd) }
        }
    }

    private fun ladder(b: Buf) {
        val x = 129
        for (yy in 100 until FLOOR) { val o = (FLOOR - yy) / 9; b.p(x + o, yy, WOOD.l); b.p(x + o + 1, yy, WOOD.d); b.p(x + 9 + o, yy, WOOD.b); b.p(x + 10 + o, yy, WOOD.dd) }
        for (yy in 104 until FLOOR step 9) { val o = (FLOOR - yy) / 9; b.rect(x + 2 + o, yy, 7, 1, WOOD.l); b.rect(x + 2 + o, yy + 1, 7, 1, WOOD.d) }
    }

    private fun picture(b: Buf) {
        val x = 60; val y = 50
        b.shadow(x + 15, y + 13, 16, 13, .35f)
        b.block(x, y, 30, 24, WOOD, 2)
        b.rect(x + 2, y + 2, 26, 20, BRASS.d)
        b.rect(x + 3, y + 3, 24, 18, rgb(0xA8D8F0))
        for (yy in 0 until 8) b.rect(x + 3, y + 3 + yy, 24, 1, mix(rgb(0x88C0E8), rgb(0xC8E8F8), yy / 8f))
        b.ell(x + 20, y + 15, 9, 4, rgb(0x4A8A50)); b.ell(x + 9, y + 16, 8, 3, rgb(0x6AAA58))
        b.rect(x + 3, y + 17, 24, 4, rgb(0x6AB060)); b.ell(x + 8, y + 8, 3, 3, rgb(0xFFF0A0))
        b.rect(x + 14, y + 13, 1, 4, rgb(0x6A4A2A)); b.ell(x + 14, y + 12, 2, 2, rgb(0x3A7A40))
        for (k in 0 until 3) b.p(x + 18 + k * 2, y + 7, rgb(0x404860))
        b.p(x + 25, y + 4, WHITE, .7f); b.p(x + 24, y + 5, WHITE, .4f)
        b.line(x + 15, y, x + 9, y - 7, INK); b.line(x + 15, y, x + 21, y - 7, INK); b.p(x + 15, y - 8, BRASS.hi)
    }

    private fun clock(b: Buf, s: NookScene, tick: Int) {
        val cx = 176; val cy = 78
        b.shadow(cx + 1, cy + 3, 10, 10, .35f)
        b.ell(cx, cy, 9, 9, BRASS.d); b.ell(cx - .5, cy - .5, 8, 8, BRASS.b); b.ell(cx - 1.0, cy - 1.0, 7, 7, BRASS.l); b.ell(cx, cy, 6, 6, CREAM.l)
        for (a in 0 until 12) { val rr = a / 12.0 * 2 * PI; b.p(cx + (sin(rr) * 5).roundToInt(), cy - (cos(rr) * 5).roundToInt(), if (a % 3 == 0) INK else CREAM.d) }
        val a = (s.hour % 12) / 12 * 2 * PI
        b.line(cx, cy, cx + (sin(a) * 3).roundToInt(), cy - (cos(a) * 3).roundToInt(), INK)
        val m = (s.hour % 1) * 2 * PI
        b.line(cx, cy, cx + (sin(m) * 5).roundToInt(), cy - (cos(m) * 5).roundToInt(), RED.d)
        b.p(cx, cy, BRASS.d); b.p(cx - 3, cy - 4, WHITE, .6f)
        // A pendulum swinging below.
        val sw = if (Art.still) 0.0 else sin(tick / 15.0) * .35
        val px = cx + (sin(sw) * 12).roundToInt(); val py = cy + 9 + (cos(sw) * 12).roundToInt()
        b.line(cx, cy + 9, px, py, BRASS.d); b.ell(px, py, 2, 2, BRASS.b); b.p(px - 1, py - 1, BRASS.hi)
    }

    private fun garland(b: Buf, s: NookScene, tick: Int, lights: MutableList<Light>) {
        val cols = intArrayOf(rgb(0xF05050), rgb(0xF0D050), rgb(0x60D0F0), rgb(0x80E080))
        val evening = s.hour >= 17 || s.hour < 7 || s.dark
        val pts = b.sag(88, 16, 186, 17, 9, rgb(0x2A1A10))
        var i = 0
        for ((k, pt) in pts.withIndex()) if (k % 9 == 4) {
            val (x, y) = pt
            val glow = if (Art.still) 1f else (.55f + .45f * sin(tick / 9.0 + i * 1.7).toFloat())
            val c = cols[i % 4]
            b.p(x, y + 1, rgb(0x2A1A10)); b.rect(x - 1, y + 2, 1, 1, rgb(0x3A3A40))
            b.ell(x - 1, y + 4, 1, 2, if (evening) mix(mix(c, INK, .45f), c, glow) else mix(c, INK, .3f))
            if (evening) b.p(x - 2, y + 3, mix(c, WHITE, .6f * glow))
            if (evening && i % 3 == 0) lights += Light(x, y + 4, 20f, .35f, c)
            i++
        }
    }

    /** A braided oval rug with a fringe. */
    private fun rug(b: Buf) {
        val cx = 86; val cy = FLOOR + 20
        b.shadow(cx, cy + 3, 52, 11, .4f)
        val rings = listOf(RED.d, RED.b, CREAM.b, BLUE.b, CREAM.l, RED.b, BRASS.b, BLUE.d, CREAM.l)
        for ((i, c) in rings.withIndex()) {
            val rx = 50 - i * 5; val ry = 10 - i
            if (ry < 1) break
            for (y in -ry..ry) for (x in -rx..rx) {
                val d = (x * x).toFloat() / (rx * rx) + (y * y).toFloat() / (ry * ry)
                if (d <= 1f) b.p(cx + x, cy + y, if ((x + y * 3 + i) % 4 == 0) mix(c, INK, .22f) else if ((x - y + i) % 5 == 0) mix(c, WHITE, .15f) else c)
            }
        }
        for (x in -48..48 step 3) { b.p(cx + x, cy + 10, CREAM.b); b.p(cx + x, cy + 11, CREAM.d) }
    }

    /** A matchbox bed: a printed label, a cotton-wool pillow, a patchwork quilt folded at the foot. */
    private fun bed(b: Buf) {
        val x = 135; val y = FLOOR - 19; val w = 54
        b.shadow(x + w / 2, FLOOR + 2, 29, 4, .6f)
        b.crate(x, y, w, 19, 4, CREAM)
        b.rect(x + 1, y + 4, w - 2, 3, RED.b); b.rect(x + 1, y + 4, w - 2, 1, RED.l); b.rect(x + 1, y + 6, w - 2, 1, RED.d)
        b.rect(x + 1, y + 7, 2, 11, mix(CREAM.d, rgb(0x6A4A3A), .4f)); b.rect(x + w - 3, y + 7, 2, 11, mix(CREAM.d, rgb(0x6A4A3A), .4f))
        // The label: a blue field with a yellow star and a few printed lines.
        b.rect(x + 5, y + 9, 20, 8, BLUE.b); b.rect(x + 5, y + 9, 20, 1, BLUE.l); b.ring(x + 6, y + 10, 18, 6, BRASS.d)
        for ((dx, dy) in listOf(15 to 11, 14 to 12, 15 to 12, 16 to 12, 13 to 13, 17 to 13, 15 to 13, 14 to 14, 16 to 14)) b.p(x + dx, y + dy, BRASS.hi)
        b.rect(x + 29, y + 10, 20, 1, CREAM.d); b.rect(x + 29, y + 12, 14, 1, CREAM.d); b.rect(x + 29, y + 14, 17, 1, CREAM.d)
        b.ell(x + w - 10, y - 1, 7, 3, CREAM.l); b.ell(x + w - 11, y - 2, 4, 1, WHITE); b.p(x + w - 15, y, CREAM.d)
        b.check(x + 4, y - 3, 16, 3, ramp(rgb(0x6A9AC8)).b, CREAM.l)
    }

    private fun blanket(b: Buf) {
        val x = 137; val y = FLOOR - 25
        b.check(x, y, 44, 8, RED.b, CREAM.l)
        for (k in 0 until 44 step 11) b.rect(x + k, y, 1, 8, RED.d)
        b.rect(x, y - 1, 44, 1, RED.l); b.rect(x, y + 8, 44, 1, RED.dd); b.rect(x + 43, y, 1, 8, RED.d)
    }

    private fun plant(b: Buf, x: Int, sill: Int, days: Int, tick: Int) {
        val h = (4 + days * 2 / 3).coerceAtMost(22)
        val sway = if (Art.still) 0 else (sin(tick / 30.0) * 1.2).roundToInt()
        for (k in 0 until h) b.p(x + 4 + (sway * k / h), sill - 7 - k, LEAF.b)
        for (i in 0 until h / 4) {
            val ly = sill - 9 - i * 4; val sx = x + 4 + sway * (i * 4) / h
            val left = i % 2 == 0
            b.rect(if (left) sx - 4 else sx + 1, ly, 4, 2, LEAF.b); b.rect(if (left) sx - 4 else sx + 2, ly, 2, 1, LEAF.l)
        }
        b.cylV(x, sill - 8, 9, 8, STEEL, true)
        for (k in 0 until 4) b.p(x + 1 + k * 2, sill - 5, STEEL.d)
        b.cylH(x - 1, sill - 9, 11, 2, STEEL)
    }

    private fun telescope(b: Buf, x: Int, sill: Int) {
        b.line(x - 4, sill - 1, x + 2, sill - 9, WOOD.d); b.line(x + 7, sill - 1, x + 2, sill - 9, WOOD.d); b.line(x + 2, sill - 1, x + 2, sill - 9, WOOD.b)
        for (i in 0 until 14) { b.p(x - 3 + i, sill - 10 - i / 2, BRASS.l); b.p(x - 3 + i, sill - 9 - i / 2, BRASS.b); b.p(x - 3 + i, sill - 8 - i / 2, BRASS.d) }
        b.rect(x + 9, sill - 18, 3, 5, BRASS.d); b.p(x + 11, sill - 17, rgb(0x9CC8F0))
    }

    /** An LED lamp on a cord from the beam with an enamel shade; it sways a little. */
    private fun lamp(b: Buf, s: NookScene, tick: Int, lights: MutableList<Light>) {
        val sw = if (Art.still) 0.0 else sin(tick / 45.0) * 1.4
        val x = 76 + sw.roundToInt()
        for (y in 15 until 34) b.p(76 + (sw * (y - 15) / 19).roundToInt(), y, rgb(0x2A1A10))
        b.rect(x - 1, 32, 3, 2, BRASS.b)
        for (i in 0 until 6) b.rect(x - 2 - i, 34 + i, 5 + 2 * i, 1, if (i == 0) ENAMEL.l else if (i < 3) ENAMEL.b else ENAMEL.d)
        b.rect(x - 7, 40, 15, 1, ENAMEL.dd); b.p(x - 4, 36, ENAMEL.hi)
        val on = s.dark || s.hour < 7.5f || s.hour >= 18f
        b.ell(x, 42, 2, 1, if (on) rgb(0xFFF8D0) else CREAM.d)
        if (on) { lights += Light(76, 44, 92f, .8f, rgb(0xFFD890)); for (k in 0 until 6) b.p(x - 6 + k * 2 + (k % 2), 44 + k / 2, rgb(0xFFF0C0), .25f) }
    }

    private fun dust(b: Buf) {
        for ((x, y) in listOf(186 to FLOOR, 176 to FLOOR + 1, 58 to FLOOR + 1)) { b.ell(x, y, 4, 3, rgb(0x9A9890)); b.p(x - 1, y - 2, rgb(0xC8C6C0)); b.p(x + 2, y - 1, rgb(0x7A7870)) }
        for (i in 0 until 14) { b.p(W - 1 - i, 16 + i / 2, rgb(0xC8C8C0), .6f); b.p(W - 1 - i / 2, 16 + i, rgb(0xC8C8C0), .6f); b.p(W - 1 - i * 2 / 3, 16 + i * 2 / 3, rgb(0xC8C8C0), .5f) }
    }

    /** Dust specks drifting in the light. */
    private fun motes(b: Buf, s: NookScene, tick: Int) {
        if (s.dark) return
        for (i in 0 until 12) {
            val t = if (Art.still) i * 37.0 else tick / 3.0 + i * 37
            val x = 40 + ((i * 29 + t / 6) % 130).toInt()
            val y = 30 + ((i * 23 + sin(t / 28.0 + i) * 8) % 120).toInt()
            b.p(x, y, rgb(0xFFF4D0), .35f + .25f * sin(t / 9.0 + i).toFloat())
        }
    }

    private fun candle(b: Buf, x: Int, tick: Int, lights: MutableList<Light>) {
        val y = FLOOR - 12
        b.ell(x + 2, FLOOR - 1, 5, 1, BRASS.b); b.cylV(x, y, 5, 10, CREAM, true)
        b.p(x + 1, y + 2, CREAM.l); b.rect(x + 2, y - 2, 1, 2, INK)
        val f = if (Art.still) 0.0 else sin(tick / 3.0)
        b.ell(x + 2, y - 4, 1, 2, rgb(0xFFB040)); b.p(x + 2, y - 3, rgb(0xFFF4C0)); b.p(x + 2 + f.roundToInt(), y - 7, rgb(0xF08030))
        lights += Light(x + 2, y - 4, 52f, .85f, rgb(0xFFB060))
    }

    private fun zzz(b: Buf, x: Int, y: Int, tick: Int) {
        for (i in 0 until 3) {
            val f = if (Art.still) i * 25 else (tick + i * 30) % 90
            val zx = x + f / 9 + (sin(f / 10.0) * 2).roundToInt(); val zy = y - f / 4
            val a = (1f - f / 90f).coerceIn(0f, 1f)
            val n = if (f > 45) 4 else 3
            for (k in 0 until n) { b.p(zx + k, zy, WHITE, a); b.p(zx + k, zy + n - 1, WHITE, a); b.p(zx + n - 1 - k, zy + k, WHITE, a) }
        }
    }

    private fun stars(b: Buf, cx: Int, cy: Int, tick: Int) {
        for (i in 0 until 4) { val a = tick / 8.0 + i * PI / 2; b.glint(cx + (cos(a) * 14).roundToInt(), cy + (sin(a) * 4).roundToInt()) }
    }

    private fun heart(b: Buf, x: Int, y: Int) {
        val c = rgb(0xF05060)
        b.rect(x + 1, y, 2, 1, c); b.rect(x + 5, y, 2, 1, c); b.rect(x, y + 1, 8, 2, c); b.rect(x + 1, y + 3, 6, 1, c); b.rect(x + 2, y + 4, 4, 1, c); b.rect(x + 3, y + 5, 2, 1, c)
        b.p(x + 1, y + 1, rgb(0xFFB0C0)); b.p(x + 2, y + 1, rgb(0xFFD0D8))
    }

    private fun footprints(b: Buf) {
        for (i in 0 until 7) { val x = 52 + i * 7; val y = FLOOR + 8 + i * 4; b.rect(x, y, 3, 1, PLANK.dd); b.rect(x + 4, y + 1, 3, 1, PLANK.dd) }
    }
}
