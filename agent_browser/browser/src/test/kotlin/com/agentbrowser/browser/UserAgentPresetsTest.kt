// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserAgentPresetsTest {

    @Test
    fun `exactly ten presets exist`() {
        assertEquals(10, UserAgentManager.Companion.PRESETS.size)
    }

    @Test
    fun `preset ids are unique`() {
        val ids = UserAgentManager.Companion.PRESETS.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `all presets carry a non-empty UA string`() {
        UserAgentManager.Companion.PRESETS.forEach { p ->
            assertTrue(p.userAgent.isNotBlank())
            assertTrue(p.userAgent.startsWith("Mozilla/5.0"))
        }
    }

    @Test
    fun `mobile presets contain Mobile token and desktop ones do not`() {
        val byId = UserAgentManager.Companion.PRESETS.associateBy { it.id }
        assertTrue(byId.getValue("android_chrome").userAgent.contains("Mobile"))
        assertTrue(byId.getValue("ios_iphone_safari").userAgent.contains("Mobile"))
        assertTrue(!byId.getValue("desktop_chrome_windows").userAgent.contains("Mobile"))
        assertTrue(!byId.getValue("desktop_firefox").userAgent.contains("Mobile"))
    }

    @Test
    fun `desktop preset ids all start with desktop_`() {
        UserAgentManager.Companion.PRESETS
            .filter { it.id.startsWith("desktop_") }
            .forEach { assertTrue(it.userAgent.contains("Windows") || it.userAgent.contains("Macintosh") || it.userAgent.contains("X11")) }
    }
}
