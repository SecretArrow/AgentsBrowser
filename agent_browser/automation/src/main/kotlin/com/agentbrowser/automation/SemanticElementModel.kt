// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.automation

import com.agentbrowser.agent.model.SemanticElement
import org.json.JSONArray
import org.json.JSONObject

/**
 * Normalizes raw DOM snapshots into the semantic element model (spec section 12).
 * Roles follow the spec list; confidence is derived from evidence quality.
 */
object SemanticElementModel {

    private val roleByTag = mapOf(
        "button" to "button", "a" to "link", "input" to "textbox", "textarea" to "textbox",
        "select" to "select", "checkbox" to "checkbox", "radio" to "radio",
        "dialog" to "dialog", "menu" to "menu", "article" to "article",
        "h1" to "heading", "h2" to "heading", "h3" to "heading", "h4" to "heading",
        "nav" to "navigation", "table" to "table", "img" to "image",
    )

    fun fromDomSnapshot(domJson: String): List<SemanticElement> {
        val out = mutableListOf<SemanticElement>()
        val root = runCatching { JSONObject(domJson) }.getOrNull() ?: return out
        walk(root, 0, out)
        return out
    }

    private fun walk(node: JSONObject, depth: Int, out: MutableList<SemanticElement>) {
        val tag = node.optString("tag").lowercase()
        val role = node.optString("role").ifBlank {
            roleByTag[tag] ?: if (depth > 0) "text" else "article"
        }
        val text = node.optString("text").ifBlank { null }
        val aria = node.optString("ariaLabel").ifBlank { null }
        val visible = node.optBoolean("visible", true)
        val enabled = node.optBoolean("enabled", true)
        val interactive = role in setOf("button", "link", "textbox", "checkbox", "radio", "select", "menu")

        if (visible && (interactive || !text.isNullOrBlank() || role in setOf("dialog", "navigation", "heading", "article", "table"))) {
            val boundsObj = node.optJSONObject("bounds")
            val confidence = when {
                aria != null && text != null && interactive -> 0.97f
                aria != null || text != null -> 0.85f
                else -> 0.6f
            }
            out.add(
                SemanticElement(
                    id = "element_${out.size + 1}",
                    role = role,
                    text = text,
                    ariaLabel = aria,
                    visible = visible,
                    enabled = enabled,
                    bounds = boundsObj?.let {
                        SemanticElement.Bounds(
                            it.optInt("x"), it.optInt("y"), it.optInt("width"), it.optInt("height"),
                        )
                    },
                    confidence = confidence,
                ),
            )
        }

        val children = node.optJSONArray("children")
        if (children != null) {
            for (i in 0 until children.length()) walk(children.getJSONObject(i), depth + 1, out)
        }
    }

    /** Compact JSON list used in prompts. */
    fun toPromptList(elements: List<SemanticElement>, limit: Int = 60): String {
        val arr = JSONArray()
        for (e in elements.take(limit)) {
            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("role", e.role)
                    .put("text", e.text ?: "")
                    .put("aria", e.ariaLabel ?: "")
                    .put("enabled", e.enabled),
            )
        }
        return arr.toString()
    }
}
