// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.agent.goal

import com.agentbrowser.agent.model.TriggerSpec
import org.json.JSONArray
import org.json.JSONObject

/** JSON (de)serialization for [TriggerSpec] persisted in the goals table. */
object TriggerSerde {

    fun encodeTrigger(t: TriggerSpec): String = JSONObject().apply {
        put("type", t::class.simpleName)
        put("id", t.id)
        put("enabled", t.enabled)
        when (t) {
            is TriggerSpec.Time -> {
                put("kind", t.kind)
                t.intervalMinutes?.let { put("intervalMinutes", it) }
                t.hourOfDay?.let { put("hourOfDay", it) }
                t.minuteOfHour?.let { put("minuteOfHour", it) }
                put("days", JSONArray(t.days))
                t.dayOfMonth?.let { put("dayOfMonth", it) }
            }
            is TriggerSpec.Browser -> {
                put("kind", t.kind)
                t.pattern?.let { put("pattern", it) }
            }
            is TriggerSpec.Content -> {
                put("kind", t.kind)
                t.selector?.let { put("selector", it) }
                t.keyword?.let { put("keyword", it) }
                t.threshold?.let { put("threshold", it) }
            }
            is TriggerSpec.State -> put("kind", t.kind)
        }
    }.toString()

    fun decodeTrigger(o: JSONObject): TriggerSpec? = runCatching {
        val id = o.getString("id")
        val enabled = o.optBoolean("enabled", true)
        when (o.optString("type")) {
            "Time" -> TriggerSpec.Time(
                id = id, enabled = enabled, kind = o.getString("kind"),
                intervalMinutes = if (o.has("intervalMinutes")) o.getLong("intervalMinutes") else null,
                hourOfDay = if (o.has("hourOfDay")) o.getInt("hourOfDay") else null,
                minuteOfHour = if (o.has("minuteOfHour")) o.getInt("minuteOfHour") else null,
                days = o.optJSONArray("days")?.let { a -> (0 until a.length()).map { a.getInt(it) } } ?: emptyList(),
                dayOfMonth = if (o.has("dayOfMonth")) o.getInt("dayOfMonth") else null,
            )
            "Browser" -> TriggerSpec.Browser(
                id = id, enabled = enabled, kind = o.getString("kind"),
                pattern = o.optString("pattern").ifEmpty { null },
            )
            "Content" -> TriggerSpec.Content(
                id = id, enabled = enabled, kind = o.getString("kind"),
                selector = o.optString("selector").ifEmpty { null },
                keyword = o.optString("keyword").ifEmpty { null },
                threshold = if (o.has("threshold")) o.getDouble("threshold") else null,
            )
            "State" -> TriggerSpec.State(id = id, enabled = enabled, kind = o.getString("kind"))
            else -> null
        }
    }.getOrNull()
}
