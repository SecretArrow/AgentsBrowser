// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.agent.model

import kotlinx.serialization.Serializable

/** Agent operating mode. Spec section 28. */
enum class AgentMode { MANUAL, COPILOT, AUTONOMOUS }

/** Lifecycle states for a goal or a run. Spec section 33. */
enum class AgentStatus {
    RUNNING, PAUSED, WAITING_APPROVAL, WAITING_AUTH,
    WAITING_NETWORK, FAILED, COMPLETED, DISABLED
}

/** Scheduling trigger kinds. Spec section 21. */
@Serializable
sealed class TriggerSpec {
    abstract val id: String
    abstract val enabled: Boolean

    @Serializable
    data class Time(
        override val id: String,
        override val enabled: Boolean = true,
        /** once | every_n_minutes | hourly | daily | weekly | weekdays | selected_days | monthly */
        val kind: String,
        val intervalMinutes: Long? = null,
        val hourOfDay: Int? = null,
        val minuteOfHour: Int? = null,
        /** ISO day numbers 1=Mon..7=Sun for selected_days */
        val days: List<Int> = emptyList(),
        val dayOfMonth: Int? = null,
    ) : TriggerSpec()

    @Serializable
    data class Browser(
        override val id: String,
        override val enabled: Boolean = true,
        /** browser_start | tab_opened | url_loaded | url_matched | domain_visited | download_completed */
        val kind: String,
        val pattern: String? = null,
    ) : TriggerSpec()

    @Serializable
    data class Content(
        override val id: String,
        override val enabled: Boolean = true,
        /** text_appears | keyword_appears | page_changes | element_changes | price_changes | new_item_appears */
        val kind: String,
        val selector: String? = null,
        val keyword: String? = null,
        val threshold: Double? = null,
    ) : TriggerSpec()

    @Serializable
    data class State(
        override val id: String,
        override val enabled: Boolean = true,
        /** network_available | charging | app_foreground */
        val kind: String,
    ) : TriggerSpec()
}

/** Structured IF/THEN condition. Spec section 24. */
@Serializable
data class Condition(
    val kind: String,
    val selector: String? = null,
    val keyword: String? = null,
    val operator: String? = null,
    val value: String? = null,
)

/** A persistent autonomous goal. Spec section 20. */
@Serializable
data class AgentGoal(
    val id: String,
    val name: String,
    val description: String = "",
    val naturalLanguageInstruction: String,
    val enabled: Boolean = true,
    val schedule: TriggerSpec.Time? = null,
    val triggers: List<TriggerSpec> = emptyList(),
    val allowedDomains: List<String> = emptyList(),
    val blockedDomains: List<String> = emptyList(),
    val allowedActions: List<String> = emptyList(),
    val blockedActions: List<String> = emptyList(),
    /** always | risky_only | never */
    val confirmationPolicy: String = "risky_only",
    /** always | on_failure | never */
    val notificationPolicy: String = "on_failure",
    /** none | minimal | standard */
    val memoryPolicy: String = "standard",
    val maxSteps: Int = 50,
    val maxRuntimeMs: Long = 15 * 60_000L,
    val maxRetries: Int = 3,
    val conditions: List<Condition> = emptyList(),
    val lastRunAt: Long? = null,
    val nextRunAt: Long? = null,
    val status: AgentStatus = AgentStatus.DISABLED,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

/** One executed agent run. */
@Serializable
data class AgentRun(
    val id: String,
    val goalId: String,
    val status: AgentStatus,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val stepsUsed: Int = 0,
    val exitReason: String? = null,
    val summary: String? = null,
)

/** A single agent step (plan → act → observe). */
@Serializable
data class AgentStep(
    val id: String,
    val runId: String,
    val index: Int,
    val thought: String? = null,
    val action: String? = null,
    val actionArgsJson: String? = null,
    val observationJson: String? = null,
    val succeeded: Boolean? = null,
    val at: Long,
)

/** Concise, user-visible audit entry (never raw chain-of-thought). Spec section 57. */
@Serializable
data class AuditEntry(
    val id: String,
    val runId: String,
    val at: Long,
    val description: String,
    val category: String,
    val succeeded: Boolean? = null,
)

/** Layered memory store. Spec section 31. */
@Serializable
data class AgentMemory(
    val id: String,
    /** global | goal | site | run | temp */
    val scope: String,
    val scopeKey: String,
    val key: String,
    val value: String,
    val updatedAt: Long,
    val expiresAt: Long? = null,
)

/** Per-domain agent permission. Spec sections 27 and 64. */
@Serializable
data class DomainPermission(
    val domain: String,
    /** allowed | blocked | approval_required */
    val read: String = "allowed",
    val navigate: String = "allowed",
    val formFill: String = "allowed",
    val submit: String = "approval_required",
    val download: String = "approval_required",
    val upload: String = "approval_required",
)

/** Actions with external consequences require approval. Spec section 26. */
@Serializable
data class ApprovalRequest(
    val id: String,
    val runId: String,
    val goalId: String,
    val domain: String,
    val action: String,
    val description: String,
    val payloadJson: String? = null,
    val createdAt: Long,
    /** pending | approved | rejected | expired */
    val status: String = "pending",
)

/** Security or policy event, safe to log (no secrets). Spec section 41. */
@Serializable
data class SecurityEvent(
    val id: String,
    val runId: String?,
    val at: Long,
    /** prompt_injection_blocked | sensitive_data_blocked | domain_blocked | loop_detected | limit_exceeded */
    val kind: String,
    val detail: String,
)

/** Normalized semantic element. Spec section 12. */
@Serializable
data class SemanticElement(
    val id: String,
    val role: String,
    val text: String? = null,
    val ariaLabel: String? = null,
    val visible: Boolean = true,
    val enabled: Boolean = true,
    val bounds: Bounds? = null,
    val confidence: Float = 1.0f,
) {
    @Serializable
    data class Bounds(val x: Int, val y: Int, val width: Int, val height: Int)
}

/** Structured tool result. Spec section 9. */
@Serializable
data class ToolResult(
    val success: Boolean,
    val action: String,
    val target: String? = null,
    val pageChanged: Boolean = false,
    val url: String? = null,
    val title: String? = null,
    val dataJson: String? = null,
    val error: String? = null,
) {
    companion object {
        fun ok(action: String, build: Builder.() -> Unit = {}) = Builder(action).apply(build).build(true)
        fun fail(action: String, error: String) = Builder(action).apply { this.error = error }.build(false)
    }

    class Builder(internal val action: String) {
        var target: String? = null
        var pageChanged: Boolean = false
        var url: String? = null
        var title: String? = null
        var dataJson: String? = null
        var error: String? = null
        internal fun build(success: Boolean) = ToolResult(
            success = success, action = action, target = target, pageChanged = pageChanged,
            url = url, title = title, dataJson = dataJson, error = error,
        )
    }
}
