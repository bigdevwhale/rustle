package app.rustle.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import app.rustle.MainActivity
import app.rustle.R
import app.rustle.container
import app.rustle.data.MIN
import app.rustle.data.State
import app.rustle.domain.Nights
import app.rustle.ru
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * Keeps an outing or a night going while the phone lies about. It counts unlocked time: too much of it
 * and he turns back from an outing; for a night, the first unlock in the morning wakes him up.
 */
class Outing : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loop: Job? = null
    private var screen: BroadcastReceiver? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        channels(this)
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, ONGOING, placeholder(), type)
        if (screen == null) {
            screen = object : BroadcastReceiver() {
                override fun onReceive(c: Context, i: Intent) { scope.launch { onScreen(i.action ?: return@launch) } }
            }
            val f = IntentFilter().apply { addAction(Intent.ACTION_USER_PRESENT); addAction(Intent.ACTION_SCREEN_OFF) }
            ContextCompat.registerReceiver(this, screen, f, ContextCompat.RECEIVER_NOT_EXPORTED)
        }
        if (loop?.isActive != true) loop = scope.launch {
            while (isActive) { if (!tick()) break; delay(20_000) }
            stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        screen?.let { runCatching { unregisterReceiver(it) } }
        scope.cancel()
        super.onDestroy()
    }

    private val store get() = container.store

    private suspend fun onScreen(action: String) {
        val now = System.currentTimeMillis()
        var ended: State? = null
        store.update { s ->
            val t = s.trip
            val n = s.night
            val next = when {
                t != null && t.ended == 0L -> when (action) {
                    Intent.ACTION_USER_PRESENT ->
                        // Free walks end when the phone is picked up (after five minutes, so a slip of the hand doesn't count).
                        if (t.minutes == 0 && now - t.start >= 5 * MIN) s.copy(trip = t.copy(ended = now))
                        else s.copy(trip = t.copy(unlockAt = if (t.unlockAt == 0L) now else t.unlockAt))
                    else -> if (t.unlockAt > 0) s.copy(trip = t.copy(peekMs = t.peekMs + (now - t.unlockAt), unlockAt = 0)) else s
                }
                n != null && n.ended == 0L -> when (action) {
                    Intent.ACTION_USER_PRESENT ->
                        if (now - n.start >= Nights.MIN_SLEEP && Nights.hourOf(now) in Nights.WAKE_HOUR..13) s.copy(night = n.copy(ended = now))
                        else s.copy(night = n.copy(unlockAt = if (n.unlockAt == 0L) now else n.unlockAt))
                    else -> if (n.unlockAt > 0) s.copy(night = n.copy(peekMs = n.peekMs + (now - n.unlockAt), unlockAt = 0)) else s
                }
                else -> s
            }
            if ((next.trip?.ended ?: 0) > 0 && (t?.ended ?: 0) == 0L || (next.night?.ended ?: 0) > 0 && (n?.ended ?: 0) == 0L) ended = next
            next
        }
        ended?.let { finished(it); stopSelf() } ?: tick()
    }

    /** Updates the notification and ends what is due. False when there is nothing left to watch. */
    private suspend fun tick(): Boolean {
        val now = System.currentTimeMillis()
        var ended = false
        store.update { s ->
            val t = s.trip
            val n = s.night
            when {
                t != null && t.ended == 0L -> {
                    val peek = t.peekMs + if (t.unlockAt > 0) now - t.unlockAt else 0
                    when {
                        t.minutes > 0 && now >= t.due -> { ended = true; s.copy(trip = t.copy(ended = t.due)) }
                        t.minutes > 0 && peek > t.allowance -> { ended = true; s.copy(trip = t.copy(ended = now, cut = true, peekMs = peek, unlockAt = 0)) }
                        else -> s
                    }
                }
                // A night nobody woke up from ends by itself in the afternoon.
                n != null && n.ended == 0L && now - n.start > 16 * 60 * MIN -> { ended = true; s.copy(night = n.copy(ended = now)) }
                else -> s
            }
        }
        val s = store.now()
        if (ended) { finished(s); return false }
        val t = s.trip?.takeIf { it.ended == 0L }
        val n = s.night?.takeIf { it.ended == 0L }
        if (t == null && n == null) return false
        nm().notify(ONGOING, ongoing(s))
        return true
    }

    private fun nm() = getSystemService(NotificationManager::class.java)

    private fun open(): PendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun placeholder(): Notification = NotificationCompat.Builder(this, CH_ONGOING).setSmallIcon(R.drawable.ic_stat).setContentTitle(getString(R.string.app_name))
        .setOngoing(true).setSilent(true).setContentIntent(open()).build()

    private fun ongoing(s: State): Notification {
        val ru = ru
        val name = s.name.ifBlank { getString(R.string.app_name) }
        val b = NotificationCompat.Builder(this, CH_ONGOING).setSmallIcon(R.drawable.ic_stat).setOngoing(true).setSilent(true).setOnlyAlertOnce(true)
            .setContentIntent(open()).setCategory(NotificationCompat.CATEGORY_PROGRESS)
        val t = s.trip
        if (t != null) {
            val where = t.district.name_.of(ru)
            b.setContentTitle(getString(R.string.n_away, name, where))
            if (t.minutes > 0) {
                val total = t.minutes * 60
                val done = ((System.currentTimeMillis() - t.start) / 1000).toInt().coerceIn(0, total)
                b.setProgress(total, done, false).setUsesChronometer(true).setChronometerCountDown(true).setWhen(t.due).setShowWhen(true)
                b.setContentText(getString(R.string.n_back_at, DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(t.due))))
                b.setShortCriticalText(getString(R.string.n_short, ((t.due - System.currentTimeMillis()) / MIN + 1).coerceAtLeast(1)))
            } else {
                b.setContentText(getString(R.string.n_free)).setUsesChronometer(true).setWhen(t.start).setShowWhen(true)
            }
        } else {
            b.setContentTitle(getString(R.string.n_sleeping, name)).setContentText(getString(R.string.n_sleeping_b))
        }
        // Android 16 lifts an ongoing progress notification into a status bar chip when asked.
        b.extras.putBoolean("android.requestPromotedOngoing", true)
        return b.build()
    }

    private fun finished(s: State) {
        nm().cancel(ONGOING)
        val name = s.name.ifBlank { getString(R.string.app_name) }
        val t = s.trip?.takeIf { it.ended > 0 }
        val (title, text) = when {
            t != null -> if (t.cut) getString(R.string.n_cut, name) to getString(R.string.n_cut_b)
                else getString(R.string.n_home, name) to getString(R.string.n_home_b)
            else -> getString(R.string.n_woke, name) to getString(R.string.n_woke_b)
        }
        nm().notify(BACK, NotificationCompat.Builder(this, CH_BACK).setSmallIcon(R.drawable.ic_stat).setContentTitle(title).setContentText(text)
            .setAutoCancel(true).setContentIntent(open()).build())
    }

    companion object {
        const val CH_ONGOING = "away"
        const val CH_BACK = "back"
        const val ONGOING = 7
        const val BACK = 8

        fun channels(ctx: Context) {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CH_ONGOING, ctx.getString(R.string.ch_away), NotificationManager.IMPORTANCE_LOW))
            nm.createNotificationChannel(NotificationChannel(CH_BACK, ctx.getString(R.string.ch_back), NotificationManager.IMPORTANCE_DEFAULT))
        }

        fun start(ctx: Context) = runCatching { ContextCompat.startForegroundService(ctx, Intent(ctx, Outing::class.java)) }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, Outing::class.java))
            ctx.getSystemService(NotificationManager::class.java).cancel(ONGOING)
        }

        fun clearBack(ctx: Context) = ctx.getSystemService(NotificationManager::class.java).cancel(BACK)

        fun interactive(ctx: Context) = (ctx.getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive
    }
}
