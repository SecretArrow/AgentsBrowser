// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.ai.provider

import com.agentbrowser.security.SecureKeyStore

/**
 * Owns the set of configured providers and hands out the right provider for a
 * logical task (spec sections 16, 17). API keys live only in [SecureKeyStore].
 */
class ProviderManager(private val keyStore: SecureKeyStore) {

    enum class Task { PLANNER, BROWSER_ACTION, VISION, SUMMARIZATION, BACKGROUND_MONITOR }

    private val providers = linkedMapOf(
        "openai" to OpenAiCompatProvider(
            id = "openai", label = "OpenAI", defaultModel = "gpt-4o-mini",
            baseUrl = "https://api.openai.com/v1",
        ),
        "anthropic" to AnthropicProvider(),
        "gemini" to GeminiProvider(),
        "openrouter" to OpenAiCompatProvider(
            id = "openrouter", label = "OpenRouter", defaultModel = "openrouter/auto",
            baseUrl = "https://openrouter.ai/api/v1",
        ),
        "ollama" to OllamaProvider(),
    )

    /** task → providerId overrides configured by the user. */
    private val routing = mutableMapOf(
        Task.PLANNER to "openai",
        Task.BROWSER_ACTION to "openai",
        Task.VISION to "openai",
        Task.SUMMARIZATION to "openai",
        Task.BACKGROUND_MONITOR to "openai",
    )

    /** task → explicit model override, when the user sets one. */
    private val modelOverrides = mutableMapOf<Task, String>()

    init {
        providers.values.forEach { p ->
            when (p) {
                is OpenAiCompatProvider -> p.apiKeyProvider = { keyStore.retrieveKey(p.id) }
                is AnthropicProvider -> p.apiKeyProvider = { keyStore.retrieveKey(p.id) }
                is GeminiProvider -> p.apiKeyProvider = { keyStore.retrieveKey(p.id) }
            }
        }
    }

    fun list(): List<AiProvider> = providers.values.toList()

    fun get(providerId: String): AiProvider? = providers[providerId]

    fun setRoute(task: Task, providerId: String) {
        require(providers.containsKey(providerId)) { "unknown provider $providerId" }
        routing[task] = providerId
    }

    fun setModelOverride(task: Task, model: String?) {
        if (model.isNullOrBlank()) modelOverrides.remove(task) else modelOverrides[task] = model
    }

    fun providerFor(task: Task): AiProvider =
        providers[routing[task]] ?: providers.values.first()

    fun modelFor(task: Task): String? = modelOverrides[task]

    fun setApiKey(providerId: String, key: String) = keyStore.storeKey(providerId, key)

    fun hasApiKey(providerId: String): Boolean = keyStore.retrieveKey(providerId) != null
}
