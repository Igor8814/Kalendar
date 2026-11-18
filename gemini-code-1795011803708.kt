package com.example.mycalendar

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var selectedDateKey: String = ""
    private lateinit var eventsContainer: LinearLayout
    private lateinit var dateTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val calendarView = CalendarView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        dateTitle = TextView(this).apply {
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 24, 0, 16)
        }

        val addButton = Button(this).apply {
            text = "+ Добавить событие"
            setOnClickListener { showAddEventDialog() }
        }

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        eventsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 16)
        }

        scrollView.addView(eventsContainer)
        rootLayout.addView(calendarView)
        rootLayout.addView(dateTitle)
        rootLayout.addView(addButton)
        rootLayout.addView(scrollView)
        setContentView(rootLayout)

        val today = Calendar.getInstance()
        updateSelectedDate(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH))

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            updateSelectedDate(year, month, dayOfMonth)
        }
    }

    private fun updateSelectedDate(year: Int, month: Int, day: Int) {
        selectedDateKey = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
        val cal = Calendar.getInstance().apply { set(year, month, day) }
        val formatter = SimpleDateFormat("d MMMM yyyy", Locale("ru"))
        dateTitle.text = "События на ${formatter.format(cal.time)}"
        renderEvents()
    }

    private fun showAddEventDialog() {
        val inputLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 12)
        }

        val timeInput = EditText(this).apply {
            hint = "Время (например, 14:00)"
        }
        val titleInput = EditText(this).apply {
            hint = "Название события или заметка"
        }

        inputLayout.addView(timeInput)
        inputLayout.addView(titleInput)

        AlertDialog.Builder(this)
            .setTitle("Новое событие")
            .setView(inputLayout)
            .setPositiveButton("Сохранить") { _, _ ->
                val time = timeInput.text.toString().trim()
                val title = titleInput.text.toString().trim()
                if (title.isNotEmpty()) {
                    val entry = if (time.isNotEmpty()) "$time — $title" else title
                    saveEvent(selectedDateKey, entry)
                    renderEvents()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun renderEvents() {
        eventsContainer.removeAllViews()
        val events = getEvents(selectedDateKey)

        if (events.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = "На этот день записей нет"
                setTextColor(Color.GRAY)
                setPadding(0, 16, 0, 16)
            }
            eventsContainer.addView(emptyView)
            return
        }

        events.forEachIndexed { index, eventText ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(16, 16, 16, 16)
            }

            val tv = TextView(this).apply {
                text = eventText
                textSize = 16f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val delBtn = Button(this).apply {
                text = "✕"
                setOnClickListener {
                    deleteEvent(selectedDateKey, index)
                    renderEvents()
                }
            }

            row.addView(tv)
            row.addView(delBtn)
            eventsContainer.addView(row)
        }
    }

    private fun getEvents(dateKey: String): MutableList<String> {
        val prefs = getSharedPreferences("calendar_events", Context.MODE_PRIVATE)
        val raw = prefs.getString(dateKey, "") ?: ""
        return if (raw.isEmpty()) mutableListOf() else raw.split("||").toMutableList()
    }

    private fun saveEvent(dateKey: String, event: String) {
        val list = getEvents(dateKey)
        list.add(event)
        list.sort()
        val prefs = getSharedPreferences("calendar_events", Context.MODE_PRIVATE)
        prefs.edit().putString(dateKey, list.joinToString("||")).apply()
    }

    private fun deleteEvent(dateKey: String, index: Int) {
        val list = getEvents(dateKey)
        if (index in list.indices) {
            list.removeAt(index)
            val prefs = getSharedPreferences("calendar_events", Context.MODE_PRIVATE)
            prefs.edit().putString(dateKey, list.joinToString("||")).apply()
        }
    }
}