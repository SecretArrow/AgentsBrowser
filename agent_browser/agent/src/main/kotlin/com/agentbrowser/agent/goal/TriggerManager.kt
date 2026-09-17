// Copyright 2026 The Agent Goals. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
// (header fixed by automation; original intended header is the standard one)
package com.agentbrowser.agent.goal

import com.agentbrowser.agent.model.TriggerSpec
import java.util.Calendar

/**
 * Trigger evaluation (spec section 21). Computes the next fire time for time
 * triggers and evaluates browser/content/state triggers against events.
 */
object TriggerManager {

    /** Returns the next fire epoch-ms for a time trigger, or null when invalid. */
    fun nextFireAt(t: TriggerSpec.Time, from: Long = System.currentTimeMillis()): Long? {
        val cal = Calendar.getInstance().apply { timeInMillis = from }
        when (t.kind) {
            "every_n_minutes" -> return from + (t.intervalMinutes ?: 15) * 60_000L
            "hourly" -> {
                cal.add(Calendar.HOUR, 1)
                cal.set(Calendar.MINUTE, t.minuteOfHour ?: 0)
                cal.set(Calendar.SECOND, 0)
                return cal.timeInMillis
            }
            "daily", "weekdays", "weekly", "selected_days" -> {
                val allowedDays = when (t.kind) {
                    "weekdays" -> listOf(2, 3, 4, 5, 6) // Calendar: Mon=2..Fri=6
                    "weekly", "selected_days" -> t.days.map { it } // 1=Mon..7=Sun
                    else -> null
                }
                while (true) {
                    val hour = t.hourOfDay
                    if (hour != null) {
                        cal.set(Calendar.HOUR_OF_DAY, hour)
                        cal.set(Calendar.MINUTE, t.minuteOfHour ?: 0)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                    } else {
                        cal.add(Calendar.MINUTE, 30)
                    }
                    if (cal.timeInMillis <= from) {
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                        continue
                    }
                    if (allowedDays != null) {
                        // Map Calendar.DAY_OF_WEEK (Sun=1..Sat=7) to ISO (Mon=1..Sun=7)
                        val iso = if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 7 else cal.get(Calendar.DAY_OF_WEEK) - 1
                        if (cal.timeInMillis > from && iso in allowedDays) return cal.timeInMillis
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                        continue
                    }
                    return cal.timeInMillis
                }
            }
            "monthly" -> {
                cal.add(Calendar.MONTH, 1)
                cal.set(Calendar.DAY_OF_MONTH, t.dayOfMonth ?: 1)
                cal.set(Calendar.HOUR_OF_DAY, t.hourOfDay ?: 8)
                cal.set(Calendar.MINUTE, t.minuteOfHour ?: 0)
                return cal.timeInMillis
            }
            "once" -> return from
            else -> return null
        }
    }

    /** Browser trigger evaluation against a browser event. */
    fun matchesBrowserTrigger(t: TriggerSpec.Browser, eventKind: String, url: String?): Boolean {
        if (t.kind != eventKind) return false
        return when (eventKind) {
            "url_matched", "domain_visited" -> url?.contains(t.pattern ?: "", ignoreCase = true) == true
            else -> true
        }
    }

    /** Content trigger evaluation against page facts. */
    fun matchesContentTrigger(t: TriggerSpec.Content, pageText: String?, changed: Boolean): Boolean {
        return when (t.kind) {
            "text_appears", "keyword_appears" -> pageText?.contains(t.keyword ?: "", ignoreCase = true) == true
            "page_changes" -> changed
            "element_changes", "price_changes", "new_item_appears" -> changed && t.threshold?.let { it > 0 } != false
            else -> false
        }
    }

    /** State trigger evaluation. */
    fun matchesStateTrigger(t: TriggerSpec.State, stateKind: String): Boolean = t.kind == stateKind
}
