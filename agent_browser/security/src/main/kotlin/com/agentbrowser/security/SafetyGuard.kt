// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.security

import com.agentbrowser.ai.guard.PromptGuard

/**
 * Hard security boundaries that no user policy or goal can override
 * (spec sections 25, 40, 41, 63).
 */
class SafetyGuard(private val state: com.agentbrowser.agent.state.StateManager) {

    /** Actions that are never exposed to the AI, regardless of configuration. */
    val forbiddenActions = setOf(
        "extractCookies", "extractPasswords", "extractApiKeys",
        "disableSandbox", "bypassCaptcha", "evadeAntiBot",
        "bypassAuthentication", "readKeystore", "shellExec",
    )

    /** Emergency stop (spec section 29): cancels active execution, keeps state. */
    @Volatile var emergencyStop = false
        private set

    fun triggerEmergencyStop() {
        emergencyStop = true
        state.markEmergencyStopped()
    }

    fun clearEmergencyStop() {
        emergencyStop = false
    }

    sealed class Verdict {
        data object Allowed : Verdict()
        data class Blocked(val reason: String) : Verdict()
    }

    /** Final gate before any tool execution. */
    fun checkBeforeAction(action: String, argsJson: String, webpageText: String?): Verdict {
        if (emergencyStop) return Verdict.Blocked("emergency stop is active")
        if (action in forbiddenActions) return Verdict.Blocked("action '$action' violates hard security boundaries")

        // Spec section 40: webpage content is untrusted and can never command the agent.
        if (webpageText != null && PromptGuard.looksLikeInjection(webpageText)) {
            return Verdict.Blocked(
                "webpage content attempted to override agent instructions; action blocked and logged",
            )
        }

        // Spec section 41: never transmit secrets, even if requested indirectly.
        val filter = SensitiveDataFilter()
        if (filter.containsSensitiveData(argsJson)) {
            return Verdict.Blocked("arguments appear to contain credentials; refusing to proceed")
        }
        return Verdict.Allowed
    }
}
