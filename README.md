# flow-lens

![Build](https://github.com/Praxious42/flow-lens/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

## Template ToDo list
- [x] Create a new [IntelliJ Platform Plugin Template][template] project.
- [X] Get familiar with the [template documentation][template].
- [X] Adjust the [pluginGroup](./gradle.properties) and [pluginName](./gradle.properties), as well as the [id](./src/main/resources/META-INF/plugin.xml) and [sources package](./src/main/kotlin).
- [X] Adjust the plugin description in `README` (see [Tips][docs:plugin-description])
- [ ] Review the [Legal Agreements](https://plugins.jetbrains.com/docs/marketplace/legal-agreements.html?from=IJPluginTemplate).
- [ ] [Publish a plugin manually](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate) for the first time.
- [ ] Set the `MARKETPLACE_ID` in the above README badges. You can obtain it once the plugin is published to JetBrains Marketplace.
- [ ] Set the [Plugin Signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html?from=IJPluginTemplate) related [secrets](https://github.com/JetBrains/intellij-platform-plugin-template#environment-variables).
- [ ] Set the [Deployment Token](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html?from=IJPluginTemplate).
- [ ] Click the <kbd>Watch</kbd> button on the top of the [IntelliJ Platform Plugin Template][template] to be notified about releases containing new features and fixes.
- [ ] Configure the [CODECOV_TOKEN](https://docs.codecov.com/docs/quick-start) secret for automated test coverage reports on PRs

<!-- Plugin description -->
This Fancy IntelliJ Platform Plugin is going to be your implementation of the brilliant ideas that you have.

This specific section is a source for the [plugin.xml](/src/main/resources/META-INF/plugin.xml) file which will be extracted by the [Gradle](/build.gradle.kts) during the build process.

To keep everything working, do not remove `<!-- ... -->` sections. 
<!-- Plugin description end -->

# FlowLens – Cloud-Ready Java Code Smell & Refactoring Assistant

<!-- Logo -->
<p align="center">
  <img src="docs/logo.png" alt="FlowLens Logo" width="180"/>
</p>

**FlowLens** is an IntelliJ IDEA plugin that helps you spot and fix performance-sensitive Java code smells before they reach production.

It analyzes your code directly in the IDE, highlights risky patterns (like blocking calls in hot paths or missing timeouts), and optionally uses an AI backend to propose refactorings tailored to your project.

FlowLens is designed as a small but realistic companion to modern Java development: static analysis lives in the IDE, deeper reasoning and AI run in a separate backend service, and you get clear, actionable feedback right where you write code.

---

## Features

### 🔍 Static inspections for cloud-ready Java

- Detect blocking calls in performance-critical code paths.
- Warn about HTTP calls without timeouts and other resiliency issues.
- Flag inefficient collection usage and concurrency antipatterns.

### 🛠 Quick fixes and navigation

- One-click quick fixes for common issues (e.g. adding timeouts, replacing problematic collections).
- Tooltips and links to documentation for each inspection.
- Navigate from a finding directly to the affected code location.

### 📊 FlowLens Report tool window

- Run a project- or module-wide analysis from IntelliJ IDEA.
- View results grouped by module, package, class, and smell type.
- See *hotspots* – methods with the highest concentration of issues.

### 🤖 AI-powered suggestions (optional)

- Sends a compact summary of findings to a backend service.
- Uses an LLM to generate human-readable summaries and refactoring ideas.
- Displays AI suggestions alongside static analysis results in the IDE.

### ☁️ Backend service integration

- Java/Spring Boot REST API for receiving analysis reports.
- Pluggable LLM integration (e.g. OpenAI or other providers).
- Dockerized backend for easy local deployment.

---

## Why FlowLens?

Traditional inspections are great at spotting local issues, but they rarely tell you **which problems matter most for runtime behavior**.

FlowLens focuses on cloud-readiness and performance-sensitive patterns, combining IDE-level static analysis with an AI “second opinion” so you can:

- Tighten up critical paths before they become bottlenecks.
- Enforce team-level guidelines around timeouts, concurrency, and resiliency.
- Get concise, prioritized feedback instead of raw warning spam.

---

## Getting Started

> ⚠️ **Work in progress** – commands and setup steps may change as the project evolves.

### Prerequisites

- IntelliJ IDEA (Community or Ultimate)
- JDK 17+ (or your project’s target version)
- Docker (for running the backend, optional but recommended)

### Installation (Plugin)

1. Build the plugin:
   ```bash
   ./gradlew buildPlugin


## Installation

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "FlowLens"</kbd> >
  <kbd>Install</kbd>
  
- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/Praxious42/flow-lens/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
