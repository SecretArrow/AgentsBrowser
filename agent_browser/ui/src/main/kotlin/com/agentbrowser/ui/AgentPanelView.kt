// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.agentbrowser.ui.databinding.AgentPanelBinding

/**
 * Page Assistant panel (spec sections 38, 45): Ask Agent input plus quick
 * actions Summarize / Explain / Translate / Find / Extract / Compare /
 * Research / Act / Automate.
 */
class AgentPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    interface Callbacks {
        fun onQuickAction(action: QuickAction)
        fun onAskSubmitted(question: String)
    }

    enum class QuickAction { SUMMARIZE, EXPLAIN, TRANSLATE, FIND, EXTRACT, COMPARE, RESEARCH, ACT, AUTOMATE }

    var callbacks: Callbacks? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.agent_panel, this, true)
        orientation = VERTICAL
        bindQuickActions()
    }

    private fun bindQuickActions() {
        val map = mapOf(
            R.id.action_summarize to QuickAction.SUMMARIZE,
            R.id.action_explain to QuickAction.EXPLAIN,
            R.id.action_translate to QuickAction.TRANSLATE,
            R.id.action_find to QuickAction.FIND,
            R.id.action_extract to QuickAction.EXTRACT,
            R.id.action_compare to QuickAction.COMPARE,
            R.id.action_research to QuickAction.RESEARCH,
            R.id.action_act to QuickAction.ACT,
            R.id.action_automate to QuickAction.AUTOMATE,
        )
        for ((id, action) in map) {
            findViewById<android.view.View>(id)?.setOnClickListener { callbacks?.onQuickAction(action) }
        }
        findViewById<android.widget.EditText>(R.id.ask_input)?.let { input ->
            findViewById<android.view.View>(R.id.ask_send)?.setOnClickListener {
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) {
                    callbacks?.onAskSubmitted(text)
                    input.setText("")
                }
            }
        }
    }

    fun setCurrentPage(url: String?, title: String?) {
        findViewById<android.widget.TextView>(R.id.current_page)?.text =
            title?.takeIf { it.isNotBlank() } ?: url ?: "Current page"
    }
}
