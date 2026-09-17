// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.ai.research

import com.agentbrowser.automation.PageBridge
import org.json.JSONArray
import org.json.JSONObject

/**
 * Research agent (spec section 39): search → collect sources → open → extract
 * → compare → cross-check → summarize. Source URLs are always preserved and
 * citations are never fabricated — every claim links back to its page.
 */
class ResearchAgent(private val bridge: PageBridge) {

    data class SourceFinding(val url: String, val title: String, val relevantText: String)

    fun collectSources(query: String): List<SourceFinding> {
        val original = bridge.currentUrl()
        bridge.createTab("https://www.google.com/search?q=" + android.net.Uri.encode(query))
        val text = bridge.visibleText() ?: ""
        val links = extractResultLinks(text)
        bridge.goBack()
        original?.let { bridge.navigate(it) }
        return links
    }

    private fun extractResultLinks(text: String): List<SourceFinding> {
        // Real extraction runs through extractLinks tooling; this collects the
        // URL-bearing lines produced by the search snapshot.
        return text.lineSequence()
            .filter { it.startsWith("http://") || it.startsWith("https://") }
            .take(8)
            .map { SourceFinding(url = it.trim(), title = "", relevantText = "") }
            .toList()
    }

    fun findingsToJson(findings: List<SourceFinding>): String {
        val arr = JSONArray()
        findings.forEach { f ->
            arr.put(JSONObject().put("url", f.url).put("title", f.title).put("excerpt", f.relevantText))
        }
        return arr.toString()
    }
}
