// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.security

/**
 * Detects likely secrets/credentials in text before it is stored in memory,
 * sent to a provider, or written to logs (spec sections 18, 41, 43).
 * This is a defense-in-depth heuristic filter, not a guarantee.
 */
class SensitiveDataFilter {

    private val patterns = listOf(
        // API keys / provider tokens
        Regex("""sk-[A-Za-z0-9_\-]{16,}"""),                       // OpenAI-style
        Regex("""sk-ant-[A-Za-z0-9_\-]{16,}"""),                   // Anthropic
        Regex("""AIza[0-9A-Za-z_\-]{30,}"""),                      // Google
        Regex("""ghp_[A-Za-z0-9]{30,}"""),                         // GitHub PAT
        Regex("""gho_[A-Za-z0-9]{30,}"""),
        Regex("""xox[baprs]-[A-Za-z0-9\-]{10,}"""),                // Slack
        // Generic assignments
        Regex("""(?i)\b(api[_-]?key|secret|token|password|passwd|pwd|authorization|private[_-]?key)\b\s*[:=]\s*\S+"""),
        // Long base64-ish blobs that look like PEM payloads
        Regex("""-----BEGIN (RSA |EC |OPENSSH |PGP )?PRIVATE KEY-----"""),
        Regex("""[A-Za-z0-9+/]{60,}={0,2}"""),
        // Cookie / session headers
        Regex("""(?i)\b(cookie|set-cookie|session[-_]?id)\b\s*[:=]\s*\S+"""),
    )

    /** Returns true when [text] appears to contain credentials or secrets. */
    fun containsSensitiveData(text: String): Boolean =
        patterns.any { it.containsMatchIn(text) }

    /** Returns [text] with likely secrets redacted. */
    fun redact(text: String): String {
        var out = text
        for (p in patterns) out = p.replace(out) { m -> "[REDACTED]" }
        return out
    }
}
