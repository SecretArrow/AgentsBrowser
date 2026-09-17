// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.agent.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Persistent storage for agent entities (spec section 59).
 * Migrations only ever ADD or ALTER — never drop existing browser data.
 */
class AgentDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) = MIGRATIONS.getValue(1)(db)

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        for (v in (oldVersion + 1)..newVersion) {
            MIGRATIONS.getValue(v)(db)
        }
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Never destroy data on downgrade; keep the schema as-is.
    }

    companion object {
        private const val DB_NAME = "agent_browser.db"
        private const val DB_VERSION = 1

        @Volatile private var instance: AgentDatabase? = null

        fun get(context: Context): AgentDatabase =
            instance ?: synchronized(this) {
                instance ?: AgentDatabase(context.applicationContext).also { instance = it }
            }

        private val MIGRATIONS: Map<Int, (SQLiteDatabase) -> Unit> = mapOf(
            1 to { db ->
                db.execSQL("""
                    CREATE TABLE goals (
                        id TEXT PRIMARY KEY, name TEXT NOT NULL, description TEXT,
                        instruction TEXT NOT NULL, enabled INTEGER DEFAULT 1,
                        schedule_json TEXT, triggers_json TEXT,
                        allowed_domains TEXT, blocked_domains TEXT,
                        allowed_actions TEXT, blocked_actions TEXT,
                        confirmation_policy TEXT, notification_policy TEXT,
                        memory_policy TEXT, max_steps INTEGER, max_runtime_ms INTEGER,
                        max_retries INTEGER, conditions_json TEXT,
                        last_run_at INTEGER, next_run_at INTEGER,
                        status TEXT, created_at INTEGER, updated_at INTEGER)
                """.trimIndent())
                db.execSQL("CREATE TABLE runs (id TEXT PRIMARY KEY, goal_id TEXT, status TEXT, started_at INTEGER, finished_at INTEGER, steps_used INTEGER, exit_reason TEXT, summary TEXT)")
                db.execSQL("CREATE TABLE steps (id TEXT PRIMARY KEY, run_id TEXT, idx INTEGER, thought TEXT, action TEXT, action_args TEXT, observation TEXT, succeeded INTEGER, at INTEGER)")
                db.execSQL("CREATE TABLE triggers (id TEXT PRIMARY KEY, goal_id TEXT, kind TEXT, spec_json TEXT, enabled INTEGER DEFAULT 1, next_fire_at INTEGER)")
                db.execSQL("CREATE TABLE memory (id TEXT PRIMARY KEY, scope TEXT, scope_key TEXT, key TEXT, value TEXT, updated_at INTEGER, expires_at INTEGER)")
                db.execSQL("CREATE UNIQUE INDEX idx_memory_scope ON memory(scope, scope_key, key)")
                db.execSQL("CREATE TABLE permissions (domain TEXT PRIMARY KEY, read TEXT, navigate TEXT, form_fill TEXT, submit TEXT, download TEXT, upload TEXT)")
                db.execSQL("CREATE TABLE approvals (id TEXT PRIMARY KEY, run_id TEXT, goal_id TEXT, domain TEXT, action TEXT, description TEXT, payload TEXT, created_at INTEGER, status TEXT)")
                db.execSQL("CREATE TABLE audit (id TEXT PRIMARY KEY, run_id TEXT, at INTEGER, description TEXT, category TEXT, succeeded INTEGER)")
                db.execSQL("CREATE TABLE events (id TEXT PRIMARY KEY, run_id TEXT, at INTEGER, kind TEXT, detail TEXT)")
            },
        )
    }
}
