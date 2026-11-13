package app.rustle.ui.workshop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
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
        // Each tab says how many things can be made on it right now, so nothing waits unseen.
        val kinds = listOf(Kind.TOOL, Kind.HOME, Kind.WEAR)
        val ready = kinds.map { k -> Catalog.builds.count { it.kind == k && it.key !in h.s.built && Catalog.canPay(h.s.inv, it.cost) } } +
            Catalog.dishes.count { Catalog.canPay(h.s.inv, it.cost) }
        app.rustle.ui.components.Cells(tabs.size, 2) { i, m -> app.rustle.ui.components.SegChip(stringResource(tabs[i]), tab == i, { tab = i }, m, count = ready[i]) }
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
                val kept = h.s.larder[d.key] ?: 0
                if (kept > 0) Text(stringResource(R.string.w_in_larder, kept), style = T.Small.copy(color = C.ok))
                // Eaten straight away, or put by in the pantry and given from the bowl later (#3).
                if (Catalog.canPay(h.s.inv, d.cost)) Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (h.away == null && h.surfing == null) PixelButton(stringResource(R.string.w_cook), { c.write { cook(d) }; bus.note(d.name.of(h.ru), ctx.getString(R.string.w_ate, h.name)) },
                        Modifier.weight(1f), small = true)
                    PixelButton(stringResource(R.string.w_cook_keep), { c.write { cook(d, keep = true) }; bus.note(d.name.of(h.ru), ctx.getString(R.string.w_kept)) },
                        Modifier.weight(1f), kind = ButtonKind.PALE, small = true)
                }
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
    val canMake = !done && Catalog.canPay(h.s.inv, b.cost)
    // Compact: the name with its one action beside it, a line of what it is, then what it costs.
    Panel(Modifier.fillMaxWidth(), fill = when { canMake -> C.copperL; done -> C.note; else -> C.paper },
        padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Clothes are tried on him right here, made or not.
            if (b.kind == Kind.WEAR) {
                val others = h.s.wear.filter { Catalog.build(it)?.slot != b.slot }.toSet()
                Portrait(h.look.copy(wear = others + b.key), scale = 1.dp)
            }
            Column(Modifier.weight(1f)) {
                Text(b.name.of(h.ru), style = T.Name, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(listOfNotNull(b.slot?.title?.of(h.ru), b.note.of(h.ru)).joinToString(" · "), style = T.Small, maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            when {
                canMake -> PixelButton(stringResource(R.string.w_make), onBuild, small = true)
                done && b.kind == Kind.WEAR -> {
                    val on = b.key in h.s.wear
                    PixelButton(stringResource(if (on) R.string.w_take_off else R.string.w_put_on), { c.write { wear(b.key, !on) } }, kind = ButtonKind.PALE, small = true)
                }
                done && b.slot != null && b.key !in h.placed -> PixelButton(stringResource(R.string.w_place_short), { c.write { place(b.slot, b.key) } }, kind = ButtonKind.PALE, small = true)
                done -> Text(stringResource(if (b.slot != null) R.string.w_standing else R.string.w_done), style = T.Small.copy(color = C.ok))
            }
        }
        // The red counts say what is missing.
        if (!done) Cost(b.cost, have, h.ru)
    }
}
