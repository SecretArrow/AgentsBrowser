// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.agent.runtime

/**
 * Failure recovery helper (spec section 58): bounded retries with exponential
 * backoff and jitter so the agent never hammers a failing site or provider.
 */
class RetryManager(
    private val maxAttempts: Int = 3,
    private val baseDelayMs: Long = 1_000,
    private val maxDelayMs: Long = 30_000,
    private val sleep: suspend (Long) -> Unit = { kotlinx.coroutines.delay(it) },
) {

    suspend fun <T> retry(tag: String, logger: ((String) -> Unit)? = null, block: suspend () -> T): T {
        var lastError: Exception? = null
        for (attempt in 1..maxAttempts) {
            try {
                return block()
            } catch (e: Exception) {
                lastError = e
                if (attempt == maxAttempts) break
                val backoff = (baseDelayMs shl (attempt - 1)).coerceAtMost(maxDelayMs)
                val jitter = (0..backoff.toInt() / 4).random().toLong()
                logger?.invoke("$tag attempt $attempt failed (${e.message}); backing off ${backoff + jitter} ms")
                sleep(backoff + jitter)
            }
        }
        throw lastError ?: IllegalStateException("$tag failed")
    }
}
