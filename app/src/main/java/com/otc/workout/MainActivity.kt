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
data class TestStandard(val label: String, val target: Int, val alternativeGroup: String? = null)

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
    private var currentLevel = 2
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

    private val advancedWorkouts: Map<Int, Map<Int, List<Exercise>>> = mapOf(
        3 to mapOf(
            1 to listOf(
                Exercise("Acceleration", "5 × 10 m + 4 × 20 m", 180, "Use full recovery; every sprint should be fast and technically clean."),
                Exercise("Broad jump", "5 × 3", 90, "Jump explosively and land quietly with knees aligned."),
                Exercise("Pistol squat", "4 × 8 / leg", 120, "Use assistance if needed; stop before technique breaks."),
                Exercise("Hard split squat", "4 × 6 / leg", 120, "Choose a challenging controlled variation."),
                Exercise("Nordic hamstring curl", "3 × 5", 150, "Use assistance to control the eccentric.")
            ),
            2 to listOf(
                Exercise("Chest-to-bar pull-up", "5 × 4–6", 150, "Pull the chest toward the bar without kicking."),
                Exercise("Explosive pull-up", "5 × 3", 150, "Prioritize height and clean reps."),
                Exercise("Straight-bar dip", "4 × 6–10", 120, "Use a stable top support and controlled depth."),
                Exercise("Advanced tuck front lever", "5 × 12–15 sec", 120, "Keep hips level and shoulders engaged."),
                Exercise("L-sit", "5 × 20 sec", 90, "Keep knees locked as your current level allows.")
            ),
            4 to listOf(
                Exercise("Reactive change of direction", "5 quality reps", 120, "React to a visual or audio cue; each rep stays sharp."),
                Exercise("Lateral bound", "4 × 4 / side", 90, "Stick each landing before the next rep."),
                Exercise("Assisted dragon squat", "4 × 4 / side", 120, "Use support and a pain-free range."),
                Exercise("Copenhagen plank", "3 × 30 sec / side", 90, "Keep hips lifted and trunk straight."),
                Exercise("Calf raise", "4 × 12", 60, "Use a full controlled range.")
            ),
            5 to listOf(
                Exercise("Pseudo-planche push-up", "5 × 6–10", 120, "Lean forward only as far as you can keep control."),
                Exercise("Pike / deficit pike push-up", "4 × 6–10", 120, "Keep elbows controlled and head path consistent."),
                Exercise("Handstand practice", "10 min", 60, "Use safe wall or freestanding progressions."),
                Exercise("Wall HSPU eccentric", "4 × 3–5", 150, "Lower slowly; stop if shoulder position collapses."),
                Exercise("L-sit / V-sit progression", "5 × 15 sec", 90, "Use the hardest clean hold you can maintain.")
            )
        ),
        4 to mapOf(
            1 to listOf(
                Exercise("Acceleration", "5 × 10 m + 3 × 20 m", 180, "Full recovery; maintain sprint quality."),
                Exercise("Single-leg jump", "4 × 3 / side", 120, "Land quietly and stabilize each repetition."),
                Exercise("Pistol squat variation", "4 × 6 / leg", 120, "Choose a variation that permits clean reps."),
                Exercise("Nordic hamstring curl", "3 × 5", 150, "Control the lowering phase."),
                Exercise("Calf raise", "4 × 12", 60, "Pause at the top.")
            ),
            2 to listOf(
                Exercise("Muscle-up transition", "5 × 3", 150, "Practice a controlled transition with an appropriate assist."),
                Exercise("Explosive chest-to-bar", "5 × 3", 150, "Pull high without swinging."),
                Exercise("Front lever progression", "5 × 10–15 sec", 150, "Use the hardest technically sound progression."),
                Exercise("Advanced pull-up", "4 × 5–6", 120, "Add difficulty only while reps stay strict."),
                Exercise("L-sit", "4 × 25 sec", 90, "Keep shoulders depressed and legs active.")
            ),
            4 to listOf(
                Exercise("Reactive agility", "6 reps", 120, "Respond to an unpredictable cue and stay balanced."),
                Exercise("Dragon squat progression", "4 × 3 / side", 150, "Use assistance as required for clean depth."),
                Exercise("Split squat", "4 × 6 / leg", 120, "Use a challenging controlled load or variation."),
                Exercise("Copenhagen plank", "3 × 30 sec / side", 90, "Maintain a straight trunk."),
                Exercise("Deceleration", "4 × 10 m", 120, "Accelerate then brake progressively with control.")
            ),
            5 to listOf(
                Exercise("Handstand practice", "12–15 min", 60, "Accumulate quality attempts with safe exits."),
                Exercise("HSPU progression", "5 × 3–6", 150, "Use a range and variation you can control."),
                Exercise("Planche lean / pseudo-planche hold", "5 × 15–25 sec", 120, "Protract the shoulders and keep elbows locked."),
                Exercise("Dips", "4 × 8–12", 120, "Use a challenging but pain-free variation."),
                Exercise("L-sit / V-sit", "5 × 15–25 sec", 90, "Keep each hold clean.")
            )
        ),
        5 to mapOf(
            1 to listOf(
                Exercise("Acceleration", "6 × 10 m + 3 × 20 m", 180, "Full recovery; stop if sprint quality drops."),
                Exercise("Bounds", "4 × 4 / side", 120, "Explode and land with control."),
                Exercise("Advanced unilateral strength", "4 × 5–6 / leg", 150, "Use a challenging clean single-leg variation."),
                Exercise("Nordic hamstring curl", "3 × 4–6", 150, "Keep eccentric control.")
            ),
            2 to listOf(
                Exercise("Muscle-up", "5 × 1–3 or hardest clean progression", 180, "No kipping unless the chosen progression specifically requires assistance."),
                Exercise("Advanced pull-up", "4 × 4–6", 150, "Use strict form and a challenging variation."),
                Exercise("Front lever", "5 × 8–12 sec", 150, "Use your hardest repeatable progression."),
                Exercise("L-sit / V-sit", "5 × 20 sec", 90, "Keep the hold strict."),
                Exercise("Hanging leg raise", "3 × 10–15", 90, "Avoid swinging.")
            ),
            4 to listOf(
                Exercise("Reactive change of direction", "6–8 reps", 150, "Keep every repetition reactive and high quality."),
                Exercise("Dragon squat", "4 × 3 / side", 150, "Use minimal assistance while preserving control."),
                Exercise("Single-leg jump", "4 × 3 / side", 120, "Land quietly and stabilize."),
                Exercise("Copenhagen plank", "3 × 30–40 sec / side", 90, "Maintain hip height."),
                Exercise("Calf / tibialis raise", "3 × 12–15", 60, "Use controlled full range.")
            ),
            5 to listOf(
                Exercise("Handstand practice", "15 min", 60, "Prioritize quality balance and safe exits."),
                Exercise("Handstand push-up", "5 × 3–6", 180, "Use a strict progression you can control."),
                Exercise("Planche progression", "5 × 10–20 sec", 150, "Keep scapula protracted and elbows locked."),
                Exercise("Hard-variation dips", "3 × 8–12", 120, "Choose a difficult but controlled variation."),
                Exercise("Upper accessory", "3 × 10–15", 75, "Use a balanced, pain-free accessory.")
            )
        ),
        6 to mapOf(
            1 to listOf(
                Exercise("Short acceleration", "4–6 × 10–20 m", 180, "Full recovery; keep every sprint crisp."),
                Exercise("Advanced unilateral power", "3–5 sets", 150, "Choose a safe advanced jump or power variation."),
                Exercise("Individualized strength maintenance", "3–4 sets", 120, "Maintain the strength pattern that supports football."),
                Exercise("Posterior-chain maintenance", "2–3 sets", 120, "Use a controlled hamstring/hip-dominant exercise.")
            ),
            2 to listOf(
                Exercise("Muscle-up variations", "4–6 quality sets", 180, "Choose a technically consistent variation."),
                Exercise("Front lever / one-arm pull-up progression", "4–6 sets", 180, "Specialize in one primary pulling skill per block."),
                Exercise("Advanced core", "3–5 sets", 90, "Use a strict L-sit, V-sit or hanging progression."),
                Exercise("Pull strength maintenance", "3–4 sets", 120, "Keep strict pulling strength.")
            ),
            4 to listOf(
                Exercise("Reactive agility", "4–8 quality reps", 150, "Stop before speed or decision quality drops."),
                Exercise("Dragon squat / advanced unilateral skill", "3–5 sets", 150, "Use the hardest repeatable clean progression."),
                Exercise("Posterior-chain maintenance", "2–4 sets", 120, "Maintain hamstring strength."),
                Exercise("Adductor maintenance", "2–4 sets", 90, "Use controlled Copenhagen or equivalent work.")
            ),
            5 to listOf(
                Exercise("Freestanding handstand", "10–15 min", 60, "Accumulate quality attempts and safe exits."),
                Exercise("HSPU specialization", "4–6 quality sets", 180, "Choose a strict progression."),
                Exercise("Planche specialization", "4–6 quality sets", 180, "Keep each hold technically sound."),
                Exercise("Advanced pushing / physique", "3–5 sets", 90, "Keep this supportive, not fatiguing for football.")
            )
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

    private val levelTests: Map<Int, List<TestStandard>> = mapOf(
        2 to listOf(
            TestStandard("Push-ups", 40), TestStandard("Dips", 15), TestStandard("Strict pull-ups", 10),
            TestStandard("Explosive pull-ups", 3), TestStandard("Pistol squats per leg", 8),
            TestStandard("L-sit progression (seconds)", 30), TestStandard("Hanging leg raises", 15),
            TestStandard("Wall handstand total (seconds)", 60), TestStandard("Consistent 10 m accelerations", 5)
        ),
        3 to listOf(
            TestStandard("Strict pull-ups", 12), TestStandard("Chest-to-bar pull-ups", 5),
            TestStandard("High explosive pull-ups", 3), TestStandard("Dips", 15),
            TestStandard("Straight-bar dips", 8), TestStandard("Advanced tuck front lever (seconds)", 20),
            TestStandard("Pseudo-planche push-ups", 5), TestStandard("Wall handstand total (seconds)", 90),
            TestStandard("Handstand kick-ups", 5), TestStandard("Pistol squats per leg", 10),
            TestStandard("Assisted dragon squats per side", 5)
        ),
        4 to listOf(
            TestStandard("High chest-to-bar pull-ups", 3), TestStandard("Straight-bar dips", 10),
            TestStandard("Controlled muscle-up transitions", 1), TestStandard("Advanced tuck / one-leg front lever (seconds)", 10),
            TestStandard("Freestanding handstand (seconds)", 20, "handstand"),
            TestStandard("Wall handstand (seconds)", 90, "handstand"),
            TestStandard("HSPU negatives / partials", 5), TestStandard("Strong planche lean (seconds)", 30),
            TestStandard("Assisted dragon squats per side", 3), TestStandard("Quality 5-10-5 shuttle runs", 5)
        ),
        5 to listOf(
            TestStandard("Clean muscle-ups", 3),
            TestStandard("Advanced front lever variation (seconds)", 10, "frontlever"),
            TestStandard("Full / near-full front lever (seconds)", 5, "frontlever"),
            TestStandard("Strict wall HSPU", 5), TestStandard("Freestanding handstand (seconds)", 30),
            TestStandard("Advanced tuck / strong planche hold (seconds)", 10),
            TestStandard("Dragon squats per side (minimal assistance)", 3),
            TestStandard("Strict pull-ups", 15)
        ),
        6 to listOf(
            TestStandard("Advanced skills performed consistently (count)", 3),
            TestStandard("Strict pull-ups or weighted equivalent (reps)", 15),
            TestStandard("Strict handstand push-ups achieved (enter 1 if yes)", 1),
            TestStandard("Advanced planche progression achieved (enter 1 if yes)", 1),
            TestStandard("L-sit / V-sit hold (seconds)", 30),
            TestStandard("Advanced lever progression achieved (enter 1 if yes)", 1),
            TestStandard("Clean dragon squat / advanced unilateral skill achieved (enter 1 if yes)", 1),
            TestStandard("Football speed and agility maintained (enter 1 for yes)", 1)
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedDay = prefs.getInt("next_day", 1)
        currentLevel = prefs.getInt("current_level", 2).coerceIn(2, 6)
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
        heading("LEVEL ${currentLevel.toString().padStart(2, '0')}")
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
        heading("LEVEL ${currentLevel.toString().padStart(2, '0')} · DAY $day")
        if (day == 3 || day == 6 || day == 7) {
            body(if (day == 7) "Full recovery day" else "Guided recovery and mobility")
            recovery.forEach { exerciseCard(it, day) }
        } else {
            body(when (day) {
                1 -> "Speed + power"; 2 -> "Pull + muscle-up preparation"
                4 -> "Agility + posterior chain"; else -> "Push + handstand"
            })
            val dayPlan = if (currentLevel == 2) workouts[day] else advancedWorkouts[currentLevel]?.get(day)
            dayPlan?.forEach { exerciseCard(it, day) }
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
        val level = currentLevel
        heading(if (level == 6) "LEVEL 6 · MASTERY TEST" else "LEVEL $level TEST")
        body("Enter your best clean result for every standard. Alternative options in the same category require only one to pass.")
        val standards = levelTests[level] ?: levelTests[6]!!
        val fields = mutableListOf<Pair<TestStandard, EditText>>()
        standards.forEach { standard ->
            val row = card()
            row.addView(TextView(this).apply {
                text = "${standard.label}  ·  Target ${standard.target}"
                setTextColor(Color.WHITE); textSize = 14f
            })
            val edit = EditText(this).apply {
                hint = "Enter result"
                setSingleLine(true)
                inputType = InputType.TYPE_CLASS_NUMBER
                setTextColor(Color.WHITE)
            }
            row.addView(edit)
            fields.add(standard to edit)
        }
        actionButton(if (level == 6) "EVALUATE MASTERY TEST" else "EVALUATE LEVEL $level TEST") {
            val values = fields.map { it.second.text.toString().toIntOrNull() ?: 0 }
            val passedSingles = fields.indices.filter { fields[it].first.alternativeGroup == null }
                .all { values[it] >= fields[it].first.target }
            val groups = fields.mapNotNull { it.first.alternativeGroup }.distinct()
            val passedGroups = groups.all { group ->
                fields.indices.any { fields[it].first.alternativeGroup == group && values[it] >= fields[it].first.target }
            }
            if (passedSingles && passedGroups) {
                if (level < 6) {
                    val nextLevel = level + 1
                    prefs.edit()
                        .putInt("current_level", nextLevel)
                        .putBoolean("level_${level}_passed", true)
                        .apply()
                    currentLevel = nextLevel
                    Toast.makeText(this, "Level $level passed! Level $nextLevel unlocked.", Toast.LENGTH_LONG).show()
                    showHome()
                } else {
                    prefs.edit().putBoolean("level_6_mastery_passed", true).apply()
                    Toast.makeText(this, "Mastery standards passed. Keep specializing and retesting.", Toast.LENGTH_LONG).show()
                    body("MASTERY STANDARD PASSED · Continue specializing in 2–3 skills while protecting football speed, agility and recovery.")
                }
            } else {
                val failed = fields.indices.filter { fields[it].first.alternativeGroup == null && values[it] < fields[it].first.target }
                    .map { fields[it].first.label }.toMutableList()
                groups.forEach { group ->
                    val groupIndices = fields.indices.filter { fields[it].first.alternativeGroup == group }
                    if (groupIndices.none { values[it] >= fields[it].first.target }) {
                        failed.add(groupIndices.joinToString(" OR ") { fields[it].first.label })
                    }
                }
                Toast.makeText(this, "Not passed yet. Keep training and retest when ready.", Toast.LENGTH_LONG).show()
                body("NOT PASSED YET · Standards to improve:\n" + failed.joinToString("\n"))
            }
        }
        attachTimerPanelIfNeeded()
    }}
