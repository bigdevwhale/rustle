package app.rustle.domain

import app.rustle.data.DAY
import app.rustle.data.State
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * Three things he asks for each week. Some are handed over from the pantry ([Ask.Bring]), the rest
 * count what happened since Monday. Each one done brings a present: something from the open districts
 * that is often not in the collection yet.
 */
sealed class Ask(val key: String) {
    abstract val goal: Int
    abstract fun progress(s: State, since: Long): Int

    /** Hand over [n] things of a shape, "for" a little reason of his. */
    class Bring(key: String, val shape: Shape, val n: Int, val why: L) : Ask(key) {
        override val goal get() = n
        override fun progress(s: State, since: Long) = Catalog.have(s.inv, shape).coerceAtMost(n)
    }

    /** Go to a district [n] times, all the way. */
    class Visit(key: String, val d: District, val n: Int) : Ask(key) {
        override val goal get() = n
        override fun progress(s: State, since: Long) = s.trips.count { it.at >= since && it.district == d && !it.cut }
    }

    /** One outing of at least [minutes], all the way. */
    class Far(key: String, val minutes: Int) : Ask(key) {
        override val goal get() = 1
        override fun progress(s: State, since: Long) = if (s.trips.any { it.at >= since && !it.cut && it.minutes >= minutes }) 1 else 0
    }

    class Nights(key: String, val n: Int) : Ask(key) {
        override val goal get() = n
        override fun progress(s: State, since: Long) = s.nights.count { it.at >= since && it.grade == Grade.GOOD }
    }

    class Trips(key: String, val n: Int) : Ask(key) {
        override val goal get() = n
        override fun progress(s: State, since: Long) = s.trips.count { it.at >= since && !it.cut }
    }

    /** New kinds of finds this week. */
    class Fresh(key: String, val n: Int) : Ask(key) {
        override val goal get() = n
        override fun progress(s: State, since: Long) = s.seen.values.count { it >= since }
    }

    fun done(s: State, since: Long) = progress(s, since) >= goal
}

object Requests {
    /** Weeks start on Monday. */
    fun week(day: Long): Long = Math.floorDiv(day + 3, 7)

    fun start(week: Long): Long =
        LocalDate.ofEpochDay(week * 7 - 3).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private val reasons: Map<Shape, L> = mapOf(
        Shape.THREAD to L("to darn a sock", "заштопать носок"),
        Shape.FEATHER to L("to stuff a pillow", "набить подушку"),
        Shape.BUTTON to L("for the neighbour's coat", "соседу на пальто"),
        Shape.BEAD to L("for a necklace for the moth", "на бусы для моли"),
        Shape.SPRING to L("the bed creaks", "кровать скрипит"),
        Shape.COIN to L("for a rainy day", "на чёрный день"),
        Shape.SCREW to L("the shelf is wobbling", "полка шатается"),
        Shape.PIN to L("to pin up the curtains", "заколоть занавески"),
        Shape.STAR to L("to decorate the window", "украсить окно"),
        Shape.SCRAP to L("to write a letter", "написать письмо"),
        Shape.LED to L("for a night light", "на ночник"),
        Shape.SUGAR to L("for jam", "на варенье"),
        Shape.SEED to L("to plant by the window", "посадить у окна"),
        Shape.MAGNET to L("to find lost pins", "искать потерянные булавки"),
    )

    /** This week's three requests, the same all week for a household with the same open districts. */
    fun of(s: State, now: Long): List<Ask> {
        val w = week(World.dayOf(now))
        val r = Random(w * 7919 + s.started / DAY)
        val open = World.open(s)
        val have = Catalog.finds.filter { it.district in open && !it.shape.food && it.rare == 1 }.map { it.shape }.distinct().filter { it in reasons }
        val shape = have.random(r)
        val bring = Ask.Bring("w${w}_bring", shape, 2 + r.nextInt(2), reasons.getValue(shape))
        val d = open.random(r)
        val second = listOf(
            Ask.Visit("w${w}_visit", d, 2),
            Ask.Far("w${w}_long", listOf(45, 60, 90)[r.nextInt(3)]),
            Ask.Trips("w${w}_trips", 4 + r.nextInt(3)),
        ).random(r)
        val third = listOf(
            Ask.Nights("w${w}_nights", 2 + r.nextInt(2)),
            Ask.Fresh("w${w}_fresh", 2),
            Ask.Trips("w${w}_trips2", 3),
        ).filter { it::class != second::class }.random(r)
        return listOf(bring, second, third)
    }

    /** What he says he wants, in a short line. */
    fun text(a: Ask, ru: Boolean): String = when (a) {
        is Ask.Bring -> if (ru) "Принеси: ${a.shape.label.ru} ×${a.n} — ${a.why.ru}" else "Bring: ${a.shape.label.en} ×${a.n} — ${a.why.en}"
        is Ask.Visit -> if (ru) "Дважды дойти до конца: ${a.d.name_.ru}" else "Go all the way: ${a.d.name_.en}, ${a.n} times"
        is Ask.Far -> if (ru) "Вылазка от ${a.minutes} минут — до самого конца" else "An outing of ${a.minutes} minutes or more, all the way"
        is Ask.Nights -> if (ru) "Хорошие ночи: ${a.n}" else "Good nights: ${a.n}"
        is Ask.Trips -> if (ru) "Вылазки до конца: ${a.n}" else "Outings all the way: ${a.n}"
        is Ask.Fresh -> if (ru) "Новые находки: ${a.n}" else "New kinds of finds: ${a.n}"
    }

    /** The present for a request: an unfound thing from an open district if there is one, else something uncommon. */
    fun gift(s: State, key: String): Find {
        val r = Random(key.hashCode().toLong())
        val open = World.open(s)
        val pool = Catalog.finds.filter { it.district in open && !it.shape.food }
        // Half the time something new for the collection, otherwise an uncommon thing for the workshop.
        val fresh = if (r.nextBoolean()) pool.filter { it.key !in s.seen && it.rare >= 2 }.randomOrNull(r) else null
        return fresh ?: pool.filter { it.rare == 2 }.randomOrNull(r) ?: pool.random(r)
    }
}
