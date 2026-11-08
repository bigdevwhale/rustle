package app.rustle.ui.trips

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
    var pick by rememberSaveable { mutableStateOf(h.open.last().name) }
    var minutes by rememberSaveable { mutableIntStateOf(25) }
    val district = District.valueOf(pick).takeIf { it in h.open } ?: District.PORT
    val notif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { nav.go(Overlay.Send(district, minutes)) }
    fun send() {
        if (Build.VERSION.SDK_INT >= 33 && !h.s.notifAsked) {
            c.write { store.update { it.copy(notifAsked = true) } }
            notif.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else nav.go(Overlay.Send(district, minutes))
    }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Tip("trips", stringResource(R.string.tip_trips_t), stringResource(R.string.tip_trips), h.look, h.s.tips)
        val busy = h.away != null || h.asleep != null || h.surfing != null
        if (busy) Panel(Modifier.fillMaxWidth(), fill = C.note) {
            Text(stringResource(when { h.away != null -> R.string.t_busy_away; h.surfing != null -> R.string.t_busy_net; else -> R.string.t_busy_sleep }, h.name), style = T.Body)
            PixelButton(stringResource(R.string.t_to_home), { nav.tab(Tab.HOME) }, Modifier.fillMaxWidth().padding(top = 8.dp), small = true)
        }
        Section(stringResource(R.string.t_where))
        for (d in District.entries) {
            val open = d in h.open
            val on = open && d == district
            val found = Catalog.of(d).count { it.key in h.s.seen }
            Panel(Modifier.fillMaxWidth(), fill = when { on -> C.copperL; open -> C.paper; else -> C.paperD }, onClick = if (open && !busy) ({ pick = d.name }) else null,
                padding = androidx.compose.foundation.layout.PaddingValues(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BufImage(Places.of(d, open), scale = 1.5.dp)
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(d.name_.of(h.ru), style = T.Name.copy(color = if (open) C.ink else C.muted), modifier = Modifier.weight(1f), maxLines = 1)
                            Text("$found/${Catalog.of(d).size}", style = T.Small)
                        }
                        if (open) Text(d.where.of(h.ru), style = T.Small, maxLines = 2)
                        else Text(stringResource(R.string.t_locked, Catalog.build(d.tool!!)!!.name.of(h.ru)), style = T.Small.copy(color = C.emberD), maxLines = 2)
                    }
                }
            }
        }
        Section(stringResource(R.string.t_how_long))
        Flow {
            for (m in LENGTHS) Chip(if (m == 0) stringResource(R.string.t_free) else stringResource(R.string.t_min, m), m == minutes, { minutes = m }, dark = true)
        }
        Text(
            if (minutes == 0) stringResource(R.string.t_free_hint)
            else stringResource(R.string.t_expect, (Loot.count(minutes, true, "backpack" in h.s.wear && "backpack" in h.s.built) + Loot.bonus(h.mood, h.full, minutes)).coerceAtLeast(1)),
            style = T.Small.copy(color = C.textDim),
        )
        if (h.mood >= Loot.CHEERFUL && minutes >= 25) Text(stringResource(R.string.mood_plus), style = T.Small.copy(color = C.hi))
        if (h.mood <= Loot.GLUM) Text(stringResource(R.string.mood_minus), style = T.Small.copy(color = C.emberL))
        if (h.full <= Loot.HUNGRY) Text(stringResource(R.string.hungry_minus), style = T.Small.copy(color = C.emberL))
        PixelButton(stringResource(R.string.t_go), ::send, Modifier.fillMaxWidth(), enabled = !busy)
        Text(stringResource(R.string.t_rules), style = T.Small.copy(color = C.textDim))

        if (h.s.trips.isNotEmpty()) {
            Section(stringResource(R.string.t_log))
            val df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            for (t in h.s.trips.take(20)) Panel(Modifier.fillMaxWidth()) {
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
