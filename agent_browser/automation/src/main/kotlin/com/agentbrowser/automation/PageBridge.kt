// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.automation

import com.agentbrowser.agent.model.SemanticElement

/**
 * Bridge from agent tools to Chromium's browser process (spec section 10).
 * Implemented in agent_browser/browser by glue code that talks to
 * WebContents / NavigationController / TabModel. The AI never gets direct
 * WebContents access — everything flows through this controlled surface.
 */
interface PageBridge {
    fun currentUrl(): String?
    fun pageTitle(): String?
    fun visibleText(): String?
    fun evaluateDomSnapshot(): String?
    fun accessibilitySnapshot(): String?
    fun captureScreenshot(): String?

    fun navigate(url: String)
    fun goBack(): Boolean
    fun goForward(): Boolean
    fun reload()
    fun stopLoading()

    fun createTab(url: String): Int
    fun closeTab(tabId: Int): Boolean
    fun switchTab(tabId: Int): Boolean
    fun listTabs(): List<TabInfo>
    fun selectTabByIndex(index: Int): Boolean

    fun clickElementBySelector(selector: String): Boolean
    fun clickAt(x: Int, y: Int)
    fun typeText(text: String)
    fun clearInput()
    fun pressKey(keyCode: String)
    fun selectOption(selector: String, value: String)
    fun setCheckbox(selector: String, checked: Boolean)

    fun waitForElement(selector: String, timeoutMs: Long): Boolean
    fun waitForNavigation(timeoutMs: Long): Boolean

    fun download(url: String): String?
    fun upload(paths: List<String>): Boolean

    /** Custom User-Agent support: apply and query the active UA. */
    fun setUserAgent(ua: String)
    fun userAgent(): String?

    /** Vision-model element location fallback (spec section 13). */
    suspend fun visionLocate(query: String, screenshotBase64: String?): SemanticElement?

    data class TabInfo(val tabId: Int, val url: String?, val title: String?)
}
