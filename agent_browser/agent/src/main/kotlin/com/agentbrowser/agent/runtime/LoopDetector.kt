// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.agent.runtime

import java.util.ArrayDeque

/**
 * Loop detection (spec section 55): pauses a task after repeated identical
 * action+observation pairs instead of hammering a website.
 */
class LoopDetector(private val threshold: Int = 3) {

    private data class Signature(val action: String, val observationHash: Int)

    private val history = ArrayDeque<Signature>()

    @Synchronized
    fun recordAndCheck(action: String, observationKey: String): Boolean {
        val sig = Signature(action, observationKey.hashCode())
        history.addLast(sig)
        while (history.size > threshold * 2) history.removeFirst()
        val same = history.count { it == sig }
        return same >= threshold
    }

    @Synchronized
    fun reset() = history.clear()
}
