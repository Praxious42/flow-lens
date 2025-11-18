# 7. Non-Functional Design

This chapter explains **how** FlowLens is designed to meet its non-functional
requirements (NFRs), as defined in Chapter 2:

- Performance
- Reliability & resilience
- Usability
- Security & privacy
- Maintainability & extensibility

For each area, we reference the relevant NFR IDs and describe the mechanisms
and design choices that support them.

---

## 7.1 Performance

Relevant requirements: **NFR-1**, **NFR-2**, **NFR-3**

### 7.1.1 IDE Responsiveness (NFR-1)

**Design goals**

- Avoid blocking the IntelliJ IDEA UI thread.
- Keep perceived latency low for common operations.

**Design mechanisms**

- **Background tasks for analysis**
    - All heavy operations (e.g. scanning files, traversing PSI trees, running
      inspections over many files) are performed using IntelliJ’s background
      task mechanisms (e.g. `Task.Backgroundable`, `ProgressManager.run`).
    - The plugin only performs short-lived operations (e.g. UI updates) on the
      Event Dispatch Thread (EDT).

- **Read/write action discipline**
    - PSI access is wrapped in appropriate **read actions**.
    - Code modifications in quick fixes are performed within **write actions**
      and are kept as small and localized as possible.

- **Incremental analysis (where feasible)**
    - For file-level analysis, only the open file (or small set of impacted files)
      is re-analyzed, instead of re-running a full project scan.
    - Future enhancements can include caching results per file and invalidating
      them on changes.

### 7.1.2 Analysis Time (NFR-2)

**Design goals**

- For a medium-sized project (e.g. ~100k LOC), a project-level analysis should
  complete in a reasonable time (e.g. under 10–15 seconds).

**Design mechanisms**

- **Scope-aware analysis**
    - Users can choose to analyze a module instead of the entire project,
      limiting the number of files processed.
    - Internal APIs accept a configurable scope, so tests and demos can use
      smaller scopes when desired.

- **Efficient traversal & filtering**
    - Inspections focus on relevant PSI elements (e.g. method calls, annotations,
      certain constructs) instead of generically traversing the entire tree.
    - Each inspection avoids repeated expensive operations (e.g. unnecessary
      resolve calls or complex dataflow analysis) unless needed.

- **Lightweight metrics**
    - `AnalysisMetrics` capture basic timing and file counts.
    - These metrics can be logged during development to detect regressions.

### 7.1.3 Backend & AI Latency (NFR-3)

**Design goals**

- Keep end-to-end AI summary request latency within a few seconds for
  typical analysis sizes, assuming normal network and provider conditions.

**Design mechanisms**

- **Compact payloads**
    - The plugin sends only a subset of top findings (`maxFindings`) to avoid
      bloating request size.
    - Code snippets are truncated to a capped length (e.g. N characters/lines).

- **Time-bounded LLM calls**
    - Backend calls to the LLM provider are made with explicit timeouts.
    - Slow responses are treated as errors, triggering fallback behavior rather
      than blocking the user indefinitely.

- **Asynchronous perception in the IDE**
    - From the user’s perspective, AI summaries are an **add-on**:
        - Static analysis results are available first.
        - AI suggestions appear when ready, without blocking the user’s normal work.

---

## 7.2 Reliability & Resilience

Relevant requirements: **NFR-4**, **NFR-5**

### 7.2.1 Degraded Operation (NFR-4)

**Design goals**

- FlowLens continues to be useful even when the backend or LLM provider is
  unavailable.

**Design mechanisms**

- **Separation of concerns**
    - Static analysis logic is entirely local to the plugin and does not depend
      on the backend being reachable.
    - AI-related features are layered on top as optional enhancements.

- **Graceful failure of backend calls**
    - Backend client wraps all HTTP calls in try/catch blocks.
    - Errors are:
        - Logged (with as much detail as is safe).
        - Translated into a simple status for the UI (e.g. “AI unavailable”).

- **Non-blocking UI behavior**
    - If the backend fails:
        - The analysis results already computed remain visible.
        - The tool window simply omits or marks the AI summary section as
          unavailable.

### 7.2.2 Error Handling & Observability (NFR-5)

**Design goals**

- Failures are detectable and diagnosable without overwhelming the user
  with technical details.

**Design mechanisms**

- **Centralized error handling**
    - Backend uses a `@ControllerAdvice` or similar mechanism to convert
      exceptions into consistent `ErrorResponse` payloads with HTTP status codes.
    - Plugin’s backend client interprets these responses and maps them to
      user-friendly messages.

- **Structured logging**
    - Backend logs:
        - Request identifiers (e.g. `analysisId`).
        - LLM request/response durations.
        - Error types and stack traces (where appropriate).
    - Logs are designed to help troubleshoot:
        - Connectivity issues.
        - LLM provider errors.
        - Payload validation problems.

- **Minimal user-facing messages**
    - Plugin shows short, descriptive messages:
        - “Could not reach FlowLens backend at http://localhost:8080.”
        - “AI suggestions are temporarily unavailable.”
    - Technical details remain in logs for development and debugging.

---

## 7.3 Usability

Relevant requirements: **NFR-6**, **NFR-7**

### 7.3.1 Non-Intrusive UX (NFR-6)

**Design goals**

- Integrate FlowLens naturally into IntelliJ IDEA without disrupting existing
  workflows.

**Design mechanisms**

- **Use of standard IntelliJ patterns**
    - Findings presented as:
        - Standard inspections with underlines and gutter icons.
        - Lightbulb quick fixes using the usual shortcuts (e.g. `Alt+Enter`).
    - A dedicated **tool window** rather than modal dialogs for reports.

- **No aggressive popups**
    - FlowLens avoids:
        - Full-screen modals.
        - Frequent dialogs or notification spam.
    - Warnings about AI unavailability or backend issues are:
        - Shown in a small status area or notification.
        - Dismissible and not blocking.

- **Configurable behavior**
    - Future enhancements (and possibly initial version) can support toggles:
        - Whether to request AI summaries automatically or only on explicit user
          action.
        - Which inspections are enabled and at what severity.

### 7.3.2 Discoverability (NFR-7)

**Design goals**

- Make it easy for users to **find** and **understand** FlowLens features.

**Design mechanisms**

- **Consistent naming**
    - Actions and tool windows are clearly labeled with “FlowLens”.
    - Examples:
        - `Tools → FlowLens → Run Analysis`
        - `FlowLens Report` as the tool window name.

- **In-plugin help link**
    - A small “Help” or “More info” link in the tool window linking to:
        - The GitHub README, or
        - A dedicated online help page.
    - Gives quick access to:
        - Overview of features.
        - Setup instructions for the backend and AI integration.

- **Meaningful descriptions**
    - Inspection descriptions are written in plain language:
        - “Blocking call in a performance-sensitive method” instead of
          cryptic messages.
    - AI summary and suggestions use concise, prioritized phrasing.

---

## 7.4 Security & Privacy

Relevant requirements: **NFR-8**, **NFR-9**

### 7.4.1 API Key Handling (NFR-8)

**Design goals**

- Protect sensitive credentials used for LLM access.

**Design mechanisms**

- **No hardcoded secrets**
    - LLM API keys are never:
        - Checked into source control.
        - Embedded in plugin binaries.

- **Externalized configuration (backend)**
    - Backend reads keys from:
        - Environment variables, and/or
        - External configuration files (e.g. `application.yml`).
    - Deployment instructions explain:
        - How to set keys securely for local development (e.g. `.env` or OS-level
          environment variables).

- **Restricted usage**
    - Only the backend knows the key.
    - The plugin never sees the LLM API key; it only talks to the backend.

### 7.4.2 Data Minimization (NFR-9)

**Design goals**

- Limit what code and metadata are sent off the developer’s machine.

**Design mechanisms**

- **Selective snippets**
    - Only **small code snippets** surrounding findings are included, where
      necessary to provide context.
    - The maximum length of `codeSnippet` fields is capped.

- **Configurable behavior**
    - Future enhancement: a configuration switch for:
        - Including/excluding `codeSnippet` from AI requests.
        - Redacting or anonymizing some identifiers.

- **Clear documentation**
    - README / system design explicitly states:
        - What is sent to the backend.
        - That the backend may forward some of this data to an LLM provider.
    - Helps users make informed decisions about enabling AI features.

---

## 7.5 Maintainability & Extensibility

Relevant requirements: **NFR-10**, **NFR-11**

### 7.5.1 Extensible Inspection Set (NFR-10)

**Design goals**

- Make it straightforward to add, update, or remove inspections without
  rewriting core plumbing.

**Design mechanisms**

- **Modular inspection design**
    - Each inspection is a separate class extending IntelliJ’s inspection base.
    - Inspections are grouped under `analysis.inspections` in the plugin.

- **Registration & configuration**
    - New inspections are registered via standard IntelliJ mechanisms and,
      optionally, a small registry helper.
    - Configuration (enable/disable, severity) is driven by IntelliJ’s existing
      inspection settings, not a custom system.

- **Stable data model**
    - `Finding` and related types are generic enough that:
        - New smell types can reuse the same structures.
        - Type is a string/enum that can grow without breaking contracts.

### 7.5.2 Clear Separation of Concerns (NFR-11)

**Design goals**

- Keep plugin and backend focused on distinct responsibilities, and keep
  cross-cutting concerns manageable.

**Design mechanisms**

- **Layered backend architecture**
    - Controllers (API layer) handle HTTP and DTOs.
    - Service layer handles business logic and decision-making.
    - LLM integration layer isolates provider-specific behavior.

- **Clean plugin layering**
    - Analysis logic is decoupled from:
        - UI rendering (tool window, editor decorations).
        - Backend communication.
    - UI components interact with:
        - A shared `AnalysisResultModel`.
        - High-level service/controller classes within the plugin, not raw PSI.

- **Shared contracts**
    - Common domain types like `Finding`, `AnalysisRun`, `AiSummary` are:
        - Clearly defined.
        - Used across plugin and backend (usually with minor DTO adaptations).
    - Reduces duplication and inconsistencies.

### 7.5.3 Testing & Refactoring Friendliness

**Additional maintainability considerations**

- **Unit tests for core logic**
    - Backend analysis and LLM prompt-building logic can be unit-tested without
      running the full Spring Boot stack.
    - Plugin inspections can be tested using IntelliJ’s testing framework or
      smaller PSI-based tests.

- **Mockable LLM client**
    - `LlmClient` interface allows:
        - Mock/fake implementations for tests.
        - Easy switching between real and mock providers.

- **Explicit extension points**
    - Planned extension areas (e.g. new smell types, new LLM models) are
      recognized and given clear, documented entry points.

---

## 7.6 Summary

FlowLens’s non-functional design focuses on:

- **Being a “good citizen” inside the IDE** (non-blocking, predictable,
  unobtrusive).
- **Failing gracefully** when external dependencies (backend, LLM) are
  unavailable.
- **Respecting user code and credentials**, with minimal and controlled
  data sharing.
- **Staying easy to extend and reason about** via a clean separation of
  concerns and a stable data model.

These design choices support the requirements outlined earlier and provide
a strong story when explaining the system’s robustness and evolution in
interviews or design reviews.
