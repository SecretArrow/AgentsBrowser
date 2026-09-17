// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.agent.runtime

import com.agentbrowser.agent.db.AgentDatabase
import com.agentbrowser.agent.model.AgentStatus
import com.agentbrowser.agent.model.ApprovalRequest
import com.agentbrowser.security.NotificationManagerAgent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import org.json.JSONObject

/**
 * Human-in-the-loop approvals (spec section 26). Risky actions suspend the run
 * with status WAITING_APPROVAL until the user approves, edits, or rejects.
 */
class ApprovalManager(
    private val db: AgentDatabase,
    private val notifications: NotificationManagerAgent,
) {

    private val pending = ConcurrentHashMap<String, CompletableDeferred<String>>()

    /** Suspends until the user decides. Returns "approved" or "rejected". */
    suspend fun requestApproval(
        runId: String,
        goalId: String,
        goalName: String,
        domain: String,
        action: String,
        description: String,
        payloadJson: String?,
    ): String {
        val id = UUID.randomUUID().toString()
        db.writableDatabase.insert(
            "approvals", null,
            JSONObject()
                .put("id", id)
                .put("run_id", runId)
                .put("goal_id", goalId)
                .put("domain", domain)
                .put("action", action)
                .put("description", description)
                .put("payload", payloadJson ?: JSONObject.NULL)
                .put("created_at", System.currentTimeMillis())
                .put("status", "pending")
                .let { cv(it) },
        )
        notifications.notifyApprovalNeeded(goalName, "$action on $domain")
        val deferred = CompletableDeferred<String>()
        pending[id] = deferred
        return try {
            deferred.await()
        } finally {
            pending.remove(id)
        }
    }

    fun decide(approvalId: String, decision: String) {
        db.writableDatabase.update(
            "approvals",
            android.content.ContentValues().apply { put("status", decision) },
            "id=?", arrayOf(approvalId),
        )
        pending[approvalId]?.complete(decision)
    }

    fun pendingApprovals(): List<ApprovalRequest> {
        val out = mutableListOf<ApprovalRequest>()
        db.readableDatabase.query(
            "approvals", null, "status=?", arrayOf("pending"), null, null, "created_at DESC",
        ).use { c ->
            while (c.moveToNext()) {
                out.add(
                    ApprovalRequest(
                        id = c.getString(0), runId = c.getString(1), goalId = c.getString(2),
                        domain = c.getString(3), action = c.getString(4),
                        description = c.getString(5), payloadJson = c.optStringOrNull(6),
                        createdAt = c.getLong(7), status = c.getString(8),
                    ),
                )
            }
        }
        return out
    }

    fun statusForRun(runId: String): AgentStatus? =
        if (pendingApprovals().any { it.runId == runId }) AgentStatus.WAITING_APPROVAL else null

    private fun cv(o: JSONObject) = android.content.ContentValues().apply {
        put("id", o.getString("id"))
        put("run_id", o.getString("run_id"))
        put("goal_id", o.getString("goal_id"))
        put("domain", o.getString("domain"))
        put("action", o.getString("action"))
        put("description", o.getString("description"))
        put("payload", o.optString("payload", ""))
        put("created_at", o.getLong("created_at"))
        put("status", o.getString("status"))
    }

    private fun android.database.Cursor.optStringOrNull(index: Int): String? =
        if (isNull(index)) null else getString(index)
}
