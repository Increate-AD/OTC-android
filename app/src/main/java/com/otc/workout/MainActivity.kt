package com.otc.workout

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.content.Context

data class Exercise(val name: String, val target: String, val restSeconds: Int, val cue: String)

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("otc_data", Context.MODE_PRIVATE) }
    private val handler = Handler(Looper.getMainLooper())
    private var timerSeconds = 0
    private var timerRunnable: Runnable? = null
    private lateinit var content: LinearLayout
    private var selectedDay = 1

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
            setPadding(18, 18, 18, 12)
        }
        root.addView(TextView(this).apply {
            text = "OTC  /  OUTWORK THE CHALLENGE"
            setTextColor(Color.WHITE); textSize = 21f; typeface = Typeface.DEFAULT_BOLD
            setPadding(4, 8, 4, 16)
        })
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        nav.addView(navButton("Home") { showHome() }, weightParams())
        nav.addView(navButton("Workout") { showWorkout(selectedDay) }, weightParams())
        nav.addView(navButton("History") { showHistory() }, weightParams())
        nav.addView(navButton("Level test") { showTest() }, weightParams())
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

    private fun weightParams() = LinearLayout.LayoutParams(0, 44, 1f).apply { setMargins(2, 4, 2, 4) }
    private fun navButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label; textSize = 10f; setOnClickListener { action() }
    }
    private fun heading(value: String) {
        content.addView(TextView(this).apply {
            text = value; textSize = 23f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE); setPadding(0, 12, 0, 10)
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
        setBackgroundColor(Color.rgb(28, 30, 38))
        content.addView(this, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 7, 0, 7) })
    }
    private fun actionButton(label: String, action: () -> Unit) {
        content.addView(Button(this).apply { text = label; setOnClickListener { action() } },
            LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 6, 0, 6) })
    }

    private fun showHome() {
        content.removeAllViews()
        heading("LEVEL 2")
        body("Football-first calisthenics training")
        card().apply {
            body("WORKOUTS COMPLETED: ${prefs.getInt("workouts_done", 0)}")
            body("CURRENT STREAK: ${prefs.getInt("streak", 0)} days")
            body("NEXT IN ORDER: Day $selectedDay")
        }
        heading("YOUR WEEK")
        for (day in 1..7) {
            val label = when (day) {
                3 -> "Day 3 · Recovery"
                6 -> "Day 6 · Mobility"
                7 -> "Day 7 · Full recovery"
                else -> "Day $day · Training"
            }
            actionButton(label) { showWorkout(day) }
        }
        body("Keep the sequence. If you miss a day, continue when ready—never double up to catch up.", 13f)
    }

    private fun showWorkout(day: Int) {
        selectedDay = day
        content.removeAllViews()
        heading("DAY $day")
        if (day == 3 || day == 6 || day == 7) {
            body(if (day == 7) "Full recovery day" else "Guided recovery and mobility")
            recovery.forEach { exerciseCard(it, "recovery_$day") }
        } else {
            body(when (day) {
                1 -> "Speed + power"
                2 -> "Pull + muscle-up preparation"
                4 -> "Agility + posterior chain"
                else -> "Push + handstand"
            })
            workouts[day]?.forEach { exerciseCard(it, "day_$day") }
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
    }

    private fun exerciseCard(ex: Exercise, key: String) {
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
        }
        c.addView(input)
        val logged = TextView(this).apply {
            text = "Sets logged: ${prefs.getInt("${key}_${ex.name}_sets", 0)}"
            setTextColor(Color.rgb(210, 210, 220))
        }
        c.addView(logged)
        c.addView(Button(this).apply {
            text = "Log completed set"
            setOnClickListener {
                val value = input.text.toString().trim()
                if (value.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Enter what you actually completed.", Toast.LENGTH_SHORT).show()
                } else {
                    val storageKey = "${key}_${ex.name}_sets"
                    val n = prefs.getInt(storageKey, 0) + 1
                    prefs.edit().putInt(storageKey, n).putString("last_${ex.name}", value).apply()
                    logged.text = "Sets logged: $n · Last: $value"
                    input.text.clear()
                    if (ex.restSeconds > 0) startRest(ex.restSeconds)
                }
            }
        })
    }

    private fun startRest(seconds: Int) {
        timerRunnable?.let { handler.removeCallbacks(it) }
        timerSeconds = seconds
        val timerView = TextView(this).apply {
            textSize = 22f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE)
            gravity = Gravity.CENTER; setPadding(0, 10, 0, 10)
        }
        content.addView(timerView, 0)
        val tick = object : Runnable {
            override fun run() {
                timerView.text = "REST  %02d:%02d".format(timerSeconds / 60, timerSeconds % 60)
                if (timerSeconds > 0) { timerSeconds--; handler.postDelayed(this, 1000) }
                else timerView.text = "REST COMPLETE"
            }
        }
        timerRunnable = tick
        handler.post(tick)
        content.addView(Button(this).apply {
            text = "Skip rest"
            setOnClickListener { handler.removeCallbacks(tick); timerView.text = "REST SKIPPED" }
        }, LinearLayout.LayoutParams(-1, -2))
    }

    private fun showHistory() {
        content.removeAllViews()
        heading("TRAINING HISTORY")
        body("Completed workouts: ${prefs.getInt("workouts_done", 0)}")
        body("Streak: ${prefs.getInt("streak", 0)} days")
        heading("RECENT SET RECORDS")
        listOf("Pistol squat", "Pull-up", "Dips", "Wall handstand", "L-sit").forEach { name ->
            body("$name: ${prefs.getString("last_$name", "No result yet")}")
        }
        body("Set logs are stored locally on this phone. Editing older records and backup/export are not included in this first build.", 13f)
    }

    private fun showTest() {
        content.removeAllViews()
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
            row.addView(TextView(this).apply { text = "$name · Target $target"; setTextColor(Color.WHITE); textSize = 15f })
            val edit = EditText(this).apply {
                hint = "Your result"; setSingleLine(true)
                inputType = android.text.InputType.TYPE_CLASS_NUMBER; setTextColor(Color.WHITE)
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
    }
}
