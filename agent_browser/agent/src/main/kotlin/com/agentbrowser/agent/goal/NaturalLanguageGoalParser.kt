// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of a time governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.agent.goal

import com.agentbrowser.agent.model.TriggerSpec

/**
 * Deterministic NL parsing for common automation phrases (spec section 37):
 * "every morning", "every hour", "every monday", "weekdays at 9", "monthly", ...
 * The result is always shown to the user for editing before activation.
 */
object NaturalLanguageGoalParser {

    data class Parsed(
        val name: String,
        val schedule: TriggerSpec.Time?,
        val triggers: List<TriggerSpec>,
    )

    private val dayNames = mapOf(
        "monday" to 1, "tuesday" to 2, "wednesday" to 3, "thursday" to 4,
        "friday" to 5, "saturday" to 6, "sunday" to 7,
    )

    fun parse(instruction: String): Parsed {
        val text = instruction.lowercase()

        var kind: String? = null
        var interval: Long? = null
        var hour: Int? = null
        var minute: Int? = null
        var days: List<Int> = emptyList()

        // "at 9 AM" / "at 14:30"
        Regex("""at\s+(\d{1,2})(?::(\d{2}))?\s*(am|pm)?""").find(text)?.let { m ->
            hour = m.groupValues[1].toInt()
            minute = m.groupValues[2].toIntOrNull() ?: 0
            if (m.groupValues[3] == "pm" && hour != 12) hour = hour!! + 12
            if (m.groupValues[3] == "am" && hour == 12) hour = 0
        }

        when {
            text.contains("every weekday") || text.contains("weekdays") -> {
                kind = "weekdays"
            }
            text.contains("every morning") -> {
                kind = "daily"
                if (hour == null) { hour = 8; minute = 0 }
            }
            Regex("""every\s+monday|tuesday|wednesday|thursday|friday|saturday|sunday""").containsMatchIn(text) -> {
                kind = "weekly"
                days = listOf(dayNames.entries.first { text.contains(it.key) }.value)
            }
            text.contains("monthly") -> {
                kind = "monthly"
            }
            Regex("""every\s+(\d+)\s+minute""").find(text)?.let { m ->
                kind = "every_n_minutes"
                interval = m.groupValues[1].toLong()
            }
            Regex("""every\s+(\d+)\s+hour""").find(text)?.let { m ->
                kind = "every_n_minutes"
                interval = m.groupValues[1].toLong() * 60
            }
            text.contains("every hour") -> {
                kind = "hourly"
            }
            text.contains("every day") || text.contains("daily") -> {
                kind = "daily"
            }
        }

        val schedule = kind?.let {
            TriggerSpec.Time(
                id = "time-primary",
                kind = it,
                intervalMinutes = interval,
                hourOfDay = hour,
                minuteOfHour = minute,
                days = days,
            )
        }

        val triggers = mutableListOf<TriggerSpec>()
        if (Regex("""\b(monitor|watch)\b""").containsMatchIn(text)) {
            triggers.add(TriggerSpec.Content(id = "content-monitor", kind = "page_changes"))
        }
        if (Regex("""notify me (if|when)|when .*chang""").containsMatchIn(text)) {
            triggers.add(TriggerSpec.Content(id = "content-notify", kind = "keyword_appears"))
        }

        val name = instruction.split(Regex("""[.!?\n]""")).firstOrNull()
            ?.trim()?.take(40) ?: "Goal"
        return Parsed(name = name.ifBlank { "Goal" }, schedule = schedule, triggers = triggers)
    }
}
