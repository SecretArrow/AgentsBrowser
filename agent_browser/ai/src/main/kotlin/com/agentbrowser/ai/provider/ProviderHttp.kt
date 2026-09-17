// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.ai.provider

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Minimal HTTP client for provider calls (java.net based — no extra deps).
 * Blocking IO happens on Dispatchers.IO; callers use suspend functions.
 */
internal object ProviderHttp {

    data class HttpResponse(val code: Int, val body: String)

    suspend fun postJson(
        url: String,
        headers: Map<String, String>,
        body: String,
        timeoutMs: Int = 120_000,
    ): HttpResponse = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 15_000
            conn.readTimeout = timeoutMs
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            HttpResponse(code, text)
        }.getOrElse { throw ProviderException("network error: ${it.message}", retryable = true) }
    }

    @Throws(ProviderException::class)
    fun check(code: Int, body: String, provider: String): String {
        if (code in 200..299) return body
        val retryable = code == 429 || code >= 500
        throw ProviderException("$provider HTTP $code: ${body.take(400)}", code, retryable)
    }
}
