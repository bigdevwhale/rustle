package app.rustle.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.rustle.R
import app.rustle.domain.Feats
import app.rustle.domain.Group
import app.rustle.domain.Streak
import app.rustle.domain.World
import app.rustle.ui.Home
import app.rustle.ui.Nav
import app.rustle.ui.art.Buf
import app.rustle.ui.art.ramp
import app.rustle.ui.art.rect
import app.rustle.ui.art.rgb
import app.rustle.ui.art.ell
import app.rustle.ui.art.outline
import app.rustle.ui.components.Bar
import app.rustle.ui.components.BufImage
import app.rustle.ui.components.Page
import app.rustle.ui.components.Panel
import app.rustle.ui.components.Section
import app.rustle.ui.theme.C
import app.rustle.ui.theme.T
import java.text.DateFormat
import java.util.Date

/** A medal on a ribbon, gold when earned, a grey outline otherwise; the ribbon colour tells the group. */
private val medals = HashMap<String, Buf>()

fun medal(g: Group, done: Boolean): Buf = medals.getOrPut(g.name + done) {
    val b = Buf(16, 16)
    val ribbon = listOf(0xC0443A, 0x2E5AB0, 0x6A3A8A, 0x5AA050, 0xD8863E, 0x2E8A8A)[g.ordinal]
    val rr = ramp(rgb(ribbon.toLong()))
    if (done) { b.rect(4, 0, 3, 6, rr.b); b.rect(9, 0, 3, 6, rr.d); b.rect(4, 0, 1, 6, rr.l) } else { b.rect(4, 0, 3, 6, rgb(0x3A4A44)); b.rect(9, 0, 3, 6, rgb(0x2E3A36)) }
    val m = if (done) ramp(rgb(0xE8B840)) else ramp(rgb(0x3A4A44))
    b.ell(8, 10, 5, 5, m.d); b.ell(7.5, 9.5, 4, 4, m.b); b.ell(7, 9, 2, 2, m.l)
    if (done) { b.p(6, 8, m.hi); b.p(8, 10, m.d); b.p(9, 9, m.d); b.p(7, 11, m.d) }
    b.outline(rgb(0x1B140E))
    b
}

@Composable
fun FeatsPage(h: Home, nav: Nav) {
    val today = World.dayOf(h.now)
    Page(stringResource(R.string.f_title), nav::close) {
        val earned = Feats.all.count { it.key in h.s.feats }
        Text(stringResource(R.string.f_count, earned, Feats.all.size), style = T.Small.copy(color = C.textDim))
        Panel(Modifier.fillMaxWidth(), fill = C.note) {
            Text(stringResource(R.string.f_streak, Streak.current(h.s, today)), style = T.Heading)
            Text(stringResource(R.string.f_streak_best, Streak.best(h.s)), style = T.Small)
            Text(stringResource(R.string.f_streak_how), style = T.Small)
        }
        val df = DateFormat.getDateInstance(DateFormat.MEDIUM)
        for (g in Group.entries) {
            val list = Feats.all.filter { it.group == g }
            Section(g.title.of(h.ru), "${list.count { it.key in h.s.feats }}/${list.size}")
            for (f in list) {
                val at = h.s.feats[f.key]
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BufImage(medal(g, at != null), scale = 2.5.dp)
                    Column(Modifier.weight(1f)) {
                        Text(f.name.of(h.ru), style = T.Name.copy(color = if (at != null) C.hi else C.textDim))
                        Text(f.note.of(h.ru).let { n -> at?.let { n.trimEnd('.') + " · " + df.format(Date(it)) } ?: n }, style = T.Small.copy(color = C.textDim))
                        if (at == null && f.goal > 1) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Bar(f.progress(h.s) / f.goal.toFloat(), C.copper, Modifier.weight(1f))
                                Text("${f.progress(h.s)}/${f.goal}", style = T.Small.copy(color = C.textDim))
                            }
                        }
                    }
                }
            }
        }
    }
}

