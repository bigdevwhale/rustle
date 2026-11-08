package app.rustle.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.rustle.R
import app.rustle.container
import app.rustle.data.State
import app.rustle.device.Phone
import app.rustle.device.rememberPhone
import app.rustle.domain.District
import app.rustle.ru
import app.rustle.ui.components.pixelBox
import app.rustle.ui.games.Maze
import app.rustle.ui.home.FeedSheet
import app.rustle.ui.home.HomeScreen
import app.rustle.ui.home.MorningSheet
import app.rustle.ui.home.ProfileSheet
import app.rustle.ui.home.ReturnSheet
import app.rustle.ui.onboarding.Onboarding
import app.rustle.ui.settings.SettingsPage
import app.rustle.ui.shelf.FindSheet
import app.rustle.ui.shelf.ShelfScreen
import app.rustle.ui.theme.C
import app.rustle.ui.theme.T
import app.rustle.ui.trips.FaceDown
import app.rustle.ui.trips.TripsScreen
import app.rustle.ui.workshop.WorkshopScreen
import kotlinx.coroutines.delay

enum class Tab { HOME, TRIPS, SHELF, WORKSHOP }

sealed class Overlay {
    data object Settings : Overlay()
    data object Profile : Overlay()
    data object Feed : Overlay()
    data object Maze : Overlay()
    data object Feats : Overlay()
    data object Needs : Overlay()
    data object Clean : Overlay()
    data class Find(val key: String) : Overlay()
    data class Send(val district: District, val minutes: Int) : Overlay()
}

class Nav(val go: (Overlay?) -> Unit, val tab: (Tab) -> Unit) {
    fun close() = go(null)
}

/** [clock], [tab0], [overlay0] and [phone0] let renders open a fixed moment; the app passes none. */
@Composable
fun RustleRoot(s: State, clock: Long? = null, tab0: Tab = Tab.HOME, overlay0: Overlay? = null, phone0: Phone? = null) {
    val ctx = LocalContext.current
    val c = ctx.container
    if (!s.onboarded) { Onboarding(); return }
    var now by remember { mutableLongStateOf(clock ?: System.currentTimeMillis()) }
    LaunchedEffect(Unit) { if (clock == null) while (true) { c.resume(); delay(15_000); now = System.currentTimeMillis() } }
    val phone = rememberPhone(phone0)
    val h = remember(s, now) { Home(s, if (clock == null) maxOf(now, System.currentTimeMillis()) else now, ctx.ru) }
    val bus = remember { Bus() }
    var tab by rememberSaveable { mutableStateOf(tab0) }
    var overlay by remember { mutableStateOf(overlay0) }
    val nav = remember { Nav({ overlay = it }, { tab = it; overlay = null }) }
    val back by c.back.collectAsStateWithLifecycle()
    val morning by c.morning.collectAsStateWithLifecycle()
    val news by c.news.collectAsStateWithLifecycle()
    val netBack by c.netBack.collectAsStateWithLifecycle()
    // New achievements and the morning present come up as notes at the top.
    LaunchedEffect(news?.stamp) {
        val n = news ?: return@LaunchedEffect
        n.gift?.let {
            if (n.request) bus.note(ctx.getString(R.string.n_request), ctx.getString(R.string.n_request_b, h.name, it.name.of(h.ru)))
            else bus.note(ctx.getString(R.string.n_morning), ctx.getString(R.string.n_morning_b, h.name, it.name.of(h.ru)))
        }
        for (f in n.feats.take(3)) bus.note(ctx.getString(R.string.n_feat), f.name.of(h.ru) + " · " + f.note.of(h.ru))
        if (n.feats.size > 3) bus.note(ctx.getString(R.string.n_feat), ctx.getString(R.string.n_feat_more, n.feats.size - 3))
        c.news.value = null
    }

    CompositionLocalProvider(LocalBus provides bus) {
        Box(Modifier.fillMaxSize().background(C.board)) {
            Column(Modifier.fillMaxSize()) {
                Header(h) { overlay = it }
                Box(Modifier.weight(1f)) {
                    when (tab) {
                        Tab.HOME -> HomeScreen(h, phone, nav)
                        Tab.TRIPS -> TripsScreen(h, nav)
                        Tab.SHELF -> ShelfScreen(h, nav)
                        Tab.WORKSHOP -> WorkshopScreen(h)
                    }
                }
                TabBar(tab) { tab = it }
            }
            when (val o = overlay) {
                Overlay.Settings -> SettingsPage(s, nav)
                Overlay.Profile -> ProfileSheet(h, nav)
                Overlay.Feed -> FeedSheet(h, nav)
                Overlay.Maze -> Maze(h, nav)
                Overlay.Feats -> app.rustle.ui.home.FeatsPage(h, nav)
                Overlay.Needs -> app.rustle.ui.home.NeedsSheet(h, nav)
                Overlay.Clean -> app.rustle.ui.home.CleanScene(h, nav)
                is Overlay.Find -> FindSheet(h, o.key, nav)
                is Overlay.Send -> FaceDown(h, o.district, o.minutes, phone, nav)
                null -> Unit
            }
            back?.let { r -> ReturnSheet(h, r) { c.back.value = null } }
            if (back == null) morning?.let { m -> MorningSheet(h, m) { c.morning.value = null } }
            if (back == null && morning == null) netBack?.let { n -> app.rustle.ui.home.NetSheet(h, n) { c.netBack.value = null } }
            Notes(Modifier.align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun Header(h: Home, go: (Overlay) -> Unit) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f).clickable { go(Overlay.Profile) }) {
            Text(h.name, style = T.Display.copy(color = C.title), maxLines = 1)
            val streak = app.rustle.domain.Streak.current(h.s, app.rustle.domain.World.dayOf(h.now))
            val weather = h.wx?.let { w ->
                (h.temp?.let { (if (it > 0) "+" else "") + it + "° " } ?: "") + stringResource(when (w) {
                    app.rustle.domain.Wx.CLEAR -> R.string.wx_clear; app.rustle.domain.Wx.CLOUDY -> R.string.wx_cloudy; app.rustle.domain.Wx.OVERCAST -> R.string.wx_overcast
                    app.rustle.domain.Wx.DRIZZLE -> R.string.wx_drizzle; app.rustle.domain.Wx.RAIN -> R.string.wx_rain; app.rustle.domain.Wx.SNOW -> R.string.wx_snow
                    app.rustle.domain.Wx.FOG -> R.string.wx_fog; app.rustle.domain.Wx.STORM -> R.string.wx_storm
                })
            }
            Text(listOfNotNull(stringResource(R.string.head_sub, h.rank.title.of(h.ru), h.days), streak.takeIf { it >= 2 }?.let { stringResource(R.string.head_streak, it) }, weather).joinToString(" · "),
                style = T.Small.copy(color = C.textDim), maxLines = 1)
        }
        val label = stringResource(R.string.settings)
        Box(Modifier.size(40.dp).background(C.boardD, androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).clickable { go(Overlay.Settings) }.semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(18.dp)) {
                val p = size.width / 9
                fun r(x: Int, y: Int, w: Int = 1, hh: Int = 1) = drawRect(C.title, Offset(x * p, y * p), Size(w * p, hh * p))
                r(1, 1, 7, 7); for ((x, y) in listOf(3 to 0, 5 to 0, 0 to 3, 0 to 5, 8 to 3, 8 to 5, 3 to 8, 5 to 8)) r(x, y)
                drawRect(C.boardD, Offset(3 * p, 3 * p), Size(3 * p, 3 * p))
                for ((x, y) in listOf(1 to 1, 7 to 1, 1 to 7, 7 to 7)) drawRect(C.boardD, Offset(x * p, y * p), Size(p, p))
            }
        }
    }
}

/** Four tabs, each a pixel picture over a word; the chosen one sits on a copper pill. */
@Composable
private fun TabBar(tab: Tab, onTab: (Tab) -> Unit) {
    Row(Modifier.fillMaxWidth().background(C.boardD).navigationBarsPadding().padding(horizontal = 6.dp, vertical = 6.dp)) {
        for ((t, l, icon) in listOf(
            Triple(Tab.HOME, R.string.tab_home, app.rustle.ui.art.Icons.home),
            Triple(Tab.TRIPS, R.string.tab_trips, app.rustle.ui.art.Icons.pack),
            Triple(Tab.SHELF, R.string.tab_shelf, app.rustle.ui.art.Icons.chest),
            Triple(Tab.WORKSHOP, R.string.tab_workshop, app.rustle.ui.art.Icons.tools),
        )) {
            val on = t == tab
            Column(
                Modifier.weight(1f).padding(horizontal = 3.dp)
                    .background(if (on) C.copper.copy(alpha = .22f) else androidx.compose.ui.graphics.Color.Transparent, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .clickable { onTab(t) }.padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                app.rustle.ui.components.BufImage(icon, scale = 1.dp)
                Text(stringResource(l), style = T.Small.copy(color = if (on) C.hi else C.textDim), textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}

@Composable
private fun Notes(modifier: Modifier) {
    val bus = LocalBus.current
    val n = bus.notes.firstOrNull() ?: return
    LaunchedEffect(n) { delay(3600); bus.notes.remove(n) }
    Column(modifier.statusBarsPadding().padding(top = 60.dp, start = 14.dp, end = 14.dp).fillMaxWidth().pixelBox(C.paper)
        .clickable { bus.notes.remove(n) }.padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(n.title, style = T.Name.copy(color = C.ember))
        Text(n.body, style = T.Say)
    }
}
