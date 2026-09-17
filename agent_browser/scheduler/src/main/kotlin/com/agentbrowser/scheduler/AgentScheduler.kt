// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.scheduler

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.agentbrowser.agent.model.AgentGoal
import java.util.concurrent.TimeUnit

/**
 * Background scheduling (spec sections 21, 22, 53): WorkManager periodic work
 * with network + battery-not-low constraints, exponential backoff, and
 * persisted state. No unlimited background execution is claimed.
 */
class AgentScheduler(private val context: Context) {

    private val wm = WorkManager.getInstance(context)

    fun scheduleGoal(goal: AgentGoal) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<AgentGoalWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInputData(androidx.work.Data.Builder().putString("goalId", goal.id).build())
            .addTag("agent_goal")
            .build()

        wm.enqueueUniquePeriodicWork("goal_${goal.id}", ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancelGoal(goalId: String) {
        wm.cancelUniqueWork("goal_$goalId")
    }

    fun runGoalNow(goalId: String) {
        wm.enqueue(
            OneTimeWorkRequestBuilder<AgentGoalWorker>()
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
                )
                .setInputData(androidx.work.Data.Builder().putString("goalId", goalId).build())
                .addTag("agent_goal_now")
                .build(),
        )
    }

    fun cancelAll() {
        wm.cancelAllWorkByTag("agent_goal")
    }
}
