package app.rustle.ui.shelf

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.rustle.R
import app.rustle.container
import app.rustle.domain.Catalog
import app.rustle.domain.District
import app.rustle.ui.Home
import app.rustle.ui.Nav
import app.rustle.ui.Overlay
import app.rustle.ui.components.FindIcon
import app.rustle.ui.components.Flow
import app.rustle.ui.components.PixelButton
import app.rustle.ui.components.Section
import app.rustle.ui.components.Sheet
import app.rustle.ui.components.Tip
import app.rustle.ui.components.tray
import app.rustle.ui.home.rarity
import app.rustle.ui.theme.C
import app.rustle.ui.theme.T
import java.text.DateFormat
import java.util.Date

@Composable
fun ShelfScreen(h: Home, nav: Nav) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Tip("shelf", stringResource(R.string.tip_shelf_t), stringResource(R.string.tip_shelf), h.look, h.s.tips)
        Text(stringResource(R.string.s_count, h.s.seen.size, Catalog.finds.size), style = T.Small.copy(color = C.textDim))
        for (d in District.entries) {
            val list = Catalog.of(d)
            Section(d.name_.of(h.ru), "${list.count { it.key in h.s.seen }}/${list.size}")
            if (d !in h.open && list.none { it.key in h.s.seen }) Text(stringResource(R.string.s_unknown), style = T.Small.copy(color = C.muted))
            Flow {
                for (f in list) {
                    val seen = f.key in h.s.seen
                    val n = h.s.inv[f.key] ?: 0
                    Box(Modifier.size(56.dp).tray().then(if (seen) Modifier.clickable { nav.go(Overlay.Find(f.key)) } else Modifier), contentAlignment = Alignment.Center) {
                        FindIcon(f, seen = seen)
                        if (n > 0) Text("$n", style = T.Small.copy(color = C.hi), modifier = Modifier.align(Alignment.BottomEnd).padding(end = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun FindSheet(h: Home, key: String, nav: Nav) {
    val c = LocalContext.current.container
    val f = Catalog.find(key) ?: return
    Sheet(nav::close) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FindIcon(f, scale = 4.dp)
            Column(Modifier.weight(1f)) {
                Text(f.name.of(h.ru), style = T.Title)
                Text(stringResource(rarity(f.rare)) + " · " + f.shape.label.of(h.ru), style = T.Small)
            }
        }
        Text(stringResource(R.string.s_where, f.district.name_.of(h.ru)), style = T.Body)
        h.s.seen[key]?.let { Text(stringResource(R.string.s_first, DateFormat.getDateInstance(DateFormat.LONG).format(Date(it))), style = T.Small) }
        Text(stringResource(R.string.s_have, h.s.inv[key] ?: 0), style = T.Small)
        val uses = Catalog.builds.filter { f.shape in it.cost }.map { it.name.of(h.ru) } + Catalog.dishes.filter { f.shape in it.cost }.map { it.name.of(h.ru) }
        if (uses.isNotEmpty()) Text(stringResource(R.string.s_uses, uses.joinToString(", ")), style = T.Small)
        if (f.food > 0 && (h.s.inv[key] ?: 0) > 0 && h.away == null) PixelButton(stringResource(R.string.s_eat, f.food), { c.write { feed(f) } }, Modifier.fillMaxWidth())
    }
}
