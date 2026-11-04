package app.rustle

import app.rustle.domain.Pose
import app.rustle.domain.Trait
import app.rustle.ui.art.Art
import app.rustle.ui.art.Buf
import app.rustle.ui.art.Critter
import app.rustle.ui.art.Look
import app.rustle.ui.art.Nook
import app.rustle.ui.art.NookScene
import org.junit.Test
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.zip.CRC32
import java.util.zip.DeflaterOutputStream

/** Draws the nook and him straight to PNGs on the desktop JVM (run with `-Prender --tests '*RenderArt*'`). */
class RenderArt {
    private val out = File(System.getProperty("render.out") ?: "build/art", "art").apply { mkdirs() }

    private fun save(b: Buf, name: String, scale: Int = 4) {
        val w = b.w * scale; val h = b.h * scale
        val raw = ByteArrayOutputStream()
        DeflaterOutputStream(raw).use { z ->
            for (y in 0 until h) {
                z.write(0)
                for (x in 0 until w) { val c = b.d[(y / scale) * b.w + x / scale]; z.write(c shr 16 and 0xFF); z.write(c shr 8 and 0xFF); z.write(c and 0xFF); z.write(c ushr 24) }
            }
        }
        val png = ByteArrayOutputStream()
        val o = DataOutputStream(png)
        o.write(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10))
        fun chunk(type: String, data: ByteArray) {
            o.writeInt(data.size); val t = type.toByteArray(); o.write(t); o.write(data)
            o.writeInt(CRC32().apply { update(t); update(data) }.value.toInt())
        }
        chunk("IHDR", ByteArrayOutputStream().also { DataOutputStream(it).apply { writeInt(w); writeInt(h); write(byteArrayOf(8, 6, 0, 0, 0)) } }.toByteArray())
        chunk("IDAT", raw.toByteArray())
        chunk("IEND", ByteArray(0))
        File(out, "$name.png").writeBytes(png.toByteArray())
    }

    private val all = setOf("lantern", "rope", "bed", "rug", "lamp", "picture", "plant", "garland", "clock", "shelf", "telescope", "earmuffs", "ladder")
    private val look = Look(0xC8946A, 0xF2DCC0, setOf(Trait.SLEEPYHEAD), setOf("scarf"))

    private fun scene(name: String, hour: Float, pose: Pose = Pose.IDLE, built: Set<String> = all, charging: Boolean = true, level: Int = 70, dark: Boolean = false, wx: app.rustle.domain.Wx? = null) {
        Art.still = true
        val b = Buf(Nook.W, Nook.H)
        Nook.draw(b, NookScene(look, pose, level, charging, hour, dark, false, false, built, listOf("key_diary", "gem_amber", "coin_foreign"), 10, wx = wx), 40)
        save(b, name)
    }

    @Test fun nook() {
        scene("day", 13f)
        scene("evening", 20.5f, Pose.WARM)
        scene("night", 1f, Pose.SLEEP)
        scene("bare", 15f, built = emptySet(), charging = false)
        scene("cold", 9f, Pose.COLD, built = setOf("rug"), charging = false, level = 8)
    }

    @Test fun items() {
        val all = app.rustle.domain.Catalog.finds
        val cols = 10
        val n = app.rustle.ui.art.Items.S + 4
        val b = Buf(cols * n + 2, (all.size + cols - 1) / cols * n + n + 2)
        b.d.fill(0xFFF3E8CF.toInt())
        all.forEachIndexed { i, f -> b.blit(app.rustle.ui.art.Items.of(f), 2 + (i % cols) * n, 2 + (i / cols) * n) }
        val I = app.rustle.ui.art.Icons
        listOf(I.bowl, I.bundle, I.maze, I.moon, I.sun, I.home, I.heart(true), I.heart(false), I.crumb(true), I.crumb(false)).forEachIndexed { i, ic -> b.blit(ic, 2 + i * n, b.h - n) }
        save(b, "items", 5)
    }

    private fun sheet(name: String, looks: List<Look>, pose: Pose = Pose.IDLE) {
        Art.still = true
        val cols = 4
        val b = Buf(cols * (Critter.W + 4), (looks.size + cols - 1) / cols * (Critter.H + 4))
        b.d.fill(0xFF1E4A3A.toInt())
        looks.forEachIndexed { i, l -> b.blit(Critter.draw(l, pose, 120), (i % cols) * (Critter.W + 4) + 2, (i / cols) * (Critter.H + 4) + 2) }
        save(b, name, 5)
    }

    @Test fun species() = sheet("species", app.rustle.domain.Species.entries.map { Look(it.body, it.accent, species = it) })

    @Test fun marks() = sheet("marks", app.rustle.domain.Marks.entries.map { Look(0xC8946A, 0xF2DCC0, marks = it) } + app.rustle.domain.Marks.entries.take(2).map { Look(0x8A8A92, 0xE8E8EE, species = app.rustle.domain.Species.CAT, marks = it) })

    @Test fun wear() = sheet("wear", app.rustle.domain.Catalog.builds.filter { it.kind == app.rustle.domain.Kind.WEAR }.map { Look(0xC8946A, 0xF2DCC0, wear = setOf(it.key)) })

    @Test fun furniture() {
        scene("front1", 15f, built = all + setOf("map", "armchair", "fern", "musicbox", "snowglobe") - setOf("picture", "plant", "telescope"))
        scene("front2", 21f, built = all + setOf("mirror", "gramophone", "radio") - setOf("picture"))
        scene("front3", 11f, built = all + setOf("pouf", "aquarium"))
    }

    @Test fun weather() {
        scene("wx_rain", 14f, wx = app.rustle.domain.Wx.RAIN)
        scene("wx_snow", 12f, wx = app.rustle.domain.Wx.SNOW)
        scene("wx_fog", 10f, wx = app.rustle.domain.Wx.FOG)
        scene("wx_storm", 22.5f, wx = app.rustle.domain.Wx.STORM)
    }

    @Test fun critter() {
        Art.still = true
        val poses = listOf(Pose.IDLE, Pose.WARM, Pose.SLEEP, Pose.COLD, Pose.HUNGRY, Pose.DIZZY, Pose.DARK, Pose.HOT)
        val b = Buf(Critter.W * poses.size + 2 * poses.size, Critter.H + 4)
        b.d.fill(0xFF1E4A3A.toInt())
        poses.forEachIndexed { i, p -> b.blit(Critter.draw(look, p, 40), i * (Critter.W + 2) + 1, 2) }
        save(b, "critter", 6)
    }
}
