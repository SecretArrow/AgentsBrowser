// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.ai.provider

import org.json.JSONArray
import org.json.JSONObject

/** Anthropic Messages API provider. */
class AnthropicProvider(
    private val baseUrl: String = "https://api.anthropic.com",
    override val defaultModel: String = "claude-sonnet-4-20250514",
) : HttpAiProvider("anthropic", "Anthropic", defaultModel) {

    var apiKeyProvider: (() -> String?)? = null

    override suspend fun complete(request: CompletionRequest): CompletionResponse {
        val model = request.model ?: defaultModel
        val sys = StringBuilder()
        val msgs = JSONArray()
        for (m in request.messages) {
            if (m.role == "system") {
                sys.append(m.content).append("\n\n")
            } else {
                msgs.put(JSONObject().put("role", m.role).put("content", m.content))
            }
        }
        if (request.imageBase64 != null) {
            val last = request.messages.last()
            val content = JSONArray().apply {
                put(JSONObject().put("type", "image").put(
                    "source",
                    JSONObject()
                        .put("type", "base64")
                        .put("media_type", request.mediaType)
                        .put("data", request.imageBase64),
                ))
                put(JSONObject().put("type", "text").put("text", last.content))
            }
            msgs.put(msgs.length().let { JSONObject().put("role", last.role).put("content", content) })
        }

        val body = JSONObject().apply {
            put("model", model)
            put("max_tokens", request.maxTokens)
            put("temperature", request.temperature)
            if (sys.isNotBlank()) put("system", sys.toString().trim())
            put("messages", msgs)
        }.toString()

        val key = apiKeyProvider?.invoke() ?: throw ProviderException("missing Anthropic API key")
        val resp = ProviderHttp.postJson(
            "$baseUrl/v1/messages",
            mapOf(
                "x-api-key" to key,
                "anthropic-version" to "2023-06-01",
            ),
            body,
        )
        val text = ProviderHttp.check(resp.code, resp.body, id)

        val json = JSONObject(text)
        val out = StringBuilder()
        json.getJSONArray("content").forEachObject { c ->
            if (c.optString("type") == "text") out.append(c.optString("text", ""))
        }
        val usage = json.optJSONObject("usage")
        return CompletionResponse(
            text = out.toString(),
            model = json.optString("model", model),
            promptTokens = usage?.optInt("input_tokens", 0) ?: 0,
            completionTokens = usage?.optInt("output_tokens", 0) ?: 0,
            finishReason = json.optString("stop_reason", null),
        )
    }
}
