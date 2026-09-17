// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.agent.state

import android.content.Context
import com.agentbrowser.agent.model.AgentRun
import com.agentbrowser.agent.model.AgentStatus
import org.json.JSONObject

/**
 * Persisted run state (spec section 32). After process restart the agent
 * restores state, re-observes the browser, validates, and only then resumes —
 * it never blindly replays clicks.
 */
class StateManager(private val context: Context) {

    private val prefs by lazy {
        context.getSharedPreferences("agent_state", Context.MODE_PRIVATE)
    }

    data class RunState(
        val run: AgentRun,
        val currentStepIndex: Int,
        val lastDescription: String,
        val waitingFor: String?,
        val goalStateJson: String,
    )

    @Synchronized
    fun save(state: RunState) {
        val obj = JSONObject().apply {
            put("runId", state.run.id)
            put("goalId", state.run.goalId)
            put("status", state.run.status.name)
            put("startedAt", state.run.startedAt)
            put("stepsUsed", state.run.stepsUsed)
            put("currentStepIndex", state.currentStepIndex)
            put("lastDescription", state.lastDescription)
            put("waitingFor", state.waitingFor ?: "")
            put("goalState", state.goalStateJson)
        }
        prefs.edit().putString(state.run.id, obj.toString()).apply()
    }

    @Synchronized
    fun load(runId: String): RunState? {
        val raw = prefs.getString(runId, null) ?: return null
        return runCatching { fromJson(JSONObject(raw)) }.getOrNull()
    }

    @Synchronized
    fun allActive(): List<RunState> {
        val out = mutableListOf<RunState>()
        for ((key, raw) in prefs.all) {
            if (key == "lastEmergencyStopAt" || raw !is String) continue
            runCatching { out.add(fromJson(JSONObject(raw))) }
        }
        return out.filter {
            it.run.status in setOf(
                AgentStatus.RUNNING, AgentStatus.PAUSED,
                AgentStatus.WAITING_APPROVAL, AgentStatus.WAITING_AUTH,
                AgentStatus.WAITING_NETWORK,
            )
        }
    }

    @Synchronized
    fun clear(runId: String) = prefs.edit().remove(runId).apply()

    @Synchronized
    fun markEmergencyStopped() {
        prefs.edit().putLong("lastEmergencyStopAt", System.currentTimeMillis()).apply()
    }

    fun wasEmergencyStopped(): Boolean = prefs.contains("lastEmergencyStopAt")

    private fun fromJson(o: JSONObject) = RunState(
        run = AgentRun(
            id = o.getString("runId"),
            goalId = o.getString("goalId"),
            status = AgentStatus.valueOf(o.getString("status")),
            startedAt = o.getLong("startedAt"),
            stepsUsed = o.optInt("stepsUsed", 0),
        ),
        currentStepIndex = o.optInt("currentStepIndex", 0),
        lastDescription = o.optString("lastDescription"),
        waitingFor = o.optString("waitingFor").ifEmpty { null },
        goalStateJson = o.optString("goalState", "{}"),
    )
}
