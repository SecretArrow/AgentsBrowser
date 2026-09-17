// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.security

import android.content.ContentValues
import com.agentbrowser.agent.db.AgentDatabase
import com.agentbrowser.agent.model.DomainPermission
import com.agentbrowser.automation.PermissionManager

/**
 * Domain permission store implementing [PermissionManager] (spec sections 27, 64).
 * User policies cannot override [SafetyGuard] hard boundaries.
 */
class DomainPolicyStore(private val db: AgentDatabase) : PermissionManager {

    companion object {
        /** Returned when the domain policy demands human approval for the action. */
        const val APPROVAL_REQUIRED = "APPROVAL_REQUIRED"

        /** Sensible default: reading is allowed, submitting anything needs approval. */
        fun defaultFor(domain: String) = DomainPermission(domain = domain)
    }

    fun get(domain: String): DomainPermission {
        db.readableDatabase.query(
            "permissions", null, "domain=?", arrayOf(domain), null, null, null,
        ).use { c ->
            if (c.moveToFirst()) {
                return DomainPermission(
                    domain = c.getString(0),
                    read = c.getString(1),
                    navigate = c.getString(2),
                    formFill = c.getString(3),
                    submit = c.getString(4),
                    download = c.getString(5),
                    upload = c.getString(6),
                )
            }
        }
        return defaultFor(domain)
    }

    fun put(p: DomainPermission) {
        val values = ContentValues().apply {
            put("domain", p.domain)
            put("read", p.read)
            put("navigate", p.navigate)
            put("form_fill", p.formFill)
            put("submit", p.submit)
            put("download", p.download)
            put("upload", p.upload)
        }
        db.writableDatabase.insertWithOnConflict(
            "permissions", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun all(): List<DomainPermission> {
        val out = mutableListOf<DomainPermission>()
        db.readableDatabase.query("permissions", null, null, null, null, null, "domain").use { c ->
            while (c.moveToNext()) {
                out.add(
                    DomainPermission(
                        domain = c.getString(0), read = c.getString(1),
                        navigate = c.getString(2), formFill = c.getString(3),
                        submit = c.getString(4), download = c.getString(5),
                        upload = c.getString(6),
                    ),
                )
            }
        }
        return out
    }

    private fun levelFor(p: DomainPermission, action: String): String = when {
        action.startsWith("extract") || action.startsWith("inspect") || action.startsWith("find") ||
            action in setOf("getCurrentUrl", "getPageTitle", "takeScreenshot", "scroll", "scrollToText") -> p.read
        action in setOf(
            "openUrl", "searchWeb", "goBack", "goForward", "reload", "stopLoading",
            "createTab", "closeTab", "switchTab", "duplicateTab", "waitForElement",
            "waitForNavigation",
        ) -> p.navigate
        action in setOf("typeText", "clearInput", "selectOption", "checkCheckbox", "uncheckCheckbox", "clickElement", "clickCoordinates", "pressKey") -> p.formFill
        action == "submitForm" || action == "pressEnter" -> p.submit
        action == "downloadFile" -> p.download
        action == "uploadFile" -> p.upload
        else -> "allowed"
    }

    override fun checkAction(domain: String, action: String): String? {
        val level = levelFor(get(domain), action)
        return when (level) {
            "blocked" -> "domain policy blocks '$action' on $domain"
            "approval_required" -> APPROVAL_REQUIRED
            else -> null
        }
    }
}
