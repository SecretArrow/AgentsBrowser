// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.agent.runtime

import com.agentbrowser.agent.db.AgentDatabase
import com.agentbrowser.security.SensitiveDataFilter
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Audit trail (spec section 57): concise, user-visible action descriptions.
 * Never stores private chain-of-thought and redacts potential secrets.
 */
class AuditLogger(private val db: AgentDatabase) {

    private val filter = SensitiveDataFilter()
    private val buffer = ConcurrentHashMap<String, MutableList<String>>()

    fun log(runId: String, category: String, description: String, succeeded: Boolean? = null) {
        val safe = filter.redact(description).take(500)
        db.writableDatabase.insert(
            "audit", null,
            JSONObject()
                .put("id", UUID.randomUUID().toString())
                .put("run_id", runId)
                .put("at", System.currentTimeMillis())
                .put("description", safe)
                .put("category", category)
                .put("succeeded", succeeded)
                .let { cvFromJson(it) },
        )
        buffer.getOrPut(runId) { mutableListOf() }.add(safe)
    }

    fun timeline(runId: String): List<String> = buffer[runId]?.toList() ?: emptyList()

    fun persistTimeline(runId: String) {
        val entries = buffer.remove(runId) ?: return
        db.writableDatabase.insert(
            "events", null,
            cvFromJson(
                JSONObject()
                    .put("id", UUID.randomUUID().toString())
                    .put("run_id", runId)
                    .put("at", System.currentTimeMillis())
                    .put("kind", "timeline")
                    .put("detail", entries.joinToString("\n")),
            ),
        )
    }

    private fun cvFromJson(o: JSONObject) = android.content.ContentValues().apply {
        put("id", o.optString("id"))
        put("run_id", o.optString("run_id"))
        put("at", o.optLong("at"))
        put("description", o.optString("description", ""))
        put("category", o.optString("category", ""))
        put("kind", o.optString("kind", ""))
        put("detail", o.optString("detail", ""))
        val s = if (o.has("succeeded") && !o.isNull("succeeded")) (if (o.getBoolean("succeeded")) 1 else 0) else null
        if (s != null) put("succeeded", s)
    }
}
