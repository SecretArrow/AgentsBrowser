// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.scheduler

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.agentbrowser.agent.model.AgentStatus
import com.agentbrowser.automation.PageBridge
import com.agentbrowser.browser.BrowserHost

/**
 * WorkManager entry point for autonomous goals (spec section 22). The runtime
 * restores persisted state, re-observes, validates, and resumes safely.
 */
class AgentGoalWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val goalId = inputData.getString("goalId") ?: return Result.failure()
        val host = BrowserHost.acquire(applicationContext) ?: return Result.retry()
        val runtime = com.agentbrowser.agent.runtime.AgentRuntime.get(applicationContext, host.pageBridge)
        return when (runtime.runGoalOnce(goalId)) {
            AgentStatus.COMPLETED -> Result.success()
            AgentStatus.FAILED -> Result.failure()
            else -> Result.success()
        }
    }
}
