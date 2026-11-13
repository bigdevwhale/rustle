package app.rustle.ui.trips

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import app.rustle.ui.art.toBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.rustle.R
import app.rustle.container
import app.rustle.domain.Catalog
import app.rustle.domain.District
import app.rustle.domain.Loot
import app.rustle.ui.Home
import app.rustle.ui.Nav
import app.rustle.ui.Overlay
import app.rustle.ui.Tab
import app.rustle.ui.art.Places
import app.rustle.ui.components.BufImage
import app.rustle.ui.components.Chip
import app.rustle.ui.components.Tip
import app.rustle.ui.components.FindIcon
import app.rustle.ui.components.Flow
import app.rustle.ui.components.Panel
import app.rustle.ui.components.PixelButton
import app.rustle.ui.components.Section
import app.rustle.ui.theme.C
import app.rustle.ui.theme.T
import java.text.DateFormat
import java.util.Date

val LENGTHS = listOf(15, 25, 45, 60, 90, 0)

@Composable
fun TripsScreen(h: Home, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    // The choice is kept in the household, so the widget sends him to the same place for as long (#4).
    var pick by rememberSaveable { mutableStateOf(h.s.pickD.takeIf { p -> h.open.any { it.name == p } } ?: h.open.last().name) }
    var minutes by rememberSaveable { mutableIntStateOf(h.s.pickMin.takeIf { it in LENGTHS } ?: 25) }
    androidx.compose.runtime.LaunchedEffect(pick, minutes) {
        if (h.s.pickD != pick || h.s.pickMin != minutes) c.write { store.update { it.copy(pickD = pick, pickMin = minutes) } }
    }
    val district = District.valueOf(pick).takeIf { it in h.open } ?: District.PORT
    val notif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { nav.go(Overlay.Send(district, minutes)) }
    fun send() {
        if (Build.VERSION.SDK_INT >= 33 && !h.s.notifAsked) {
            c.write { store.update { it.copy(notifAsked = true) } }
            notif.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else nav.go(Overlay.Send(district, minutes))
    }
    val busy = h.away != null || h.asleep != null || h.surfing != null
    // The choice scrolls; what it comes to and the button to send him stay at the bottom, always in reach.
    Column(Modifier.fillMaxSize()) {
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Tip("trips", stringResource(R.string.tip_trips_t), stringResource(R.string.tip_trips), h.look, h.s.tips)
        if (busy) Panel(Modifier.fillMaxWidth(), fill = C.note) {
            Text(stringResource(when { h.away != null -> R.string.t_busy_away; h.surfing != null -> R.string.t_busy_net; else -> R.string.t_busy_sleep }, h.name), style = T.Body)
            PixelButton(stringResource(R.string.t_to_home), { nav.tab(Tab.HOME) }, Modifier.fillMaxWidth().padding(top = 8.dp), small = true)
        }
        // The places as a row of picture cards: all five in reach with a swipe, the chosen one lit.
        Section(stringResource(R.string.t_where))
        // All five places as even rows: the picture, the name on one line, one short line under it.
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (d in District.entries) {
                val open = d in h.open
                val on = open && d == district
                val found = Catalog.of(d).count { it.key in h.s.seen }
                Panel(Modifier.fillMaxWidth(), fill = when { on -> C.copperL; open -> C.paper; else -> C.paperD }, onClick = if (open && !busy) ({ pick = d.name }) else null,
                    padding = androidx.compose.foundation.layout.PaddingValues(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BufImage(Places.of(d, open), scale = 1.dp)
                        Column(Modifier.weight(1f)) {
                            Text(d.name_.of(h.ru), style = T.Name.copy(color = if (open) C.ink else C.muted), maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text(if (open) stringResource(R.string.t_found, found, Catalog.of(d).size) else stringResource(R.string.t_locked_short, Catalog.build(d.tool!!)!!.name.of(h.ru)),
                                style = T.Small.copy(color = if (open) C.muted else C.emberD), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                        if (on) Text("✓", style = T.Name.copy(color = C.emberD))
                    }
                }
            }
        }
        Text(district.where.of(h.ru), style = T.Small.copy(color = C.textDim), maxLines = 2)
        Section(stringResource(R.string.t_how_long), stringResource(R.string.t_how_long_note))
        app.rustle.ui.components.Cells(LENGTHS.size, LENGTHS.size, gap = 4.dp) { i, m ->
            val len = LENGTHS[i]
            app.rustle.ui.components.SegChip(if (len == 0) "∞" else "$len", len == minutes, { minutes = len }, m)
        }
        // What it comes to, in one line, and what mood and hunger do to it.
        val count = (Loot.count(minutes, true, "backpack" in h.s.wear && "backpack" in h.s.built) + Loot.bonus(h.mood, h.full, minutes)).coerceAtLeast(1)
        Text(if (minutes == 0) stringResource(R.string.t_free_hint) else stringResource(R.string.t_expect, count), style = T.Small.copy(color = C.textDim))
        val mods = listOfNotNull(
            stringResource(R.string.mood_plus).takeIf { h.mood >= Loot.CHEERFUL && minutes >= 25 },
            stringResource(R.string.mood_minus).takeIf { h.mood <= Loot.GLUM },
            stringResource(R.string.hungry_minus).takeIf { h.full <= Loot.HUNGRY },
        )
        if (mods.isNotEmpty()) Text(mods.joinToString(" · "), style = T.Small.copy(color = if (h.mood >= Loot.CHEERFUL) C.hi else C.emberL))
        if (h.s.trips.isNotEmpty()) PixelButton(stringResource(R.string.t_log_open, h.s.trips.size), { nav.go(Overlay.TripLog) }, Modifier.fillMaxWidth(), kind = app.rustle.ui.components.Kind.PALE, small = true)
    }
    if (!busy) Column(Modifier.fillMaxWidth().background(C.boardD).padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val back = if (minutes > 0) " · " + stringResource(R.string.t_back_at, DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(h.now + minutes * app.rustle.data.MIN))) else ""
        Text(district.name_.of(h.ru) + " · " + (if (minutes == 0) stringResource(R.string.t_free) else stringResource(R.string.t_min, minutes)) + back,
            style = T.Small.copy(color = C.text), maxLines = 1)
        PixelButton(stringResource(R.string.t_go), ::send, Modifier.fillMaxWidth())
    }
    }
}

/** Where he has been: each outing with its finds, on a sheet of its own. */
@Composable
fun TripLogSheet(h: Home, nav: Nav) {
    app.rustle.ui.components.Sheet(nav::close) {
        Text(stringResource(R.string.t_log), style = T.Title)
        val df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (t in h.s.trips.take(30)) Panel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t.district.name_.of(h.ru), style = T.Name, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.t_min, t.minutes), style = T.Small)
                }
                Text(df.format(Date(t.at)) + if (t.cut) " · " + stringResource(R.string.t_cut) else "", style = T.Small.copy(color = if (t.cut) C.emberD else C.muted))
                if (t.finds.isNotEmpty()) Flow(Modifier.padding(top = 4.dp)) { for (k in t.finds) Catalog.find(k)?.let { FindIcon(it, scale = 1.dp) } }
            }
        }
    }
}
