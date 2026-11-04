package app.rustle.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import app.rustle.BuildConfig
import app.rustle.R
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.shape.RoundedCornerShape
import app.rustle.container
import app.rustle.data.State
import app.rustle.ui.Nav
import app.rustle.ui.components.Chip
import app.rustle.ui.components.Field
import app.rustle.ui.components.Flow
import app.rustle.ui.components.Kind
import app.rustle.ui.components.Page
import app.rustle.ui.components.Panel
import app.rustle.ui.components.PixelButton
import app.rustle.ui.components.Section
import app.rustle.ui.onboarding.ColourPicker
import app.rustle.ui.onboarding.MarksPicker
import app.rustle.ui.onboarding.SpeciesPicker
import app.rustle.ru
import app.rustle.ui.theme.C
import app.rustle.ui.theme.Hue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import app.rustle.ui.theme.T

/** The StillCraft bot: deep links file a bug or an idea for this app, or follow its releases. */
object Community {
    private const val BOT = "https://t.me/ArcaneWhisperBot?start="
    const val SLUG = "rustle"
    const val BUG = "${BOT}bug_$SLUG"
    const val IDEA = "${BOT}wish_$SLUG"
    const val FOLLOW = "${BOT}f_$SLUG"
    const val CHAT = "https://t.me/+Q4gFEDk8O842OGVi"
}

/** The settings' own pages. */
private enum class Sub { LOOK, THEME, WEATHER, LANG, EXTRAS, BACKGROUND, COMMUNITY, ABOUT }

/**
 * Settings as a short list of rows, each with what is set now; a row opens its own page. Keeps the
 * first screen calm instead of one long sheet of everything.
 */
@Composable
fun SettingsPage(s: State, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val ru = ctx.ru
    var sub by remember { mutableStateOf<Sub?>(null) }
    fun open(url: String) { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    val cur = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-').substringBefore(',')

    when (sub) {
        null -> Page(stringResource(R.string.settings), nav::close) {
            val kind = app.rustle.domain.Species.of(s.species).title.of(ru)
            Row(stringResource(R.string.set_row_pet), "${s.name.ifBlank { kind }} · $kind") { sub = Sub.LOOK }
            Row(stringResource(R.string.set_look), stringResource(when (s.theme) { "dark" -> R.string.set_theme_dark; "light" -> R.string.set_theme_light; else -> R.string.set_theme_system }) +
                " · " + stringResource(hueName(Hue.of(s.hue)))) { sub = Sub.THEME }
            Row(stringResource(R.string.set_weather), when {
                !s.weatherOn -> stringResource(R.string.set_weather_off)
                s.lat.isNaN() -> stringResource(R.string.set_weather_nowhere)
                else -> s.placeName.ifBlank { stringResource(R.string.set_weather_here) }
            }) { sub = Sub.WEATHER }
            Row(stringResource(R.string.set_lang), stringResource(when (cur) { "ru" -> R.string.lang_ru; "en" -> R.string.lang_en; else -> R.string.lang_system })) { sub = Sub.LANG }
            Row(stringResource(R.string.set_extras), stringResource(R.string.set_extras_sub)) { sub = Sub.EXTRAS }
            Row(stringResource(R.string.set_battery), stringResource(R.string.set_battery_sub)) { sub = Sub.BACKGROUND }
            Row(stringResource(R.string.community), stringResource(R.string.set_community_sub)) { sub = Sub.COMMUNITY }
            Row(stringResource(R.string.about), stringResource(R.string.set_about_sub, BuildConfig.VERSION_NAME)) { sub = Sub.ABOUT }
        }

        Sub.LOOK -> {
            var name by remember { mutableStateOf(s.name) }
            Page(stringResource(R.string.set_row_pet), { if (name.isNotBlank() && name != s.name) c.write { store.update { it.copy(name = name.trim()) } }; sub = null }) {
                Section(stringResource(R.string.set_name))
                Field(name, { name = it.take(18) }, stringResource(R.string.ob_name_hint))
                Section(stringResource(R.string.set_kind))
                SpeciesPicker(app.rustle.domain.Species.of(s.species), s.body, s.accent, app.rustle.domain.Marks.of(s.marks), ru) { sp ->
                    c.write { look(species = sp.name, body = sp.body, accent = sp.accent) }
                }
                Section(stringResource(R.string.set_colour))
                ColourPicker(s.body) { b, a -> c.write { look(body = b, accent = a) } }
                Section(stringResource(R.string.set_marks))
                MarksPicker(app.rustle.domain.Marks.of(s.marks), ru) { m -> c.write { look(marks = m.name) } }
            }
        }

        Sub.THEME -> Page(stringResource(R.string.set_look), { sub = null }) {
            Section(stringResource(R.string.set_theme))
            Flow {
                for ((key, l) in listOf("system" to R.string.set_theme_system, "dark" to R.string.set_theme_dark, "light" to R.string.set_theme_light))
                    Chip(stringResource(l), s.theme == key, { c.write { store.update { it.copy(theme = key) } } }, dark = true)
            }
            Section(stringResource(R.string.set_hue))
            Flow {
                for (hue in Hue.entries) {
                    val p = hue.palette(C.pal.dark)
                    val on = s.hue == hue.name
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { c.write { store.update { it.copy(hue = hue.name) } } }.padding(2.dp)) {
                        Box(Modifier.size(52.dp).background(p.board, RoundedCornerShape(14.dp))
                            .border(if (on) 3.dp else 1.dp, if (on) C.copper else C.textDim.copy(alpha = .5f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center) {
                            Box(Modifier.size(22.dp, 6.dp).background(p.boardD, RoundedCornerShape(3.dp)))
                        }
                        Text(stringResource(hueName(hue)), style = T.Small.copy(color = if (on) C.title else C.textDim))
                    }
                }
            }
            Text(stringResource(R.string.set_look_note), style = T.Small.copy(color = C.textDim))
        }

        Sub.WEATHER -> WeatherPage(s) { sub = null }

        Sub.LANG -> Page(stringResource(R.string.set_lang), { sub = null }) {
            for ((tag, l) in listOf("" to R.string.lang_system, "ru" to R.string.lang_ru, "en" to R.string.lang_en))
                Choice(stringResource(l), cur == tag) { AppCompatDelegate.setApplicationLocales(if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)) }
        }

        Sub.EXTRAS -> Page(stringResource(R.string.set_extras), { sub = null }) {
            Section(stringResource(R.string.set_widget))
            Text(stringResource(R.string.set_widget_text), style = T.Small.copy(color = C.textDim))
            val wm = ctx.getSystemService(android.appwidget.AppWidgetManager::class.java)
            if (wm != null && wm.isRequestPinAppWidgetSupported)
                PixelButton(stringResource(R.string.set_widget_add), { wm.requestPinAppWidget(android.content.ComponentName(ctx, app.rustle.widget.Widget::class.java), null, null) }, kind = Kind.WOOD, small = true)
            Section(stringResource(R.string.set_tips))
            Text(stringResource(R.string.set_tips_text), style = T.Small.copy(color = C.textDim))
            PixelButton(stringResource(R.string.set_tips_again), { c.write { store.update { it.copy(tips = emptySet()) } }; nav.close() }, kind = Kind.WOOD, small = true)
        }

        Sub.BACKGROUND -> Page(stringResource(R.string.set_battery), { sub = null }) {
            Text(stringResource(R.string.set_battery_text), style = T.Body.copy(color = C.text))
            PixelButton(stringResource(R.string.set_battery_open), {
                runCatching { ctx.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + ctx.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }, Modifier.fillMaxWidth())
        }

        Sub.COMMUNITY -> Page(stringResource(R.string.community), { sub = null }) {
            Text(stringResource(R.string.community_text), style = T.Body.copy(color = C.text))
            for ((label, subtitle, url) in listOf(
                Triple(R.string.community_bug, R.string.community_bug_sub, Community.BUG),
                Triple(R.string.community_idea, R.string.community_idea_sub, Community.IDEA),
                Triple(R.string.community_follow, R.string.community_follow_sub, Community.FOLLOW),
                Triple(R.string.community_chat, R.string.community_chat_sub, Community.CHAT),
            )) Row(stringResource(label), stringResource(subtitle)) { open(url) }
        }

        Sub.ABOUT -> Page(stringResource(R.string.about), { sub = null }) {
            Text(stringResource(R.string.about_text, BuildConfig.VERSION_NAME), style = T.Body.copy(color = C.text))
            PixelButton(stringResource(R.string.source), { open("https://github.com/bigdevwhale/rustle") }, kind = Kind.WOOD, small = true)
        }
    }
}

private fun hueName(h: Hue) = when (h) { Hue.GREEN -> R.string.hue_green; Hue.NAVY -> R.string.hue_navy; Hue.PLUM -> R.string.hue_plum; Hue.COCOA -> R.string.hue_cocoa; Hue.GRAPHITE -> R.string.hue_graphite }

/** A settings row: a title, what is set now, an arrow. */
@Composable
private fun Row(title: String, value: String, onClick: () -> Unit) {
    Panel(Modifier.fillMaxWidth(), onClick = onClick) {
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = T.Name)
                Text(value, style = T.Small, maxLines = 1)
            }
            Text("›", style = T.Heading.copy(color = C.copperD))
        }
    }
}

/** One of several choices, with a dot when chosen. */
@Composable
private fun Choice(title: String, on: Boolean, onClick: () -> Unit) {
    Panel(Modifier.fillMaxWidth(), fill = if (on) C.copperL else C.paper, onClick = onClick) {
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = T.Name, modifier = Modifier.weight(1f))
            if (on) Text("●", style = T.Body.copy(color = C.copperD))
        }
    }
}

/** The window's weather: on or off, where it looks out, and the permission to find out. */
@Composable
fun WeatherPage(s: State, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val c = ctx.container
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        c.write { store.update { it.copy(locAsked = true) } }
        if (ok) { busy = true; c.write { failed = !locate(); refreshWeather(force = true); busy = false } }
    }
    Page(stringResource(R.string.set_weather), onBack) {
        Text(stringResource(R.string.set_weather_text), style = T.Body.copy(color = C.text))
        for ((on, l) in listOf(true to R.string.set_weather_on, false to R.string.set_weather_off))
            Choice(stringResource(l), s.weatherOn == on) { c.write { store.update { it.copy(weatherOn = on) }; if (on) refreshWeather(force = true) } }
        if (s.weatherOn) {
            Section(stringResource(R.string.set_weather_place))
            Text(if (s.lat.isNaN()) stringResource(R.string.set_weather_nowhere_long) else stringResource(R.string.set_weather_at, s.placeName.ifBlank { stringResource(R.string.set_weather_here) }),
                style = T.Body.copy(color = C.text))
            if (failed) Text(stringResource(R.string.set_weather_fail), style = T.Small.copy(color = C.emberL))
            PixelButton(stringResource(if (busy) R.string.set_weather_busy else if (s.lat.isNaN()) R.string.set_weather_find else R.string.set_weather_update), {
                if (androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    busy = true; c.write { failed = !locate(); refreshWeather(force = true); busy = false }
                } else ask.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
            }, Modifier.fillMaxWidth(), enabled = !busy)
            Text(stringResource(R.string.set_weather_privacy), style = T.Small.copy(color = C.textDim))
        }
    }
}
