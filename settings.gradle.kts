pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AgentsBrowser"

// Logical module names map onto the agent_browser/ directory tree so
// dependencies read project(":core"), project(":ai"), etc.
include(":core", ":ai", ":automation", ":security", ":memory", ":agent", ":scheduler", ":browser", ":ui")

project(":core").projectDir = file("agent_browser/core")
project(":ai").projectDir = file("agent_browser/ai")
project(":automation").projectDir = file("agent_browser/automation")
project(":security").projectDir = file("agent_browser/security")
project(":memory").projectDir = file("agent_browser/memory")
project(":agent").projectDir = file("agent_browser/agent")
project(":scheduler").projectDir = file("agent_browser/scheduler")
project(":browser").projectDir = file("agent_browser/browser")
project(":ui").projectDir = file("agent_browser/ui")
