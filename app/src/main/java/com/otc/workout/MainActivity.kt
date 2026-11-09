package com.otc.workout

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.graphics.Typeface
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.InputType
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Exercise(val name: String, val target: String, val restSeconds: Int, val cue: String)
data class SetRecord(val id: Long, val day: Int, val exercise: String, val value: String, val timestamp: Long)

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("otc_data", Context.MODE_PRIVATE) }
    private val handler = Handler(Looper.getMainLooper())
    private var timerSeconds = 0
    private var timerActive = false
    private var timerPaused = false
    private var timerRunnable: Runnable? = null
    private var timerPanel: LinearLayout? = null
    private var timerLabel: TextView? = null
    private var pauseButton: TextView? = null
    private lateinit var content: LinearLayout
    private var selectedDay = 1
    private var currentScreen = "home"

    private val workouts = mapOf(
        1 to listOf(
            Exercise("Acceleration", "6 × 10 m + 4 × 20 m", 180, "Run fast with full recovery between efforts."),
            Exercise("Bounds", "3 × 5 / side", 90, "Explode forward; land quietly and under control."),
            Exercise("Pistol squat", "4 × 6 / leg", 120, "Use assistance if needed. Record the reps you actually complete."),
            Exercise("Split squat", "3 × 8 / leg", 90, "Keep the front foot planted and torso controlled."),
            Exercise("Calf raise", "3 × 15", 60, "Pause briefly at the top and lower slowly.")
        ),
        2 to listOf(
            Exercise("Pull-up", "5 × 6–8", 120, "Start from a controlled hang; avoid swinging."),
            Exercise("Explosive pull-up", "4 × 3", 150, "Pull fast while keeping every rep clean."),
            Exercise("Dips", "4 × 8–12", 120, "Use a comfortable shoulder range."),
            Exercise("Advanced tuck front lever", "4 × 8–12 sec", 120, "Brace the trunk and keep the hold consistent."),
            Exercise("L-sit progression", "5 × 15 sec", 90, "Press down through the bars and keep shoulders active.")
        ),
        4 to listOf(
            Exercise("5-10-5 shuttle", "5 reps", 120, "Change direction with low hips and controlled foot placement."),
            Exercise("Lateral acceleration", "4 × 5 m / side", 120, "Push hard from the outside leg."),
            Exercise("Nordic hamstring curl", "3 × 5", 150, "Lower slowly; use assistance to maintain control."),
            Exercise("Copenhagen plank", "3 × 25 sec / side", 90, "Keep hips lifted and trunk aligned."),
            Exercise("Assisted dragon squat", "3 × 4 / side", 120, "Use support and stay within a pain-free range.")
        ),
        5 to listOf(
            Exercise("Decline / pseudo-planche push-up", "4 × 8–12", 120, "Keep the body rigid and elbows controlled."),
            Exercise("Pike push-up", "4 × 8–10", 120, "Move the head forward and down between the hands."),
            Exercise("Wall handstand", "5 × 30–45 sec", 90, "Push tall through the shoulders; stop before form breaks."),
            Exercise("Dips", "3 × 10–15", 120, "Progress the variation only when reps are controlled."),
            Exercise("L-sit", "5 × 15–20 sec", 90, "Keep shoulders depressed and legs active.")
        )
    )

    private val recovery = listOf(
        Exercise("Easy walk or gentle movement", "5–8 min", 0, "Keep the effort easy; this is recovery, not conditioning."),
        Exercise("Ankle mobility", "2 × 10 / side", 20, "Move smoothly through a comfortable range."),
        Exercise("Hip flexor stretch", "2 × 30 sec / side", 20, "Squeeze the glute gently; do not force the stretch."),
        Exercise("Adductor rock-back", "2 × 10", 20, "Move slowly and keep the spine comfortable."),
        Exercise("Hamstring stretch", "2 × 30 sec / side", 20, "Use gentle tension, never sharp pain."),
        Exercise("Thoracic rotations", "2 × 8 / side", 20, "Rotate through the upper back."),
        Exercise("Slow breathing", "2 min", 0, "Breathe calmly through the nose if comfortable.")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedDay = prefs.getInt("next_day", 1)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(12, 13, 17))
            setPadding(20, 18, 20, 14)
        }
        root.addView(TextView(this).apply {
            text = "OTC"; setTextColor(Color.WHITE); textSize = 28f
            typeface = Typeface.DEFAULT_BOLD; setPadding(4, 10, 4, 2)
        })
        root.addView(TextView(this).apply {
            text = "OUTWORK THE CHALLENGE"; setTextColor(Color.rgb(190, 55, 70))
            textSize = 10f; letterSpacing = 0.18f; setPadding(5, 0, 4, 12)
        })
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        nav.addView(navButton("HOME") { showHome() }, navParams())
        nav.addView(navButton("WORKOUT") { showWorkout(selectedDay) }, navParams())
        nav.addView(navButton("HISTORY") { showHistory() }, navParams())
        nav.addView(navButton("LEVEL TEST") { showTest() }, navParams())
        root.addView(nav)
        val scroll = ScrollView(this)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 14, 0, 28)
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        showHome()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (currentScreen != "home") showHome() else super.onBackPressed()
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data?.data == null) return
        val uri = data.data!!
        try {
            when (requestCode) {
                4101 -> exportBackup(uri)
                4102 -> importBackup(uri)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Backup operation failed: ${e.message ?: "unknown error"}", Toast.LENGTH_LONG).show()
        }
    }

    private fun startBackupExport() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "OTC_backup_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())}.json")
        }
        startActivityForResult(intent, 4101)
    }

    private fun exportBackup(uri: Uri) {
        val root = JSONObject()
        root.put("format", "otc-backup-v1")
        root.put("createdAt", System.currentTimeMillis())
        val saved = JSONObject()
        prefs.all.forEach { (key, value) ->
            val entry = JSONObject()
            when (value) {
                is String -> { entry.put("type", "string"); entry.put("value", value) }
                is Int -> { entry.put("type", "int"); entry.put("value", value) }
                is Long -> { entry.put("type", "long"); entry.put("value", value) }
                is Float -> { entry.put("type", "float"); entry.put("value", value.toDouble()) }
                is Boolean -> { entry.put("type", "boolean"); entry.put("value", value) }
                is Set<*> -> {
                    entry.put("type", "string_set")
                    val values = JSONArray()
                    value.filterIsInstance<String>().forEach { values.put(it) }
                    entry.put("value", values)
                }
            }
            saved.put(key, entry)
        }
        root.put("preferences", saved)
        contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(root.toString(2)) }
            ?: throw IllegalStateException("Could not open the selected file.")
        Toast.makeText(this, "Backup exported successfully.", Toast.LENGTH_LONG).show()
    }

    private fun startBackupImport() {
        AlertDialog.Builder(this)
            .setTitle("Restore OTC backup?")
            .setMessage("Restoring replaces the app's current saved data with the backup. Export a backup of your current data first if you want to keep it.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Choose backup") { _, _ ->
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                }
                startActivityForResult(intent, 4102)
            }.show()
    }

    private fun importBackup(uri: Uri) {
        val raw = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: throw IllegalStateException("Could not read the selected file.")
        val root = JSONObject(raw)
        require(root.optString("format") == "otc-backup-v1") { "This is not a valid OTC backup." }
        val saved = root.optJSONObject("preferences")
            ?: throw IllegalArgumentException("Backup data is missing.")
        val editor = prefs.edit().clear()
        val keys = saved.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val entry = saved.optJSONObject(key) ?: continue
            val value = entry.opt("value")
            when (entry.optString("type")) {
                "string" -> editor.putString(key, value as? String ?: "")
                "int" -> editor.putInt(key, (value as? Number)?.toInt() ?: 0)
                "long" -> editor.putLong(key, (value as? Number)?.toLong() ?: 0L)
                "float" -> editor.putFloat(key, (value as? Number)?.toFloat() ?: 0f)
                "boolean" -> editor.putBoolean(key, value as? Boolean ?: false)
                "string_set" -> {
                    val set = mutableSetOf<String>()
                    (value as? JSONArray)?.let { array ->
                        for (i in 0 until array.length()) set.add(array.optString(i))
                    }
                    editor.putStringSet(key, set)
                }
            }
        }
        editor.commit()
        selectedDay = prefs.getInt("next_day", 1).coerceIn(1, 7)
        showHome()
        Toast.makeText(this, "Backup restored successfully.", Toast.LENGTH_LONG).show()
    }

    private fun weightParams() = LinearLayout.LayoutParams(0, 44, 1f).apply { setMargins(2, 4, 2, 4) }
    private fun navParams() = LinearLayout.LayoutParams(0, 54, 1f).apply {
        setMargins(3, 5, 3, 5)
    }
    private fun navButton(label: String, action: () -> Unit) = TextView(this).apply {
        text = label; textSize = 11f
        typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        isClickable = true; isFocusable = true
        setPadding(2, 0, 2, 0)
        background = GradientDrawable().apply {
            setColor(Color.rgb(39, 41, 51))
            cornerRadius = 12f
            setStroke(1, Color.rgb(105, 47, 60))
        }
        setOnClickListener { action() }
    }
    private fun heading(value: String) {
        content.addView(TextView(this).apply {
            text = value; textSize = 24f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE); setPadding(0, 16, 0, 10)
        })
    }
    private fun body(value: String, size: Float = 15f) {
        content.addView(TextView(this).apply {
            text = value; textSize = size; setTextColor(Color.rgb(205, 208, 218))
            setPadding(0, 5, 0, 7)
        })
    }
    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(14, 12, 14, 12)
        background = GradientDrawable().apply {
            setColor(Color.rgb(29, 31, 40)); cornerRadius = 18f
            setStroke(1, Color.rgb(55, 48, 57))
        }
        content.addView(this, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 7, 0, 7) })
    }
    private fun actionButton(label: String, action: () -> Unit) {
        content.addView(Button(this).apply {
            text = label; isAllCaps = false; textSize = 14f; setTextColor(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(Color.rgb(133, 28, 45))
            setOnClickListener { action() }
        }, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 6, 0, 6) })
    }

    private fun controlButton(
        label: String,
        height: Int = 52,
        backgroundColor: Int = Color.rgb(91, 42, 54),
        action: () -> Unit
    ) = TextView(this).apply {
        text = label
        textSize = 13f
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        includeFontPadding = false
        minHeight = height
        isClickable = true
        isFocusable = true
        setPadding(10, 0, 10, 0)
        background = GradientDrawable().apply {
            setColor(backgroundColor)
            cornerRadius = 12f
            setStroke(1, Color.rgb(130, 58, 72))
        }
        setOnClickListener { action() }
    }

    private fun loadRecords(): MutableList<SetRecord> {
        val result = mutableListOf<SetRecord>()
        val array = JSONArray(prefs.getString("set_history_json", "[]"))
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            result.add(SetRecord(
                item.optLong("id"), item.optInt("day"), item.optString("exercise"),
                item.optString("value"), item.optLong("timestamp")
            ))
        }
        return result
    }

    private fun saveRecords(records: List<SetRecord>) {
        val array = JSONArray()
        records.forEach { record ->
            array.put(JSONObject().apply {
                put("id", record.id); put("day", record.day); put("exercise", record.exercise)
                put("value", record.value); put("timestamp", record.timestamp)
            })
        }
        prefs.edit().putString("set_history_json", array.toString()).apply()
    }

    private fun addRecord(day: Int, exercise: String, value: String): Int {
        val records = loadRecords()
        records.add(SetRecord(System.currentTimeMillis(), day, exercise, value, System.currentTimeMillis()))
        saveRecords(records)
        prefs.edit().putString("last_$exercise", value).apply()
        return records.count { it.day == day && it.exercise == exercise }
    }

    private fun showHome() {
        currentScreen = "home"; content.removeAllViews()
        heading("LEVEL 02")
        body("FOOTBALL-FIRST CALISTHENICS", 11f)
        val stats = card()
        stats.addView(TextView(this).apply {
            text = "YOUR TRAINING STATUS"; setTextColor(Color.rgb(204, 70, 83))
            textSize = 11f; letterSpacing = 0.12f; typeface = Typeface.DEFAULT_BOLD
        })
        stats.addView(TextView(this).apply {
            text = "${prefs.getInt("workouts_done", 0)}"; setTextColor(Color.WHITE)
            textSize = 34f; typeface = Typeface.DEFAULT_BOLD; setPadding(0, 8, 0, 0)
        })
        stats.addView(TextView(this).apply {
            text = "WORKOUTS COMPLETED"; setTextColor(Color.rgb(180, 185, 198)); textSize = 11f
        })
        stats.addView(TextView(this).apply {
            text = "STREAK  ·  ${prefs.getInt("streak", 0)} days       NEXT  ·  Day $selectedDay"
            setTextColor(Color.WHITE); textSize = 13f; setPadding(0, 14, 0, 2)
        })
        heading("YOUR WEEK")
        for (day in 1..7) {
            val label = when (day) {
                3 -> "Day 3 · Recovery"; 6 -> "Day 6 · Mobility"; 7 -> "Day 7 · Full recovery"
                else -> "Day $day · Training"
            }
            actionButton(label) { showWorkout(day) }
        }
        body("Keep the sequence. If you miss a day, continue when ready—never double up to catch up.", 13f)
        attachTimerPanelIfNeeded()
    }

    private fun showWorkout(day: Int) {
        currentScreen = "workout"; selectedDay = day; content.removeAllViews()
        heading("DAY $day")
        if (day == 3 || day == 6 || day == 7) {
            body(if (day == 7) "Full recovery day" else "Guided recovery and mobility")
            recovery.forEach { exerciseCard(it, day) }
        } else {
            body(when (day) {
                1 -> "Speed + power"; 2 -> "Pull + muscle-up preparation"
                4 -> "Agility + posterior chain"; else -> "Push + handstand"
            })
            workouts[day]?.forEach { exerciseCard(it, day) }
        }
        actionButton("Complete day and continue") {
            val done = prefs.getInt("workouts_done", 0) + 1
            val streak = prefs.getInt("streak", 0) + 1
            val next = if (day == 7) 1 else day + 1
            prefs.edit().putInt("workouts_done", done).putInt("streak", streak).putInt("next_day", next).apply()
            selectedDay = next
            Toast.makeText(this, "Day saved. Next: Day $next", Toast.LENGTH_LONG).show()
            showHome()
        }
        attachTimerPanelIfNeeded()
    }

    private fun exerciseCard(ex: Exercise, day: Int) {
        val c = card()
        c.addView(TextView(this).apply {
            text = ex.name; textSize = 17f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE)
        })
        c.addView(TextView(this).apply {
            text = "Target: ${ex.target}    •    Rest: " + if (ex.restSeconds == 0) "No timer" else "${ex.restSeconds}s"
            setTextColor(Color.rgb(180, 185, 198)); textSize = 13f; setPadding(0, 5, 0, 5)
        })
        c.addView(TextView(this).apply { text = ex.cue; setTextColor(Color.LTGRAY); textSize = 13f })
        val input = EditText(this).apply {
            hint = "Actual reps / seconds / result"; setSingleLine(true)
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY)
            backgroundTintList = ColorStateList.valueOf(Color.rgb(190, 55, 70))
            setPadding(10, 4, 10, 4)
        }
        c.addView(input)
        val legacyPrefix = if (day == 3 || day == 6 || day == 7) "recovery_$day" else "day_$day"
        val legacyCount = prefs.getInt("${legacyPrefix}_${ex.name}_sets", 0)
        val logged = TextView(this).apply {
            text = "Sets logged: ${legacyCount + loadRecords().count { it.day == day && it.exercise == ex.name }}"
            setTextColor(Color.rgb(210, 210, 220))
        }
        c.addView(logged)
        c.addView(controlButton("LOG COMPLETED SET", 54, Color.rgb(133, 28, 45)) {
            val value = input.text.toString().trim()
            if (value.isEmpty()) {
                Toast.makeText(this@MainActivity, "Enter what you actually completed.", Toast.LENGTH_SHORT).show()
            } else {
                val count = addRecord(day, ex.name, value)
                logged.text = "Sets logged: ${legacyCount + count} · Last: $value"
                input.text.clear()
                if (ex.restSeconds > 0) startRest(ex.restSeconds)
            }
        }, LinearLayout.LayoutParams(-1, 54).apply { setMargins(0, 6, 0, 6) })
    }

    private fun attachTimerPanelIfNeeded() {
        if (timerSeconds > 0 || timerActive) renderTimerPanel()
    }

    private fun renderTimerPanel() {
        timerPanel?.let { if (it.parent === content) content.removeView(it) }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(14, 10, 14, 10)
            background = GradientDrawable().apply {
                setColor(Color.rgb(65, 24, 34)); cornerRadius = 16f
                setStroke(1, Color.rgb(160, 48, 64))
            }
        }
        timerLabel = TextView(this).apply {
            textSize = 23f; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            includeFontPadding = false
            setTextColor(Color.WHITE); gravity = Gravity.CENTER
            setPadding(0, 6, 0, 4); text = timerText()
        }
        panel.addView(timerLabel)
        panel.addView(TextView(this).apply {
            text = "REST CONTROLS"; textSize = 10f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            includeFontPadding = false
            letterSpacing = 0.08f; setTextColor(Color.rgb(235, 185, 192))
            gravity = Gravity.CENTER; setPadding(0, 7, 0, 7)
        })
        val topRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        val bottomRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        val minus = controlButton("-15 SEC", 56) { adjustTimer(-15) }
        pauseButton = controlButton(if (timerPaused) "RESUME" else "PAUSE", 56, Color.rgb(155, 35, 54)) { toggleTimerPause() }
        val plus = controlButton("+15 SEC", 56) { adjustTimer(15) }
        val skip = controlButton("SKIP REST", 56) { skipTimer() }
        topRow.addView(minus, timerButtonParams())
        topRow.addView(plus, timerButtonParams())
        bottomRow.addView(pauseButton, timerButtonParams())
        bottomRow.addView(skip, timerButtonParams())
        panel.addView(topRow)
        panel.addView(bottomRow)
        timerPanel = panel
        content.addView(panel, 0, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, 10) })
    }

    private fun timerButtonParams() = LinearLayout.LayoutParams(0, 56, 1f).apply {
        setMargins(5, 5, 5, 5)
    }


    private fun timerText(): String = when {
        timerSeconds <= 0 -> "REST COMPLETE"
        else -> "REST  %02d:%02d".format(timerSeconds / 60, timerSeconds % 60)
    }

    private fun updateTimerDisplay() {
        timerLabel?.text = timerText()
        pauseButton?.text = if (timerPaused) "RESUME" else "PAUSE"
    }

    private fun startRest(seconds: Int) {
        timerRunnable?.let { handler.removeCallbacks(it) }
        timerSeconds = seconds; timerActive = true; timerPaused = false
        renderTimerPanel()
        scheduleTimerTick()
    }

    private fun scheduleTimerTick() {
        timerRunnable?.let { handler.removeCallbacks(it) }
        if (!timerActive || timerPaused) return
        val tick = object : Runnable {
            override fun run() {
                if (!timerActive || timerPaused) return
                if (timerSeconds > 0) timerSeconds--
                updateTimerDisplay()
                if (timerSeconds > 0) handler.postDelayed(this, 1000)
                else timerActive = false
            }
        }
        timerRunnable = tick
        handler.postDelayed(tick, 1000)
    }

    private fun toggleTimerPause() {
        if (!timerActive) return
        timerPaused = !timerPaused
        if (timerPaused) timerRunnable?.let { handler.removeCallbacks(it) } else scheduleTimerTick()
        updateTimerDisplay()
    }

    private fun adjustTimer(delta: Int) {
        timerSeconds = (timerSeconds + delta).coerceIn(0, 3600)
        if (timerSeconds == 0) {
            timerActive = false; timerPaused = false
            timerRunnable?.let { handler.removeCallbacks(it) }
        } else if (timerActive && !timerPaused) scheduleTimerTick()
        updateTimerDisplay()
    }

    private fun skipTimer() {
        timerRunnable?.let { handler.removeCallbacks(it) }
        timerActive = false; timerPaused = false; timerSeconds = 0
        updateTimerDisplay()
    }

    private fun showHistory() {
        currentScreen = "history"; content.removeAllViews()
        heading("TRAINING HISTORY")
        body("Completed workouts: ${prefs.getInt("workouts_done", 0)}")
        body("Streak: ${prefs.getInt("streak", 0)} days")
        heading("BACKUP & RESTORE")
        body("Save your workout history and app progress to a JSON backup file.", 13f)
        actionButton("EXPORT BACKUP") { startBackupExport() }
        actionButton("IMPORT BACKUP") { startBackupImport() }
        heading("ALL SET RECORDS")
        val records = loadRecords().sortedByDescending { it.timestamp }
        if (records.isEmpty()) {
            body("No set records yet. Log your first completed set in a workout.")
        } else {
            val formatter = SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault())
            records.forEach { record ->
                val item = card()
                item.addView(TextView(this).apply {
                    text = record.exercise
                    textSize = 17f
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                    includeFontPadding = false
                    setTextColor(Color.WHITE)
                    setPadding(0, 0, 0, 8)
                })
                item.addView(TextView(this).apply {
                    text = "DAY ${record.day}"
                    textSize = 11f
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                    includeFontPadding = false
                    setTextColor(Color.rgb(220, 155, 166))
                    setPadding(0, 0, 0, 4)
                })
                item.addView(TextView(this).apply {
                    text = "RESULT  ·  ${record.value}"
                    textSize = 14f
                    includeFontPadding = false
                    setTextColor(Color.rgb(225, 227, 234))
                    setPadding(0, 0, 0, 5)
                })
                item.addView(TextView(this).apply {
                    text = formatter.format(Date(record.timestamp))
                    textSize = 12f
                    includeFontPadding = false
                    setTextColor(Color.rgb(165, 171, 185))
                    setPadding(0, 0, 0, 12)
                })
                val actions = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                actions.addView(controlButton("EDIT", 48, Color.rgb(65, 75, 95)) { editRecord(record) },
                    LinearLayout.LayoutParams(0, 52, 1f).apply { setMargins(4, 2, 4, 2) })
                actions.addView(controlButton("DELETE", 48, Color.rgb(133, 28, 45)) { confirmDeleteRecord(record) },
                    LinearLayout.LayoutParams(0, 52, 1f).apply { setMargins(4, 2, 4, 2) })
                item.addView(actions)
            }
        }
        attachTimerPanelIfNeeded()
    }

    private fun editRecord(record: SetRecord) {
        val input = EditText(this).apply {
            setText(record.value); setSingleLine(true); setTextColor(Color.BLACK)
            inputType = InputType.TYPE_CLASS_TEXT
        }
        AlertDialog.Builder(this)
            .setTitle("Edit set result")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val newValue = input.text.toString().trim()
                if (newValue.isNotEmpty()) {
                    val updated = loadRecords().map {
                        if (it.id == record.id) it.copy(value = newValue) else it
                    }
                    saveRecords(updated)
                    showHistory()
                    Toast.makeText(this, "Set record updated.", Toast.LENGTH_SHORT).show()
                }
            }.show()
    }

    private fun confirmDeleteRecord(record: SetRecord) {
        AlertDialog.Builder(this)
            .setTitle("Delete set record?")
            .setMessage("This removes only this saved set result.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                saveRecords(loadRecords().filterNot { it.id == record.id })
                showHistory()
                Toast.makeText(this, "Set record deleted.", Toast.LENGTH_SHORT).show()
            }.show()
    }

    private fun showTest() {
        currentScreen = "test"; content.removeAllViews()
        heading("LEVEL 2 TEST")
        body("Enter your best valid result for each standard. You must meet every target to pass.")
        val standards = listOf(
            "Push-ups" to 40, "Dips" to 15, "Strict pull-ups" to 10,
            "Explosive pull-ups" to 3, "Pistol squats per leg" to 8,
            "L-sit progression (seconds)" to 30, "Hanging leg raises" to 15,
            "Wall handstand total (seconds)" to 60, "Consistent 10 m accelerations" to 5
        )
        val fields = mutableListOf<Pair<String, EditText>>()
        standards.forEach { (name, target) ->
            val row = card()
            row.addView(TextView(this).apply {
                text = "$name · Target $target"; setTextColor(Color.WHITE); textSize = 15f
            })
            val edit = EditText(this).apply {
                hint = "Your result"; setSingleLine(true)
                inputType = InputType.TYPE_CLASS_NUMBER; setTextColor(Color.WHITE)
            }
            row.addView(edit); fields.add(name to edit)
        }
        actionButton("Evaluate test") {
            val values = fields.map { it.second.text.toString().toIntOrNull() ?: 0 }
            val passed = values.indices.all { values[it] >= standards[it].second }
            if (passed) {
                prefs.edit().putBoolean("level2_passed", true).apply()
                Toast.makeText(this, "Level 2 passed! Level 3 unlocked.", Toast.LENGTH_LONG).show()
                body("PASSED · Level 3 unlocked. The next level's full training plan is not included in this first build.")
            } else {
                val failed = fields.indices.filter { values[it] < standards[it].second }.joinToString { fields[it].first }
                Toast.makeText(this, "Not passed yet. Retest when ready.", Toast.LENGTH_LONG).show()
                body("Not passed yet. Standards to improve: $failed")
            }
        }
        attachTimerPanelIfNeeded()
    }
}
