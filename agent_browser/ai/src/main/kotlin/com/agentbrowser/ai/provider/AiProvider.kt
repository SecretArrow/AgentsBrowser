// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.ai.provider

import org.json.JSONArray
import org.json.JSONObject

/** A single chat message exchanged with a provider. */
data class ChatMessage(val role: String, val content: String)

/** Non-streaming completion request. */
data class CompletionRequest(
    val messages: List<ChatMessage>,
    val model: String? = null,
    val maxTokens: Int = 2048,
    val temperature: Double = 0.2,
    /** Optional image (base64 PNG/JPEG) for vision-capable models. */
    val imageBase64: String? = null,
    val mediaType: String = "image/png",
)

/** Normalized completion result across providers. */
data class CompletionResponse(
    val text: String,
    val model: String,
    val promptTokens: Int,
    val completionTokens: Int,
    val finishReason: String?,
)

/** Errors surfaced uniformly to the agent loop. */
class ProviderException(message: String, val httpCode: Int = 0, val retryable: Boolean = false) :
    Exception(message)

/**
 * Common interface for all AI providers (spec section 16):
 * OpenAI-compatible, Anthropic, Gemini, OpenRouter, Ollama, custom endpoints.
 */
interface AiProvider {
    val id: String
    /** Human-readable provider label for settings UI. */
    val label: String
    /** Default model used when the router does not override it. */
    val defaultModel: String
    /** True when API key required; false for local providers like Ollama. */
    val requiresApiKey: Boolean

    @Throws(ProviderException::class)
    suspend fun complete(request: CompletionRequest): CompletionResponse
}

/** Base class carrying shared HTTP plumbing for HTTP-based providers. */
abstract class HttpAiProvider(
    override val id: String,
    override val label: String,
    override val defaultModel: String,
    override val requiresApiKey: Boolean = true,
) : AiProvider

internal fun JSONArray.forEachObject(block: (JSONObject) -> Unit) {
    for (i in 0 until length()) block(getJSONObject(i))
}
