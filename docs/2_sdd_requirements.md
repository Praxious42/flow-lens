# 2. Requirements

This chapter defines what FlowLens must do (functional requirements), how it
should behave (non-functional requirements), and which constraints shape the
design.

---

## 2.1 Functional Requirements

### 2.1.1 IntelliJ Plugin – Analysis & Inspections

**FR-1 – Project / module analysis**

- The plugin SHALL allow the user to trigger a FlowLens analysis for:
    - The current project.
    - The current module (if applicable).

**FR-2 – File-level analysis**

- The plugin SHALL automatically re-analyze an open Java file when it is saved,
  or when explicitly requested by the user (e.g. via a “Re-run FlowLens on File”
  action).

**FR-3 – Static inspections**

- The plugin SHALL provide a set of custom static inspections for Java code,
  including at minimum:
    - Detection of blocking calls in performance-sensitive contexts.
    - Detection of HTTP or remote calls without explicit timeouts.
    - Detection of inefficient or outdated collection and concurrency patterns
      in selected contexts.

**FR-4 – Inspection configuration**

- The plugin SHALL allow users to:
    - Enable or disable each FlowLens inspection.
    - Configure the severity level (e.g. warning / weak warning / information)
      via IntelliJ’s standard inspections settings UI.

### 2.1.2 IntelliJ Plugin – Feedback & Navigation

**FR-5 – Inline feedback**

- The plugin SHALL display findings as standard IntelliJ inspections, including:
    - Underlines / gutter markers at the relevant code locations.
    - Tooltips with a concise description of the issue.

**FR-6 – Quick fixes**

- For at least one inspection category, the plugin SHALL provide one or more
  quick fixes (e.g. adding a timeout argument, replacing an API usage with a
  safer alternative).

**FR-7 – FlowLens Report tool window**

- The plugin SHALL provide a “FlowLens Report” tool window that:
    - Lists all findings from the most recent analysis run.
    - Allows grouping or filtering by module, package, class, and smell type.
    - Allows navigation: clicking a finding opens the corresponding file and
      selects the relevant code region.

### 2.1.3 IntelliJ Plugin – AI Integration

**FR-8 – Result export**

- After a project/module analysis, the plugin SHALL be able to construct a
  compact summary of findings (e.g. in JSON) and send it to the backend service.

**FR-9 – AI summary retrieval**

- The plugin SHALL be able to request an AI-generated summary and refactoring
  suggestions from the backend for a given analysis run and display them in:
    - A dedicated “AI Suggestions” tab or section within the FlowLens Report
      tool window.

**FR-10 – Backend configuration**

- The plugin SHALL provide a way to configure the backend base URL (and, if
  applicable, authentication token) via a settings page or configuration file.

### 2.1.4 Backend Service

**FR-11 – REST API for analysis reports**

- The backend service SHALL expose a REST endpoint that accepts an analysis
  report from the plugin, containing:
    - Metadata about the project / module / analysis run.
    - A list of findings (code smell type, location, severity, short description).

**FR-12 – AI summarization endpoint**

- The backend service SHALL expose a REST endpoint that:
    - Receives a reference to an analysis run (or its data).
    - Invokes an LLM provider to generate:
        - A natural-language summary of key issues.
        - A structured list of refactoring suggestions.
    - Returns this information to the plugin in a structured format.

**FR-13 – LLM provider integration**

- The backend service SHALL integrate with at least one LLM provider via HTTP,
  using configurable API keys and endpoints.

**FR-14 – Basic persistence (optional / stretch)**

- (Stretch goal) The backend service MAY persist recent analysis runs and AI
  suggestions to a storage layer (e.g. in-memory or a simple database) to
  support later retrieval or comparison.

---

## 2.2 Non-Functional Requirements

### 2.2.1 Performance

**NFR-1 – IDE responsiveness**

- FlowLens analyses SHALL NOT block the IntelliJ IDEA UI thread.
- The plugin SHALL perform PSI traversal and inspections in background tasks
  where possible.

**NFR-2 – Analysis time**

- For a typical medium-sized Java project (e.g. up to ~100k lines of Java code),
  a full project-level FlowLens analysis SHOULD complete within a reasonable
  time (e.g. under 10–15 seconds) on a modern development machine.

**NFR-3 – Backend latency**

- For an average-sized analysis report, the round-trip time from plugin to
  backend to LLM and back SHOULD aim to remain under a few seconds (e.g.
  < 5 seconds) for AI summaries, assuming network conditions and LLM provider
  performance are normal.

### 2.2.2 Reliability & Resilience

**NFR-4 – Degraded operation**

- If the backend service or LLM provider is unavailable, the plugin SHALL:
    - Continue to provide local static inspections and quick fixes.
    - Display a clear, non-intrusive warning in the UI indicating that AI
      suggestions are currently unavailable.

**NFR-5 – Error handling**

- Failures in backend calls or AI responses SHALL be logged with sufficient
  detail on the backend.
- The plugin SHALL surface user-friendly error messages without exposing
  sensitive details.

### 2.2.3 Usability

**NFR-6 – Non-intrusive UX**

- FlowLens SHALL integrate with IntelliJ IDEA idioms:
    - Use standard inspection and tool window patterns.
    - Avoid modal dialogs and disruptive popups for analysis results.
- AI suggestions SHALL be opt-in (via explicit action or configuration), or
  clearly indicated as an additional layer on top of static analysis.

**NFR-7 – Discoverability**

- FlowLens SHALL provide:
    - Clear naming for menu items and tool windows.
    - A short “Getting Started” or help link from within the plugin to the
      project README or documentation.

### 2.2.4 Security & Privacy

**NFR-8 – API key handling**

- LLM provider API keys SHALL NOT be hardcoded in the source code.
- Keys SHALL be provided via environment variables or configuration files and
  documented in deployment instructions.

**NFR-9 – Data transmitted to backend**

- Only the minimum necessary code snippets and metadata SHALL be sent to the
  backend/LLM (e.g. excerpts around findings rather than entire files).
- The scope of what is sent SHALL be clearly documented so users understand
  potential privacy implications.

### 2.2.5 Maintainability & Extensibility

**NFR-10 – Extensible inspection set**

- The codebase SHALL be structured so that new inspections can be added with
  minimal impact on existing ones (e.g. via a registry or well-defined
  extension interfaces).

**NFR-11 – Clear separation of concerns**

- IntelliJ plugin code SHALL remain focused on IDE integration and static
  analysis.
- Backend code SHALL remain focused on report processing, AI integration and
  optional persistence.
- Cross-cutting concerns (logging, error handling) SHALL be implemented in a
  consistent, centralized way within each component.

---

## 2.3 Constraints

### 2.3.1 Technical Constraints

- **C-1 – IDE Platform**
    - The plugin MUST target the IntelliJ Platform (IntelliJ IDEA) using the
      official IntelliJ Platform SDK and recommended Gradle-based build setup.

- **C-2 – Language & Runtime**
    - The plugin implementation MUST be primarily in Java.
    - The backend MUST run on a supported Java LTS version (e.g. Java 17 or 21)
      and MAY use Spring Boot for rapid development.

- **C-3 – External Dependencies**
    - LLM integration MUST rely on a publicly accessible HTTP API from a provider
      that can be called from the backend environment.
    - The system MUST not rely on closed, proprietary internal JetBrains APIs.

### 2.3.2 Project Constraints

- **C-4 – Timebox**
    - The initial version of FlowLens is designed to be implemented by a single
      developer within roughly three months of part-time work (~10–12 hours per
      week). This constrains scope and depth of features.

- **C-5 – Environment**
    - The primary target environment for demonstration is:
        - Local IntelliJ IDEA running the plugin.
        - Local backend service running either directly (Java process) or via
          Docker/docker-compose.

- **C-6 – Licensing & Distribution**
    - The project SHALL use a permissive open-source license (e.g. MIT or
      Apache-2.0) if published publicly.
    - Any third-party dependencies (including LLM clients) MUST be compatible
      with the chosen license and with redistribution of the plugin.

---

These requirements form the basis for the subsequent design and architecture
decisions described in the following chapters.
