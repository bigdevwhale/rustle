package app.rustle.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.rustle.Morning
import app.rustle.R
import app.rustle.Return
import app.rustle.container
import app.rustle.domain.Catalog
import app.rustle.domain.Grade
import app.rustle.domain.Lines
import app.rustle.domain.Pose
import app.rustle.domain.Rank
import app.rustle.domain.Trait
import app.rustle.ui.Home
import app.rustle.ui.Nav
import app.rustle.ui.Tab
import app.rustle.ui.components.Bar
import app.rustle.ui.components.FindIcon
import app.rustle.ui.components.Kind
import app.rustle.ui.components.Panel
import app.rustle.ui.components.PixelButton
import app.rustle.ui.components.Portrait
import app.rustle.ui.components.Says
import app.rustle.ui.components.Sheet
import app.rustle.ui.theme.C
import app.rustle.ui.theme.T

@Composable
fun FeedSheet(h: Home, nav: Nav) {
    val c = LocalContext.current.container
    Sheet(nav::close) {
        Text(stringResource(R.string.feed_title), style = T.Title)
        Text(stringResource(R.string.feed_sub, h.full.toInt()), style = T.Small)
        if (h.food.isEmpty()) {
            Says(stringResource(R.string.feed_empty), who = h.name)
            PixelButton(stringResource(R.string.h_send), { nav.tab(Tab.TRIPS) }, Modifier.fillMaxWidth())
        }
        for (f in h.food) Panel(Modifier.fillMaxWidth(), onClick = { c.write { feed(f) } }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FindIcon(f)
                Column(Modifier.weight(1f)) {
                    Text(f.name.of(h.ru), style = T.Name)
                    Text(stringResource(R.string.feed_value, f.food), style = T.Small)
                }
                Text("×${h.s.inv[f.key] ?: 0}", style = T.Heading.copy(color = C.copperD))
            }
        }
        Text(stringResource(R.string.feed_kitchen), style = T.Small)
        PixelButton(stringResource(R.string.feed_to_kitchen), { nav.tab(Tab.WORKSHOP) }, Modifier.fillMaxWidth(), kind = Kind.PALE, small = true)
    }
}

@Composable
fun ProfileSheet(h: Home, nav: Nav) {
    Sheet(nav::close) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Portrait(h.look, if (h.asleep != null) Pose.SLEEP else Pose.IDLE, scale = 4.dp)
            Column(Modifier.weight(1f)) {
                Text(h.name, style = T.Title)
                Text(h.rank.title.of(h.ru), style = T.Name.copy(color = C.copperD))
                Text(stringResource(R.string.p_days, h.days), style = T.Small)
            }
        }
        val next = Rank.next(h.s)
        val pts = Rank.points(h.s)
        if (next != null) {
            Text(stringResource(R.string.p_next, next.title.of(h.ru), next.from - pts), style = T.Small)
            Bar((pts - h.rank.from).toFloat() / (next.from - h.rank.from), C.copper)
        }
        val today = app.rustle.domain.World.dayOf(h.now)
        Text(stringResource(R.string.f_streak, app.rustle.domain.Streak.current(h.s, today)), style = T.Body.copy(color = C.copperD))
        PixelButton(stringResource(R.string.f_open, h.s.feats.size, app.rustle.domain.Feats.all.size), { nav.go(app.rustle.ui.Overlay.Feats) }, Modifier.fillMaxWidth(), small = true)
        Text(stringResource(R.string.p_traits), style = T.Heading)
        Text(stringResource(R.string.p_traits_sub), style = T.Small)
        for (t in Trait.entries) {
            val on = t in h.traits
            Panel(Modifier.fillMaxWidth(), fill = if (on) C.note else C.paperD) {
                Text((if (on) "✦ " else "· ") + t.name_.of(h.ru), style = T.Name.copy(color = if (on) C.ink else C.muted))
                Text(t.note.of(h.ru), style = T.Small)
            }
        }
        Text(stringResource(R.string.p_stats), style = T.Heading)
        val hours = h.s.trips.sumOf { it.minutes } / 60f
        for ((l, v) in listOf(
            R.string.p_trips to h.s.trips.size.toString(),
            R.string.p_hours to String.format(java.util.Locale.ROOT, "%.1f", hours),
            R.string.p_nights to h.s.nights.count { it.grade == Grade.GOOD }.toString(),
            R.string.p_found to "${h.s.seen.size}/${Catalog.finds.size}",
            R.string.p_built to "${h.s.built.size}/${Catalog.builds.size}",
            R.string.p_pets to h.s.pets.toString(),
        )) Row(Modifier.fillMaxWidth()) {
            Text(stringResource(l), style = T.Body, modifier = Modifier.weight(1f))
            Text(v, style = T.Body.copy(color = C.copperD))
        }
    }
}

/** He's home: the note he brings and what is in the backpack. */
@Composable
fun ReturnSheet(h: Home, r: Return, onDone: () -> Unit) {
    val c = LocalContext.current.container
    Sheet(onDone) {
        Text(stringResource(if (r.cut) R.string.r_cut else R.string.r_home, h.name), style = T.Title)
        Text(stringResource(R.string.r_sub, r.district.name_.of(h.ru), r.minutes), style = T.Small)
        Says(c.lineFor(r, h.ru), who = h.name)
        if (r.finds.isNotEmpty()) MoodNote(r.mood, r.full, r.minutes)
        if (r.finds.isNotEmpty()) Text(stringResource(R.string.r_finds), style = T.Heading)
        for (f in r.finds) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FindIcon(f)
            Column(Modifier.weight(1f)) {
                Text(f.name.of(h.ru), style = T.Body)
                val fresh = f.key in r.fresh
                Text(stringResource(rarity(f.rare)) + if (fresh) " · " + stringResource(R.string.r_new) else "", style = T.Small.copy(color = if (fresh) C.ember else C.muted))
            }
        }
        PixelButton(stringResource(R.string.r_ok), onDone, Modifier.fillMaxWidth().padding(top = 6.dp))
    }
}

@Composable
fun MorningSheet(h: Home, m: Morning, onDone: () -> Unit) {
    Sheet(onDone) {
        Text(stringResource(R.string.m_title), style = T.Title)
        Text(stringResource(R.string.m_sub, String.format(java.util.Locale.ROOT, "%.1f", m.hours), m.peekMin), style = T.Small)
        Text(stringResource(when (m.grade) { Grade.GOOD -> R.string.m_good; Grade.OK -> R.string.m_fine; Grade.BAD -> R.string.m_bad }), style = T.Heading.copy(color = C.copperD))
        Says(Lines.night.getValue(m.grade)[m.line % 2].of(h.ru), who = h.name)
        m.find?.let { f ->
            Text(stringResource(R.string.m_dream), style = T.Small)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FindIcon(f); Text(f.name.of(h.ru), style = T.Body)
            }
        }
        PixelButton(stringResource(R.string.m_ok), onDone, Modifier.fillMaxWidth().padding(top = 6.dp))
    }
}

fun rarity(rare: Int) = when (rare) { 1 -> R.string.rare_1; 2 -> R.string.rare_2; else -> R.string.rare_3 }


/** What hunger and mood are, what raises them and what they do to an outing. */
@Composable
fun NeedsSheet(h: Home, nav: Nav) {
    Sheet(nav::close) {
        Text(stringResource(R.string.nd_title), style = T.Title)
        val b = app.rustle.domain.Loot.bonus(h.mood, h.full, 45)
        Panel(Modifier.fillMaxWidth(), fill = if (b > 0) C.copperL else if (b < 0) C.paperD else C.note) {
            Text(stringResource(R.string.nd_now), style = T.Small)
            Text(stringResource(when { b > 0 -> R.string.nd_now_good; b < 0 -> R.string.nd_now_bad; else -> R.string.nd_now_plain }), style = T.Body)
        }
        Text(stringResource(R.string.h_mood), style = T.Heading)
        Text(stringResource(R.string.nd_mood_up), style = T.Small)
        Text(stringResource(R.string.nd_mood_down), style = T.Small)
        Text(stringResource(R.string.nd_mood_does), style = T.Body)
        Text(stringResource(R.string.h_full), style = T.Heading)
        Text(stringResource(R.string.nd_full_how), style = T.Small)
        Text(stringResource(R.string.nd_full_does), style = T.Body)
    }
}

/** A line under the outing's finds when his mood or hunger changed the haul. */
@Composable
fun MoodNote(mood: Double, full: Double, minutes: Int) {
    val lines = buildList {
        if (mood >= app.rustle.domain.Loot.CHEERFUL && minutes >= 25) add(stringResource(R.string.mood_plus))
        if (mood <= app.rustle.domain.Loot.GLUM) add(stringResource(R.string.mood_minus))
        if (full <= app.rustle.domain.Loot.HUNGRY) add(stringResource(R.string.hungry_minus))
    }
    for (l in lines) Text(l, style = T.Small.copy(color = C.emberD))
}
