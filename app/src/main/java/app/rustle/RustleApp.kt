package app.rustle

import android.app.Application
import android.content.Context
import app.rustle.data.DemoSeed
import app.rustle.data.MIN
import app.rustle.data.Night
import app.rustle.data.NightLog
import app.rustle.data.State
import app.rustle.data.Store
import app.rustle.data.Trip
import app.rustle.domain.Catalog
import app.rustle.domain.Dish
import app.rustle.domain.District
import app.rustle.domain.Feat
import app.rustle.domain.Feats
import app.rustle.domain.Find
import app.rustle.domain.Grade
import app.rustle.domain.Kind
import app.rustle.domain.Lines
import app.rustle.domain.Needs
import app.rustle.domain.Net
import app.rustle.domain.SpringClean
import app.rustle.domain.Nights
import app.rustle.domain.Slot
import app.rustle.domain.Sky
import app.rustle.domain.Requests
import app.rustle.domain.Ask
import app.rustle.domain.World
import app.rustle.service.Outing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.random.Random

/** What he came back with, for the welcome-home sheet. */
data class Return(val district: District, val minutes: Int, val cut: Boolean, val finds: List<Find>, val line: Int, val fresh: Set<String> = emptySet(), val mood: Double = 50.0, val full: Double = 50.0)

/** How the night went, for the morning sheet. */
data class Morning(val grade: Grade, val hours: Float, val peekMin: Int, val find: Find?, val line: Int)

/** Something to tell on screen: achievements just earned, a morning present. */
data class News(val feats: List<Feat> = emptyList(), val gift: Find? = null, val request: Boolean = false, val stamp: Long = System.nanoTime())

class AppContainer(context: Context) {
    val app: Context = context.applicationContext
    val store = Store(app)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** True while the demo build fills the house, so the first screen doesn't jump. */
    val seeding = MutableStateFlow(BuildConfig.DEMO)
    val back = MutableStateFlow<Return?>(null)
    val morning = MutableStateFlow<Morning?>(null)
    val news = MutableStateFlow<News?>(null)
    /** The piece of news he brought back from the Net ([Net.news] index), for its sheet (#1). */
    val netBack = MutableStateFlow<Int?>(null)

    fun write(block: suspend AppContainer.() -> Unit) { scope.launch { block() } }

    /** Marks today as a day he was looked after. */
    private fun State.cared(now: Long) = copy(careDays = careDays + World.dayOf(now))

    suspend fun onboard(name: String, body: Int, accent: Int, species: String, marks: String) {
        val now = System.currentTimeMillis()
        store.update {
            it.copy(onboarded = true, name = name.trim(), body = body, accent = accent, species = species, marks = marks, started = it.started.takeIf { s -> s > 0 } ?: now,
                fullAt = now, moodAt = now, full = 70.0, mood = 75.0,
                // A first find, so the pantry is never empty: a rusk from the moving box.
                inv = it.inv.ifEmpty { mapOf("rusk" to 2, "sugar" to 1) }, seen = it.seen.ifEmpty { mapOf("rusk" to now, "sugar" to now) })
        }
    }

    /** A stroke: cheers him up, a little less once he's had plenty today. */
    suspend fun pet() {
        val now = System.currentTimeMillis()
        val day = World.dayOf(now)
        store.update { s ->
            val today = if (s.petDay == day) s.petToday else 0
            Needs.cheer(s, now, if (today < 12) 2 else 0).copy(pets = s.pets + 1, petDay = day, petToday = today + 1).cared(now)
        }
    }

    suspend fun feed(f: Find) {
        val now = System.currentTimeMillis()
        store.update { s ->
            val n = s.inv[f.key] ?: 0
            if (n <= 0 || f.food <= 0) s else Needs.feed(s, now, f.food, 3).copy(inv = (s.inv + (f.key to n - 1)).filterValues { it > 0 }).cared(now)
        }
    }

    /** Cooks a dish: he eats it at once, or with [keep] it goes into the pantry for later (#3). */
    suspend fun cook(d: Dish, keep: Boolean = false) {
        val now = System.currentTimeMillis()
        store.update { s ->
            if (!Catalog.canPay(s.inv, d.cost)) s
            else {
                val made = s.copy(inv = Catalog.pay(s.inv, d.cost), dishes = s.dishes + 1, cooked = s.cooked + (d.key to (s.cooked[d.key] ?: 0) + 1))
                if (keep) made.copy(larder = made.larder + (d.key to (made.larder[d.key] ?: 0) + 1)).cared(now)
                else Needs.feed(made, now, d.full, d.mood).cared(now)
            }
        }
    }

    /** A dish from the pantry. */
    suspend fun eat(d: Dish) {
        val now = System.currentTimeMillis()
        store.update { s ->
            val n = s.larder[d.key] ?: 0
            if (n <= 0) s else Needs.feed(s, now, d.full, d.mood).copy(larder = (s.larder + (d.key to n - 1)).filterValues { it > 0 }).cared(now)
        }
    }

    suspend fun build(key: String) {
        val b = Catalog.build(key) ?: return
        store.update { s ->
            if (key in s.built || !Catalog.canPay(s.inv, b.cost)) s
            else {
                val next = s.copy(inv = Catalog.pay(s.inv, b.cost), built = s.built + key)
                // A new thing goes on at once, and a new piece of furniture takes its place.
                when {
                    b.kind == Kind.WEAR -> next.copy(wear = putOn(next.wear, b.key))
                    b.slot != null -> next.copy(place = next.place + (b.slot.name to key))
                    else -> next
                }
            }
        }
    }

    /** One thing per slot: putting on a hat takes the other hat off. */
    private fun putOn(wear: Set<String>, key: String): Set<String> {
        val slot = Catalog.build(key)?.slot
        return wear.filter { Catalog.build(it)?.slot != slot || slot == null }.toSet() + key
    }

    suspend fun wear(key: String, on: Boolean) = store.update { s -> if (key !in s.built) s else s.copy(wear = if (on) putOn(s.wear, key) else s.wear - key) }

    suspend fun place(slot: Slot, key: String) = store.update { s -> if (key !in s.built) s else s.copy(place = s.place + (slot.name to key)) }

    suspend fun look(species: String? = null, marks: String? = null, body: Int? = null, accent: Int? = null) = store.update {
        it.copy(species = species ?: it.species, marks = marks ?: it.marks, body = body ?: it.body, accent = accent ?: it.accent)
    }

    suspend fun startTrip(d: District, minutes: Int) {
        val now = System.currentTimeMillis()
        // The screen is on when he leaves: that minute counts as unlocked until it goes dark.
        store.update { it.copy(trip = Trip(d, now, minutes, unlockAt = if (Outing.interactive(app)) now else 0)).cared(now) }
        Outing.start(app)
    }

    /** Calls him home early. */
    suspend fun callHome() {
        val now = System.currentTimeMillis()
        store.update { s -> s.trip?.let { t -> s.copy(trip = t.copy(ended = now, cut = t.minutes > 0 && now < t.due)) } ?: s }
        Outing.stop(app)
        claimIfBack()
    }

    /** Opens the backpack if he is home. */
    suspend fun claimIfBack() {
        val now = System.currentTimeMillis()
        var got: Return? = null
        store.update { s ->
            val t = s.trip ?: return@update s
            if (!t.over(now)) return@update s
            val minutes = ((t.endAt(now) - t.start) / MIN).toInt()
            val line = Random.nextInt(4)
            val (next, finds) = World.claim(s, t, now, line)
            got = Return(t.district, minutes, next.trips.first().cut, finds, line, finds.map { it.key }.filter { it !in s.seen }.toSet(), Needs.mood(s, t.start), Needs.full(s, t.start))
            next.cared(now)
        }
        got?.let { back.value = it; Outing.stop(app); Outing.clearBack(app) }
    }

    suspend fun toBed(charging: Boolean) {
        val now = System.currentTimeMillis()
        store.update { it.copy(night = Night(now, charging, unlockAt = if (Outing.interactive(app)) now else 0)).cared(now) }
        Outing.start(app)
    }

    /** Good morning: ends the night if it is one, or quietly calls bedtime off if he only just lay down. */
    suspend fun wake() {
        val now = System.currentTimeMillis()
        val n = store.now().night ?: return
        if (n.ended == 0L && now - n.start < Nights.MIN_SLEEP) {
            store.update { it.copy(night = null) }
            Outing.stop(app)
            return
        }
        store.update { s -> s.night?.let { s.copy(night = if (it.ended > 0) it else Nights.settle(it, now).copy(ended = now)) } ?: s }
        claimMorning()
    }

    suspend fun claimMorning() {
        val now = System.currentTimeMillis()
        var got: Morning? = null
        store.update { s ->
            val n = s.night ?: return@update s
            if (n.ended == 0L) return@update s
            val g = Nights.grade(n, n.ended)
            val find = Nights.dreamFind(g, World.open(s), n.start)
            val hours = Nights.hours(n, n.ended)
            val peek = Nights.peekMin(n)
            val line = Random.nextInt(2)
            got = Morning(g, hours, peek, find, line)
            var inv = s.inv
            var seen = s.seen
            find?.let { f -> inv = inv + (f.key to (inv[f.key] ?: 0) + 1); if (f.key !in seen) seen = seen + (f.key to now) }
            Needs.cheer(s, now, Nights.mood(g, "bed" in s.built)).copy(night = null, inv = inv, seen = seen,
                nights = (listOf(NightLog(n.start, hours, peek, g, find?.key)) + s.nights).take(400)).cared(now)
        }
        got?.let { morning.value = it; Outing.stop(app); Outing.clearBack(app) }
    }

    /** A maze run: the first two a day are worth a lump of sugar. */
    suspend fun gameWon(): Boolean {
        val now = System.currentTimeMillis()
        val day = World.dayOf(now)
        var paid = false
        store.update { s ->
            val today = if (s.gameDay == day) s.gameToday else 0
            paid = today < 2
            val inv = if (paid) s.inv + ("sugar" to (s.inv["sugar"] ?: 0) + 1) else s.inv
            Needs.cheer(s, now, if (paid) 8 else 2).copy(gameDay = day, gameToday = today + 1, inv = inv, gamesWon = s.gamesWon + 1).cared(now)
        }
        return paid
    }

    /**
     * The morning ritual: the first time the app is opened between five and noon, he says good morning and
     * hands over something he found under his pillow.
     */
    private suspend fun greetMorning() {
        val now = System.currentTimeMillis()
        val day = World.dayOf(now)
        if (Nights.hourOf(now) !in 5..11) return
        var gift: Find? = null
        store.update { s ->
            if (!s.onboarded || s.morningDay == day || s.trip?.ended == 0L || s.night?.ended == 0L) return@update s
            val pool = World.open(s).flatMap { Catalog.of(it) }.filter { it.shape.food && it.rare == 1 }
            val f = pool.random(Random(day))
            gift = f
            Needs.cheer(s, now, 5).copy(morningDay = day, mornings = s.mornings + 1, inv = s.inv + (f.key to (s.inv[f.key] ?: 0) + 1),
                seen = if (f.key in s.seen) s.seen else s.seen + (f.key to now)).cared(now)
        }
        gift?.let { news.value = News(gift = it) }
    }

    /** Hands in a weekly request: takes what was asked for, gives a present, cheers him up. */
    suspend fun claimRequest(key: String) {
        val now = System.currentTimeMillis()
        var gift: Find? = null
        store.update { s ->
            if (key in s.requests) return@update s
            val week = Requests.week(World.dayOf(now))
            val since = Requests.start(week)
            val ask = Requests.of(s, now).firstOrNull { it.key == key } ?: return@update s
            if (!ask.done(s, since)) return@update s
            val paid = if (ask is Ask.Bring) s.copy(inv = Catalog.pay(s.inv, mapOf(ask.shape to ask.n))) else s
            val f = Requests.gift(paid, key)
            gift = f
            Needs.cheer(paid, now, 15).copy(requests = paid.requests + (key to now), inv = paid.inv + (f.key to (paid.inv[f.key] ?: 0) + 1),
                seen = if (f.key in paid.seen) paid.seen else paid.seen + (f.key to now)).cared(now)
        }
        gift?.let { news.value = News(gift = it, request = true) }
    }

    /** Sews a cloth from threads for the spring-clean (#1). */
    suspend fun sewCloth() {
        store.update { s -> if (!SpringClean.canSew(s)) s else s.copy(inv = Catalog.pay(s.inv, SpringClean.clothCost), cloths = s.cloths + 1) }
    }

    /** The spring-clean done (#1): the cloth is worn through, he is pleased, and something turns up from under the dust. */
    suspend fun clean(): Find? {
        val now = System.currentTimeMillis()
        var got: Find? = null
        store.update { s ->
            if (s.cloths <= 0 || !SpringClean.due(s, now)) return@update s
            val f = SpringClean.find(s, World.dayOf(now))
            got = f
            Needs.cheer(s, now, SpringClean.MOOD).copy(cloths = s.cloths - 1, cleanAt = now, cleans = s.cleans + 1,
                inv = s.inv + (f.key to (s.inv[f.key] ?: 0) + 1), seen = if (f.key in s.seen) s.seen else s.seen + (f.key to now)).cared(now)
        }
        return got
    }

    /** Off into the Net while the phone is on Wi-Fi (#1). */
    suspend fun goNet(wifi: Boolean) {
        val now = System.currentTimeMillis()
        store.update { s -> if (!Net.canGo(s, now, wifi)) s else s.copy(netAt = now, netDay = World.dayOf(now)).cared(now) }
    }

    /** Back from the Net with a piece of news (#1). */
    suspend fun claimNet() {
        val now = System.currentTimeMillis()
        var line: Int? = null
        store.update { s ->
            if (!Net.back(s, now)) return@update s
            line = Net.line(s, s.netTrips)
            Needs.cheer(s, now, 6).copy(netAt = 0, netTrips = s.netTrips + 1)
        }
        line?.let { netBack.value = it }
    }

    /**
     * Finds roughly where the phone is from the last known position (network or GPS, whichever the
     * phone already has), rounds it to about a kilometre and names the town if the phone can.
     */
    @android.annotation.SuppressLint("MissingPermission")
    suspend fun locate(): Boolean {
        if (androidx.core.content.ContextCompat.checkSelfPermission(app, android.Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) return false
        val lm = app.getSystemService(android.location.LocationManager::class.java) ?: return false
        var loc = lm.getProviders(true).mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }.maxByOrNull { it.time }
        if (loc == null && android.os.Build.VERSION.SDK_INT >= 30) {
            loc = kotlinx.coroutines.withTimeoutOrNull(15_000) {
                kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                    val provider = if (lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) android.location.LocationManager.NETWORK_PROVIDER else android.location.LocationManager.GPS_PROVIDER
                    runCatching { lm.getCurrentLocation(provider, null, app.mainExecutor) { l -> if (cont.isActive) cont.resumeWith(Result.success(l)) } }
                        .onFailure { if (cont.isActive) cont.resumeWith(Result.success(null)) }
                }
            }
        }
        val l = loc ?: return false
        val lat = Math.round(l.latitude * 100) / 100.0
        val lon = Math.round(l.longitude * 100) / 100.0
        val name = runCatching {
            @Suppress("DEPRECATION")
            android.location.Geocoder(app).getFromLocation(lat, lon, 1)?.firstOrNull()?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
        }.getOrNull().orEmpty()
        store.update { it.copy(lat = lat, lon = lon, placeName = name, wxAt = 0) }
        return true
    }

    /** Asks for the weather if the window's place is known and the last answer is over half an hour old. */
    suspend fun refreshWeather(force: Boolean = false) {
        val s = store.now()
        if (!s.weatherOn || s.lat.isNaN() || BuildConfig.SHOTS) return
        if (!force && System.currentTimeMillis() - s.wxAt < 30 * MIN) return
        val o = runCatching { kotlinx.coroutines.withContext(Dispatchers.IO) { Sky.fetch(s.lat, s.lon) } }.getOrNull() ?: return
        store.update { it.copy(wx = o.wx.name, temp = o.temp ?: Int.MIN_VALUE, wxAt = o.at) }
    }

    /** Writes down achievements the household has just reached and tells about them. */
    suspend fun award() {
        val now = System.currentTimeMillis()
        var fresh: List<Feat> = emptyList()
        store.update { s ->
            if (!s.onboarded) return@update s
            fresh = Feats.fresh(s)
            if (fresh.isEmpty()) s else s.copy(feats = s.feats + fresh.associate { it.key to now })
        }
        if (fresh.isNotEmpty()) news.value = News(feats = fresh)
    }

    /** On opening: take what is due, greet the morning, restart the service if the phone killed it mid-outing. */
    suspend fun resume() {
        claimIfBack()
        claimMorning()
        claimNet()
        greetMorning()
        refreshWeather()
        val s = store.now()
        if ((s.trip?.ended == 0L) || (s.night?.ended == 0L)) Outing.start(app)
    }

    fun lineFor(r: Return, ru: Boolean): String = when {
        r.finds.isEmpty() -> Lines.doorstep.of(ru)
        r.cut -> Lines.cut[r.line % Lines.cut.size].of(ru)
        else -> Lines.trip.getValue(r.district).let { it[r.line % it.size] }.of(ru)
    }
}

class RustleApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.LANG.isNotEmpty()) androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(androidx.core.os.LocaleListCompat.forLanguageTags(BuildConfig.LANG))
        container = AppContainer(this)
        // Every change to the household redraws the widget and checks for new achievements.
        container.scope.launch {
            container.store.state.distinctUntilChanged().collect {
                app.rustle.widget.Widget.refresh(this@RustleApp)
                container.award()
            }
        }
        if (BuildConfig.DEMO) container.scope.launch { try { DemoSeed.seedIfEmpty(container) } finally { container.seeding.value = false } }
    }
}

val Context.container: AppContainer get() = (applicationContext as RustleApp).container

// A screenshot build started in a language (`-Plang`) keeps to it whatever the phone speaks.
val Context.ru: Boolean get() = if (BuildConfig.LANG.isNotEmpty()) BuildConfig.LANG == "ru" else resources.configuration.locales[0].language == "ru"
