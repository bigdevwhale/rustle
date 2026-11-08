package app.rustle.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import app.rustle.MainActivity
import app.rustle.R
import app.rustle.container
import app.rustle.data.State
import app.rustle.device.Device
import app.rustle.domain.Nights
import app.rustle.domain.Pose
import app.rustle.ru
import app.rustle.ui.Home
import app.rustle.ui.art.Buf
import app.rustle.ui.art.Icons
import app.rustle.ui.art.Nook
import app.rustle.ui.art.NookScene
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * The home-screen widget: his corner of the nook as a still pixel picture, how fed and how cheerful he
 * is, where he is, and buttons to stroke him, feed him and send him out (or put him to bed, or wake him).
 * Redrawn whenever the household changes, and every half an hour for the clock and the hunger.
 */
class Widget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        scope.launch { try { render(context) } finally { pending.finish() } }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != null && action.startsWith(PREFIX)) {
            val pending = goAsync()
            scope.launch {
                try { act(context, action.removePrefix(PREFIX)) } finally { pending.finish() }
            }
            return
        }
        super.onReceive(context, intent)
    }

    private suspend fun act(ctx: Context, what: String) {
        val c = ctx.container
        val s = c.store.now()
        val h = Home(s, System.currentTimeMillis(), ctx.ru)
        when (what) {
            PET -> if (h.away == null && h.asleep == null && h.surfing == null) c.pet()
            FEED -> {
                val f = h.food.firstOrNull()
                if (f == null) toast(ctx, ctx.getString(R.string.wd_no_food)) else { c.feed(f); toast(ctx, ctx.getString(R.string.wd_fed, f.name.of(ctx.ru))) }
            }
            SEND -> if (h.away == null && h.asleep == null && h.surfing == null) {
                val last = s.trips.firstOrNull()
                val d = last?.district?.takeIf { it in h.open } ?: h.open.last()
                val m = last?.minutes?.takeIf { it in listOf(15, 25, 45, 60, 90) } ?: 25
                c.startTrip(d, m)
                toast(ctx, ctx.getString(R.string.wd_sent, d.name_.of(ctx.ru)))
            }
            HOME -> c.callHome()
            BED -> c.toBed(Device.battery(ctx).charging)
            WAKE -> c.wake()
        }
        render(ctx)
    }

    private suspend fun toast(ctx: Context, text: String) = kotlinx.coroutines.withContext(Dispatchers.Main) { Toast.makeText(ctx, text, Toast.LENGTH_SHORT).show() }

    companion object {
        private const val PREFIX = "app.rustle.widget."
        private const val PET = "pet"
        private const val FEED = "feed"
        private const val SEND = "send"
        private const val HOME = "home"
        private const val BED = "bed"
        private const val WAKE = "wake"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        /** Redraws every placed widget; cheap to call often. */
        fun refresh(ctx: Context) {
            val ids = AppWidgetManager.getInstance(ctx).getAppWidgetIds(ComponentName(ctx, Widget::class.java))
            if (ids.isNotEmpty()) scope.launch { runCatching { render(ctx) } }
        }

        private fun pending(ctx: Context, what: String): PendingIntent =
            PendingIntent.getBroadcast(ctx, what.hashCode(), Intent(ctx, Widget::class.java).setAction(PREFIX + what), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        suspend fun render(ctx: Context) {
            val manager = AppWidgetManager.getInstance(ctx)
            val ids = manager.getAppWidgetIds(ComponentName(ctx, Widget::class.java))
            if (ids.isEmpty()) return
            val s = ctx.container.store.now()
            val views = views(ctx, s)
            manager.updateAppWidget(ids, views)
        }

        private fun views(ctx: Context, s: State): RemoteViews {
            val now = System.currentTimeMillis()
            val ru = ctx.ru
            val h = Home(s, now, ru)
            val phone = Device.battery(ctx)
            val v = RemoteViews(ctx.packageName, R.layout.widget)
            val open = PendingIntent.getActivity(ctx, 1, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            v.setOnClickPendingIntent(R.id.w_root, open)
            if (!s.onboarded) {
                v.setTextViewText(R.id.w_name, ctx.getString(R.string.app_name))
                v.setTextViewText(R.id.w_status, ctx.getString(R.string.wd_move_in))
                v.setViewVisibility(R.id.w_buttons, View.GONE)
                return v
            }
            val pose = h.pose(phone.copy(lux = null))
            v.setImageViewBitmap(R.id.w_scene, scene(h, pose, phone.level, phone.charging))
            v.setImageViewBitmap(R.id.w_pips, pips(h))
            v.setTextViewText(R.id.w_name, h.name)
            val away = h.away
            val asleep = h.asleep
            val time = DateFormat.getTimeInstance(DateFormat.SHORT)
            v.setTextViewText(R.id.w_status, when {
                away != null && away.minutes > 0 -> ctx.getString(R.string.wd_away, away.district.name_.of(ru), time.format(Date(away.due)))
                away != null -> ctx.getString(R.string.wd_away_free, away.district.name_.of(ru))
                asleep != null -> ctx.getString(R.string.wd_asleep, time.format(Date(asleep.start)))
                pose == Pose.COLD -> ctx.getString(R.string.wd_cold)
                pose == Pose.HUNGRY -> ctx.getString(R.string.wd_hungry)
                phone.charging -> ctx.getString(R.string.wd_warm)
                else -> ctx.getString(R.string.wd_home)
            })
            v.setViewVisibility(R.id.w_buttons, View.VISIBLE)
            v.setOnClickPendingIntent(R.id.w_scene, if (away == null && asleep == null) pending(ctx, PET) else open)
            fun button(id: Int, text: Int?, what: String?) {
                if (text == null || what == null) { v.setViewVisibility(id, View.GONE); return }
                v.setViewVisibility(id, View.VISIBLE)
                v.setTextViewText(id, ctx.getString(text))
                v.setOnClickPendingIntent(id, pending(ctx, what))
            }
            when {
                away != null -> { button(R.id.w_b1, R.string.wd_call, HOME); button(R.id.w_b2, null, null); button(R.id.w_b3, null, null) }
                asleep != null -> { button(R.id.w_b1, R.string.h_morning, WAKE); button(R.id.w_b2, null, null); button(R.id.w_b3, null, null) }
                else -> {
                    button(R.id.w_b1, R.string.h_feed, FEED)
                    button(R.id.w_b2, R.string.wd_send, SEND)
                    button(R.id.w_b3, if (Nights.bedtime(now)) R.string.h_bed else R.string.wd_pet, if (Nights.bedtime(now)) BED else PET)
                }
            }
            return v
        }

        /** His corner of the nook, cut around him (or around the hatch when he is out). */
        private fun scene(h: Home, pose: Pose, level: Int, charging: Boolean): Bitmap {
            val full = Buf(Nook.W, Nook.H)
            val ns = NookScene(h.look, pose, level, charging, h.hour, false, false, false, h.placed, h.best, h.days, wx = h.wx)
            val tick = 120
            Nook.draw(full, ns, tick)
            val spot = Nook.spot(ns, tick)
            val w = 112; val hgt = 96
            val cx = if (spot < 0) 96 else spot + 24
            val x0 = (cx - w / 2).coerceIn(0, Nook.W - w)
            val y0 = (if (pose == Pose.SLEEP && "bed" in h.s.built) Nook.FLOOR - 76 else Nook.FLOOR - 70).coerceIn(0, Nook.H - hgt)
            val crop = Buf(w, hgt)
            for (y in 0 until hgt) System.arraycopy(full.d, (y0 + y) * Nook.W + x0, crop.d, y * w, w)
            return crop.scaled(4)
        }

        /** Five crumbs and five hearts in a row. */
        private fun pips(h: Home): Bitmap {
            val b = Buf(24 * 10 + 12, 24)
            val f = ((h.full + 10) / 20).toInt().coerceIn(0, 5)
            val m = ((h.mood + 10) / 20).toInt().coerceIn(0, 5)
            for (i in 0 until 5) b.blit(Icons.crumb(i < f), i * 24, 0)
            for (i in 0 until 5) b.blit(Icons.heart(i < m), 132 + i * 24, 0)
            return b.scaled(2)
        }

        private fun Buf.scaled(k: Int): Bitmap {
            val out = IntArray(w * k * h * k)
            for (y in 0 until h * k) for (x in 0 until w * k) out[y * w * k + x] = d[(y / k) * w + x / k]
            return Bitmap.createBitmap(out, w * k, h * k, Bitmap.Config.ARGB_8888)
        }

    }
}
