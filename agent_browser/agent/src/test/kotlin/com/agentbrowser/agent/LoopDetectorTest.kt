// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.agent

import com.agentbrowser.agent.runtime.LoopDetector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoopDetectorTest {

    @Test
    fun `flags after threshold repeats of same action and observation`() {
        val d = LoopDetector(threshold = 3)
        assertFalse(d.recordAndCheck("clickElement", "same"))
        assertFalse(d.recordAndCheck("clickElement", "same"))
        assertTrue(d.recordAndCheck("clickElement", "same"))
    }

    @Test
    fun `varying observations do not trigger loop`() {
        val d = LoopDetector(threshold = 3)
        assertFalse(d.recordAndCheck("clickElement", "a"))
        assertFalse(d.recordAndCheck("clickElement", "b"))
        assertFalse(d.recordAndCheck("clickElement", "c"))
    }

    @Test
    fun `reset clears history`() {
        val d = LoopDetector(threshold = 2)
        d.recordAndCheck("clickElement", "x")
        d.reset()
        assertFalse(d.recordAndCheck("clickElement", "x"))
    }
}
