// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.agent

import com.agentbrowser.agent.goal.NaturalLanguageGoalParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NaturalLanguageGoalParserTest {

    @Test
    fun `parses weekday morning schedule`() {
        val p = NaturalLanguageGoalParser.parse(
            "Every weekday at 9 AM open my dashboard and tell me what needs attention",
        )
        assertNotNull(p.schedule)
        assertEquals("weekdays", p.schedule!!.kind)
        assertEquals(9, p.schedule!!.hourOfDay)
        assertEquals(0, p.schedule!!.minuteOfHour)
    }

    @Test
    fun `parses every N minutes interval with notify trigger`() {
        val p = NaturalLanguageGoalParser.parse(
            "Check this page every 30 minutes and notify me if it changes",
        )
        assertNotNull(p.schedule)
        assertEquals(30L, p.schedule!!.intervalMinutes)
        assertTrue(p.triggers.isNotEmpty())
    }

    @Test
    fun `parses daily morning default hour`() {
        val p = NaturalLanguageGoalParser.parse("Every morning check AI news")
        assertNotNull(p.schedule)
        assertEquals("daily", p.schedule!!.kind)
        assertEquals(8, p.schedule!!.hourOfDay)
    }

    @Test
    fun `instruction without schedule yields null schedule`() {
        val p = NaturalLanguageGoalParser.parse("Summarize the current page")
        assertEquals(null, p.schedule)
    }
}
