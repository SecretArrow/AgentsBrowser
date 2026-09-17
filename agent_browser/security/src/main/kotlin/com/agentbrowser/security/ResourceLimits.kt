// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.security

import com.agentbrowser.agent.model.AgentGoal

/**
 * Per-goal resource limits (spec section 54). When any limit is exceeded the
 * caller must STOP, SAVE STATE, and NOTIFY the user.
 */
class ResourceLimits {

    data class Usage(
        var steps: Int = 0,
        var actions: Int = 0,
        var downloads: Int = 0,
        var notifications: Int = 0,
        var retries: Int = 0,
        val startedAtMs: Long = System.currentTimeMillis(),
    )

    private val runs = java.util.concurrent.ConcurrentHashMap<String, Usage>()

    fun beginRun(runId: String) {
        runs[runId] = Usage()
    }

    fun usage(runId: String): Usage = runs.getOrPut(runId) { Usage() }

    fun endRun(runId: String) {
        runs.remove(runId)
    }

    /** Returns the first exceeded limit as a human-readable reason, or null. */
    fun checkExceeded(runId: String, goal: AgentGoal): String? {
        val u = usage(runId)
        val runtimeMs = System.currentTimeMillis() - u.startedAtMs
        return when {
            u.steps >= goal.maxSteps -> "max steps reached (${goal.maxSteps})"
            runtimeMs >= goal.maxRuntimeMs -> "max runtime reached (${goal.maxRuntimeMs} ms)"
            u.retries >= goal.maxRetries -> "max retries reached (${goal.maxRetries})"
            u.downloads >= 10 -> "max downloads reached (10)"
            u.notifications >= 20 -> "max notifications reached (20)"
            else -> null
        }
    }

    fun recordStep(runId: String) { usage(runId).steps++ }
    fun recordAction(runId: String) { usage(runId).actions++ }
    fun recordDownload(runId: String) { usage(runId).downloads++ }
    fun recordNotification(runId: String) { usage(runId).notifications++ }
    fun recordRetry(runId: String) { usage(runId).retries++ }
}
