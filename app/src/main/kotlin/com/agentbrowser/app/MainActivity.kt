// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.app

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.agentbrowser.ai.provider.ChatMessage
import com.agentbrowser.ai.provider.CompletionRequest
import com.agentbrowser.ai.provider.ProviderManager
import com.agentbrowser.browser.UserAgentManager
import com.agentbrowser.security.SecureKeyStore
import com.agentbrowser.ui.AgentPanelView
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Hosted-lane browser shell: a real WebView browser wired to the agent stack
 * (AI panel, custom UA + 10 presets, Control Center). The full Chromium fork
 * APK comes from the self-hosted chromium-build lane; this app is the fast,
 * installable hosted release artifact.
 */
class MainActivity : Activity() {

    private lateinit var web: WebView
    private lateinit var answer: TextView
    private lateinit var uaManager: UserAgentManager
    private lateinit var providers: ProviderManager
    private val scope = MainScope()

    private companion object {
        val QUICK_PROMPTS = mapOf(
            AgentPanelView.QuickAction.SUMMARIZE to "Summarize this page concisely.",
            AgentPanelView.QuickAction.EXPLAIN to "Explain the main content of this page simply.",
            AgentPanelView.QuickAction.TRANSLATE to "Translate this page's content to Indonesian.",
            AgentPanelView.QuickAction.FIND to "List the key facts found on this page.",
            AgentPanelView.QuickAction.EXTRACT to "Extract the structured data from this page.",
            AgentPanelView.QuickAction.COMPARE to "Identify differing viewpoints or options on this page.",
            AgentPanelView.QuickAction.RESEARCH to "Identify the research questions this page answers.",
            AgentPanelView.QuickAction.ACT to "Suggest the next actions possible on this page.",
            AgentPanelView.QuickAction.AUTOMATE to "Describe how to automate the task on this page.",
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        uaManager = UserAgentManager(this)
        providers = ProviderManager(SecureKeyStore(this))

        web = findViewById(R.id.web_view)
        answer = findViewById(R.id.answer)
        val panel: AgentPanelView = findViewById(R.id.agent_panel)

        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                applyEffectiveUa(url)
                panel.setCurrentPage(url, view.title)
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback,
            ) = callback.invoke(origin, true, false)
        }
        if (savedInstanceState != null) web.restoreState(savedInstanceState)
        else web.loadUrl("https://www.google.com")

        findViewById<TextView>(R.id.btn_ua).setOnClickListener { showUaDialog() }
        findViewById<TextView>(R.id.btn_center).setOnClickListener {
            startActivity(android.content.Intent(this, com.agentbrowser.ui.AgentControlCenterActivity::class.java))
        }

        panel.callbacks = object : AgentPanelView.Callbacks {
            override fun onQuickAction(action: AgentPanelView.QuickAction) {
                askAgent(QUICK_PROMPTS[action] ?: "Summarize this page.")
            }

            override fun onAskSubmitted(question: String) = askAgent(question)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        scope.cancel()
        (web.parent as? ViewGroup)?.removeView(web)
        web.destroy()
        super.onDestroy()
    }

    private fun showUaDialog() {
        val presets = uaManager.presets()
        val labels = presets.map { it.label }.toTypedArray() + arrayOf("Custom…")
        AlertDialog.Builder(this)
            .setTitle(R.string.ua_dialog_title)
            .setItems(labels) { _, which ->
                if (which < presets.size) {
                    uaManager.setGlobalPreset(presets[which].id)
                    applyEffectiveUa(web.url)
                    toastUa(presets[which].userAgent)
                } else {
                    promptCustomUa()
                }
            }
            .show()
    }

    private fun promptCustomUa() {
        val input = EditText(this)
        input.setText(uaManager.globalUserAgent().orEmpty())
        AlertDialog.Builder(this)
            .setTitle(R.string.ua_dialog_title)
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                uaManager.setGlobalCustom(input.text.toString())
                applyEffectiveUa(web.url)
                uaManager.globalUserAgent()?.let { toastUa(it) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /** Effective UA = global override (preset or custom), applied natively. */
    private fun applyEffectiveUa(url: String?) {
        web.settings.userAgentString =
            uaManager.effectiveFor(0, web.settings.userAgentString)
        if (url != null && web.settings.userAgentString != null) {
            // Re-trigger header construction with the new UA without losing history.
            web.evaluateJavascript("void 0", null)
        }
    }

    private fun toastUa(ua: String) {
        Toast.makeText(this, getString(R.string.ua_applied, ua.take(72)), Toast.LENGTH_SHORT).show()
    }

    private fun pageText(callback: (String) -> Unit) {
        web.evaluateJavascript(
            "(document.body && document.body.innerText || '').slice(0, 12000)",
        ) { json -> callback(json?.removeSurrounding("\"") ?: "") }
    }

    private fun askAgent(instruction: String) {
        val provider = providers.providerFor(ProviderManager.Task.SUMMARIZATION)
        if (provider.requiresApiKey && !providers.hasApiKey(provider.id)) {
            Toast.makeText(this, getString(R.string.no_api_key, provider.label), Toast.LENGTH_LONG).show()
            return
        }
        answer.visibility = TextView.VISIBLE
        answer.text = getString(R.string.thinking)
        pageText { page ->
            scope.launch {
                val result = runCatching {
                    provider.complete(
                        CompletionRequest(
                            messages = listOf(
                                ChatMessage("system", "You help inside a mobile browser. Answer concisely."),
                                ChatMessage("user", "$instruction\n\nPAGE:\n$page"),
                            ),
                            model = providers.modelFor(ProviderManager.Task.SUMMARIZATION),
                        ),
                    )
                }
                val text = result.fold(
                    onSuccess = { it.text },
                    onFailure = { getString(R.string.ai_error, it.message ?: "unknown") },
                )
                answer.text = text
            }
        }
    }
}
