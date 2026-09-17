// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.browser

/**
 * New-tab branding (spec section 46). Rendered as a local chrome:// page by
 * the browser layer; kept here so CI can validate structure without Chromium.
 */
object NtpBranding {

    const val NTP_URL = "chrome://agent-browser/newtab"

    fun html(activeAutomations: Int = 0): String = """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8">
          <title>Agent Browser</title>
          <style>
            body { font-family: sans-serif; background: #f6f7f9; color: #202124;
                   display: flex; flex-direction: column; align-items: center;
                   justify-content: center; min-height: 100vh; margin: 0; }
            h1 { font-weight: 600; letter-spacing: .12em; }
            .search { width: min(560px, 86vw); padding: 14px 20px; border-radius: 24px;
                      border: 1px solid #dadce0; font-size: 16px; }
            .shortcuts { display: flex; gap: 12px; margin-top: 24px; }
            .tile { background: white; border: 1px solid #dadce0; border-radius: 12px;
                    padding: 12px 20px; }
            .ask { margin-top: 28px; padding: 10px 22px; border-radius: 20px;
                   background: #1a73e8; color: white; }
            .automations { margin-top: 18px; color: #5f6368; }
          </style>
        </head>
        <body>
          <h1>AGENT BROWSER</h1>
          <input class="search" placeholder="Search or enter address">
          <div class="shortcuts">
            <div class="tile">GitHub</div>
            <div class="tile">YouTube</div>
          </div>
          <button class="ask">🤖 Ask Agent</button>
          <div class="automations">Active Automations: $activeAutomations</div>
        </body>
        </html>
    """.trimIndent()
}
