package app.rustle.ui.workshop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import app.rustle.ui.components.Portrait
import app.rustle.ui.components.Flow
import app.rustle.ui.components.Chip
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.rustle.R
import app.rustle.container
import app.rustle.domain.Build
import app.rustle.domain.Catalog
import app.rustle.domain.District
import app.rustle.domain.Kind
import app.rustle.ui.Home
import app.rustle.ui.LocalBus
import app.rustle.ui.components.Cost
import app.rustle.ui.components.Panel
import app.rustle.ui.components.PixelButton
import app.rustle.ui.components.Section
import app.rustle.ui.components.Tip
import app.rustle.ui.theme.C
import app.rustle.ui.theme.T
import app.rustle.ui.components.Kind as ButtonKind

@Composable
fun WorkshopScreen(h: Home) {
    val ctx = LocalContext.current
    val c = ctx.container
    val bus = LocalBus.current
    val have = { s: app.rustle.domain.Shape -> Catalog.have(h.s.inv, s) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Tip("workshop", stringResource(R.string.tip_workshop_t), stringResource(R.string.tip_workshop), h.look, h.s.tips)
        val tabs = listOf(R.string.w_tools, R.string.w_home, R.string.w_wear, R.string.w_kitchen)
        Flow { tabs.forEachIndexed { i, t -> Chip(stringResource(t), tab == i, { tab = i }, dark = true) } }
        if (tab < 3) {
            val kind = listOf(Kind.TOOL, Kind.HOME, Kind.WEAR)[tab]
            val list = Catalog.builds.filter { it.kind == kind }
            Text(stringResource(R.string.w_count, list.count { it.key in h.s.built }, list.size), style = T.Small.copy(color = C.textDim))
            // Ready to make first, then the rest, then what is done.
            val sorted = list.sortedBy { if (it.key in h.s.built) 2 else if (Catalog.canPay(h.s.inv, it.cost)) 0 else 1 }
            for (b in sorted) BuildCard(h, b, have) {
                c.write { build(b.key) }
                val opened = District.entries.firstOrNull { it.tool == b.key }
                bus.note(ctx.getString(R.string.w_made, b.name.of(h.ru)),
                    if (opened != null) ctx.getString(R.string.w_opened, opened.name_.of(h.ru)) else b.note.of(h.ru))
            }
            if (tab == 0) ClothCard(h, have)
        } else {
            Text(stringResource(R.string.w_kitchen_sub), style = T.Small.copy(color = C.textDim))
            for (d in Catalog.dishes) Panel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(d.name.of(h.ru), style = T.Name, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.w_dish, d.full, d.mood), style = T.Small)
                }
                Cost(d.cost, have, h.ru)
                if (Catalog.canPay(h.s.inv, d.cost) && h.away == null && h.surfing == null) PixelButton(stringResource(R.string.w_cook), { c.write { cook(d) }; bus.note(d.name.of(h.ru), ctx.getString(R.string.w_ate, h.name)) },
                    Modifier.fillMaxWidth().padding(top = 6.dp), small = true)
            }
        }
    }
}

/** Cloths for the spring-clean (#1): sewn again and again, each one worn out by one clean. */
@Composable
private fun ClothCard(h: Home, have: (app.rustle.domain.Shape) -> Int) {
    val ctx = LocalContext.current
    val c = ctx.container
    val bus = LocalBus.current
    val sc = app.rustle.domain.SpringClean
    Panel(Modifier.fillMaxWidth(), fill = if (h.s.cloths > 0) C.note else C.paper) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(sc.cloth.of(h.ru), style = T.Name, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.w_cloth_stock, h.s.cloths), style = T.Small.copy(color = if (h.s.cloths > 0) C.ok else C.muted))
        }
        Text(sc.clothNote.of(h.ru), style = T.Small)
        if (h.s.cloths >= sc.MAX_CLOTHS) Text(stringResource(R.string.w_cloth_full), style = T.Small.copy(color = C.muted))
        else {
            Cost(sc.clothCost, have, h.ru)
            if (sc.canSew(h.s)) PixelButton(stringResource(R.string.w_sew), {
                c.write { sewCloth() }
                bus.note(ctx.getString(R.string.w_made, sc.cloth.of(h.ru)), sc.clothNote.of(h.ru))
            }, Modifier.fillMaxWidth().padding(top = 6.dp), small = true)
        }
    }
}

@Composable
private fun BuildCard(h: Home, b: Build, have: (app.rustle.domain.Shape) -> Int, onBuild: () -> Unit) {
    val c = LocalContext.current.container
    val done = b.key in h.s.built
    Panel(Modifier.fillMaxWidth(), fill = if (done) C.note else C.paper) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Clothes are tried on him right here, made or not.
            if (b.kind == Kind.WEAR) {
                val others = h.s.wear.filter { Catalog.build(it)?.slot != b.slot }.toSet()
                Portrait(h.look.copy(wear = others + b.key), scale = 1.5.dp)
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(b.name.of(h.ru), style = T.Name, modifier = Modifier.weight(1f))
                    if (done) Text(stringResource(R.string.w_done), style = T.Small.copy(color = C.ok))
                }
                b.slot?.let { Text(it.title.of(h.ru), style = T.Small.copy(color = C.copperD)) }
                Text(b.note.of(h.ru), style = T.Small)
            }
        }
        if (!done) {
            Cost(b.cost, have, h.ru)
            // Only offered when everything is there; the red counts already say what is missing.
            if (Catalog.canPay(h.s.inv, b.cost)) PixelButton(stringResource(R.string.w_make), onBuild, Modifier.fillMaxWidth().padding(top = 6.dp), small = true)
        } else if (b.kind == Kind.WEAR) {
            val on = b.key in h.s.wear
            PixelButton(stringResource(if (on) R.string.w_take_off else R.string.w_put_on), { c.write { wear(b.key, !on) } },
                Modifier.fillMaxWidth().padding(top = 6.dp), kind = ButtonKind.PALE, small = true)
        } else if (b.slot != null) {
            val here = b.key in h.placed
            if (here) Text(stringResource(R.string.w_standing), style = T.Small.copy(color = C.ok))
            else PixelButton(stringResource(R.string.w_place), { c.write { place(b.slot, b.key) } }, Modifier.fillMaxWidth().padding(top = 6.dp), kind = ButtonKind.PALE, small = true)
        }
    }
}
