package app.rustle.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.rustle.domain.District
import app.rustle.domain.Grade
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore("rustle")
private val KEY = stringPreferencesKey("state")

/** The whole household as one JSON document: small, and edited atomically by the app and the outing service alike. */
class Store(context: Context) {
    private val store = context.applicationContext.dataStore

    val state: Flow<State> = store.data.map { p -> p[KEY]?.let { runCatching { Json.read(it) }.getOrNull() } ?: State() }

    suspend fun now(): State = state.first()

    suspend fun update(change: (State) -> State) {
        store.edit { p ->
            val s = p[KEY]?.let { runCatching { Json.read(it) }.getOrNull() } ?: State()
            p[KEY] = Json.write(change(s))
        }
    }
}

object Json {
    fun write(s: State): String = JSONObject().apply {
        put("onboarded", s.onboarded); put("name", s.name); put("body", s.body); put("accent", s.accent); put("started", s.started)
        put("full", s.full); put("fullAt", s.fullAt); put("mood", s.mood); put("moodAt", s.moodAt)
        put("inv", JSONObject().also { o -> s.inv.forEach { (k, v) -> o.put(k, v) } })
        put("seen", JSONObject().also { o -> s.seen.forEach { (k, v) -> o.put(k, v) } })
        put("built", JSONArray(s.built.sorted())); put("wear", JSONArray(s.wear.sorted()))
        s.trip?.let { t ->
            put("trip", JSONObject().put("d", t.district.name).put("start", t.start).put("min", t.minutes).put("peek", t.peekMs)
                .put("unlock", t.unlockAt).put("ended", t.ended).put("cut", t.cut))
        }
        s.night?.let { n -> put("night", JSONObject().put("start", n.start).put("ch", n.charging).put("peek", n.peekMs).put("unlock", n.unlockAt).put("ended", n.ended)) }
        put("trips", JSONArray().also { a ->
            s.trips.forEach { t -> a.put(JSONObject().put("at", t.at).put("d", t.district.name).put("min", t.minutes).put("cut", t.cut).put("f", JSONArray(t.finds)).put("line", t.line)) }
        })
        put("nights", JSONArray().also { a ->
            s.nights.forEach { n -> a.put(JSONObject().put("at", n.at).put("h", n.hours.toDouble()).put("peek", n.peekMin).put("g", n.grade.name).put("f", n.find ?: "")) }
        })
        put("pets", s.pets); put("petDay", s.petDay); put("petToday", s.petToday)
        put("gameDay", s.gameDay); put("gameToday", s.gameToday); put("dishes", s.dishes); put("notifAsked", s.notifAsked); put("tips", JSONArray(s.tips.sorted()))
        put("species", s.species); put("marks", s.marks)
        put("place", JSONObject().also { o -> s.place.forEach { (k, v) -> o.put(k, v) } })
        put("feats", JSONObject().also { o -> s.feats.forEach { (k, v) -> o.put(k, v) } })
        put("care", JSONArray(s.careDays.sorted().takeLast(500)))
        put("morningDay", s.morningDay); put("mornings", s.mornings); put("gamesWon", s.gamesWon)
        put("cooked", JSONObject().also { o -> s.cooked.forEach { (k, v) -> o.put(k, v) } })
        put("theme", s.theme); put("hue", s.hue); if (!s.lat.isNaN()) { put("lat", s.lat); put("lon", s.lon) }; put("place", s.placeName)
        put("weatherOn", s.weatherOn); put("locAsked", s.locAsked); put("wx", s.wx); put("temp", s.temp); put("wxAt", s.wxAt); put("dry", s.dryLong); put("requests", JSONObject().also { o -> s.requests.forEach { (k, v) -> o.put(k, v) } })
        put("cloths", s.cloths); put("cleanAt", s.cleanAt); put("cleans", s.cleans); put("netAt", s.netAt); put("netDay", s.netDay); put("netTrips", s.netTrips)
    }.toString()

    private fun JSONObject.map(name: String): Map<String, Long> =
        optJSONObject(name)?.let { o -> o.keys().asSequence().associateWith { o.getLong(it) } } ?: emptyMap()

    private fun JSONObject.set(name: String): Set<String> =
        optJSONArray(name)?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() } ?: emptySet()

    private fun district(s: String) = District.entries.firstOrNull { it.name == s } ?: District.PORT

    fun read(text: String): State {
        val o = JSONObject(text)
        val trip = o.optJSONObject("trip")?.let {
            Trip(district(it.getString("d")), it.getLong("start"), it.getInt("min"), it.optLong("peek"), it.optLong("unlock"), it.optLong("ended"), it.optBoolean("cut"))
        }
        val night = o.optJSONObject("night")?.let { Night(it.getLong("start"), it.optBoolean("ch"), it.optLong("peek"), it.optLong("unlock"), it.optLong("ended")) }
        val trips = o.optJSONArray("trips")?.let { a ->
            (0 until a.length()).map { i ->
                val t = a.getJSONObject(i)
                val f = t.getJSONArray("f")
                TripLog(t.getLong("at"), district(t.getString("d")), t.getInt("min"), t.optBoolean("cut"), (0 until f.length()).map { f.getString(it) }, t.optInt("line"))
            }
        } ?: emptyList()
        val nights = o.optJSONArray("nights")?.let { a ->
            (0 until a.length()).map { i ->
                val n = a.getJSONObject(i)
                NightLog(n.getLong("at"), n.getDouble("h").toFloat(), n.getInt("peek"), runCatching { Grade.valueOf(n.getString("g")) }.getOrDefault(Grade.OK),
                    n.optString("f").takeIf { it.isNotBlank() })
            }
        } ?: emptyList()
        return State(
            onboarded = o.optBoolean("onboarded"), name = o.optString("name"), body = o.optInt("body", 0xC8946A), accent = o.optInt("accent", 0xF0D8B8),
            started = o.optLong("started"), full = o.optDouble("full", 70.0), fullAt = o.optLong("fullAt"), mood = o.optDouble("mood", 70.0), moodAt = o.optLong("moodAt"),
            inv = o.map("inv").mapValues { it.value.toInt() }, seen = o.map("seen"), built = o.set("built"), wear = o.set("wear"),
            trip = trip, night = night, trips = trips, nights = nights,
            pets = o.optInt("pets"), petDay = o.optLong("petDay"), petToday = o.optInt("petToday"),
            gameDay = o.optLong("gameDay"), gameToday = o.optInt("gameToday"), dishes = o.optInt("dishes"), notifAsked = o.optBoolean("notifAsked"), tips = o.set("tips"),
            species = o.optString("species", "MOUSE").ifBlank { "MOUSE" }, marks = o.optString("marks", "NONE").ifBlank { "NONE" },
            place = o.optJSONObject("place")?.let { p -> p.keys().asSequence().associateWith { p.getString(it) } } ?: emptyMap(),
            feats = o.map("feats"), careDays = o.optJSONArray("care")?.let { a -> (0 until a.length()).map { a.getLong(it) }.toSet() } ?: emptySet(),
            morningDay = o.optLong("morningDay"), mornings = o.optInt("mornings"), gamesWon = o.optInt("gamesWon"),
            cooked = o.map("cooked").mapValues { it.value.toInt() }, dryLong = o.optInt("dry"), theme = o.optString("theme", "system").ifBlank { "system" }, lat = o.optDouble("lat"), lon = o.optDouble("lon"), placeName = o.optString("place"),
            weatherOn = o.optBoolean("weatherOn", true), locAsked = o.optBoolean("locAsked"), wx = o.optString("wx"), temp = o.optInt("temp", Int.MIN_VALUE), wxAt = o.optLong("wxAt"), hue = o.optString("hue", "GREEN").ifBlank { "GREEN" }, requests = o.map("requests"),
            cloths = o.optInt("cloths"), cleanAt = o.optLong("cleanAt"), cleans = o.optInt("cleans"), netAt = o.optLong("netAt"), netDay = o.optLong("netDay"), netTrips = o.optInt("netTrips"),
        )
    }
}
