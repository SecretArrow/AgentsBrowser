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
            if (goal.schedule != null) {
                put("schedule_json", TriggerSerde.encodeTrigger(goal.schedule!!))
            } else {
                putNull("schedule_json")
            }
            put("triggers_json", JSONArray(goal.triggers.map { TriggerSerde.encodeTrigger(it) }).toString())
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
            if (goal.lastRunAt != null) put("last_run_at", goal.lastRunAt) else putNull("last_run_at")
            if (goal.nextRunAt != null) put("next_run_at", goal.nextRunAt) else putNull("next_run_at")
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
                fun s(col: String): String? = c.getString(c.getColumnIndexOrThrow(col))
                fun n(col: String): Int = c.getInt(c.getColumnIndexOrThrow(col))
                fun l(col: String): Long = c.getLong(c.getColumnIndexOrThrow(col))
                fun lngOrNull(col: String): Long? = if (c.isNull(c.getColumnIndexOrThrow(col))) null else c.getLong(c.getColumnIndexOrThrow(col))
                fun list(col: String): List<String> = jsonArrayToList(s(col))
                out.add(
                    AgentGoal(
                        id = s("id")!!,
                        name = s("name")!!,
                        description = s("description") ?: "",
                        naturalLanguageInstruction = s("instruction")!!,
                        enabled = n("enabled") == 1,
                        schedule = s("schedule_json")?.let {
                            TriggerSerde.decodeTrigger(JSONObject(it)) as? TriggerSpec.Time
                        },
                        triggers = s("triggers_json")?.let { json ->
                            JSONArray(json).let { arr ->
                                (0 until arr.length()).mapNotNull { i -> TriggerSerde.decodeTrigger(arr.getJSONObject(i)) }
                            }
                        } ?: emptyList(),
                        allowedDomains = list("allowed_domains"),
                        blockedDomains = list("blocked_domains"),
                        allowedActions = list("allowed_actions"),
                        blockedActions = list("blocked_actions"),
                        confirmationPolicy = s("confirmation_policy") ?: "risky_only",
                        notificationPolicy = s("notification_policy") ?: "on_failure",
                        memoryPolicy = s("memory_policy") ?: "standard",
                        maxSteps = n("max_steps"),
                        maxRuntimeMs = l("max_runtime_ms"),
                        maxRetries = n("max_retries"),
                        lastRunAt = lngOrNull("last_run_at"),
                        nextRunAt = lngOrNull("next_run_at"),
                        status = AgentStatus.valueOf(s("status") ?: "DISABLED"),
                        createdAt = l("created_at"),
                        updatedAt = l("updated_at"),
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
