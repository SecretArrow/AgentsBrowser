// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.automation

import com.agentbrowser.agent.model.ToolResult

/** Risk classification used for approval gating (spec section 26). */
enum class ActionRisk { SAFE, RISKY, FORBIDDEN }

/**
 * A controlled browser capability exposed to the agent (spec section 9).
 * Implementations bridge to Chromium WebContents and must return structured
 * [ToolResult]s.
 */
interface AgentTool {
    val name: String
    val description: String
    val risk: ActionRisk
    suspend fun execute(argsJson: String): ToolResult
}

/** Registry with permission-aware lookup. */
class ToolRegistry(private val permissionManager: PermissionManager? = null) {

    private val tools = linkedMapOf<String, AgentTool>()

    fun register(tool: AgentTool) {
        tools[tool.name] = tool
    }

    fun get(name: String): AgentTool? = tools[name]

    fun names(): List<String> = tools.keys.toList()

    fun schemaJson(): String {
        val arr = org.json.JSONArray()
        for (t in tools.values) {
            arr.put(
                org.json.JSONObject()
                    .put("name", t.name)
                    .put("description", t.description)
                    .put("risk", t.risk.name),
            )
        }
        return arr.toString()
    }

    /**
     * Executes a tool after checking goal restrictions and domain policy.
     * Returns a failed ToolResult rather than throwing when blocked.
     */
    suspend fun runForGoal(
        goal: com.agentbrowser.agent.model.AgentGoal?,
        domain: String?,
        name: String,
        argsJson: String,
    ): ToolResult {
        val tool = tools[name]
            ?: return ToolResult.fail(name, "unknown action: $name")
        if (goal != null) {
            if (goal.blockedActions.contains(name))
                return ToolResult.fail(name, "action '$name' is blocked for this goal")
            if (goal.allowedActions.isNotEmpty() && !goal.allowedActions.contains(name))
                return ToolResult.fail(name, "action '$name' is not allowed for this goal")
        }
        if (permissionManager != null && domain != null) {
            val verdict = permissionManager.checkAction(domain, name)
            if (verdict != null) return ToolResult.fail(name, verdict)
        }
        return try {
            tool.execute(argsJson)
        } catch (e: Exception) {
            ToolResult.fail(name, "action failed: ${e.message}")
        }
    }
}

/** Permission evaluation over domain policy (spec sections 27 and 64). */
interface PermissionManager {
    /** Returns null when allowed, otherwise a human-readable refusal reason. */
    fun checkAction(domain: String, action: String): String?
}
