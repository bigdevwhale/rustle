package app.rustle.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.rustle.R
import app.rustle.container
import app.rustle.data.MIN
import app.rustle.device.Phone
import app.rustle.domain.Lines
import app.rustle.domain.Nights
import app.rustle.domain.Pose
import app.rustle.ui.Home
import app.rustle.ui.Nav
import app.rustle.ui.Overlay
import app.rustle.ui.Tab
import app.rustle.ui.art.Buf
import app.rustle.ui.art.Icons
import app.rustle.ui.art.Nook
import app.rustle.ui.art.NookScene
import app.rustle.ui.art.PixelCanvas
import app.rustle.ui.components.Bar
import app.rustle.ui.components.Cost
import app.rustle.ui.components.Tip
import app.rustle.domain.Catalog
import app.rustle.ui.components.BufImage
import app.rustle.ui.components.Kind
import app.rustle.ui.components.PX
import app.rustle.ui.components.Panel
import app.rustle.ui.components.PixelButton
import app.rustle.ui.components.pixelBox
import app.rustle.ui.theme.C
import app.rustle.ui.theme.T
import java.text.DateFormat
import java.util.Date

@Composable
fun HomeScreen(h: Home, phone: Phone, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val pose = h.pose(phone)
    var lastTick by remember { mutableIntStateOf(0) }
    var pettedTick by remember { mutableIntStateOf(-100) }
    var hush by remember { mutableIntStateOf(0) }
    val scene = NookScene(
        look = h.look, pose = pose, level = phone.level, charging = phone.charging, hour = h.hour, dark = phone.dark, sunny = phone.sunny,
        dusty = phone.storage > .85f || h.cleanDue, built = h.placed, shelf = h.best, sproutDays = h.days, pettedTick = pettedTick, wx = h.wx,
    )
    val away = h.away
    val asleep = h.asleep
    val line = when {
        away != null || h.surfing != null -> null
        asleep != null -> if (hush % 2 == 1) stringResource(R.string.h_shh) else Lines.say.getValue(Pose.SLEEP)[(h.now / (5 * MIN)).toInt() % 2].of(h.ru)
        pose == Pose.COLD && "scarf" in h.s.wear -> stringResource(R.string.h_scarf)
        // A week since the last spring-clean: now and then he mentions the dust (#1).
        pose == Pose.IDLE && h.cleanDue && (h.now / (3 * MIN)) % 3 == 1L -> Lines.dusty[((h.now / (9 * MIN)) % Lines.dusty.size).toInt()].of(h.ru)
        // Now and then, when it is idle, he talks about the weather outside.
        pose == Pose.IDLE && h.wx != null && (h.now / (3 * MIN)) % 3 == 0L -> Lines.weather.getValue(h.wx).let { it[((h.now / (9 * MIN)) % it.size).toInt()].of(h.ru) }
        else -> Lines.say.getValue(pose).let { it[((h.now / (3 * MIN)) % it.size).toInt()].of(h.ru) }
    }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().background(C.boardD).pointerInput(scene) {
            detectTapGestures { at ->
                // Only a tap on him counts: the scene is the nook scaled to the full width.
                val k = size.width / Nook.W.toFloat()
                if (!Nook.onHim(scene, lastTick, at.x / k, at.y / k)) return@detectTapGestures
                when (pose) {
                    Pose.AWAY -> Unit
                    Pose.SLEEP -> hush++
                    else -> { pettedTick = lastTick; c.write { pet() } }
                }
            }
        }) {
            PixelCanvas(Nook.W, Nook.H, Modifier.fillMaxWidth()) { b, t -> lastTick = t; Nook.draw(b, scene, t) }
            if (line != null) Bubble(line, Modifier.align(Alignment.TopCenter).padding(start = 12.dp, end = 12.dp, top = 30.dp))
            StoveChip(phone, Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp))
        }
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Tip("home", stringResource(R.string.tip_home_t), stringResource(R.string.tip_home), h.look, h.s.tips)
            if (h.s.weatherOn && !h.s.locAsked && h.s.lat.isNaN() && "home" in h.s.tips) WeatherCard()
            if (away != null) Panel(Modifier.fillMaxWidth(), fill = C.note) {
                Text(stringResource(R.string.h_away, h.name, away.district.name_.of(h.ru)), style = T.Name)
                if (away.minutes > 0) {
                    val left = ((away.due - h.now) / MIN + 1).coerceAtLeast(1)
                    Text(stringResource(R.string.h_away_due, DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(away.due)), left), style = T.Small)
                    Bar((h.now - away.start).toFloat() / (away.minutes * MIN), C.copper, Modifier.padding(top = 6.dp))
                } else Text(stringResource(R.string.h_away_free), style = T.Small)
                // While the screen is on, how long before he turns back (#2).
                if (away.minutes > 0 && away.unlockAt > 0) {
                    val left = ((away.allowance - away.peek(h.now)) / MIN + 1).coerceAtLeast(1)
                    Text(stringResource(R.string.h_away_peek, left), style = T.Small.copy(color = C.emberD), modifier = Modifier.padding(top = 6.dp))
                } else Text(stringResource(R.string.h_away_hint), style = T.Small.copy(color = C.muted), modifier = Modifier.padding(top = 6.dp))
                PixelButton(stringResource(R.string.h_call_home), { c.write { callHome() } }, Modifier.fillMaxWidth().padding(top = 8.dp), kind = Kind.PALE, small = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pips(stringResource(R.string.h_full), h.full, { Icons.crumb(it) }, Modifier.weight(1f).clickable { nav.go(Overlay.Needs) },
                    low = stringResource(R.string.h_full_low).takeIf { h.full <= app.rustle.domain.Loot.HUNGRY })
                Pips(stringResource(R.string.h_mood), h.mood, { Icons.heart(it) }, Modifier.weight(1f).clickable { nav.go(Overlay.Needs) },
                    low = stringResource(R.string.h_mood_low).takeIf { h.mood <= app.rustle.domain.Loot.GLUM })
            }
            when {
                asleep != null -> {
                    Text(stringResource(R.string.h_asleep, DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(asleep.start))), style = T.Small.copy(color = C.textDim))
                    Tile(Icons.sun, stringResource(R.string.h_morning), Modifier.fillMaxWidth(), C.copper) { c.write { wake() } }
                }
                h.surfing != null -> NetAway(h, h.surfing)
                away == null -> {
                    val bed = Nights.bedtime(h.now) || app.rustle.BuildConfig.EVENING
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Tile(Icons.bowl, stringResource(R.string.h_feed), Modifier.weight(1f)) { nav.go(Overlay.Feed) }
                        Tile(Icons.pack, stringResource(R.string.h_send), Modifier.weight(1f), C.copper) { nav.tab(Tab.TRIPS) }
                        Tile(Icons.maze, stringResource(R.string.h_maze), Modifier.weight(1f)) { nav.go(Overlay.Maze) }
                        if (bed) Tile(Icons.moon, stringResource(R.string.h_bed), Modifier.weight(1f)) { c.write { toBed(phone.charging) } }
                    }
                    if (bed && !phone.charging) Text(stringResource(R.string.h_bed_hint), style = T.Small.copy(color = C.textDim))
                    if (h.s.trips.isEmpty()) Text(stringResource(R.string.h_first), style = T.Small.copy(color = C.textDim))
                }
            }
            // Everything else waits on small plates in one row; each opens its own sheet, so the home screen
            // fits without scrolling.
            Errands(h, phone, nav)
            Box(Modifier.height(4.dp))
        }
    }
}

/** What he says, on a paper note over the floor of the scene. */
@Composable
private fun Bubble(text: String, modifier: Modifier) {
    Box(modifier.fillMaxWidth()) {
        Text(text, style = T.Say, modifier = Modifier.fillMaxWidth().pixelBox(C.paper).padding(horizontal = 12.dp, vertical = 9.dp))
    }
}

/** The stove's state as a small plate in the corner. */
@Composable
private fun StoveChip(p: Phone, modifier: Modifier) {
    val text = when {
        p.charging -> stringResource(R.string.h_stove_lit, p.level)
        p.low -> stringResource(R.string.h_stove_cold, p.level)
        else -> stringResource(R.string.h_stove, p.level)
    }
    Text(text, style = T.Small.copy(color = if (p.low) C.emberL else C.copperL),
        modifier = modifier.background(Color(0xB0091410)).padding(horizontal = 6.dp, vertical = 2.dp))
}

/** A need as five pictograms that fill up. */
@Composable
private fun Pips(label: String, value: Double, icon: (Boolean) -> Buf, modifier: Modifier, low: String? = null) {
    Column(modifier.pixelBox(C.boardD, outline = C.night, bevel = null, lip = null).padding(horizontal = 8.dp, vertical = 6.dp)
        .semantics(mergeDescendants = true) { contentDescription = "$label ${value.toInt()}/100" + (low?.let { ", $it" } ?: "") }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = T.Small.copy(color = C.textDim), modifier = Modifier.weight(1f), maxLines = 1)
            // Running low: said in words too, not only by empty pips.
            if (low != null) Text(low, style = T.Small.copy(color = C.emberL), maxLines = 1)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            val n = ((value + 10) / 20).toInt().coerceIn(0, 5)
            for (i in 0 until 5) BufImage(icon(i < n), scale = 1.dp)
        }
    }
}

/** An action as a square tile: a pictogram and a word. */
@Composable
private fun Tile(icon: Buf, label: String, modifier: Modifier, fill: Color = C.paper, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column(
        modifier.offset { IntOffset(0, if (pressed) PX.roundToPx() else 0) }.pixelBox(fill, pressed = pressed, lip = if (fill == C.copper) C.copperD else C.paperD)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        BufImage(icon, scale = 1.5.dp)
        Text(label, style = T.Button.copy(color = C.ink, fontSize = 15.sp), textAlign = TextAlign.Center, maxLines = 1)
    }
}

/** The plates under the actions: this week's requests, the spring-clean, the Net, what to make next. */
@Composable
private fun Errands(h: Home, phone: Phone, nav: Nav) {
    val plates = mutableListOf<@Composable (Modifier) -> Unit>()
    val week = app.rustle.domain.Requests.week(app.rustle.domain.World.dayOf(h.now))
    val since = app.rustle.domain.Requests.start(week)
    val asks = app.rustle.domain.Requests.of(h.s, h.now)
    val open = asks.count { it.key !in h.s.requests }
    val ready = asks.count { it.key !in h.s.requests && it.progress(h.s, since) >= it.goal }
    if (open > 0) plates += { m -> Errand(stringResource(R.string.e_asks), if (ready > 0) stringResource(R.string.e_ready, ready) else stringResource(R.string.e_left, open), ready > 0, m) { nav.go(Overlay.Card("asks")) } }
    val idle = h.away == null && h.asleep == null && h.surfing == null
    if (idle && h.cleanDue) plates += { m -> Errand(stringResource(R.string.e_clean), stringResource(if (h.s.cloths > 0) R.string.e_clean_go else R.string.e_clean_cloth), h.s.cloths > 0, m) {
        if (h.s.cloths > 0) nav.go(Overlay.Clean) else nav.go(Overlay.Card("clean")) } }
    if (idle && app.rustle.domain.Net.canGo(h.s, h.now, phone.wifi)) plates += { m -> Errand(stringResource(R.string.e_net), stringResource(R.string.e_net_go, app.rustle.domain.Net.MINUTES), true, m) { nav.go(Overlay.Card("net")) } }
    val goal = Catalog.builds.firstOrNull { it.kind == app.rustle.domain.Kind.TOOL && it.key !in h.s.built } ?: Catalog.builds.firstOrNull { it.key !in h.s.built }
    if (goal != null) plates += { m -> Errand(stringResource(R.string.e_make), goal.name.of(h.ru), Catalog.canPay(h.s.inv, goal.cost), m) { nav.go(Overlay.Card("goal")) } }
    if (plates.isEmpty()) return
    // One row of equal plates across the width, all in view (there are four at most).
    app.rustle.ui.components.Cells(plates.size, plates.size) { i, m -> plates[i](m) }
}

/** One plate: what it is, the state of it, lit up when something can be done now. */
@Composable
private fun Errand(title: String, note: String, hot: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Panel(modifier, fill = if (hot) C.copperL else C.paper, onClick = onClick, padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 6.dp)) {
        Text(title, style = T.Name.copy(fontSize = 14.sp), maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(note, style = T.Small.copy(color = if (hot) C.emberD else C.muted, fontSize = 12.sp), maxLines = 2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

/** The sheet behind a plate. */
@Composable
fun HomeCardSheet(h: Home, phone: Phone, which: String, nav: Nav) {
    app.rustle.ui.components.Sheet(nav::close) {
        when (which) {
            "asks" -> Asks(h)
            "clean" -> CleanCard(h, nav)
            "net" -> NetCard(h, phone) { nav.close() }
            "goal" -> Goal(h, nav)
        }
    }
}

/** The next thing worth making, so there is always somewhere to go: tools first, then the nook. */
@Composable
private fun Goal(h: Home, nav: Nav) {
    val b = Catalog.builds.firstOrNull { it.kind == app.rustle.domain.Kind.TOOL && it.key !in h.s.built }
        ?: Catalog.builds.firstOrNull { it.key !in h.s.built } ?: return
    val ready = Catalog.canPay(h.s.inv, b.cost)
    Panel(Modifier.fillMaxWidth(), fill = if (ready) C.copperL else C.paper, onClick = { nav.tab(Tab.WORKSHOP) }) {
        Text(stringResource(R.string.h_goal), style = T.Small.copy(color = C.emberD))
        Text(b.name.of(h.ru), style = T.Name)
        Text(b.note.of(h.ru), style = T.Small)
        Cost(b.cost, { Catalog.have(h.s.inv, it) }, h.ru)
        if (ready) Text(stringResource(R.string.h_goal_ready), style = T.Small.copy(color = C.ok))
    }
}

/** This week's three requests: what he wants, how far along, and a button when it is done. */
@Composable
private fun Asks(h: Home) {
    val c = LocalContext.current.container
    val week = app.rustle.domain.Requests.week(app.rustle.domain.World.dayOf(h.now))
    val since = app.rustle.domain.Requests.start(week)
    val left = ((app.rustle.domain.Requests.start(week + 1) - h.now) / app.rustle.data.DAY + 1).toInt()
    Panel(Modifier.fillMaxWidth(), fill = C.note) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.a_title), style = T.Name.copy(color = C.emberD), modifier = Modifier.weight(1f))
            Text(stringResource(R.string.a_left, left), style = T.Small)
        }
        Text(stringResource(R.string.a_intro), style = T.Small)
        val explained = mutableSetOf<Int>()
        for (a in app.rustle.domain.Requests.of(h.s, h.now)) {
            val claimed = a.key in h.s.requests
            val p = a.progress(h.s, since)
            Column(Modifier.padding(top = 8.dp)) {
                Text(app.rustle.domain.Requests.text(a, h.ru), style = T.Say.copy(color = if (claimed) C.muted else C.ink))
                val how = when (a) {
                    is app.rustle.domain.Ask.Bring -> R.string.a_how_bring
                    is app.rustle.domain.Ask.Nights -> R.string.a_how_nights
                    is app.rustle.domain.Ask.Fresh -> R.string.a_how_fresh
                    else -> R.string.a_how_trips
                }
                if (!claimed && explained.add(how)) Text(stringResource(how), style = T.Small)
                when {
                    claimed -> Text(stringResource(R.string.a_done), style = T.Small.copy(color = C.ok))
                    p >= a.goal -> PixelButton(stringResource(if (a is app.rustle.domain.Ask.Bring) R.string.a_give else R.string.a_claim), { c.write { claimRequest(a.key) } },
                        Modifier.fillMaxWidth().padding(top = 4.dp), small = true)
                    else -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 2.dp)) {
                        Bar(p / a.goal.toFloat(), C.copper, Modifier.weight(1f))
                        Text("$p/${a.goal}", style = T.Small)
                    }
                }
            }
        }
    }
}

/** A week since the last spring-clean (#1): wipe the phone with a cloth, or go and sew one. */
@Composable
private fun CleanCard(h: Home, nav: Nav) {
    val sc = app.rustle.domain.SpringClean
    val have = h.s.cloths > 0
    Panel(Modifier.fillMaxWidth(), fill = if (have) C.copperL else C.note) {
        Text(stringResource(R.string.h_clean_t), style = T.Name)
        if (have) {
            Text(stringResource(R.string.h_clean_have, h.s.cloths), style = T.Small)
            PixelButton(stringResource(R.string.h_clean_go), { nav.go(Overlay.Clean) }, Modifier.fillMaxWidth().padding(top = 8.dp), small = true)
        } else {
            Text(stringResource(R.string.h_clean_none), style = T.Small)
            Cost(sc.clothCost, { Catalog.have(h.s.inv, it) }, h.ru)
            PixelButton(stringResource(R.string.h_clean_to), { nav.tab(Tab.WORKSHOP) }, Modifier.fillMaxWidth().padding(top = 8.dp), kind = Kind.PALE, small = true)
        }
    }
}

/** On Wi-Fi, once a day, he may pop into the Net (#1). */
@Composable
private fun NetCard(h: Home, phone: Phone, done: () -> Unit = {}) {
    val ctx = LocalContext.current
    val c = ctx.container
    val bus = app.rustle.ui.LocalBus.current
    val n = app.rustle.domain.Net.MINUTES
    Panel(Modifier.fillMaxWidth(), fill = C.note) {
        Text(stringResource(R.string.h_net_t), style = T.Name)
        Text(stringResource(R.string.h_net_text, h.name, n), style = T.Small)
        PixelButton(stringResource(R.string.h_net_go), {
            c.write { goNet(phone.wifi) }
            bus.note(ctx.getString(R.string.n_net), ctx.getString(R.string.n_net_b, h.name, n))
            done()
        }, Modifier.fillMaxWidth().padding(top = 8.dp), small = true)
    }
}

/** While he is in the Net: when he will be back (#1). */
@Composable
private fun NetAway(h: Home, start: Long) {
    val due = start + app.rustle.domain.Net.LENGTH
    Panel(Modifier.fillMaxWidth(), fill = C.note) {
        Text(stringResource(R.string.h_net_away, h.name), style = T.Name)
        Text(stringResource(R.string.h_away_due, DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(due)), ((due - h.now) / MIN + 1).coerceAtLeast(1)), style = T.Small)
        Bar((h.now - start).toFloat() / app.rustle.domain.Net.LENGTH, C.copper, Modifier.padding(top = 6.dp))
        Text(stringResource(R.string.h_net_hint), style = T.Small.copy(color = C.muted), modifier = Modifier.padding(top = 6.dp))
    }
}

/** Asks once whether the window may show the weather where the phone is. */
@Composable
private fun WeatherCard() {
    val c = LocalContext.current.container
    val ask = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { ok ->
        c.write { store.update { it.copy(locAsked = true) }; if (ok && locate()) refreshWeather(force = true) }
    }
    Panel(Modifier.fillMaxWidth(), fill = C.note) {
        Text(stringResource(R.string.w_card_title), style = T.Name)
        Text(stringResource(R.string.w_card_text), style = T.Small, modifier = Modifier.padding(top = 2.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            PixelButton(stringResource(R.string.w_card_yes), { ask.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION) }, Modifier.weight(1f), small = true)
            PixelButton(stringResource(R.string.w_card_no), { c.write { store.update { it.copy(locAsked = true) } } }, kind = Kind.PALE, small = true)
        }
    }
}
