// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.memory

import com.agentbrowser.agent.db.AgentDatabase
import com.agentbrowser.security.SensitiveDataFilter
import android.content.ContentValues

/**
 * Layered agent memory (spec section 31): global, goal, site, run, and temp.
 * Passwords and private keys are never stored — writes pass through
 * [SensitiveDataFilter] and matches are rejected.
 */
class MemoryManager(private val db: AgentDatabase) {

    enum class Scope(val id: String) {
        GLOBAL("global"), GOAL("goal"), SITE("site"), RUN("run"), TEMP("temp")
    }

    private val filter = SensitiveDataFilter()

    fun remember(scope: Scope, scopeKey: String, key: String, value: String, ttlMs: Long? = null): Boolean {
        if (filter.containsSensitiveData(value)) return false
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("id", "$scope:$scopeKey:$key")
            put("scope", scope.id)
            put("scope_key", scopeKey)
            put("key", key)
            put("value", value)
            put("updated_at", now)
            put("expires_at", ttlMs?.let { now + it })
        }
        db.writableDatabase.insertWithOnConflict(
            "memory", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
        return true
    }

    fun recall(scope: Scope, scopeKey: String, key: String): String? {
        db.readableDatabase.query(
            "memory", arrayOf("value", "expires_at"),
            "scope=? AND scope_key=? AND key=?",
            arrayOf(scope.id, scopeKey, key), null, null, null
        ).use { c ->
            if (!c.moveToFirst()) return null
            val exp = c.getLong(1)
            if (exp in 1..System.currentTimeMillis()) {
                forget(scope, scopeKey, key)
                return null
            }
            return c.getString(0)
        }
    }

    fun forget(scope: Scope, scopeKey: String, key: String) {
        db.writableDatabase.delete("memory", "scope=? AND scope_key=? AND key=?",
            arrayOf(scope.id, scopeKey, key))
    }

    fun forgetSite(domain: String) = forget(Scope.SITE, domain, "*")

    fun relevantContextFor(goalId: String, domain: String, limit: Int = 20): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        val dbp = db.readableDatabase
        for (pair in listOf(listOf(Scope.GLOBAL.id, "_"), listOf(Scope.GOAL.id, goalId), listOf(Scope.SITE.id, domain))) {
            dbp.query("memory", arrayOf("key", "value"), "scope=? AND scope_key=?",
                arrayOf(pair[0], pair[1]), null, null, "updated_at DESC", "$limit").use { c ->
                while (c.moveToNext()) out.add(c.getString(0) to c.getString(1))
            }
        }
        return out
    }

    fun purgeExpired() {
        db.writableDatabase.delete("memory", "expires_at IS NOT NULL AND expires_at < ?",
            arrayOf(System.currentTimeMillis().toString()))
    }
}
