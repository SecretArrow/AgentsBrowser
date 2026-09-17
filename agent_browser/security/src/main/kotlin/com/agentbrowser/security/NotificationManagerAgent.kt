// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.security

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

/**
 * Real Android notifications (spec section 34): run completion, approvals,
 * authentication pauses, and failures.
 */
class NotificationManagerAgent(private val context: Context) {

    companion object {
        const val CHANNEL_RUNS = "agent_runs"
        const val CHANNEL_APPROVALS = "agent_approvals"
        const val CHANNEL_SECURITY = "agent_security"
        const val OPEN_REPORT_ACTION = "com.agentbrowser.OPEN_REPORT"
        const val REVIEW_ACTION = "com.agentbrowser.REVIEW_APPROVAL"
        const val OPEN_SITE_ACTION = "com.agentbrowser.OPEN_SITE"
    }

    private val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_RUNS, "Agent runs", NotificationManager.IMPORTANCE_DEFAULT),
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_APPROVALS, "Approvals", NotificationManager.IMPORTANCE_HIGH),
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_SECURITY, "Security", NotificationManager.IMPORTANCE_HIGH),
        )
    }

    fun notifyRunCompleted(goalName: String, summary: String, reportId: String) {
        post(
            CHANNEL_RUNS, goalName.hashCode(),
            "Agent Browser", "$goalName completed. $summary",
            NotificationCompat.Action(0, "Open Report", actionIntent(OPEN_REPORT_ACTION, reportId)),
        )
    }

    fun notifyApprovalNeeded(goalName: String, description: String) {
        post(
            CHANNEL_APPROVALS, goalName.hashCode(),
            "Agent Browser", "An autonomous task needs your approval: $description",
            NotificationCompat.Action(0, "Review", actionIntent(REVIEW_ACTION, goalName)),
        )
    }

    fun notifyAuthRequired(url: String) {
        post(
            CHANNEL_SECURITY, url.hashCode(),
            "Agent Browser", "Task paused because authentication is required.",
            NotificationCompat.Action(0, "Open Website", actionIntent(OPEN_SITE_ACTION, url)),
        )
    }

    private fun post(channel: String, id: Int, title: String, text: String, action: NotificationCompat.Action? = null) {
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
        if (action != null) builder.addAction(action)
        nm.notify(id, builder.build())
    }

    private fun actionIntent(action: String, extra: String) =
        android.app.PendingIntent.getBroadcast(
            context,
            extra.hashCode(),
            android.content.Intent(action).setPackage(context.packageName).putExtra("extra", extra),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
}
