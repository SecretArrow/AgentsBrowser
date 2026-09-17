// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.browser

import android.content.Context

/**
 * Custom User-Agent control with 10 built-in presets plus a free-form custom
 * string. Supports a global override and per-tab overrides; the effective UA
 * for a tab is: tab override → global override → default.
 *
 * The Chromium browser layer applies the value through
 * `ContentBrowserClient::OverrideUserAgent` (patch series keeps this in
 * agent_browser/), so it is a real UA override, not a JavaScript spoof.
 */
class UserAgentManager(context: Context) {

    data class UaPreset(
        val id: String,
        val label: String,
        val userAgent: String,
    )

    companion object {
        private const val CHROME_VERSION = "131.0.6778.200"
        private const val SAFARI_WEBKIT = "605.1.15"

        /** Ten built-in presets (order matters: shown in UI as-is). */
        val PRESETS: List<UaPreset> = listOf(
            UaPreset(
                "default",
                "Default (Agent Browser / Android)",
                "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/$CHROME_VERSION Mobile Safari/537.36 AgentBrowser/1.0",
            ),
            UaPreset(
                "android_chrome",
                "Android — Chrome",
                "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/$CHROME_VERSION Mobile Safari/537.36",
            ),
            UaPreset(
                "desktop_chrome_windows",
                "Desktop — Chrome (Windows)",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/$CHROME_VERSION Safari/537.36",
            ),
            UaPreset(
                "desktop_chrome_macos",
                "Desktop — Chrome (macOS)",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/$CHROME_VERSION Safari/537.36",
            ),
            UaPreset(
                "desktop_chrome_linux",
                "Desktop — Chrome (Linux)",
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/$CHROME_VERSION Safari/537.36",
            ),
            UaPreset(
                "desktop_firefox",
                "Desktop — Firefox (Windows)",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:133.0) Gecko/20100101 Firefox/133.0",
            ),
            UaPreset(
                "desktop_safari",
                "Desktop — Safari (macOS)",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/$SAFARI_WEBKIT " +
                    "(KHTML, like Gecko) Version/18.2 Safari/$SAFARI_WEBKIT",
            ),
            UaPreset(
                "desktop_edge",
                "Desktop — Edge (Windows)",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/$CHROME_VERSION Safari/537.36 Edg/131.0.2903.112",
            ),
            UaPreset(
                "ios_iphone_safari",
                "iPhone — Safari (iOS 18)",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 18_1 like Mac OS X) AppleWebKit/$SAFARI_WEBKIT " +
                    "(KHTML, like Gecko) Version/18.1 Mobile/15E148 Safari/604.1",
            ),
            UaPreset(
                "ipados_safari",
                "iPad — Safari (iPadOS 18)",
                "Mozilla/5.0 (iPad; CPU OS 18_1 like Mac OS X) AppleWebKit/$SAFARI_WEBKIT " +
                    "(KHTML, like Gecko) Version/18.1 Safari/$SAFARI_WEBKIT",
            ),
        )

        private const val PREFS = "agent_ua"
        private const val KEY_GLOBAL = "global_ua"
        private const val KEY_TAB_PREFIX = "tab_ua_"
        private const val KEY_GLOBAL_PRESET = "global_preset"
    }

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun presets(): List<UaPreset> = PRESETS

    fun presetById(id: String): UaPreset? = PRESETS.firstOrNull { it.id == id }

    /** Sets the global UA from a preset id, or clears it with null. */
    fun setGlobalPreset(presetId: String?) {
        if (presetId == null) {
            prefs.edit().remove(KEY_GLOBAL).remove(KEY_GLOBAL_PRESET).apply()
        } else {
            val preset = presetById(presetId) ?: return
            prefs.edit().putString(KEY_GLOBAL, preset.userAgent)
                .putString(KEY_GLOBAL_PRESET, preset.id).apply()
        }
    }

    /** Sets a free-form custom global UA; blank clears the override. */
    fun setGlobalCustom(ua: String) {
        if (ua.isBlank()) {
            setGlobalPreset(null)
        } else {
            prefs.edit().putString(KEY_GLOBAL, ua.trim()).remove(KEY_GLOBAL_PRESET).apply()
        }
    }

    /** Per-tab override; blank or null clears the tab's override. */
    fun setForTab(tabId: Int, ua: String?) {
        val key = KEY_TAB_PREFIX + tabId
        if (ua.isNullOrBlank()) {
            prefs.edit().remove(key).apply()
        } else {
            prefs.edit().putString(key, ua.trim()).apply()
        }
    }

    fun globalUserAgent(): String? = prefs.getString(KEY_GLOBAL, null)

    fun globalPresetId(): String? = prefs.getString(KEY_GLOBAL_PRESET, null)

    fun userAgentForTab(tabId: Int): String? =
        prefs.getString(KEY_TAB_PREFIX + tabId, null) ?: globalUserAgent()

    /** Effective UA resolution used by the browser layer and by tools. */
    fun effectiveFor(tabId: Int, defaultUa: String): String =
        userAgentForTab(tabId) ?: defaultUa

    fun clearAllTabOverrides() {
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith(KEY_TAB_PREFIX) }.forEach { editor.remove(it) }
        editor.apply()
    }
}
