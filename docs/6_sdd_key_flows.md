# 6. Key Flows

This chapter describes the main runtime flows in FlowLens. Each flow shows:

- Which components are involved.
- The order of interactions.
- Key decisions and error paths.

These flows can be used as the basis for sequence diagrams (e.g. in PlantUML,
Mermaid, Excalidraw, or draw.io) and for manual test scenarios.

---

## 6.1 Overview of Flows

The primary flows covered here are:

1. **Flow A – Local Static Analysis in the IDE**  
   User runs FlowLens analysis; plugin produces findings locally.

2. **Flow B – AI-Augmented Summary & Suggestions**  
   Plugin sends findings to backend and retrieves AI-generated advice.

3. **Flow C – Applying a Quick Fix in the IDE**  
   User accepts a suggested quick fix to change code.

4. **Flow D – Configuring and Testing Backend Integration**  
   User configures backend URL and verifies connectivity.

Each flow assumes IntelliJ IDEA is running with the FlowLens plugin installed.
Where relevant, we’ll explicitly mention failure conditions and fallbacks.

---

## 6.2 Flow A – Local Static Analysis in the IDE

**Goal:** Run FlowLens analysis over a project or module and display findings
using IntelliJ’s inspection mechanisms and the FlowLens Report tool window.

### 6.2.1 Actors and Components

- **User**
- **IntelliJ Platform**
- **FlowLens Plugin:**
    - `Inspection Engine`
    - `AnalysisResultModel`
    - `FlowLens Report Tool Window`

### 6.2.2 Steps (Happy Path)

1. **User triggers analysis**
    - The user initiates FlowLens analysis via:
        - A menu action (e.g. `Tools → Run FlowLens Analysis`), or
        - A button in the FlowLens Report tool window.

2. **IDE delegates to plugin action**
    - IntelliJ invokes a FlowLens action handler (e.g. `RunAnalysisAction`).
    - The handler determines analysis scope (project or module).

3. **Analysis job scheduled**
    - The plugin schedules an analysis job on a background thread:
        - Ensures no heavy work is done on the UI thread.
        - Uses IntelliJ’s recommended background task APIs.

4. **PSI traversal and inspection execution**
    - The `Inspection Engine`:
        - Enumerates Java files in the selected scope.
        - For each file:
            - Obtains a PSI tree (read access).
            - Applies each registered FlowLens inspection.
        - For each identified issue:
            - Creates a `Finding` object with location, type, severity, message.
            - Registers a `ProblemDescriptor` with IntelliJ so the issue appears
              inline (underlines, gutter icons, tooltips).

5. **Aggregate results in AnalysisResultModel**
    - As findings are produced:
        - They are added to the in-memory `AnalysisResultModel`.
    - When the analysis completes:
        - The model contains the full set of findings for this run.
        - Metrics (e.g. files scanned, duration) are recorded.

6. **Update FlowLens Report tool window**
    - On the UI thread, the plugin:
        - Notifies the FlowLens Report view that a new analysis run is available.
        - The tool window:
            - Reads from `AnalysisResultModel`.
            - Renders findings grouped by module/package/class and smell type.
            - Shows basic metrics (optional).

7. **User explores results**
    - The user:
        - Clicks on entries in the tool window to navigate to code locations.
        - Hovers over underlined code to read inline messages.
        - Optionally expands/collapses groups to triage issues.

### 6.2.3 Failure / Edge Cases

- **Interrupted analysis**
    - If the user cancels the analysis or the IDE requests shutdown:
        - The analysis job stops traversal gracefully.
        - Partial findings may be discarded or flagged as incomplete.
- **PSI access issues**
    - If files change during analysis:
        - Analysis for that file may be re-scheduled.
        - The plugin should handle stale PSI references by re-resolving them.

---

## 6.3 Flow B – AI-Augmented Summary & Suggestions

**Goal:** Enrich a completed analysis run with an AI-generated summary and
suggestions from the backend service.

### 6.3.1 Actors and Components

- **User**
- **FlowLens Plugin:**
    - `AnalysisResultModel`
    - `Backend Client`
    - `FlowLens Report Tool Window`
- **FlowLens Backend:**
    - `REST API Layer`
    - `Analysis Service`
    - `LLM Integration Module`
- **External LLM Provider**

### 6.3.2 Steps (Happy Path)

1. **User requests AI summary**
    - After Flow A completes, the user:
        - Clicks a “Request AI Summary” button in the tool window, or
        - Has an option enabled that automatically requests AI analysis.

2. **Plugin prepares request payload**
    - `Backend Client`:
        - Reads the latest `AnalysisRun` from `AnalysisResultModel`.
        - Selects a subset of findings (e.g. up to `maxFindings`).
        - Maps them to an `AnalysisReportDto`:
            - Includes `analysisId`, scope, timestamp, metrics, and selected findings.
            - Truncates `codeSnippet` fields to a safe length.

3. **Plugin sends Analysis Report to backend**
    - `Backend Client` performs an HTTP `POST /api/analysis`:
        - Uses configured backend base URL.
        - Sends JSON payload.
        - Applies a reasonable timeout.

4. **Backend REST API receives request**
    - `AnalysisController`:
        - Validates required fields.
        - Maps `AnalysisReportRequest` DTO to `AnalysisRun` + `Finding` domain models.
    - If validation fails:
        - Returns an HTTP 400 error with an `ErrorResponse`.

5. **Analysis Service registers the run**
    - `AnalysisService`:
        - Stores the `AnalysisRun` (in memory or persistence).
        - May perform simple pre-processing (grouping, severity counts).

6. **Backend requests AI summary**
    - Depending on chosen design, either:
        - AI summarization happens immediately within the same `POST /analysis`
          request, or
        - The plugin makes a second call like `POST /api/analysis/{analysisId}/summary`.
    - In either case:
        - `AnalysisService` selects key findings (e.g. by severity).
        - Builds an internal representation of the prompt.

7. **LLM Integration Module calls external API**
    - `LlmClient`:
        - Constructs the text prompt using:
            - High-level description of FlowLens and smell types.
            - Structured list of top findings with brief context/snippets.
        - Sends HTTPS request to LLM provider with API key.
        - Waits for response (respecting timeouts and retries).

8. **LLM Provider returns AI output**
    - The provider returns:
        - A natural-language response string.
        - Possibly structured content (e.g. JSON embedded in text).

9. **Backend parses AI response**
    - `LlmClient` / `AnalysisService`:
        - Extracts:
            - `AiSummary` (title + body).
            - One or more `AiSuggestion` objects:
                - Category, priority, related finding IDs, description.
        - Wraps them in an `AiSummaryResponse` DTO.

10. **Backend responds to plugin**
    - `AnalysisController` returns HTTP 200 with JSON body:
        - `analysisId`
        - `summary`
        - `suggestions` (list)

11. **Plugin updates UI with AI results**
    - `Backend Client` deserializes response into:
        - `AiSummary`
        - List of `AiSuggestion`
    - `AnalysisResultModel` stores AI data against the current analysis run.
    - FlowLens Report:
        - Updates or shows an “AI Suggestions” / “AI Summary” section.
        - Highlights high-priority suggestions.
        - Links each suggestion to associated findings for navigation.

### 6.3.3 Failure / Edge Cases

- **Backend unreachable / timeout**
    - `Backend Client`:
        - Receives a network error or timeout.
        - Records the error and returns a failure result.
    - FlowLens Report:
        - Displays a small message:  
          “AI suggestions are currently unavailable. Static analysis results remain
          accessible.”

- **Backend returns error (4xx/5xx)**
    - Plugin:
        - Parses `ErrorResponse` if present (e.g. invalid payload).
        - Shows a user-friendly message and logs technical detail at debug level.

- **LLM provider failure**
    - Backend:
        - Catches the error, logs it.
        - Returns a specific error payload (e.g. `aiUnavailable: true`).
    - Plugin:
        - Treats this similarly to backend failure; shows a non-intrusive message.

In all these cases, **static analysis results remain available**; only the
AI-enhanced features are degraded.

---

## 6.4 Flow C – Applying a Quick Fix in the IDE

**Goal:** Allow the user to apply a FlowLens-provided quick fix to address a
specific finding, using IntelliJ’s standard quick fix mechanism.

### 6.4.1 Actors and Components

- **User**
- **IntelliJ Platform**
- **FlowLens Plugin:**
    - `Inspection Engine` (ProblemDescriptors)
    - `Quick Fix` implementations

### 6.4.2 Steps (Happy Path)

1. **User notices a finding**
    - In the editor, the user sees:
        - Underlined code, or
        - A gutter icon, or
        - A lightbulb icon (intention/quick fix available).

2. **User invokes quick fix**
    - The user presses the standard shortcut (e.g. `Alt+Enter`) or clicks the
      lightbulb icon.
    - IntelliJ shows available quick fixes for that `ProblemDescriptor`.

3. **User selects FlowLens quick fix**
    - The user chooses one of the FlowLens quick fixes (e.g. “Add timeout
      argument” or “Replace Vector with ConcurrentHashMap”).

4. **IntelliJ calls quick fix implementation**
    - IntelliJ invokes `applyFix` on the selected `LocalQuickFix`.
    - The plugin:
        - Obtains a write action.
        - Locates the relevant PSI element(s).
        - Applies the desired transformation (e.g. adding argument, replacing
          method call or type).

5. **IDE updates editor and PSI**
    - The code change is reflected in the editor.
    - IntelliJ re-parses the modified file.
    - FlowLens inspections may re-run for the modified region/file.

6. **Finding is resolved**
    - The original finding may:
        - Disappear (if the issue is fixed).
        - Reappear with updated location/description (if partially addressed).
    - The FlowLens Report view will update on the next analysis run, or
      incrementally if wired to live inspections.

### 6.4.3 Failure / Edge Cases

- **Refactoring not applicable**
    - If `applyFix` determines that the expected PSI pattern is not present
      (e.g. due to concurrent edits), it should:
        - Abort gracefully without changing code.
        - Optionally show a brief message: “Quick fix is no longer applicable.”

- **Conflicting edits**
    - If another plugin or user action modified the same region:
        - IntelliJ’s standard conflict resolution applies.
        - FlowLens quick fix must keep changes minimal and well-scoped.

---

## 6.5 Flow D – Configuring and Testing Backend Integration

**Goal:** Configure the backend URL/API in the plugin and verify that the
plugin can successfully communicate with the backend.

### 6.5.1 Actors and Components

- **User**
- **FlowLens Plugin:**
    - `Settings / Configuration UI`
    - `Backend Client`
- **FlowLens Backend**

### 6.5.2 Steps (Happy Path)

1. **User opens FlowLens settings**
    - In IntelliJ:
        - `Settings → Tools → FlowLens` (example path).
    - A FlowLens-specific settings page appears.

2. **User configures backend URL**
    - User enters:
        - Backend base URL (e.g. `http://localhost:8080`).
        - Optional authentication token or header key (if used).
    - User saves the configuration.

3. **Plugin stores configuration**
    - Settings are written using IntelliJ’s persistent settings APIs.
    - `BackendConfig` is updated in memory.

4. **User tests connection (optional but recommended)**
    - User clicks a “Test Connection” button.
    - Plugin’s `Backend Client`:
        - Sends a lightweight `GET /api/health` or similar endpoint.
        - Waits for HTTP 200 OK.

5. **Backend responds**
    - If healthy, backend returns success JSON (e.g. `{ "status": "UP" }`).

6. **Plugin displays result**
    - On success:
        - Shows a small “Connection successful” notification.
    - On failure:
        - Shows a clear error:
            - “Failed to connect to FlowLens backend at http://localhost:8080”
            - Suggests checking server status or URL.

### 6.5.3 Failure / Edge Cases

- **Invalid URL**
    - Plugin detects malformed URL and prevents saving, showing inline validation
      error.

- **Backend unreachable**
    - “Test Connection” call fails:
        - Plugin shows friendly error, logs details.
        - Settings may still be saved, but user is warned that backend is not
          currently reachable.

---

## 6.6 Summary

These key flows define how FlowLens behaves in practice:

- **Flow A** ensures local static analysis is robust and IDE-friendly.
- **Flow B** shows how AI integration is layered on top of static analysis via
  a backend service.
- **Flow C** covers how users act on findings through safe, minimal quick fixes.
- **Flow D** describes configuration and connectivity checks for the backend.

Together, they provide a concrete, end-to-end picture of the system’s runtime
behavior and serve as a basis for sequence diagrams, testing, and discussions
during design and code reviews.
