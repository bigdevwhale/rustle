package app.rustle.domain

import app.rustle.data.DAY
import app.rustle.data.HOUR
import app.rustle.data.MIN
import app.rustle.data.Night
import app.rustle.data.State
import app.rustle.data.Trip
import java.time.Instant
import java.time.ZoneId
import kotlin.random.Random

enum class Grade { GOOD, OK, BAD }

/** What an outing brings back. Pure: the same trip always rolls the same finds. */
object Loot {
    /** How many finds a walk of [minutes] brings. */
    fun count(minutes: Int, finished: Boolean, backpack: Boolean): Int {
        val base = when {
            minutes < 10 -> 0
            minutes < 20 -> 1
            minutes < 40 -> 2
            minutes < 70 -> 3
            minutes < 100 -> 4
            else -> 5
        }
        if (base == 0) return 0
        return base + (if (finished) 1 else 0) + (if (backpack && minutes >= 30) 1 else 0)
    }

    /** Long outings in a row without a wonder after which the next long one is sure to bring one. */
    const val PITY = 6

    /**
     * [seen] makes things not yet in the collection turn up a little more often; [pity] puts a wonder
     * nobody has found yet into a long, finished walk.
     */
    /** Hearts at or above this make him cheerful; at or below [GLUM], glum. Crumbs at or below [HUNGRY], hungry. */
    const val CHEERFUL = 70.0
    const val GLUM = 30.0
    const val HUNGRY = 20.0

    /**
     * How his mood and belly change an outing's haul: cheerful brings one more from a walk of 25 minutes
     * or longer, glum or hungry one fewer each. Read when he sets off.
     */
    fun bonus(mood: Double, full: Double, minutes: Int): Int =
        (if (mood >= CHEERFUL && minutes >= 25) 1 else 0) - (if (mood <= GLUM) 1 else 0) - (if (full <= HUNGRY) 1 else 0)

    fun roll(d: District, minutes: Int, finished: Boolean, backpack: Boolean, telescope: Boolean, seed: Long, seen: Set<String> = emptySet(), pity: Boolean = false, bonus: Int = 0): List<Find> {
        val base = count(minutes, finished, backpack)
        val n = if (base == 0) 0 else (base + bonus).coerceAtLeast(1)
        if (n == 0) return emptyList()
        val r = Random(seed)
        val pool = Catalog.of(d)
        val rareOk = minutes >= 45
        fun weight(f: Find) = when (f.rare) {
            1 -> 10.0
            2 -> 3.0 + minutes / 30.0
            else -> if (rareOk) (if (telescope) 2.5 else 1.4) + minutes / 60.0 else 0.0
        } * (if (f.key !in seen && f.rare > 1) 1.3 else 1.0)
        fun pick(from: List<Find>): Find {
            val total = from.sumOf { weight(it) }
            var x = r.nextDouble() * total
            for (f in from) { x -= weight(f); if (x <= 0) return f }
            return from.last()
        }
        val out = mutableListOf<Find>()
        // Always something to eat from a proper walk, so he never comes home with an empty pocket and an empty belly.
        if (n >= 3) pool.filter { it.shape.food }.takeIf { it.isNotEmpty() }?.let { out += pick(it) }
        if (pity && rareOk && finished) pool.filter { it.rare == 3 }.sortedBy { if (it.key in seen) 1 else 0 }.firstOrNull()?.let { out += it }
        while (out.size < n) out += pick(pool)
        return out
    }
}

/** Hunger and mood run down with time; nothing ever dies, a hungry one just sits wrapped in a blanket. */
object Needs {
    const val FULL_PER_HOUR = 4.2
    const val MOOD_PER_HOUR = 2.0

    fun full(s: State, now: Long) = (s.full - (now - s.fullAt).coerceAtLeast(0) / HOUR.toDouble() * FULL_PER_HOUR).coerceIn(0.0, 100.0)
    fun mood(s: State, now: Long) = (s.mood - (now - s.moodAt).coerceAtLeast(0) / HOUR.toDouble() * MOOD_PER_HOUR).coerceIn(0.0, 100.0)

    fun feed(s: State, now: Long, food: Int, mood: Int = 0) =
        s.copy(full = (full(s, now) + food).coerceAtMost(100.0), fullAt = now, mood = (mood(s, now) + mood).coerceAtMost(100.0), moodAt = now)

    fun cheer(s: State, now: Long, by: Int) = s.copy(mood = (mood(s, now) + by).coerceIn(0.0, 100.0), moodAt = now)
}

/** Nights: a long sleep with the phone left alone is a good one. */
object Nights {
    /** He wakes up on the first unlock after this hour, if he has slept at least [MIN_SLEEP]. */
    const val WAKE_HOUR = 5
    const val MIN_SLEEP = 4 * HOUR
    /** A quarter of an hour with the screen dark and he is asleep: what came before was the phone before bed. */
    const val SETTLE = 15 * MIN

    /** Marks him asleep once the screen has been dark for [SETTLE]. */
    fun settle(n: Night, now: Long): Night = if (n.settled == 0L && n.downAt > 0 && now - n.downAt >= SETTLE) n.copy(settled = n.downAt) else n

    /** When he fell asleep: the start of the first quiet spell, or bedtime if the phone was never seen put down. */
    fun asleepFrom(n: Night, now: Long): Long = settle(n, now).settled.takeIf { it > 0 } ?: n.start

    /** The screen went dark. Before he is asleep it only starts the quiet spell; after, it closes an unlock. */
    fun screenOff(n: Night, now: Long): Night = when {
        n.settled == 0L -> n.copy(downAt = now, unlockAt = 0)
        n.unlockAt > 0 -> n.copy(peekMs = n.peekMs + (now - n.unlockAt), unlockAt = 0)
        else -> n
    }

    /** The phone was unlocked. Before he is asleep it costs nothing: the quiet spell starts over. */
    fun unlocked(n: Night, now: Long): Night {
        val m = settle(n, now)
        return if (m.settled == 0L) m.copy(downAt = 0, unlockAt = now) else m.copy(unlockAt = if (m.unlockAt == 0L) now else m.unlockAt)
    }

    /** The first unlock of the morning, after enough sleep, wakes him up. */
    fun morning(n: Night, now: Long): Boolean {
        val m = settle(n, now)
        return m.settled > 0 && now - m.settled >= MIN_SLEEP && hourOf(now) in WAKE_HOUR..13
    }

    fun hours(n: Night, end: Long): Float = (end - asleepFrom(n, end)).coerceAtLeast(0) / HOUR.toFloat()

    /** Minutes the screen was on while he slept. */
    fun peekMin(n: Night): Int = (n.peekMs / MIN).toInt()

    fun grade(n: Night, end: Long): Grade {
        val hours = hours(n, end)
        val peek = peekMin(n)
        return when {
            hours >= 5.5 && peek <= 10 -> Grade.GOOD
            hours >= 4 && peek <= 30 -> Grade.OK
            else -> Grade.BAD
        }
    }

    fun mood(g: Grade, bed: Boolean) = when (g) { Grade.GOOD -> if (bed) 30 else 22; Grade.OK -> 10; Grade.BAD -> -6 }

    fun hourOf(ms: Long): Int = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).hour

    /** Evenings and the small hours: the time "to bed" makes sense. */
    fun bedtime(now: Long) = hourOf(now).let { it >= 19 || it < 4 }

    /** A good night brings a find from his dream, an ordinary one sometimes a crumb. */
    fun dreamFind(g: Grade, open: List<District>, seed: Long): Find? {
        val r = Random(seed)
        val pool = open.flatMap { Catalog.of(it) }.filter { it.rare <= 2 }
        return when (g) {
            Grade.GOOD -> pool.random(r)
            Grade.OK -> pool.filter { it.shape.food }.takeIf { r.nextDouble() < .5 }?.random(r)
            Grade.BAD -> null
        }
    }
}

/** Traits grow from how the household lives, and show on him. */
enum class Trait(val name_: L, val note: L) {
    WANDERER(L("Wanderer", "Странник"), L("Ten hours on outings: a windblown tuft.", "Десять часов в вылазках: вихор торчит от ветра.")),
    SLEEPYHEAD(L("Sleepyhead", "Соня"), L("Seven good nights: rosy cheeks.", "Семь хороших ночей: румяные щёки.")),
    OWL(L("Night owl", "Сова"), L("Three restless nights: ear tufts like an owl's.", "Три беспокойные ночи: кисточки на ушах, как у совы.")),
    CUDDLY(L("Cuddly", "Неженка"), L("Stroked three hundred times: extra fluffy.", "Гладили триста раз: пушистее обычного.")),
    GOURMET(L("Gourmet", "Гурман"), L("Ten dishes from the kitchen: a round belly.", "Десять блюд с кухни: круглый живот.")),
    ;

    companion object {
        fun of(s: State): Set<Trait> = buildSet {
            if (s.trips.sumOf { it.minutes } >= 600) add(WANDERER)
            if (s.nights.count { it.grade == Grade.GOOD } >= 7) add(SLEEPYHEAD)
            if (s.nights.count { it.grade == Grade.BAD } >= 3) add(OWL)
            if (s.pets >= 300) add(CUDDLY)
            if (s.dishes >= 10) add(GOURMET)
        }
    }
}

enum class Rank(val title: L, val from: Int) {
    TENANT(L("Tenant", "Квартирант"), 0),
    LODGER(L("Lodger", "Жилец"), 25),
    MASTER(L("Master of the house", "Хозяин"), 80),
    SPIRIT(L("House spirit", "Домовой"), 200);

    companion object {
        fun points(s: State) = s.seen.size * 2 + s.trips.count { !it.cut } + s.nights.count { it.grade == Grade.GOOD } * 2 + s.built.size * 3
        fun of(s: State) = entries.last { points(s) >= it.from }
        fun next(s: State) = entries.firstOrNull { it.from > points(s) }
    }
}

object World {
    fun open(s: State): List<District> = District.entries.filter { it.tool == null || it.tool in s.built }
    fun dayOf(ms: Long): Long = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
    fun started(s: State, now: Long) = if (s.started > 0) s.started else now
    fun days(s: State, now: Long) = ((now - started(s, now)) / DAY).toInt() + 1

    /** Takes [trip]'s finds: rolled once from its start, put in hand, and the trip written into the log. */
    fun claim(s: State, trip: Trip, now: Long, line: Int): Pair<State, List<Find>> {
        val end = trip.endAt(now)
        val minutes = ((end - trip.start) / MIN).toInt()
        val finished = !trip.cut && (trip.minutes == 0 || end >= trip.due)
        val long = finished && minutes >= 45
        val bonus = Loot.bonus(Needs.mood(s, trip.start), Needs.full(s, trip.start), minutes)
        val finds = Loot.roll(trip.district, minutes, finished, "backpack" in s.wear && "backpack" in s.built, "telescope" in s.built, trip.start,
            s.seen.keys, pity = long && s.dryLong >= Loot.PITY - 1, bonus = bonus)
        var inv = s.inv
        var seen = s.seen
        for (f in finds) { inv = inv + (f.key to (inv[f.key] ?: 0) + 1); if (f.key !in seen) seen = seen + (f.key to end) }
        val log = app.rustle.data.TripLog(trip.start, trip.district, minutes, !finished, finds.map { it.key }, line)
        val cheered = Needs.cheer(s, now, if (finished) 12 else 3)
        // Counts long outings since the last wonder, for the pity rule.
        val dry = when { finds.any { it.rare == 3 } -> 0; long -> s.dryLong + 1; else -> s.dryLong }
        return cheered.copy(inv = inv, seen = seen, trip = null, trips = (listOf(log) + s.trips).take(400), dryLong = dry) to finds
    }
}
