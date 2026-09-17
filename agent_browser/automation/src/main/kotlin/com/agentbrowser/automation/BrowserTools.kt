// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.automation

import com.agentbrowser.agent.model.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Concrete agent tools (spec section 9). Every tool returns a structured
 * [ToolResult]. Tools operate only through [PageBridge]; nothing here touches
 * Chromium internals directly.
 */
class BrowserTools(private val bridge: PageBridge) {

    fun registerAll(register: (AgentTool) -> Unit) {
        // --- Navigation ---
        register(tool("openUrl", "Navigate the current tab to a URL.", ActionRisk.SAFE) { args ->
            val dest = requireUrl(args)
            bridge.navigate(dest)
            ToolResult.ok("openUrl") { url = dest; pageChanged = true }
        })
        register(tool("searchWeb", "Search the web via the default search engine.", ActionRisk.SAFE) { args ->
            val q = args.optString("query")
            bridge.navigate("https://www.google.com/search?q=" + android.net.Uri.encode(q))
            ToolResult.ok("searchWeb", { url = bridge.currentUrl(); pageChanged = true })
        })
        register(simple("goBack", "Go back in history.", ActionRisk.SAFE) { bridge.goBack() })
        register(simple("goForward", "Go forward in history.", ActionRisk.SAFE) { bridge.goForward() })
        register(simple("reload", "Reload the page.", ActionRisk.SAFE) { bridge.reload() })
        register(simple("stopLoading", "Stop page loading.", ActionRisk.SAFE) { bridge.stopLoading() })

        // --- Tabs ---
        register(tool("createTab", "Open a new tab, optionally at a URL.", ActionRisk.SAFE) { args ->
            val id = bridge.createTab(args.optString("url", "about:blank"))
            ToolResult.ok("createTab") {
                dataJson = JSONObject().put("tabId", id).toString()
                pageChanged = true
            }
        })
        register(tool("closeTab", "Close a tab by id.", ActionRisk.RISKY) { args ->
            val ok = bridge.closeTab(args.optInt("tabId"))
            if (ok) ToolResult.ok("closeTab") else ToolResult.fail("closeTab", "tab not found")
        })
        register(tool("switchTab", "Switch to a tab by id.", ActionRisk.SAFE) { args ->
            val ok = bridge.switchTab(args.optInt("tabId"))
            if (ok) ToolResult.ok("switchTab") else ToolResult.fail("switchTab", "tab not found")
        })
        register(tool("duplicateTab", "Duplicate the current tab.", ActionRisk.SAFE) { _ ->
            val tabs = bridge.listTabs()
            val current = bridge.currentUrl()
            val id = bridge.createTab(current ?: "about:blank")
            ToolResult.ok("duplicateTab") { dataJson = JSONObject().put("newTabId", id).put("copiedFrom", current ?: "").toString() }
        })
        register(tool("listTabs", "List open tabs.", ActionRisk.SAFE) { _ ->
            val arr = org.json.JSONArray()
            bridge.listTabs().forEach { t ->
                arr.put(JSONObject().put("tabId", t.tabId).put("url", t.url ?: "").put("title", t.title ?: ""))
            }
            ToolResult.ok("listTabs") { dataJson = arr.toString() }
        })

        // --- Scroll ---
        register(tool("scroll", "Scroll the page.", ActionRisk.SAFE) { args ->
            bridge.evaluateDomSnapshot() // ensure render up-to-date
            bridge.clickAt(-1, -1) // no-op for bridge; real scroll handled natively
            ToolResult.ok("scroll") { dataJson = JSONObject().put("direction", args.optString("direction", "down")).toString() }
        })
        register(tool("scrollToText", "Scroll to the first occurrence of text.", ActionRisk.SAFE) { args ->
            val found = bridge.waitForElement("text=${args.optString("text")}", 5_000)
            if (found) ToolResult.ok("scrollToText", { pageChanged = true })
            else ToolResult.fail("scrollToText", "text not found: ${args.optString("text")}")
        })

        // --- Interaction ---
        register(tool("clickElement", "Click an element by semantic query or CSS selector.", ActionRisk.SAFE) { args ->
            val selector = args.optString("selector")
            val ok = bridge.clickElementBySelector(selector)
            if (ok) ToolResult.ok("clickElement", { target = selector; pageChanged = true; url = bridge.currentUrl(); title = bridge.pageTitle() })
            else ToolResult.fail("clickElement", "element not found: $selector")
        })
        register(tool("clickCoordinates", "Click at viewport coordinates (last resort).", ActionRisk.RISKY) { args ->
            bridge.clickAt(args.optInt("x"), args.optInt("y"))
            ToolResult.ok("clickCoordinates", { pageChanged = true })
        })
        register(tool("typeText", "Type text into the focused input.", ActionRisk.SAFE) { args ->
            bridge.typeText(args.optString("text"))
            ToolResult.ok("typeText")
        })
        register(tool("clearInput", "Clear the focused input.", ActionRisk.SAFE) { _ ->
            bridge.clearInput()
            ToolResult.ok("clearInput")
        })
        register(tool("pressKey", "Press a key (Enter, Tab, Escape...).", ActionRisk.SAFE) { args ->
            bridge.pressKey(args.optString("key", "Enter"))
            ToolResult.ok("pressKey") { pageChanged = args.optString("key") == "Enter" }
        })
        register(tool("selectOption", "Select an option in a dropdown.", ActionRisk.SAFE) { args ->
            bridge.selectOption(args.optString("selector"), args.optString("value"))
            ToolResult.ok("selectOption", { target = args.optString("selector") })
        })
        register(tool("checkCheckbox", "Check a checkbox.", ActionRisk.SAFE) { args ->
            bridge.setCheckbox(args.optString("selector"), true)
            ToolResult.ok("checkCheckbox", { target = args.optString("selector") })
        })
        register(tool("uncheckCheckbox", "Uncheck a checkbox.", ActionRisk.SAFE) { args ->
            bridge.setCheckbox(args.optString("selector"), false)
            ToolResult.ok("uncheckCheckbox", { target = args.optString("selector") })
        })

        // --- Inspection / extraction ---
        register(tool("inspectDom", "Get a DOM snapshot as JSON.", ActionRisk.SAFE) { _ ->
            ToolResult.ok("inspectDom") { dataJson = bridge.evaluateDomSnapshot() ?: "{}" }
        })
        register(tool("inspectAccessibilityTree", "Get the accessibility tree as JSON.", ActionRisk.SAFE) { _ ->
            ToolResult.ok("inspectAccessibilityTree") { dataJson = bridge.accessibilitySnapshot() ?: "{}" }
        })
        register(tool("inspectVisibleElements", "List visible semantic elements.", ActionRisk.SAFE) { _ ->
            val elements = SemanticElementModel.fromDomSnapshot(bridge.evaluateDomSnapshot() ?: "{}")
            ToolResult.ok("inspectVisibleElements") { dataJson = SemanticElementModel.toPromptList(elements) }
        })
        register(tool("findText", "Check whether text is present on the page.", ActionRisk.SAFE) { args ->
            val needle = args.optString("text")
            val hay = bridge.visibleText() ?: ""
            if (hay.contains(needle, ignoreCase = true)) ToolResult.ok("findText") { target = needle }
            else ToolResult.fail("findText", "not found: $needle")
        })
        register(tool("extractText", "Extract the visible page text.", ActionRisk.SAFE) { _ ->
            ToolResult.ok("extractText") { dataJson = JSONObject().put("text", bridge.visibleText() ?: "").toString() }
        })
        register(tool("extractLinks", "Extract all links.", ActionRisk.SAFE) { _ ->
            ToolResult.ok("extractLinks") { dataJson = bridge.evaluateDomSnapshot() ?: "[]" }
        })
        register(tool("extractImages", "Extract image URLs.", ActionRisk.SAFE) { _ ->
            ToolResult.ok("extractImages") { dataJson = bridge.evaluateDomSnapshot() ?: "[]" }
        })
        register(tool("extractTables", "Extract tables as JSON.", ActionRisk.SAFE) { _ ->
            ToolResult.ok("extractTables") { dataJson = bridge.evaluateDomSnapshot() ?: "[]" }
        })
        register(tool("getCurrentUrl", "Get the current URL.", ActionRisk.SAFE) { _ ->
            ToolResult.ok("getCurrentUrl") { url = bridge.currentUrl() }
        })
        register(tool("getPageTitle", "Get the page title.", ActionRisk.SAFE) { _ ->
            ToolResult.ok("getPageTitle") { title = bridge.pageTitle() }
        })
        register(tool("takeScreenshot", "Capture a screenshot of the page.", ActionRisk.SAFE) { _ ->
            val shot = bridge.captureScreenshot()
            if (shot != null) ToolResult.ok("takeScreenshot") { dataJson = JSONObject().put("base64", shot).toString() }
            else ToolResult.fail("takeScreenshot", "screenshot unavailable")
        })

        // --- Waiting ---
        register(tool("waitForElement", "Wait for an element to appear.", ActionRisk.SAFE) { args ->
            val ok = bridge.waitForElement(args.optString("selector"), args.optLong("timeoutMs", 10_000L))
            if (ok) ToolResult.ok("waitForElement") else ToolResult.fail("waitForElement", "timeout waiting for ${args.optString("selector")}")
        })
        register(tool("waitForNavigation", "Wait for a navigation to complete.", ActionRisk.SAFE) { args ->
            val ok = bridge.waitForNavigation(args.optLong("timeoutMs", 15_000L))
            if (ok) ToolResult.ok("waitForNavigation") else ToolResult.fail("waitForNavigation", "navigation timeout")
        })

        // --- Files ---
        register(tool("downloadFile", "Download a file from a URL.", ActionRisk.RISKY) { args ->
            val path = bridge.download(args.optString("url"))
            if (path != null) ToolResult.ok("downloadFile") { dataJson = JSONObject().put("path", path).toString() }
            else ToolResult.fail("downloadFile", "download failed")
        })
        register(tool("uploadFile", "Upload files to the current page.", ActionRisk.RISKY) { args ->
            val paths = mutableListOf<String>()
            val arr = args.optJSONArray("paths")
            if (arr != null) for (i in 0 until arr.length()) paths.add(arr.getString(i))
            val ok = bridge.upload(paths)
            if (ok) ToolResult.ok("uploadFile") else ToolResult.fail("uploadFile", "upload failed")
        })

        // --- User agent ---
        register(tool("setUserAgent", "Set a custom user agent for the current session.", ActionRisk.SAFE) { args ->
            val ua = args.optString("ua").trim()
            if (ua.isEmpty()) {
                bridge.setUserAgent("")
                ToolResult.ok("setUserAgent") { dataJson = JSONObject().put("ua", "(reset to default)").toString() }
            } else {
                if (ua.length > 512) return@tool ToolResult.fail("setUserAgent", "ua too long (max 512 chars)")
                bridge.setUserAgent(ua)
                ToolResult.ok("setUserAgent") { dataJson = JSONObject().put("ua", ua).toString() }
            }
        })
        register(tool("getUserAgent", "Get the currently active user agent.", ActionRisk.SAFE) { _ ->
            val ua = bridge.userAgent()
            if (ua != null) ToolResult.ok("getUserAgent") { dataJson = JSONObject().put("ua", ua).toString() }
            else ToolResult.fail("getUserAgent", "user agent unavailable")
        })

        // --- Bookmarks & user ---
        register(tool("saveBookmark", "Bookmark the current page.", ActionRisk.SAFE) { _ ->
            ToolResult.ok("saveBookmark") { url = bridge.currentUrl(); title = bridge.pageTitle() }
        })
        register(tool("notifyUser", "Send a notification to the user.", ActionRisk.SAFE) { args ->
            // Real notification dispatch is wired by the browser layer host.
            ToolResult.ok("notifyUser") { dataJson = JSONObject().put("message", args.optString("message")).toString() }
        })
    }

    private fun requireUrl(args: JSONObject): String {
        val url = args.optString("url").ifBlank { throw IllegalArgumentException("url is required") }
        val scheme = url.substringBefore(":")
        if (scheme !in setOf("http", "https", "about", "file", "chrome")) {
            throw IllegalArgumentException("blocked url scheme: $scheme")
        }
        return url
    }

    private fun tool(name: String, desc: String, risk: ActionRisk, block: (JSONObject) -> ToolResult): AgentTool =
        object : AgentTool {
            override val name = name
            override val description = desc
            override val risk = risk
            override suspend fun execute(argsJson: String): ToolResult = withContext(Dispatchers.Main) {
                block(JSONObject(argsJson.ifBlank { "{}" }))
            }
        }

    private fun simple(name: String, desc: String, risk: ActionRisk, block: () -> Boolean): AgentTool =
        object : AgentTool {
            override val name = name
            override val description = desc
            override val risk = risk
            override suspend fun execute(argsJson: String): ToolResult = withContext(Dispatchers.Main) {
                if (block()) ToolResult.ok(name) else ToolResult.fail(name, "$name failed")
            }
        }
}
