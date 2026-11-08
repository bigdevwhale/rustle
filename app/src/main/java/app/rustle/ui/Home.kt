package app.rustle.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.staticCompositionLocalOf
import app.rustle.data.State
import app.rustle.device.Phone
import app.rustle.domain.Catalog
import app.rustle.domain.District
import app.rustle.domain.Needs
import app.rustle.domain.Pose
import app.rustle.domain.Rank
import app.rustle.domain.Trait
import app.rustle.domain.World
import app.rustle.ui.art.Look
import java.time.Instant
import java.time.ZoneId

/** Everything the screens read, made once per change. */
class Home(val s: State, val now: Long, val ru: Boolean) {
    val full = Needs.full(s, now)
    val mood = Needs.mood(s, now)
    val traits = Trait.of(s)
    val rank = Rank.of(s)
    val open: List<District> = World.open(s)
    val look = Look(s.body, s.accent, traits, s.wear, app.rustle.domain.Species.of(s.species), app.rustle.domain.Marks.of(s.marks))
    /** What stands in the nook: one thing per place. */
    val placed: Set<String> = Catalog.placed(s.built, s.place)
    val away = s.trip?.takeIf { it.ended == 0L && !it.over(now) }
    val asleep = s.night?.takeIf { it.ended == 0L }
    /** When he went into the Net, while he is there (#1). */
    val surfing: Long? = s.netAt.takeIf { app.rustle.domain.Net.inNet(s, now) }
    /** A week since the last spring-clean (#1). */
    val cleanDue = app.rustle.domain.SpringClean.due(s, now)
    val name = s.name.ifBlank { if (ru) "Шуршик" else "Rustle" }
    val days = World.days(s, now)
    /** Whether the window knows a place, and the weather there if it is fresh (three hours). */
    val placed_ = !s.lat.isNaN()
    val wx: app.rustle.domain.Wx? = if (s.weatherOn && now - s.wxAt < 3 * app.rustle.data.HOUR) runCatching { app.rustle.domain.Wx.valueOf(s.wx) }.getOrNull() else null
    val temp: Int? = if (wx != null && s.temp != Int.MIN_VALUE) s.temp else null

    /**
     * The hour the scene paints: the clock, or — when the place is known — an hour matched to where the
     * sun really is there, so dusk in the window comes with the real sunset.
     */
    val hour: Float = run {
        if (app.rustle.BuildConfig.SHOTS) return@run 19.2f
        val clock = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).let { it.hour + it.minute / 60f }
        if (!placed_) return@run clock
        val alt = app.rustle.domain.Sky.sunAlt(now, s.lat, s.lon)
        val rising = app.rustle.domain.Sky.sunAlt(now + 20 * 60_000L, s.lat, s.lon) > alt
        when {
            alt < -6 -> if (clock in 4f..12f) 4.5f else 1f
            alt < 8 -> if (rising) (5 + (alt + 6) / 14 * 4).toFloat() else (18 + (8 - alt) / 14 * 4).toFloat()
            else -> clock.coerceIn(9f, 17.5f)
        }
    }
    val food = s.inv.mapNotNull { (k, n) -> Catalog.find(k)?.takeIf { it.food > 0 && n > 0 } }.sortedBy { it.food }

    fun pose(p: Phone): Pose = when {
        away != null || surfing != null -> Pose.AWAY
        asleep != null -> Pose.SLEEP
        p.dizzy(now) -> Pose.DIZZY
        p.low -> Pose.COLD
        p.hot -> Pose.HOT
        full < 20 -> Pose.HUNGRY
        p.dark -> Pose.DARK
        p.sunny -> Pose.SUN
        p.charging -> Pose.WARM
        else -> Pose.IDLE
    }

    /** The rarest finds, for the shelf. */
    val best: List<String> = s.seen.keys.mapNotNull { Catalog.find(it) }.filter { !it.shape.food }.sortedWith(compareByDescending<app.rustle.domain.Find> { it.rare }.thenBy { s.seen[it.key] }).take(4).map { it.key }
}

/** A toast with a heading, queued so several can follow one another. */
data class Note(val title: String, val body: String)

@Stable
class Bus {
    val notes = mutableStateListOf<Note>()
    fun note(title: String, body: String) { notes += Note(title, body) }
}

val LocalBus = staticCompositionLocalOf { Bus() }
