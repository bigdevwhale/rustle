package app.rustle.data

import app.rustle.domain.District
import app.rustle.domain.Grade

const val MIN = 60_000L
const val HOUR = 60 * MIN
const val DAY = 24 * HOUR

/**
 * An outing under way. [minutes] is the planned length, 0 for "until you pick up the phone".
 * [unlockAt] is when the phone was last unlocked (0 while it lies locked); [peekMs] adds up the unlocked time.
 * [ended] is set when he turned back or arrived; the finds are rolled when they are taken.
 */
data class Trip(
    val district: District,
    val start: Long,
    val minutes: Int,
    val peekMs: Long = 0,
    val unlockAt: Long = 0,
    val ended: Long = 0,
    val cut: Boolean = false,
) {
    val due: Long get() = if (minutes > 0) start + minutes * MIN else 0
    fun over(now: Long) = ended > 0 || (minutes > 0 && now >= due)
    fun endAt(now: Long) = when { ended > 0 -> ended; minutes > 0 && now >= due -> due; else -> now }
    /** Unlocked time he lets pass before turning back: a tenth of the way, at least two minutes, plus a minute to put the phone down. */
    val allowance: Long get() = maxOf(2 * MIN, minutes * MIN / 10) + MIN
}

/** A night on the stove: from "to bed" to the first unlock of the morning. */
data class Night(val start: Long, val charging: Boolean, val peekMs: Long = 0, val unlockAt: Long = 0, val ended: Long = 0)

data class TripLog(val at: Long, val district: District, val minutes: Int, val cut: Boolean, val finds: List<String>, val line: Int)

data class NightLog(val at: Long, val hours: Float, val peekMin: Int, val grade: Grade, val find: String?)

data class State(
    val onboarded: Boolean = false,
    val name: String = "",
    /** Fur and the second colour (ears, tail tip), as 0xRRGGBB. */
    val body: Int = 0xC8946A,
    val accent: Int = 0xF0D8B8,
    val started: Long = 0,
    val full: Double = 70.0,
    val fullAt: Long = 0,
    val mood: Double = 70.0,
    val moodAt: Long = 0,
    /** Finds in hand by key, and when each kind was first found. */
    val inv: Map<String, Int> = emptyMap(),
    val seen: Map<String, Long> = emptyMap(),
    val built: Set<String> = emptySet(),
    /** What he wears from what was made. */
    val wear: Set<String> = emptySet(),
    val trip: Trip? = null,
    val night: Night? = null,
    val trips: List<TripLog> = emptyList(),
    val nights: List<NightLog> = emptyList(),
    val pets: Int = 0,
    val petDay: Long = 0,
    val petToday: Int = 0,
    val gameDay: Long = 0,
    val gameToday: Int = 0,
    val dishes: Int = 0,
    val notifAsked: Boolean = false,
    /** Screens whose first-visit explanation has been read. */
    val tips: Set<String> = emptySet(),
    /** What kind of creature he is and the pattern on his fur ([app.rustle.domain.Species], [app.rustle.domain.Marks] names). */
    val species: String = "MOUSE",
    val marks: String = "NONE",
    /** The thing chosen for each place in the nook, by [app.rustle.domain.Slot] name. */
    val place: Map<String, String> = emptyMap(),
    /** Achievements and when each was earned. */
    val feats: Map<String, Long> = emptyMap(),
    /** Days (epoch days) on which he was looked after: fed, stroked, sent out, put to bed. */
    val careDays: Set<Long> = emptySet(),
    /** The last morning he was greeted, and how many mornings in all. */
    val morningDay: Long = 0,
    val mornings: Int = 0,
    val gamesWon: Int = 0,
    /** How many times each dish was cooked. */
    val cooked: Map<String, Int> = emptyMap(),
    /** Long outings since the last wonder. */
    val dryLong: Int = 0,
    /** Weekly requests handed in, by key, and when. */
    val requests: Map<String, Long> = emptyMap(),
    /** "system", "dark" or "light", and the background hue ([app.rustle.ui.theme.Hue] name). */
    val theme: String = "system",
    val hue: String = "GREEN",
    /** The window's place, rounded to about a kilometre; NaN until it is known. */
    val lat: Double = Double.NaN,
    val lon: Double = Double.NaN,
    val placeName: String = "",
    /** Whether the window shows real weather, and whether we have asked for the location yet. */
    val weatherOn: Boolean = true,
    val locAsked: Boolean = false,
    /** The last weather answer: a [app.rustle.domain.Wx] name, degrees, and when it came. */
    val wx: String = "",
    val temp: Int = Int.MIN_VALUE,
    val wxAt: Long = 0,
)
