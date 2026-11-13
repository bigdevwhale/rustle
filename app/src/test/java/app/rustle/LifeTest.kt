package app.rustle

import app.rustle.data.DAY
import app.rustle.data.HOUR
import app.rustle.data.Json
import app.rustle.data.MIN
import app.rustle.data.Night
import app.rustle.data.State
import app.rustle.data.Trip
import app.rustle.domain.Catalog
import app.rustle.domain.District
import app.rustle.domain.Grade
import app.rustle.domain.Kind
import app.rustle.domain.Loot
import app.rustle.domain.Needs
import app.rustle.domain.Nights
import app.rustle.domain.Shape
import app.rustle.domain.World
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LifeTest {
    @Test fun keysAreUnique() {
        assertEquals(Catalog.finds.size, Catalog.finds.map { it.key }.toSet().size)
        assertEquals(Catalog.builds.size, Catalog.builds.map { it.key }.toSet().size)
    }

    /** Every tool can be paid for from the districts open before it, so nobody gets stuck. */
    @Test fun toolsAreReachableInOrder() {
        val open = mutableSetOf(District.PORT)
        for (d in District.entries.drop(1)) {
            val tool = Catalog.build(d.tool!!)!!
            for (s in tool.cost.keys) assertTrue("${tool.key} needs $s", Catalog.finds.any { it.shape == s && it.district in open && it.rare <= 2 })
            open += d
        }
        for (b in Catalog.builds) for (s in b.cost.keys) assertTrue("${b.key} $s", Catalog.finds.any { it.shape == s })
        for (d in Catalog.dishes) for (s in d.cost.keys) assertTrue(s.food)
        assertTrue(Catalog.builds.count { it.kind == Kind.TOOL } == District.entries.size - 1)
    }

    @Test fun lootIsStableAndGrowsWithTime() {
        val a = Loot.roll(District.PORT, 45, true, false, false, 42L)
        assertEquals(a.map { it.key }, Loot.roll(District.PORT, 45, true, false, false, 42L).map { it.key })
        assertEquals(0, Loot.count(5, true, false))
        assertTrue(Loot.count(90, true, true) > Loot.count(25, true, false))
        assertTrue(a.any { it.shape.food })
        // No wonders on short walks.
        repeat(200) { assertTrue(Loot.roll(District.DOME, 25, true, false, true, it.toLong()).none { f -> f.rare == 3 }) }
    }

    @Test fun pityBringsAWonder() {
        // A long, finished outing with pity on always carries a wonder from that district.
        repeat(20) { seed ->
            val got = Loot.roll(District.CELLAR, 60, true, false, false, seed.toLong(), emptySet(), pity = true)
            assertTrue(got.any { it.rare == 3 })
        }
        val t = Trip(District.CELLAR, 1_000_000, 60)
        var s = State(dryLong = Loot.PITY - 1)
        s = World.claim(s, t, t.due, 0).first
        assertEquals(0, s.dryLong)
    }

    @Test fun weeklyRequestsAreStableAndReachable() {
        val s = State(onboarded = true, started = 0)
        val now = 100 * DAY
        val a = app.rustle.domain.Requests.of(s, now).map { it.key }
        assertEquals(a, app.rustle.domain.Requests.of(s, now + HOUR).map { it.key })
        assertEquals(3, a.toSet().size)
        assertTrue(app.rustle.domain.Requests.of(s, now + 7 * DAY).map { it.key } != a)
    }

    @Test fun payTakesCommonsFirst() {
        val inv = mapOf("thread_red" to 2, "thread_gold" to 2)
        val left = Catalog.pay(inv, mapOf(Shape.THREAD to 3))
        assertEquals(mapOf("thread_gold" to 1), left)
    }

    @Test fun needsRunDownButNeverBelowZero() {
        val s = State(full = 50.0, fullAt = 0)
        assertEquals(50 - Needs.FULL_PER_HOUR * 10, Needs.full(s, 10 * HOUR), 1e-6)
        assertEquals(0.0, Needs.full(s, 10 * DAY), 1e-6)
        assertEquals(100.0, Needs.full(Needs.feed(s, 0, 80), 0), 1e-6)
    }

    @Test fun nights() {
        val n = Night(0, true, peekMs = 3 * MIN)
        assertEquals(Grade.GOOD, Nights.grade(n, 7 * HOUR))
        assertEquals(Grade.OK, Nights.grade(n.copy(peekMs = 15 * MIN), 5 * HOUR))
        assertEquals(Grade.BAD, Nights.grade(n.copy(peekMs = 40 * MIN), 7 * HOUR))
    }

    /** Scrolling in bed after "to bed" doesn't spoil the night; he falls asleep once the phone is put down (#7). */
    @Test fun nightStartsWhenThePhoneIsPutDown() {
        val bed = 22 * HOUR
        var n = Night(bed, true, unlockAt = bed)
        // Forty minutes of the phone before sleep: off for a moment, on again, then put down for good.
        n = Nights.screenOff(n, bed + 20 * MIN)
        n = Nights.unlocked(n, bed + 22 * MIN)
        n = Nights.screenOff(n, bed + 40 * MIN)
        assertEquals(0L, n.peekMs)
        n = Nights.settle(n, bed + 60 * MIN)
        assertEquals(bed + 40 * MIN, n.settled)
        // A glance at the clock in the night counts; the morning unlock ends it.
        n = Nights.unlocked(n, bed + 4 * HOUR)
        n = Nights.screenOff(n, bed + 4 * HOUR + 2 * MIN)
        assertEquals(2 * MIN, n.peekMs)
        assertEquals(Grade.GOOD, Nights.grade(n, bed + 7 * HOUR))
        assertTrue(Nights.hours(n, bed + 7 * HOUR) in 6.3f..6.4f)
    }

    /** A night where the phone was never seen put down (the watcher was stopped) still counts from bedtime. */
    @Test fun nightWithoutScreenEvents() {
        val n = Night(0, true)
        assertEquals(Grade.GOOD, Nights.grade(n, 7 * HOUR))
    }

    @Test fun claimPutsFindsAway() {
        val t = Trip(District.PORT, 1_000_000, 30)
        val (s, finds) = World.claim(State(), t, t.due + 5 * MIN, 0)
        assertTrue(finds.isNotEmpty())
        assertEquals(finds.size, s.inv.values.sum())
        assertEquals(30, s.trips.single().minutes)
        assertEquals(null, s.trip)
    }

    @Test fun jsonRoundTrip() {
        val s = State(onboarded = true, name = "Шуршик", inv = mapOf("rusk" to 2), seen = mapOf("rusk" to 5L), built = setOf("lantern"),
            trip = Trip(District.CELLAR, 10, 25, peekMs = 7, unlockAt = 3), night = Night(1, true, 2, 0, 0))
        assertEquals(s, Json.read(Json.write(s)))
    }
    /** The spring-clean and the Net (#1): new fields survive a save and old saves load with them empty. */
    @Test fun choresSaveAndOldSavesLoad() {
        val s = State(onboarded = true, cloths = 2, cleanAt = 5 * DAY, cleans = 3, netAt = 77, netDay = 20, netTrips = 9)
        assertEquals(s, Json.read(Json.write(s)))
        val old = Json.read("""{"onboarded":true,"name":"x"}""")
        assertEquals(0, old.cloths); assertEquals(0L, old.cleanAt); assertEquals(0L, old.netAt); assertEquals(0, old.netTrips)
    }

    @Test fun springCleanComesWeekly() {
        val sc = app.rustle.domain.SpringClean
        val s = State(onboarded = true, started = 10 * DAY)
        assertTrue(!sc.due(s, 16 * DAY))
        assertTrue(sc.due(s, 17 * DAY))
        assertTrue(!sc.due(s.copy(cleanAt = 17 * DAY), 23 * DAY))
        assertTrue(sc.due(s.copy(cleanAt = 17 * DAY), 24 * DAY))
        assertTrue(!sc.canSew(State(inv = mapOf("thread_red" to 2))))
        assertTrue(sc.canSew(State(inv = mapOf("thread_red" to 2, "thread_gold" to 1))))
        assertTrue(!sc.canSew(State(inv = mapOf("thread_red" to 9), cloths = sc.MAX_CLOTHS)))
        assertTrue(!sc.find(s, 3).shape.food)
    }

    @Test fun netOnWifiOnceADay() {
        val net = app.rustle.domain.Net
        val now = 100 * DAY + 12 * HOUR
        val s = State(onboarded = true)
        assertTrue(net.canGo(s, now, wifi = true))
        assertTrue(!net.canGo(s, now, wifi = false))
        assertTrue(!net.canGo(s.copy(netDay = World.dayOf(now)), now, wifi = true))
        assertTrue(!net.canGo(s.copy(trip = Trip(District.PORT, now - MIN, 25)), now, wifi = true))
        assertTrue(!net.canGo(s.copy(night = Night(now - HOUR, true)), now, wifi = true))
        val gone = s.copy(netAt = now)
        assertTrue(net.inNet(gone, now + 5 * MIN) && !net.back(gone, now + 5 * MIN))
        assertTrue(net.back(gone, now + net.LENGTH))
        // A whole round of news before any repeats, and lines that keep to the house style.
        assertTrue(net.news.size >= 60)
        assertEquals(net.news.indices.toSet(), net.news.indices.map { net.line(s, it) }.toSet())
        for (l in net.news) assertTrue(l.ru, '—' !in l.ru && '—' !in l.en)
    }
}
