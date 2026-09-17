// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.ai.provider

import org.json.JSONArray
import org.json.JSONObject

/** Google Gemini generateContent provider. */
class GeminiProvider(
    private val baseUrl: String = "https://generativelanguage.googleapis.com/v1beta",
    override val defaultModel: String = "gemini-2.0-flash",
) : HttpAiProvider("gemini", "Google Gemini", defaultModel) {

    var apiKeyProvider: (() -> String?)? = null

    override suspend fun complete(request: CompletionRequest): CompletionResponse {
        val model = request.model ?: defaultModel
        val sys = StringBuilder()
        val contents = JSONArray()
        for (m in request.messages) {
            if (m.role == "system") {
                sys.append(m.content).append("\n\n")
                continue
            }
            val role = if (m.role == "assistant") "model" else "user"
            contents.put(
                JSONObject().put("role", role).put(
                    "parts",
                    JSONArray().put(JSONObject().put("text", m.content)),
                ),
            )
        }
        if (request.imageBase64 != null) {
            contents.put(
                JSONObject()
                    .put("role", "user")
                    .put(
                        "parts",
                        JSONArray()
                            .put(JSONObject().put("inline_data", JSONObject().put("mime_type", request.mediaType).put("data", request.imageBase64)))
                            .put(JSONObject().put("text", request.messages.last().content)),
                    ),
            )
        }

        val body = JSONObject().apply {
            if (sys.isNotBlank()) put("system_instruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", sys.toString().trim()))))
            put("contents", contents)
            put("generationConfig", JSONObject().put("temperature", request.temperature).put("maxOutputTokens", request.maxTokens))
        }.toString()

        val key = apiKeyProvider?.invoke() ?: throw ProviderException("missing Gemini API key")
        val resp = ProviderHttp.postJson("$baseUrl/models/$model:generateContent?key=$key", emptyMap(), body)
        val text = ProviderHttp.check(resp.code, resp.body, id)

        val json = JSONObject(text)
        val out = StringBuilder()
        val candidates = json.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val parts = candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts")
            parts.forEachObject { p -> out.append(p.optString("text", "")) }
        }
        val usage = json.optJSONObject("usageMetadata")
        return CompletionResponse(
            text = out.toString(),
            model = model,
            promptTokens = usage?.optInt("promptTokenCount", 0) ?: 0,
            completionTokens = usage?.optInt("candidatesTokenCount", 0) ?: 0,
            finishReason = candidates?.getJSONObject(0)?.optString("finishReason", null),
        )
    }
}
