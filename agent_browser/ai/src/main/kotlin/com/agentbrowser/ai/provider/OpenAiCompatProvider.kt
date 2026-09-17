// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.ai.provider

import org.json.JSONArray
import org.json.JSONObject

/**
 * OpenAI-compatible chat-completions provider. Covers OpenAI, OpenRouter,
 * custom OpenAI-compatible endpoints, and Ollama's /v1 API — so the whole
 * OpenAI-compatible family (spec section 16) shares this implementation.
 */
open class OpenAiCompatProvider(
    override val id: String,
    override val label: String,
    override val defaultModel: String,
    private val baseUrl: String,
    /** True when the endpoint expects "Bearer <key>". */
    private val bearerAuth: Boolean = true,
) : HttpAiProvider(id, label, defaultModel, requiresApiKey = bearerAuth) {

    override suspend fun complete(request: CompletionRequest): CompletionResponse {
        val model = request.model ?: defaultModel
        val contentJson: Any = if (request.imageBase64 != null) {
            JSONArray().apply {
                put(JSONObject().put("type", "text").put("text", request.messages.last().content))
                put(JSONObject().put("type", "image_url").put(
                    "image_url",
                    JSONObject().put("url", "data:${request.mediaType};base64,${request.imageBase64}"),
                ))
            }
        } else {
            request.messages.last().content
        }

        val body = JSONObject().apply {
            put("model", model)
            put("max_tokens", request.maxTokens)
            put("temperature", request.temperature)
            put(
                "messages",
                JSONArray().apply {
                    for (m in request.messages.dropLast(1)) {
                        put(JSONObject().put("role", m.role).put("content", m.content))
                    }
                    put(JSONObject().put("role", request.messages.last().role).put("content", contentJson))
                },
            )
        }.toString()

        val headers = mutableMapOf<String, String>()
        // Auth is injected by ProviderManager via apiKeyProvider; kept simple here.
        val key = apiKeyProvider?.invoke()
        if (key != null && bearerAuth) headers["Authorization"] = "Bearer $key"

        val resp = ProviderHttp.postJson("$baseUrl/chat/completions", headers, body)
        val text = ProviderHttp.check(resp.code, resp.body, id)

        val json = JSONObject(text)
        val choice = json.getJSONArray("choices").getJSONObject(0)
        val message = choice.getJSONObject("message")
        val usage = json.optJSONObject("usage")
        return CompletionResponse(
            text = message.optString("content", ""),
            model = json.optString("model", model),
            promptTokens = usage?.optInt("prompt_tokens", 0) ?: 0,
            completionTokens = usage?.optInt("completion_tokens", 0) ?: 0,
            finishReason = choice.optString("finish_reason", null),
        )
    }

    /** Injected by [com.agentbrowser.ai.provider.ProviderManager]. */
    var apiKeyProvider: (() -> String?)? = null
}

/** Ollama local provider — no API key, default OpenAI-compatible endpoint. */
class OllamaProvider(
    baseUrl: String = "http://127.0.0.1:11434/v1",
    defaultModel: String = "llama3.2",
) : OpenAiCompatProvider("ollama", "Ollama (local)", defaultModel, baseUrl, bearerAuth = false) {
    override val requiresApiKey: Boolean = false
}
