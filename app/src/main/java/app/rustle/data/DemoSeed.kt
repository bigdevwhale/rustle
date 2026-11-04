package app.rustle.data

import app.rustle.AppContainer
import app.rustle.domain.District
import app.rustle.domain.Grade
import app.rustle.domain.Loot
import app.rustle.ru
import kotlin.random.Random

/** The demo build moves into a lived-in nook: three weeks of outings and nights, a few things made. */
object DemoSeed {
    suspend fun seedIfEmpty(c: AppContainer) {
        if (c.store.now().onboarded) return
        c.store.update { seeded(System.currentTimeMillis(), c.app.ru) }
    }

    fun seeded(now: Long, ru: Boolean): State {
        val r = Random(7)
        val start = now - 21 * DAY
        val trips = mutableListOf<TripLog>()
        val nights = mutableListOf<NightLog>()
        val seen = mutableMapOf<String, Long>()
        val inv = mutableMapOf<String, Int>()
        for (day in 0 until 21) {
            val d = when { day < 6 -> District.PORT; day < 12 -> listOf(District.PORT, District.CELLAR)[day % 2]; else -> listOf(District.CELLAR, District.BRIDGE, District.PORT)[day % 3] }
            val at = start + day * DAY + 10 * HOUR + r.nextLong(4 * HOUR)
            val min = listOf(15, 25, 45, 60, 25, 90)[r.nextInt(6)]
            val cut = r.nextDouble() < .15
            val finds = Loot.roll(d, if (cut) min / 2 else min, !cut, day > 9, false, at)
            for (f in finds) { inv[f.key] = (inv[f.key] ?: 0) + 1; seen.putIfAbsent(f.key, at + min * MIN) }
            trips += TripLog(at, d, if (cut) min / 2 else min, cut, finds.map { it.key }, r.nextInt(4))
            if (day % 2 == 0) {
                val g = listOf(Grade.GOOD, Grade.GOOD, Grade.OK, Grade.GOOD, Grade.BAD)[r.nextInt(5)]
                nights += NightLog(start + day * DAY - 2 * HOUR, if (g == Grade.GOOD) 7.5f else 5.5f, if (g == Grade.GOOD) 1 else 14, g, null)
            }
        }
        // Enough left over to build one more thing on the workshop tab.
        for (k in listOf("thread_red", "thread_blue", "thread_green", "led_amber", "spring_coil", "pin_brass", "pin_silver", "pin_safety", "screw_tiny", "button_red", "button_shirt", "seed_sunflower", "bead_glass", "raisin", "honey", "rusk"))
            inv[k] = (inv[k] ?: 0) + 2
        for (k in inv.keys) seen.putIfAbsent(k, start + DAY)
        val today = app.rustle.domain.World.dayOf(now)
        val st = State(
            onboarded = true, name = if (ru) "Шуршик" else "Rustle", body = 0xC8946A, accent = 0xF2DCC0, started = start,
            full = 64.0, fullAt = now, mood = 82.0, moodAt = now, inv = inv, seen = seen,
            built = setOf("lantern", "rope", "bed", "rug", "lamp", "shelf", "picture", "plant", "garland", "clock", "armchair", "fern", "scarf", "backpack", "beret", "glasses", "bowtie"),
            wear = setOf("scarf", "beret"),
            trips = trips.sortedByDescending { it.at }, nights = nights.sortedByDescending { it.at }, pets = 240, dishes = 6, notifAsked = true,
            tips = setOf("home", "trips", "shelf", "workshop", "maze"), careDays = (0L..11L).map { today - it }.toSet(), mornings = 9, gamesWon = 4,
            cooked = mapOf("tea" to 3, "pie" to 3), species = "MOUSE", marks = "NONE",
            lat = 59.94, lon = 30.31, placeName = if (ru) "Санкт-Петербург" else "Saint Petersburg", locAsked = true, wx = "SNOW", temp = -2, wxAt = now,
            place = mapOf("FRONT_L" to "armchair", "FRONT_R" to "fern"),
        )
        return st.copy(feats = app.rustle.domain.Feats.fresh(st).associate { it.key to now - DAY })
    }
}
