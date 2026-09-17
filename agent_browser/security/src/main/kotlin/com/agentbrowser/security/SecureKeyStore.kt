// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.
package com.agentbrowser.security

import android.content.Context
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Secure storage for AI provider API keys (spec section 18).
 * Keys are encrypted with an Android Keystore AES-GCM key and stored in app
 * private preferences — never in source, plaintext prefs, logs, or memory.
 */
class SecureKeyStore(private val context: Context) {

    private val ks: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val alias = "agent_browser_keystore_v1"

    private fun ensureKey(): SecretKey {
        (ks.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance("AES", "AndroidKeyStore")
        gen.init(android.security.keystore.KeyGenParameterSpec.Builder(
            alias,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                android.security.keystore.KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build())
        return gen.generateKey()
    }

    @Synchronized
    fun storeKey(provider: String, apiKey: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, ensureKey())
        val iv = cipher.iv
        val ct = cipher.doFinal(apiKey.toByteArray(Charsets.UTF_8))
        prefs().edit()
            .putString("iv:$provider", Base64.encodeToString(iv, Base64.NO_WRAP))
            .putString("ct:$provider", Base64.encodeToString(ct, Base64.NO_WRAP))
            .apply()
    }

    @Synchronized
    fun retrieveKey(provider: String): String? {
        val ivB64 = prefs().getString("iv:$provider", null) ?: return null
        val ctB64 = prefs().getString("ct:$provider", null) ?: return null
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, ensureKey(), GCMParameterSpec(128, Base64.decode(ivB64, Base64.NO_WRAP)))
            String(cipher.doFinal(Base64.decode(ctB64, Base64.NO_WRAP)), Charsets.UTF_8)
        }.getOrNull()
    }

    @Synchronized
    fun removeKey(provider: String) {
        prefs().edit().remove("iv:$provider").remove("ct:$provider").apply()
    }

    fun storedProviders(): List<String> =
        prefs().all.keys.filter { it.startsWith("ct:") }.map { it.removePrefix("ct:") }

    private fun prefs() = context.getSharedPreferences("agent_keys", Context.MODE_PRIVATE)
}
