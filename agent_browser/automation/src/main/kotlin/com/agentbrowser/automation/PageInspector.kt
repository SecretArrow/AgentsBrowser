// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.automation

import com.agentbrowser.agent.model.SemanticElement

/**
 * Layered page understanding (spec sections 11, 13): structured info first,
 * visual interpretation as fallback. [PageBridge] is implemented by the
 * Chromium browser layer.
 */
class PageInspector(private val bridge: PageBridge) {

    data class PageFacts(
        val url: String?,
        val title: String?,
        val visibleText: String?,
        val elements: List<SemanticElement>,
        val domJson: String?,
        val a11yJson: String?,
        val screenshotBase64: String?,
    )

    /** Observes the page through the highest-confidence available layers. */
    suspend fun observe(includeScreenshot: Boolean = false): PageFacts {
        val dom = bridge.evaluateDomSnapshot()
        val elements = dom?.let { SemanticElementModel.fromDomSnapshot(it) } ?: emptyList()
        return PageFacts(
            url = bridge.currentUrl(),
            title = bridge.pageTitle(),
            visibleText = bridge.visibleText(),
            elements = elements,
            domJson = dom,
            a11yJson = bridge.accessibilitySnapshot(),
            screenshotBase64 = if (includeScreenshot) bridge.captureScreenshot() else null,
        )
    }

    /** Visual fallback when DOM/a11y cannot identify a target (spec section 13). */
    suspend fun resolveTarget(query: String): SemanticElement? {
        val facts = observe(includeScreenshot = false)
        val q = query.trim().lowercase()
        facts.elements
            .filter { it.visible && it.enabled }
            .firstOrNull { it.ariaLabel?.lowercase()?.contains(q) == true || it.text?.lowercase()?.contains(q) == true }
            ?.let { return it }
        // Fallback: vision model over the screenshot (implemented in the browser layer).
        return bridge.visionLocate(query, facts.screenshotBase64 ?: bridge.captureScreenshot())
    }
}
