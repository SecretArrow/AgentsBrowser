// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.browser

import com.agentbrowser.automation.PageBridge

/**
 * Minimal host interface between the Chromium browser layer and the agent.
 * The Chromium-side implementation (agent_browser/browser) provides the real
 * PageBridge over WebContents/Tabs; CI can compile the agent stack against
 * this interface without a full Chromium checkout.
 */
interface BrowserHost {
    val pageBridge: PageBridge

    companion object {
        /** Acquires the host for background execution; null when browser unavailable. */
        fun acquire(context: android.content.Context): BrowserHost? =
            runCatching {
                val cls = Class.forName("com.agentbrowser.browser.ChromiumBrowserHost")
                val ctor = cls.constructors.firstOrNull { it.parameterTypes.size == 1 && it.parameterTypes[0] == android.content.Context::class.java }
                @Suppress("UNCHECKED_CAST")
                ctor?.newInstance(context) as? BrowserHost
            }.getOrNull()
    }
}
