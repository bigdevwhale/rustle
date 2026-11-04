package app.rustle.ui.games

import android.content.Context
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.rustle.R
import app.rustle.container
import app.rustle.device.Device
import app.rustle.ui.Home
import app.rustle.ui.Nav
import app.rustle.ui.art.Buf
import app.rustle.ui.art.Items
import app.rustle.ui.art.ell
import app.rustle.ui.art.mix
import app.rustle.ui.art.ramp
import app.rustle.ui.art.rect
import app.rustle.ui.art.rgb
import app.rustle.ui.components.Kind
import app.rustle.ui.components.Page
import app.rustle.ui.components.PixelButton
import app.rustle.ui.components.Says
import app.rustle.ui.components.Tip
import app.rustle.ui.theme.C
import app.rustle.ui.theme.T
import app.rustle.domain.Catalog
import kotlin.math.roundToInt
import kotlin.random.Random

/** A maze of tracks on the board, carved by a depth-first walk: true is a wall. */
class Grid(val cols: Int, val rows: Int, seed: Long) {
    val w = cols * 2 + 1
    val h = rows * 2 + 1
    val wall = BooleanArray(w * h) { true }

    init {
        val r = Random(seed)
        val seen = BooleanArray(cols * rows)
        val stack = ArrayDeque<Int>()
        stack.addLast(0); seen[0] = true; open(1, 1)
        while (stack.isNotEmpty()) {
            val c = stack.last()
            val cx = c % cols; val cy = c / cols
            val next = listOf(0 to -1, 1 to 0, 0 to 1, -1 to 0).map { (dx, dy) -> cx + dx to cy + dy }
                .filter { (x, y) -> x in 0 until cols && y in 0 until rows && !seen[y * cols + x] }
            if (next.isEmpty()) { stack.removeLast(); continue }
            val (nx, ny) = next[r.nextInt(next.size)]
            seen[ny * cols + nx] = true
            open(cx * 2 + 1 + (nx - cx), cy * 2 + 1 + (ny - cy)); open(nx * 2 + 1, ny * 2 + 1)
            stack.addLast(ny * cols + nx)
        }
    }

    private fun open(x: Int, y: Int) { wall[y * w + x] = false }
    fun isWall(x: Int, y: Int) = x !in 0 until w || y !in 0 until h || wall[y * w + x]
}

private const val TILE = 6
private const val COLS = 9
private const val ROWS = 11

@Composable
fun Maze(h: Home, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    var round by remember { mutableIntStateOf(0) }
    val grid = remember(round) { Grid(COLS, ROWS, System.nanoTime()) }
    val bw = grid.w * TILE
    val bh = grid.h * TILE
    val frame = remember { Buf(bw, bh) }
    val bitmap = remember { Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888) }
    val image = remember { bitmap.asImageBitmap() }
    val tilt = remember { FloatArray(2) }
    val pos = remember(round) { floatArrayOf(TILE * 1.5f, TILE * 1.5f, 0f, 0f) }
    var redraw by remember { mutableIntStateOf(0) }
    var won by remember(round) { mutableStateOf(false) }
    var paid by remember { mutableStateOf<Boolean?>(null) }
    val sensor = remember { Device.hasAccelerometer(ctx) }

    DisposableEffect(Unit) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val lis = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) { tilt[0] = -e.values[0]; tilt[1] = e.values[1] }
            override fun onAccuracyChanged(s: Sensor?, a: Int) = Unit
        }
        sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sm.registerListener(lis, it, SensorManager.SENSOR_DELAY_GAME) }
        onDispose { sm.unregisterListener(lis) }
    }
    LaunchedEffect(round) {
        var last = 0L
        while (!won) withFrameMillis { ms ->
            val dt = if (last == 0L) 16f else (ms - last).coerceIn(1, 40).toFloat()
            last = ms
            step(grid, pos, tilt, dt)
            val gx = (pos[0] / TILE).toInt(); val gy = (pos[1] / TILE).toInt()
            if (gx == grid.w - 2 && gy == grid.h - 2 && !won) { won = true; c.write { paid = gameWon() } }
            redraw++
        }
    }

    Page(stringResource(R.string.g_title), nav::close) {
        Tip("maze", stringResource(R.string.tip_maze_t), stringResource(R.string.tip_maze), h.look, h.s.tips)
        Text(stringResource(if (sensor) R.string.g_sub else R.string.g_sub_drag), style = T.Small.copy(color = C.textDim))
        Canvas(Modifier.fillMaxWidth().aspectRatio(bw.toFloat() / bh).pointerInput(Unit) {
            detectDragGestures(onDragEnd = { tilt[0] = 0f; tilt[1] = 0f }) { ch, d -> ch.consume(); tilt[0] = (d.x * .6f).coerceIn(-6f, 6f); tilt[1] = (d.y * .6f).coerceIn(-6f, 6f) }
        }) {
            redraw.let { }
            paint(frame, grid, pos, h.look.body, h.look.accent)
            bitmap.setPixels(frame.d, 0, bw, 0, 0, bw, bh)
            drawImage(image, IntOffset.Zero, IntSize(bw, bh), IntOffset.Zero, IntSize(size.width.roundToInt(), size.height.roundToInt()), filterQuality = FilterQuality.None)
        }
        if (won) {
            Says(stringResource(when (paid) { true -> R.string.g_won_sugar; false -> R.string.g_won; null -> R.string.g_won }), who = h.name)
            PixelButton(stringResource(R.string.g_again), { round++; paid = null }, Modifier.fillMaxWidth())
            PixelButton(stringResource(R.string.close), nav::close, Modifier.fillMaxWidth(), kind = Kind.PALE, small = true)
        }
    }
}

/** Rolls him by the tilt; walls stop him one axis at a time so he slides along them. */
private fun step(g: Grid, p: FloatArray, tilt: FloatArray, dt: Float) {
    val k = dt / 16f
    p[2] = (p[2] + tilt[0] * .045f * k) * .96f
    p[3] = (p[3] + tilt[1] * .045f * k) * .96f
    val r = 2.2f
    fun hits(x: Float, y: Float): Boolean {
        for ((dx, dy) in listOf(-r to -r, r to -r, -r to r, r to r)) if (g.isWall(((x + dx) / TILE).toInt(), ((y + dy) / TILE).toInt())) return true
        return false
    }
    val max = 2.5f
    val vx = p[2].coerceIn(-max, max) * k
    val vy = p[3].coerceIn(-max, max) * k
    if (!hits(p[0] + vx, p[1])) p[0] += vx else p[2] *= -.25f
    if (!hits(p[0], p[1] + vy)) p[1] += vy else p[3] *= -.25f
}

private fun paint(b: Buf, g: Grid, p: FloatArray, body: Int, accent: Int) {
    val board = rgb(0x173A2E)
    val copper = rgb(0xE0954A)
    b.rect(0, 0, b.w, b.h, board)
    for (y in 0 until g.h) for (x in 0 until g.w) if (g.isWall(x, y)) {
        b.rect(x * TILE, y * TILE, TILE, TILE, rgb(0x0E241C))
        if (!g.isWall(x, y + 1)) b.rect(x * TILE, y * TILE + TILE - 1, TILE, 1, mix(copper, board, .4f))
        if (!g.isWall(x, y - 1)) b.rect(x * TILE, y * TILE, TILE, 1, copper)
    }
    // The goal: a lump of sugar in the far corner.
    Catalog.find("sugar_cube")?.let { b.blit(Items.of(it), (g.w - 2) * TILE - 3, (g.h - 2) * TILE - 3) }
    val x = p[0].roundToInt(); val y = p[1].roundToInt()
    val r = ramp(body)
    b.ell(x - 2, y - 3, 1, 1, r.b); b.ell(x + 2, y - 3, 1, 1, r.b)
    b.ell(x, y, 3, 3, r.b); b.ell(x - 1, y - 1, 2, 2, r.l)
    b.p(x - 1, y, rgb(0x1B140E)); b.p(x + 1, y, rgb(0x1B140E)); b.p(x, y + 1, mix(accent, rgb(0xE88A8A), .5f))
}
