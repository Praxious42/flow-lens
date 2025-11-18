# 1. Introduction

## 1.1 Context

Modern Java applications often run in cloud or distributed environments where
**performance, resiliency and observability** matter as much as correctness.
While IntelliJ IDEA already provides rich inspections and refactorings, many
performance-sensitive or “cloud-readiness” issues remain hard to spot early:

- Blocking calls hiding in hot paths.
- Missing timeouts and retries on outbound calls.
- Concurrency and collection usage patterns that don’t scale under load.

At the same time, large language models (LLMs) are increasingly used to help
developers understand and improve code bases, but are usually exposed as
generic chat tools, disconnected from the IDE’s precise static analysis.

FlowLens aims to bridge that gap.

## 1.2 Problem Statement

Developers lack an easy way to:

- Identify **performance-sensitive code smells** (blocking I/O, missing timeouts,
  concurrency antipatterns) early in the development cycle.
- Prioritize which findings **actually matter** for runtime behavior among a sea
  of generic warnings.
- Get **targeted, contextual refactoring guidance** that combines static analysis
  precision with LLM-style explanation, directly inside IntelliJ IDEA.

As a result, issues often surface late (under load tests or in production),
and existing tools either flood developers with raw findings or stay too
generic to be actionable.

## 1.3 Goals

FlowLens is an IntelliJ IDEA plugin plus backend service with the following
goals:

1. **Expose focused, cloud-aware inspections for Java code**  
   Detect a small but high-impact set of smells related to performance,
   concurrency and resiliency, directly in the IDE.

2. **Provide actionable, IDE-native feedback**  
   Surface findings via inline inspections, quick fixes, and a dedicated tool
   window that supports navigation and triage.

3. **Prioritize issues with AI-backed summaries**  
   Use an LLM (via a backend service) to summarize and prioritize findings,
   helping developers understand “what to fix first and why”.

4. **Respect developer workflows and IDE performance**  
   Run analyses in a way that does not block the UI thread, scales to realistic
   project sizes, and fits naturally into existing IntelliJ usage.

5. **Demonstrate a clean, extensible architecture**  
   Separate IDE-level static analysis from server-side processing and AI
   integration so new inspections and backends can be added over time.

## 1.4 Non-Goals

To keep the scope focused and realistic, FlowLens explicitly does **not** aim to:

- Replace full observability / APM tools (e.g. production tracing, live metrics).
- Provide exhaustive static analysis for all Java or JVM languages.
- Implement full auto-refactoring powered by AI (all refactoring suggestions
  remain advisory, not automatically applied).
- Support every build and runtime environment out of the box (initial focus is
  standard Java projects in IntelliJ IDEA).

These non-goals are important to keep the implementation achievable within a
single-developer, time-boxed effort.

## 1.5 High-Level Solution Overview

FlowLens consists of two main parts:

- **IntelliJ IDEA Plugin (client)**
    - Implements custom inspections, quick fixes and a code completion
      contributor for Java.
    - Collects findings during local analysis runs.
    - Presents results inline and in a dedicated “FlowLens Report” tool window.
    - Optionally sends a compact summary of findings to the backend.

- **Backend Service (server)**
    - Exposes a REST API to receive analysis reports from the plugin.
    - Integrates with an LLM provider to produce human-readable summaries and
      refactoring suggestions.
    - Returns structured recommendations that the plugin displays to the user.

From a developer’s perspective:

1. They trigger a **FlowLens analysis** on their current project or module.
2. The **plugin** runs targeted inspections and aggregates findings.
3. Findings are shown immediately in the IDE; optionally, the **backend**
   generates an AI summary and prioritized refactoring hints.
4. The developer reviews issues in the editor and tool window, and applies
   quick fixes where appropriate.

## 1.6 Intended Audience

This document is intended for:

- **Developers** working on FlowLens (plugin or backend).
- **Reviewers and interviewers** evaluating the design and implementation.
- **Future contributors** who need a high-level understanding of the system
  before diving into the codebase.

The remainder of the document assumes familiarity with Java, IntelliJ IDEA, and
basic client–server concepts, but does not require prior knowledge of the
IntelliJ Platform SDK specifics.