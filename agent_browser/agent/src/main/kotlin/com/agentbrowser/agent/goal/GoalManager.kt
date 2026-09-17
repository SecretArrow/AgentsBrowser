// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.agent.goal

import com.agentbrowser.agent.db.AgentDatabase
import com.agentbrowser.agent.model.AgentGoal
import com.agentbrowser.agent.model.AgentStatus
import com.agentbrowser.agent.model.TriggerSpec
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistent goal store (spec sections 20, 36). Includes a deterministic
 * natural-language interpreter that turns instructions like
 * "Every weekday at 9 AM open my dashboard and tell me what needs attention"
 * into a structured goal preview the user can edit before activating.
 */
class GoalManager(private val db: AgentDatabase) {

    fun createFromInstruction(instruction: String): AgentGoal {
        val parsed = NaturalLanguageGoalParser.parse(instruction)
        return AgentGoal(
            id = UUID.randomUUID().toString(),
            name = parsed.name,
            naturalLanguageInstruction = instruction,
            schedule = parsed.schedule,
            triggers = parsed.triggers,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            status = if (parsed.schedule != null || parsed.triggers.isNotEmpty()) {
                AgentStatus.DISABLED
            } else {
                AgentStatus.DISABLED
            },
        )
    }

    fun save(goal: AgentGoal) {
        val v = android.content.ContentValues().apply {
            put("id", goal.id)
            put("name", goal.name)
            put("description", goal.description)
            put("instruction", goal.naturalLanguageInstruction)
            put("enabled", goal.enabled)
            put("schedule_json", goal.schedule?.let { o -> JSONObject(mapSerde.encodeTrigger(it)).toString() } ?: JSONObject.NULL)
            put("triggers_json", JSONArray(goal.triggers.map { JSONObject(mapSerde.encodeTrigger(it)).toString() }).toString())
            put("allowed_domains", JSONArray(goal.allowedDomains).toString())
            put("blocked_domains", JSONArray(goal.blockedDomains).toString())
            put("allowed_actions", JSONArray(goal.allowedActions).toString())
            put("blocked_actions", JSONArray(goal.blockedActions).toString())
            put("confirmation_policy", goal.confirmationPolicy)
            put("notification_policy", goal.notificationPolicy)
            put("memory_policy", goal.memoryPolicy)
            put("max_steps", goal.maxSteps)
            put("max_runtime_ms", goal.maxRuntimeMs)
            put("max_retries", goal.maxRetries)
            put("conditions_json", "[]")
            put("last_run_at", goal.lastRunAt ?: JSONObject.NULL)
            put("next_run_at", goal.nextRunAt ?: JSONObject.NULL)
            put("status", goal.status.name)
            put("created_at", goal.createdAt)
            put("updated_at", System.currentTimeMillis())
        }
        db.writableDatabase.insertWithOnConflict(
            "goals", null, v, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun get(id: String): AgentGoal? = all().firstOrNull { it.id == id }

    fun all(): List<AgentGoal> {
        val out = mutableListOf<AgentGoal>()
        db.readableDatabase.query("goals", null, null, null, null, null, "created_at DESC").use { c ->
            while (c.moveToNext()) {
                val scheduleJson = c.getString(c.getColumnIndexOrThrow("schedule_json"))
                val triggersJson = c.getString(c.getColumnIndexOrThrow("triggers_json"))
                out.add(
                    AgentGoal(
                        id = c.getString(0),
                        name = c.getString(1),
                        description = c.getString(2) ?: "",
                        naturalLanguageInstruction = c.getString(3),
                        enabled = c.getInt(4) == 1,
                        schedule = scheduleJson?.let { mapSerde.decodeTrigger(JSONObject(it)) } as? TriggerSpec.Time,
                        triggers = JSONArray(triggersJson).let { arr ->
                            (0 until arr.length()).mapNotNull { i ->
                                mapSerde.decodeTrigger(arr.getJSONObject(i))
                            }
                        },
                        allowedDomains = jsonArrayToList(c.getString(8)),
                        blockedDomains = jsonArrayToList(c.getString(9)),
                        allowedActions = jsonArrayToList(c.getString(10)),
                        blockedActions = jsonArrayToList(c.getString(11)),
                        confirmationPolicy = c.getString(12),
                        notificationPolicy = c.getString(13),
                        memoryPolicy = c.getString(14),
                        maxSteps = c.getInt(15),
                        maxRuntimeMs = c.getLong(16),
                        maxRetries = c.getInt(17),
                        lastRunAt = if (c.isNull(19)) null else c.getLong(19),
                        nextRunAt = if (c.isNull(20)) null else c.getLong(20),
                        status = AgentStatus.valueOf(c.getString(21)),
                        createdAt = c.getLong(22),
                        updatedAt = c.getLong(23),
                    ),
                )
            }
        }
        return out
    }

    fun delete(id: String) {
        db.writableDatabase.delete("goals", "id=?", arrayOf(id))
    }

    fun setEnabled(id: String, enabled: Boolean) {
        get(id)?.let { save(it.copy(enabled = enabled, status = if (enabled) it.status else AgentStatus.DISABLED)) }
    }

    private fun jsonArrayToList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }
        }.getOrDefault(emptyList())
    }
}
