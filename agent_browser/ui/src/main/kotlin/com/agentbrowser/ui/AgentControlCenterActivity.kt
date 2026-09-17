// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.ui

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.agentbrowser.agent.db.AgentDatabase
import com.agentbrowser.agent.model.AgentStatus
import com.agentbrowser.agent.state.StateManager
import com.agentbrowser.security.SafetyGuard

/**
 * Agent Control Center (spec section 35): overview counters for
 * Running / Scheduled / Needs Approval / Paused / Failed, quick access to
 * Goals, Approvals, History, Memory, Permissions, AI Providers, Logs, and an
 * always-visible EMERGENCY STOP (spec section 29).
 */
class AgentControlCenterActivity : Activity() {

    private lateinit var db: AgentDatabase
    private lateinit var safety: com.agentbrowser.security.SafetyGuard
    private lateinit var state: com.agentbrowser.agent.state.StateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = AgentDatabase.get(this)
        state = StateManager(this)
        safety = SafetyGuard(state)

        val pad = (resources.displayMetrics.density * 16).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        val title = TextView(this).apply {
            text = getString(R.string.control_center_title)
            textSize = 20f
        }
        root.addView(title)

        val counters = TextView(this).apply { textSize = 16f }
        root.addView(counters)

        val stopAll = Button(this).apply { text = getString(R.string.stop_all_agents) }
        root.addView(stopAll)

        setContentView(root)

        stopAll.setOnClickListener {
            safety.triggerEmergencyStop()
            render(counters)
        }
        render(counters)
    }

    private fun render(counters: TextView) {
        val goals = db.writableDatabase
        val runs = mutableListOf<Pair<String, Int>>()
        goals.query("runs", arrayOf("status"), null, null, null, null, null).use { c ->
            while (c.moveToNext()) runs.add(c.getString(0) to 1)
        }
        val approvals = db.readableDatabase.query(
            "approvals", arrayOf("COUNT(*)"), "status=?", arrayOf("pending"), null, null, null,
        ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

        val running = runs.count { it.first == AgentStatus.RUNNING.name }
        val paused = runs.count { it.first == AgentStatus.PAUSED.name }
        val failed = runs.count { it.first == AgentStatus.FAILED.name }
        val scheduled = db.readableDatabase.query(
            "goals", arrayOf("COUNT(*)"), "enabled=1", null, null, null, null,
        ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

        counters.text = """
            Running: $running
            Scheduled: $scheduled
            Needs Approval: $approvals
            Paused: $paused
            Failed: $failed
        """.trimIndent()
    }
}
