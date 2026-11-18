package com.aiwolfie.tiktokautoscroll

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }
    private lateinit var mode: RadioGroup
    private lateinit var fixed: EditText
    private lateinit var randomMin: EditText
    private lateinit var randomMax: EditText
    private lateinit var delayMin: EditText
    private lateinit var delayMax: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val page = ScrollView(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
        }
        page.addView(box)
        box.addView(TextView(this).apply {
            text = "TikTok AutoScroll"
            textSize = 27f
            gravity = Gravity.CENTER_VERTICAL
        })
        box.addView(TextView(this).apply {
            text = "Fixed Timer + Random Timer"
            textSize = 14f
            setPadding(0, 5, 0, 18)
        })

        box.addView(label("Pilih mode"))
        mode = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        mode.addView(RadioButton(this).apply { id = MODE_FIXED; text = "Opsi 1 — Fixed Timer" })
        mode.addView(RadioButton(this).apply { id = MODE_RANDOM; text = "Opsi 2 — Random Timer" })
        box.addView(mode)

        box.addView(label("Fixed Timer — interval (detik)"))
        fixed = numberField("30"); box.addView(fixed)
        box.addView(label("Random Timer — minimum interval (detik)"))
        randomMin = numberField("20"); box.addView(randomMin)
        box.addView(label("Random Timer — maksimum interval (detik)"))
        randomMax = numberField("45"); box.addView(randomMax)
        box.addView(label("Jeda acak sebelum swipe minimum (ms)"))
        delayMin = numberField("400"); box.addView(delayMin)
        box.addView(label("Jeda acak sebelum swipe maksimum (ms)"))
        delayMax = numberField("1200"); box.addView(delayMax)

        box.addView(TextView(this).apply {
            text = "Random Timer memilih interval baru setelah setiap swipe. Swipe hanya berjalan saat TikTok berada di layar depan."
            textSize = 12f
            setPadding(0, 10, 0, 14)
        })
        box.addView(Button(this).apply {
            text = "1. Aktifkan Accessibility Service"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        box.addView(Button(this).apply {
            text = "2. Simpan Pengaturan"
            setOnClickListener { saveSettings() }
        })
        box.addView(Button(this).apply {
            text = "START"
            setOnClickListener {
                saveSettings()
                prefs.edit().putBoolean(KEY_ENABLED, true).apply()
                status.text = "Status: AKTIF — buka TikTok. Tekan STOP untuk berhenti."
            }
        })
        box.addView(Button(this).apply {
            text = "STOP"
            setOnClickListener {
                prefs.edit().putBoolean(KEY_ENABLED, false).apply()
                status.text = "Status: berhenti"
            }
        })
        status = TextView(this).apply {
            textSize = 16f
            setPadding(0, 14, 0, 12)
        }
        box.addView(status)
        box.addView(TextView(this).apply {
            text = "Auto Detect akhir video dihapus. Accessibility Service hanya memeriksa aplikasi foreground untuk membatasi swipe ke TikTok. Tidak ada data yang dikirim ke server."
            textSize = 12f
        })
        setContentView(page)
        loadSettings()
    }

    private fun label(value: String) = TextView(this).apply {
        text = value
        textSize = 14f
        setPadding(0, 12, 0, 4)
    }

    private fun numberField(value: String) = EditText(this).apply {
        setSingleLine(true)
        inputType = InputType.TYPE_CLASS_NUMBER
        setText(value)
    }

    private fun loadSettings() {
        mode.check(if (prefs.getString(KEY_MODE, FIXED) == RANDOM) MODE_RANDOM else MODE_FIXED)
        fixed.setText(prefs.getInt(KEY_FIXED_SECONDS, 30).toString())
        randomMin.setText(prefs.getInt(KEY_RANDOM_MIN_SECONDS, 20).toString())
        randomMax.setText(prefs.getInt(KEY_RANDOM_MAX_SECONDS, 45).toString())
        delayMin.setText(prefs.getInt(KEY_DELAY_MIN_MS, 400).toString())
        delayMax.setText(prefs.getInt(KEY_DELAY_MAX_MS, 1200).toString())
        status.text = if (prefs.getBoolean(KEY_ENABLED, false)) "Status: AKTIF" else "Status: berhenti"
    }

    private fun saveSettings() {
        val fixedValue = fixed.text.toString().toIntOrNull()?.coerceIn(5, 3600) ?: 30
        val minSeconds = randomMin.text.toString().toIntOrNull()?.coerceIn(5, 3600) ?: 20
        val maxSeconds = maxOf(minSeconds, randomMax.text.toString().toIntOrNull()?.coerceIn(5, 3600) ?: 45)
        val minMs = delayMin.text.toString().toIntOrNull()?.coerceIn(100, 10000) ?: 400
        val maxMs = maxOf(minMs, delayMax.text.toString().toIntOrNull()?.coerceIn(100, 10000) ?: 1200)
        val selectedMode = if (mode.checkedRadioButtonId == MODE_RANDOM) RANDOM else FIXED

        prefs.edit()
            .putString(KEY_MODE, selectedMode)
            .putInt(KEY_FIXED_SECONDS, fixedValue)
            .putInt(KEY_RANDOM_MIN_SECONDS, minSeconds)
            .putInt(KEY_RANDOM_MAX_SECONDS, maxSeconds)
            .putInt(KEY_DELAY_MIN_MS, minMs)
            .putInt(KEY_DELAY_MAX_MS, maxMs)
            .apply()

        fixed.setText(fixedValue.toString())
        randomMin.setText(minSeconds.toString())
        randomMax.setText(maxSeconds.toString())
        delayMin.setText(minMs.toString())
        delayMax.setText(maxMs.toString())
        Toast.makeText(this, "Pengaturan disimpan", Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val PREFS = "autoscroll_settings"
        const val KEY_ENABLED = "enabled"
        const val KEY_MODE = "mode"
        const val KEY_FIXED_SECONDS = "fixed_seconds"
        const val KEY_RANDOM_MIN_SECONDS = "random_min_seconds"
        const val KEY_RANDOM_MAX_SECONDS = "random_max_seconds"
        const val KEY_DELAY_MIN_MS = "min_delay_ms"
        const val KEY_DELAY_MAX_MS = "max_delay_ms"
        const val FIXED = "fixed"
        const val RANDOM = "random"
        const val MODE_FIXED = 2101
        const val MODE_RANDOM = 2102
    }
}
