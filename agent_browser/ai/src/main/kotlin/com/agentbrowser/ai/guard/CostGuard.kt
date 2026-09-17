// Copyright 2026 The Agent Browser Authors. All requests reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.ai.guard

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Cost control (spec section 56): per-run and per-day token/request budgets.
 * When a budget is exceeded, the agent must stop and persist state instead of
 * silently spending more.
 */
class CostGuard {

    data class Budget(
        val maxRequestsPerDay: Int = 500,
        val maxTokensPerDay: Long = 2_000_000,
        val maxRequestsPerRun: Int = 100,
        val maxTokensPerRun: Long = 200_000,
    )

    var budget = Budget()

    private val dayRequests = AtomicInteger()
    private val dayTokens = AtomicLong()
    private val dayStamp = AtomicLong(today())
    private val runRequests = java.util.concurrent.ConcurrentHashMap<String, AtomicInteger>()
    private val runTokens = java.util.concurrent.ConcurrentHashMap<String, AtomicLong>()

    sealed class Verdict {
        data object Allowed : Verdict()
        data class Exceeded(val reason: String) : Verdict()
    }

    @Synchronized
    fun check(runId: String): Verdict {
        rollDayIfNeeded()
        val runReq = runRequests[runId]?.get() ?: 0
        val runTok = runTokens[runId]?.get() ?: 0L
        if (dayRequests.get() >= budget.maxRequestsPerDay)
            return Verdict.Exceeded("daily request budget reached (${budget.maxRequestsPerDay})")
        if (dayTokens.get() >= budget.maxTokensPerDay)
            return Verdict.Exceeded("daily token budget reached (${budget.maxTokensPerDay})")
        if (runReq >= budget.maxRequestsPerRun)
            return Verdict.Exceeded("run request budget reached (${budget.maxRequestsPerRun})")
        if (runTok >= budget.maxTokensPerRun)
            return Verdict.Exceeded("run token budget reached (${budget.maxTokensPerRun})")
        return Verdict.Allowed
    }

    fun record(runId: String, promptTokens: Int, completionTokens: Int) {
        rollDayIfNeeded()
        dayRequests.incrementAndGet()
        dayTokens.addAndGet((promptTokens + completionTokens).toLong())
        runRequests.getOrPut(runId) { AtomicInteger() }.incrementAndGet()
        runTokens.getOrPut(runId) { AtomicLong() }.addAndGet((promptTokens + completionTokens).toLong())
    }

    fun endRun(runId: String) {
        runRequests.remove(runId)
        runTokens.remove(runId)
    }

    @Synchronized
    private fun rollDayIfNeeded() {
        val today = today()
        if (dayStamp.get() != today) {
            dayStamp.set(today)
            dayRequests.set(0)
            dayTokens.set(0)
        }
    }

    private fun today(): Long = System.currentTimeMillis() / 86_400_000L
}
