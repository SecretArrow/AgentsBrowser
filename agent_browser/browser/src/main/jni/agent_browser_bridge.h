// Copyright 2026 The Agent Browser Authors. All rights reserved.
// Use of this source code is governed by an Apache-2.0 license that can be
// found in the LICENSE file.

#ifndef AGENT_BROWSER_BROWSER_SRC_MAIN_JNI_AGENT_BROWSER_BRIDGE_H_
#define AGENT_BROWSER_BROWSER_SRC_MAIN_JNI_AGENT_BROWSER_BRIDGE_H_

#include <jni.h>

#include "base/android/jni_weak_ref.h"
#include "base/memory/no_destructor.h"

namespace agent_browser {

// Forward declaration of the generated binding holder; jni_zero generates the
// full JNI registration table from Kotlin/Java @JniNatives declarations.
struct AgentBrowserBridgeJava;

// Controlled C++ side of the agent ↔ WebContents bridge.
class AgentBrowserBridge {
 public:
  static void BindJavaRef(JNIEnv* env, const base::android::JavaRef<jobject>& obj);
};

}  // namespace agent_browser

#endif  // AGENT_BROWSER_BROWSER_SRC_MAIN_JNI_AGENT_BROWSER_BRIDGE_H_
