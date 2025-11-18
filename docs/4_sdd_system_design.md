# 4. Architecture & Components

This chapter describes the internal architecture of FlowLens and breaks it down
into its main components:

- IntelliJ IDEA plugin
- Backend service
- LLM integration

The goal is to show how responsibilities are split, how pieces talk to one
another, and where future extensions can be added.

---

## 4.1 Architectural Principles

FlowLens follows a few simple architectural principles:

1. **Separation of concerns**
    - The IntelliJ plugin focuses on IDE integration and static analysis.
    - The backend focuses on report handling and AI/LLM integration.
    - The LLM provider remains behind a thin adapter.

2. **Explicit boundaries & contracts**
    - Plugin ↔ Backend communication uses explicit JSON data contracts.
    - Backend ↔ LLM communication is encapsulated behind an interface.

3. **Extensibility**
    - New inspections should be easy to add without changing core plumbing.
    - LLM providers should be swappable with minimal code changes.

4. **Performance & responsiveness**
    - All heavy work in the plugin runs off the UI thread.
    - Backend and LLM calls are asynchronous from the plugin’s perspective.

---

## 4.2 IntelliJ Plugin Architecture

### 4.2.1 Package Structure (Proposed)

A possible Java package layout for the plugin:

- `com.flowlens.plugin`
    - `analysis` – inspection rules and analysis orchestration
        - `inspections` – individual inspection implementations
        - `model` – in-memory representation of findings
    - `ui`
        - `toolwindow` – FlowLens Report tool window UI
        - `actions` – menu actions, toolbar actions
    - `backend`
        - `client` – HTTP client for talking to the backend
        - `dto` – DTOs for plugin ↔ backend JSON
        - `config` – backend URL and settings
    - `completion` – optional completion contributor(s)
    - `settings` – plugin-specific settings / config UI (if needed)

This structure keeps analysis, UI, and backend interaction in clearly separated
areas, making the plugin easier to reason about and extend.

### 4.2.2 Core Plugin Components

#### 4.2.2.1 Inspection Engine

**Responsibility**

- Implement FlowLens-specific static inspections for Java code:
    - Blocking calls in performance-sensitive contexts.
    - HTTP calls without explicit timeouts.
    - Risky or outdated collection/concurrency usage.

**Key IntelliJ Concepts**

- **PSI (Program Structure Interface)** for reading Java syntax trees.
- **Inspection API** (e.g. `LocalInspectionTool`) to define inspection rules.

**Example Classes**

- `BlockingCallInspection`
- `MissingTimeoutInspection`
- `InefficientCollectionInspection`
- `FlowLensInspectionRegistrar` (optional helper to register / group inspections)

**Behavior**

- When IntelliJ runs inspections:
    - The inspection classes visit PSI elements (methods, calls, etc.).
    - For each identified issue, they:
        - Register a `ProblemDescriptor` with a short description and location.
        - Optionally provide a `LocalQuickFix`.

#### 4.2.2.2 Quick Fixes & Completion Contributor

**Quick Fixes**

- Implemented as classes extending `LocalQuickFix`.
- Responsibilities:
    - Modify PSI safely inside write actions.
    - Provide user-friendly fix descriptions (“Add timeout argument”, etc.).

**Example Classes**

- `AddTimeoutQuickFix`
- `ReplaceCollectionQuickFix`

**Completion Contributor (Optional)**

- Implemented using IntelliJ’s `CompletionContributor`.
- Responsibilities:
    - Recognize contexts where FlowLens can assist (e.g. log statements, async
      patterns).
    - Suggest small templates (e.g. `CompletableFuture.supplyAsync(() -> {...})`).

**Design Notes**

- Completion is deliberately narrow in scope to keep complexity manageable.
- Quick fixes focus on simple, low-risk transformations.

#### 4.2.2.3 FlowLens Report Tool Window

**Responsibility**

- Present analysis results in a dedicated, explorable UI.
- Provide navigation & filtering capabilities.
- Display AI-generated summaries and suggestions when available.

**Key Elements**

- Tool window factory class, e.g. `FlowLensToolWindowFactory`.
- UI controller class, e.g. `FlowLensReportView`.
- Models representing data rendered in the tool window (e.g. `FindingViewModel`).

**Typical Structure**

- A tree/table or list layout showing:
    - Modules / packages / classes.
    - Findings grouped by smell type or severity.
- Details panel showing:
    - Selected finding details (message, code location, context).
    - AI summary and suggestions (if available).

**Integration with the Analysis Engine**

- The analysis engine publishes findings to a shared in-memory model
  (`AnalysisResultModel`).
- The tool window subscribes or pulls from this model when:
    - An analysis completes.
    - The user requests refresh.

#### 4.2.2.4 Backend Client

**Responsibility**

- Communicate with the FlowLens backend over HTTP.
- Serialize/deserialize analysis reports and AI responses.
- Handle configuration and error conditions.

**Example Classes**

- `FlowLensBackendClient`
- `BackendConfig` / `BackendSettings`
- DTOs under `backend.dto`:
    - `AnalysisReportDto`
    - `FindingDto`
    - `AiSummaryResponseDto`
    - `AiSuggestionDto`

**Typical Flow**

1. Analysis completes, `AnalysisResultModel` is populated.
2. `FlowLensBackendClient` converts it into an `AnalysisReportDto`.
3. Sends `POST /analysis` to the backend.
4. Optionally, sends a separate request to `POST /analysis/{id}/summary`
   or receives the summary in a combined response.
5. Parses the response and updates the tool window’s model for AI suggestions.

**Design Choices**

- Use a simple HTTP client (e.g. Java’s built-in `HttpClient` or a lightweight
  third-party library) to keep dependencies minimal.
- All network calls:
    - Are off the UI thread.
    - Have timeouts.
    - Provide clear error signals back to the UI.

---

## 4.3 Backend Service Architecture

### 4.3.1 Package Structure (Proposed)

For a Spring Boot backend:

- `com.flowlens.server`
    - `api` – REST controllers and request/response DTOs
    - `service` – core business logic
    - `llm` – integration with the LLM provider(s)
    - `model` – internal domain models (AnalysisRun, Finding, AiSummary, etc.)
    - `persistence` – repositories / storage abstractions (optional)
    - `config` – configuration classes (API keys, URLs, timeouts)
    - `util` – cross-cutting utilities (logging helpers, mappers, etc.)

This structure separates HTTP concerns (controllers), business logic (service),
and integration (LLM) into distinct layers.

### 4.3.2 Core Backend Components

#### 4.3.2.1 REST API Layer

**Responsibility**

- Accept HTTP requests from the plugin.
- Validate input.
- Map DTOs to internal models and call the service layer.

**Example Classes**

- `AnalysisController`
    - `POST /analysis`
    - `GET /analysis/{id}` (if persistence is implemented)
    - `POST /analysis/{id}/summary` (or equivalent endpoint)
- `ErrorHandler` or `ControllerAdvice`
    - Central exception handling and error response formatting.

**DTO Examples**

- `AnalysisReportRequest`
- `AiSummaryResponse`
- `ErrorResponse`

**Design Notes**

- Keep controllers thin:
    - No business logic.
    - Just mapping and delegation to services.

#### 4.3.2.2 Analysis Report Processor (Service Layer)

**Responsibility**

- Handle the lifecycle of an analysis report.
- Prepare data for LLM consumption.
- Apply basic prioritization logic (e.g. grouping by severity, type).

**Example Classes**

- `AnalysisService`
    - `registerAnalysis(AnalysisReportRequest request): AnalysisRun`
    - `generateSummary(AnalysisRun run): AiSummary`

- Internal models in `model`:
    - `AnalysisRun`
    - `Finding`
    - `AiSummary`
    - `AiSuggestion`

**Key Behaviors**

- When an analysis report is received:
    - Validate that it is within size and field limits.
    - Map DTO → `AnalysisRun` + list of `Finding`.
    - Optionally store it (in-memory or persistent).

- When a summary is requested:
    - Select a subset of findings to send to the LLM (e.g. top N by severity).
    - Generate a clear, structured prompt for the LLM.

#### 4.3.2.3 LLM Integration Module

**Responsibility**

- Isolate all interaction with the external LLM provider.
- Convert internal models into prompts and parse responses.

**Example Interfaces & Classes**

- Interface:
    - `LlmClient`
        - `AiSummary callLlm(AnalysisRun run, List<Finding> findings);`

- Implementations:
    - `OpenAiClient`
    - `MockLlmClient` (for local/offline development and tests)

- Config:
    - `LlmConfig` (API key, base URL, model name, timeout).

**Design Considerations**

- **Provider-agnostic interface**:
    - The rest of the backend depends on `LlmClient`, not on any specific provider.
    - This reduces coupling and makes it easier to switch providers.

- **Prompt design**:
    - The module is responsible for building a safe, concise prompt:
        - Brief description of FlowLens’ smell types.
        - Selected findings with short code snippets.
    - It should also handle output parsing into structured objects.

#### 4.3.2.4 Persistence Layer (Optional / Stretch)

If persistence is implemented:

- **Responsibility**
    - Store and retrieve `AnalysisRun`, `Finding`, and `AiSummary` entities.

- **Possible Implementations**
    - In-memory store (e.g. a concurrent map) for a simple first version.
    - A relational database (e.g. PostgreSQL) via JPA or JDBC.

- **Example Classes**
    - `AnalysisRunRepository`
    - `FindingRepository`
    - `AiSummaryRepository`

- **Use Cases**
    - Allow the plugin to fetch past runs (not strictly needed for a demo).
    - Help during debugging and performance testing.

---

## 4.4 LLM Provider Integration

### 4.4.1 External Boundary

The LLM provider is considered **external** to FlowLens:

- FlowLens does not control the model, infrastructure, or uptime.
- FlowLens interacts only through a documented HTTP API.

### 4.4.2 Adapter Responsibility

The adapter (`LlmClient` implementation):

- Knows:
    - Endpoint URLs.
    - Authentication mechanism (e.g. API key header).
    - Request/response formats.
- Hides:
    - Provider-specific quirks from the rest of the backend.

### 4.4.3 Error Handling & Fallback

To protect the user experience:

- Network or provider errors:
    - Are caught and converted into an internal error type.
    - Are returned to the plugin as “AI unavailable” rather than crashing.

- Rate limits / quotas:
    - Could be surfaced as a specific message to the user (e.g. “AI usage limit
      reached”) if the provider exposes that information.

---

## 4.5 Cross-Cutting Concerns

### 4.5.1 Logging

- **Plugin**
    - Logs only minimal information (mainly internal errors and debug logs),
      respecting that it runs inside the user’s IDE.
- **Backend**
    - Logs:
        - Incoming requests (anonymized/summarized).
        - LLM call attempts, durations, and high-level errors.
    - Avoids logging sensitive content from the user’s code where possible.

### 4.5.2 Configuration

- **Plugin Configuration**
    - Backend base URL.
    - Toggle for requesting AI summaries by default or only on demand.

- **Backend Configuration**
    - Server port, timeouts.
    - LLM API key, model name, base URL.
    - Optional persistence settings (database URL, credentials).

Configuration is typically externalized via:

- Properties / YAML files (backend).
- IntelliJ settings page or configuration file (plugin).

### 4.5.3 Error Propagation & User Feedback

- Internal errors are logged, but the user sees:
    - Friendly, concise messages.
    - Clear indication when AI functionality is unavailable.
- The plugin should **never crash the IDE**; all failures must be contained.

---

## 4.6 Summary of Responsibilities

| Component                        | Responsibilities                                                        |
|----------------------------------|-------------------------------------------------------------------------|
| IntelliJ Plugin – Inspections    | PSI traversal, code smell detection, problem descriptors               |
| IntelliJ Plugin – Quick Fixes    | Simple, safe refactoring actions                                       |
| IntelliJ Plugin – Tool Window    | Aggregated view of findings, navigation, AI suggestions                |
| IntelliJ Plugin – Backend Client | HTTP communication with backend, config, error handling                |
| Backend – REST API Layer         | Endpoints, validation, mapping DTOs ↔ models                           |
| Backend – Service Layer          | Analysis lifecycle, prioritization, prompt preparation                 |
| Backend – LLM Integration        | Talking to external LLM API, prompt/response handling                  |
| Backend – Persistence (optional) | Storing analysis runs and AI summaries                                 |
| LLM Provider                     | Language generation based on prompts supplied by FlowLens              |

This architecture keeps responsibilities clear and boundaries explicit, making
the system easier to evolve, test, and explain in an interview setting.
