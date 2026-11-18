package com.aiwolfie.tiktokautoscroll

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import kotlin.random.Random

class AutoScrollAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE) }
    private var nextScrollAt = 0L
    private var swipePending = false

    private val timerLoop = object : Runnable {
        override fun run() {
            if (!prefs.getBoolean(MainActivity.KEY_ENABLED, false)) {
                nextScrollAt = 0L
                swipePending = false
            } else if (!swipePending) {
                val now = System.currentTimeMillis()
                if (nextScrollAt == 0L) nextScrollAt = now + chooseIntervalMs()
                if (now >= nextScrollAt) {
                    if (isTikTokForeground()) scheduleSwipe()
                    else nextScrollAt = now + 1000L
                }
            }
            handler.postDelayed(this, 300L)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        handler.removeCallbacks(timerLoop)
        nextScrollAt = 0L
        handler.post(timerLoop)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Timer-only operation; no fragile end-of-video label matching.
    }

    private fun isTikTokForeground(): Boolean {
        val root = rootInActiveWindow ?: return false
        val packageName = root.packageName?.toString()
        root.recycle()
        return packageName == "com.zhiliaoapp.musically" ||
            packageName == "com.ss.android.ugc.trill" ||
            packageName == "com.zhiliaoapp.musically.go"
    }

    private fun chooseIntervalMs(): Long {
        return if (prefs.getString(MainActivity.KEY_MODE, MainActivity.FIXED) == MainActivity.RANDOM) {
            val min = prefs.getInt(MainActivity.KEY_RANDOM_MIN_SECONDS, 20).coerceIn(5, 3600)
            val max = prefs.getInt(MainActivity.KEY_RANDOM_MAX_SECONDS, 45).coerceIn(min, 3600)
            Random.nextLong(min.toLong(), max.toLong() + 1L) * 1000L
        } else {
            prefs.getInt(MainActivity.KEY_FIXED_SECONDS, 30).coerceIn(5, 3600) * 1000L
        }
    }

    private fun scheduleSwipe() {
        if (swipePending || !prefs.getBoolean(MainActivity.KEY_ENABLED, false)) return
        swipePending = true
        val minDelay = prefs.getInt(MainActivity.KEY_DELAY_MIN_MS, 400).coerceIn(100, 10000)
        val maxDelay = prefs.getInt(MainActivity.KEY_DELAY_MAX_MS, 1200).coerceIn(minDelay, 10000)
        val delay = Random.nextLong(minDelay.toLong(), maxDelay.toLong() + 1L)

        handler.postDelayed({
            if (prefs.getBoolean(MainActivity.KEY_ENABLED, false) && isTikTokForeground()) {
                performSwipe()
            } else {
                nextScrollAt = System.currentTimeMillis() + 1000L
            }
            swipePending = false
        }, delay)
    }

    private fun performSwipe() {
        val metrics = resources.displayMetrics
        val x = metrics.widthPixels * 0.5f
        val path = Path().apply {
            moveTo(x, metrics.heightPixels * 0.82f)
            lineTo(x, metrics.heightPixels * 0.25f)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 250L))
            .build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                nextScrollAt = System.currentTimeMillis() + chooseIntervalMs()
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                nextScrollAt = System.currentTimeMillis() + chooseIntervalMs()
            }
        }, null)
    }

    override fun onInterrupt() {
        prefs.edit().putBoolean(MainActivity.KEY_ENABLED, false).apply()
        handler.removeCallbacks(timerLoop)
        nextScrollAt = 0L
        swipePending = false
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
