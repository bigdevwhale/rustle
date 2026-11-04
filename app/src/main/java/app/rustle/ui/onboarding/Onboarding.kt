package app.rustle.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import app.rustle.domain.Marks
import app.rustle.domain.Pose
import app.rustle.domain.Species
import app.rustle.ru
import app.rustle.ui.art.Look
import app.rustle.ui.components.Field
import app.rustle.ui.components.Kind
import app.rustle.ui.components.Panel
import app.rustle.ui.components.PixelButton
import app.rustle.ui.components.Portrait
import app.rustle.ui.components.Says
import app.rustle.ui.theme.C
import app.rustle.ui.theme.T

@Composable
fun Onboarding() {
    val ctx = LocalContext.current
    val c = ctx.container
    val ru = ctx.ru
    var step by rememberSaveable { mutableIntStateOf(0) }
    var species by rememberSaveable { mutableStateOf(Species.MOUSE.name) }
    var name by rememberSaveable { mutableStateOf(Species.MOUSE.title.of(ru)) }
    var named by rememberSaveable { mutableStateOf(false) }
    var body by rememberSaveable { mutableIntStateOf(Species.MOUSE.body) }
    var accent by rememberSaveable { mutableIntStateOf(Species.MOUSE.accent) }
    var marks by rememberSaveable { mutableStateOf(Marks.NONE.name) }
    val sp = Species.of(species)
    val look = Look(body, accent, species = sp, marks = Marks.of(marks))
    Column(
        Modifier.fillMaxSize().background(C.board).statusBarsPadding().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Portrait(look, if (step == 3) Pose.WARM else Pose.IDLE, Modifier.padding(top = 12.dp), scale = 4.dp)
        when (step) {
            0 -> {
                Says(stringResource(R.string.ob_1), who = "?")
                Text(stringResource(R.string.ob_1b), style = T.Body.copy(color = C.text))
                PixelButton(stringResource(R.string.next), { step = 1 }, Modifier.fillMaxWidth())
            }
            1 -> {
                Says(stringResource(R.string.ob_kind), who = "?")
                SpeciesPicker(sp, body, accent, Marks.of(marks), ru) { picked ->
                    species = picked.name; body = picked.body; accent = picked.accent
                    if (!named) name = picked.title.of(ru)
                }
                PixelButton(stringResource(R.string.next), { step = 2 }, Modifier.fillMaxWidth())
            }
            2 -> {
                Says(stringResource(R.string.ob_2), who = "?")
                Field(name, { name = it.take(18); named = true }, stringResource(R.string.ob_name_hint))
                ColourPicker(body) { b, a -> body = b; accent = a }
                Text(stringResource(R.string.set_marks), style = T.Small.copy(color = C.textDim))
                MarksPicker(Marks.of(marks), ru) { marks = it.name }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PixelButton(stringResource(R.string.back), { step = 1 }, kind = Kind.PALE)
                    PixelButton(stringResource(R.string.next), { step = 3 }, Modifier.weight(1f), enabled = name.isNotBlank())
                }
            }
            else -> {
                Says(stringResource(R.string.ob_3), who = name)
                for ((t, b) in listOf(R.string.ob_3_trip to R.string.ob_3_trip_b, R.string.ob_3_night to R.string.ob_3_night_b, R.string.ob_3_home to R.string.ob_3_home_b))
                    Panel(Modifier.fillMaxWidth()) { Text(stringResource(t), style = T.Name); Text(stringResource(b), style = T.Small) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PixelButton(stringResource(R.string.back), { step = 2 }, kind = Kind.PALE)
                    PixelButton(stringResource(R.string.ob_start), { c.write { onboard(name, body, accent, species, marks) } }, Modifier.weight(1f))
                }
            }
        }
    }
}
