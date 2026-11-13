package app.rustle.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.rustle.R
import app.rustle.ui.art.Buf
import app.rustle.ui.art.toBitmap
import app.rustle.ui.theme.C
import app.rustle.ui.theme.T
import kotlin.math.roundToInt

/** One "UI pixel": outlines, bevels and notched corners come in multiples of it. */
val PX: Dp = 2.dp

/** Corner radius of cards and buttons. */
val CORNER: Dp = 12.dp

/**
 * A soft card or a chunky button: rounded, a thin dark edge, a faint light line along the top and a
 * darker lip underneath that sinks away when [pressed]. [bevel] and [lip] null make it flat.
 */
fun Modifier.pixelBox(fill: Color, outline: Color = C.ink, bevel: Color? = Color.White.copy(alpha = .35f), lip: Color? = Color(0x40200C08), pressed: Boolean = false): Modifier = drawBehind {
    val r = CORNER.toPx()
    val e = 1.5.dp.toPx()
    val depth = if (lip != null && !pressed) 3.dp.toPx() else 0f
    val w = size.width
    val h = size.height
    if (depth > 0) drawRoundRect(mixDark(fill), Offset(0f, depth), Size(w, h - depth), CornerRadius(r))
    drawRoundRect(outline.copy(alpha = .55f), Offset.Zero, Size(w, h - depth), CornerRadius(r))
    drawRoundRect(fill, Offset(e, e), Size(w - 2 * e, h - depth - 2 * e), CornerRadius(r - e))
    if (bevel != null && !pressed) drawRoundRect(bevel, Offset(r * .6f, e + 1.dp.toPx()), Size(w - r * 1.2f, 1.5.dp.toPx()), CornerRadius(1.dp.toPx()))
}

/** A darker shade of [c] for a button's lip. */
private fun mixDark(c: Color) = Color(red = c.red * .62f, green = c.green * .58f, blue = c.blue * .6f, alpha = 1f)

/** A speech bubble: a paper card with a copper edge. */
fun Modifier.sayBox(fill: Color = C.paper): Modifier = drawBehind {
    val r = 16.dp.toPx()
    val e = 2.dp.toPx()
    drawRoundRect(C.copperD, Offset.Zero, size, CornerRadius(r))
    drawRoundRect(fill, Offset(e, e), Size(size.width - 2 * e, size.height - 2 * e), CornerRadius(r - e))
}

/** A dark sunken tray for slots and tiles. */
fun Modifier.tray(edge: Color = C.ink): Modifier = drawBehind {
    val r = 10.dp.toPx()
    drawRoundRect(edge.copy(alpha = .6f), Offset.Zero, size, CornerRadius(r))
    drawRoundRect(C.boardD, Offset(1.dp.toPx(), 2.dp.toPx()), Size(size.width - 2.dp.toPx(), size.height - 3.dp.toPx()), CornerRadius(r))
}

@Composable
fun Panel(modifier: Modifier = Modifier, fill: Color = C.paper, onClick: (() -> Unit)? = null, padding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 10.dp), content: @Composable ColumnScope.() -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column(
        modifier.pixelBox(fill, pressed = pressed && onClick != null)
            .then(if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** A lodger says something. */
@Composable
fun Says(text: String, modifier: Modifier = Modifier, who: String? = null) {
    Column(modifier.fillMaxWidth().sayBox().padding(horizontal = 14.dp, vertical = 10.dp)) {
        if (who != null) Text(who, style = T.Name.copy(color = C.emberD, fontSize = 16.sp))
        Text(text, style = T.Say)
    }
}

enum class Kind { BRASS, CRIM, PALE, WOOD }

@Composable
fun PixelButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, kind: Kind = Kind.BRASS, enabled: Boolean = true, small: Boolean = false) {
    val (fill, ink, lip) = when (kind) {
        Kind.BRASS -> Triple(C.copper, C.ink, C.copperD)
        Kind.CRIM -> Triple(C.ember, C.paper, C.emberD)
        Kind.PALE -> Triple(C.paper, C.ink, C.paperD)
        Kind.WOOD -> Triple(C.boardL, C.text, C.boardD)
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val a = if (enabled) 1f else .4f
    Box(
        modifier
            .offset { IntOffset(0, if (pressed) PX.roundToPx() else 0) }
            .pixelBox(fill.copy(alpha = a), C.ink, Color.White.copy(alpha = .35f * a), lip, pressed)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .heightIn(min = if (small) 36.dp else 46.dp)
            .padding(horizontal = if (small) 10.dp else 14.dp, vertical = if (small) 6.dp else 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = T.Button.copy(color = ink.copy(alpha = a), fontSize = if (small) 16.sp else 19.sp), textAlign = TextAlign.Center, maxLines = 1)
    }
}

/** A choice chip; [color] paints it as a genre plate when on. */
@Composable
fun Chip(text: String, on: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color? = null, dark: Boolean = false) {
    val fill = when { on -> color ?: C.copper; dark -> C.boardL; else -> C.paperD }
    val ink = when { on && color != null -> C.paper; on -> C.ink; dark -> C.text; else -> C.ink }
    Text(text, style = T.Small.copy(color = ink), maxLines = 1,
        modifier = modifier.pixelBox(fill, bevel = null, lip = null).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 7.dp))
}

/**
 * Equal cells laid out [columns] to a row across the full width: every choice in view at once, none cut off
 * and nothing to swipe. [cell] draws one cell into the modifier it is given.
 */
@Composable
fun Cells(count: Int, columns: Int, modifier: Modifier = Modifier, gap: Dp = 6.dp, cell: @Composable (Int, Modifier) -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
        for (r in 0 until (count + columns - 1) / columns) Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(gap)) {
            for (c in 0 until columns) {
                val i = r * columns + c
                if (i < count) cell(i, Modifier.weight(1f).fillMaxHeight()) else Box(Modifier.weight(1f))
            }
        }
    }
}

/** A choice as an equal cell: the label centred, a count on a small plate when there is one. */
@Composable
fun SegChip(text: String, on: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, count: Int = 0) {
    Row(modifier.pixelBox(if (on) C.copper else C.boardL, bevel = null, lip = null).clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = T.Small.copy(color = if (on) C.ink else C.text), maxLines = 1, textAlign = TextAlign.Center,
            overflow = androidx.compose.ui.text.style.TextOverflow.Visible)
        if (count > 0) Text("$count", style = T.Small.copy(color = C.paper, fontSize = 11.sp), maxLines = 1,
            modifier = Modifier.padding(start = 4.dp).background(C.ember, androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).padding(horizontal = 5.dp))
    }
}

/** A small plate with a label. */
@Composable
fun Plate(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(text, style = T.Small.copy(color = C.paper), modifier = modifier.pixelBox(color, bevel = null, lip = null).padding(horizontal = 8.dp, vertical = 3.dp))
}

/** A status pill on tiles. */
@Composable
fun Pill(text: String, fill: Color, ink: Color) {
    Text(text, style = T.Small.copy(color = ink), modifier = Modifier.background(fill).padding(horizontal = 6.dp, vertical = 1.dp), maxLines = 1)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Flow(modifier: Modifier = Modifier, content: @Composable FlowRowScope.() -> Unit) =
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)

/** A still pixel picture scaled with hard pixels, [scale] dp per art pixel. */
@Composable
fun BufImage(buf: Buf, modifier: Modifier = Modifier, scale: Dp = 2.dp, smooth: Boolean = false) {
    val image = remember(buf) { buf.toBitmap().asImageBitmap() }
    Canvas(modifier.size(scale * buf.w, scale * buf.h)) {
        drawImage(image, srcOffset = IntOffset.Zero, srcSize = IntSize(buf.w, buf.h), dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()), filterQuality = if (smooth) FilterQuality.Medium else FilterQuality.None)
    }
}

/** A plain pixel text field. */
@Composable
fun Field(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier, style: TextStyle = T.Body, single: Boolean = true, onDone: (() -> Unit)? = null) {
    BasicTextField(value, onChange, modifier.fillMaxWidth().pixelBox(C.paper), textStyle = style, singleLine = single, cursorBrush = SolidColor(C.ink),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = if (onDone != null) ImeAction.Search else ImeAction.Default),
        keyboardActions = KeyboardActions(onSearch = { onDone?.invoke() }),
        decorationBox = { inner -> Box(Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) { if (value.isEmpty()) Text(hint, style = style.copy(color = C.muted)); inner() } })
}

/**
 * A paper sheet from the bottom over a dim layer: a grab bar and a close cross on top; tap outside,
 * swipe down, or back closes it.
 */
@Composable
fun Sheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onDismiss)
    val closeLabel = stringResource(R.string.close)
    Box(Modifier.fillMaxSize().background(Color(0xB0140A06)).clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss).imePadding(), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier.fillMaxWidth().widthIn(max = 560.dp).heightIn(max = 760.dp)
                .drawBehind { drawRect(C.paper); drawRect(C.ink, size = Size(size.width, PX.toPx())); drawRect(C.copper, Offset(0f, PX.toPx()), Size(size.width, 2 * PX.toPx())) }
                .clickable(remember { MutableInteractionSource() }, null) {}
                .pointerInput(Unit) { var dy = 0f; detectVerticalDragGestures(onDragStart = { dy = 0f }, onDragEnd = { if (dy > 120) onDismiss() }) { _, d -> dy += d } }
                .navigationBarsPadding(),
        ) {
            Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Box(Modifier.align(Alignment.TopCenter).width(48.dp).height(4.dp).background(C.paperD))
                Box(Modifier.align(Alignment.TopEnd).padding(end = 6.dp).size(40.dp).clickable(onClick = onDismiss).semantics { contentDescription = closeLabel }, contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(16.dp)) {
                        val p = size.width / 8
                        for (i in 0 until 8) { drawRect(C.muted, Offset(i * p, i * p), Size(p, p)); drawRect(C.muted, Offset((7 - i) * p, i * p), Size(p, p)) }
                    }
                }
            }
            Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
        }
    }
}

/** A full-screen page over the tabs with a back arrow. */
@Composable
fun Page(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(C.board).clickable(remember { MutableInteractionSource() }, null) {}.imePadding()) {
        Row(Modifier.fillMaxWidth().background(C.boardD).statusBarsPadding().padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(20.dp)) {
                    val p = size.width / 10
                    for (i in 0 until 5) {
                        drawRect(C.title, Offset((1 + i) * p, (5 - i) * p - p / 2), Size(p * 2, p))
                        drawRect(C.title, Offset((1 + i) * p, (4 + i) * p + p / 2), Size(p * 2, p))
                    }
                }
            }
            Spacer(Modifier.width(4.dp))
            Text(title, style = T.Title.copy(color = C.title), modifier = Modifier.weight(1f), maxLines = 1)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 12.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
fun Gap(h: Dp = 8.dp) = Spacer(Modifier.height(h))

/** A section label on wood. */
@Composable
fun Section(text: String, note: String? = null, modifier: Modifier = Modifier) {
    Row(modifier.padding(top = 6.dp), verticalAlignment = Alignment.Bottom) {
        Text(text, style = T.Heading.copy(color = C.title))
        if (note != null) Text("  $note", style = T.Small.copy(color = C.textDim))
    }
}

fun Buf.toImage() = toBitmap().asImageBitmap()

/** A pixel gauge: a dark tray with a filled run of [fraction]. */
@Composable
fun Bar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(10.dp)) {
        val r = CornerRadius(size.height / 2)
        drawRoundRect(Color(0x33000000), size = size, cornerRadius = r)
        val w = size.width * fraction.coerceIn(0f, 1f)
        if (w > 0) {
            drawRoundRect(color, size = Size(w.coerceAtLeast(size.height), size.height), cornerRadius = r)
            drawRoundRect(Color.White.copy(alpha = .35f), Offset(size.height / 3, 1.5.dp.toPx()), Size((w - size.height / 1.5f).coerceAtLeast(0f), 1.5.dp.toPx()), CornerRadius(1.dp.toPx()))
        }
    }
}
