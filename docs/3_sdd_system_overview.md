# 3. System Overview

This chapter provides a high-level view of the FlowLens system: its main
components, how they interact, and the core data flows between them.

---

## 3.1 High-Level Architecture

FlowLens is composed of three major parts:

1. **IntelliJ IDEA Plugin (FlowLens Plugin)**
    - Runs inside the IntelliJ IDEA process.
    - Performs static analysis of Java code, presents findings, and interacts
      with the user.

2. **Backend Service (FlowLens Server)**
    - Runs as a separate Java/Spring Boot application.
    - Receives analysis reports from the plugin and integrates with an LLM
      provider to produce summaries and refactoring suggestions.

3. **LLM Provider (External Service)**
    - A third-party large language model API accessed over HTTPS.
    - Consumes compact, structured analysis data and returns natural-language
      output.

At a high level:

- The **plugin** is responsible for *understanding the code* (via IntelliJ PSI
  and inspections) and *driving the user experience*.
- The **backend** is responsible for *processing analysis results* and
  *augmenting them with AI-generated insight*.
- The **LLM provider** is responsible for *language generation* given prompts
  assembled by the backend.

### 3.1.1 Logical Architecture Diagram (Textual Description)

A typical architecture diagram for FlowLens would show:

- A box labeled **IntelliJ IDEA (FlowLens Plugin)**, containing:
    - `Inspection Engine`
    - `FlowLens Report Tool Window`
    - `Backend Client`

- A box labeled **FlowLens Server (Backend Service)**, containing:
    - `REST API Layer`
    - `Analysis Report Processor`
    - `LLM Integration Module`
    - (Optional) `Persistence Layer`

- A box labeled **LLM Provider** outside the system boundary.

Arrows would represent:

- Plugin → Backend:
    - `POST /analysis` (analysis report)
    - `POST /analysis/{id}/summary` (AI summary request)

- Backend → LLM Provider:
    - HTTPS calls to the LLM API with prompt + context.

- Backend → Plugin:
    - `200 OK` responses with structured summaries and suggestions.

---

## 3.2 Main Components

### 3.2.1 IntelliJ IDEA Plugin

The plugin consists of the following logical sub-components:

- **Inspection Engine**
    - Implements custom FlowLens inspections (blocking calls, missing timeouts,
      concurrency/collection issues).
    - Hooks into IntelliJ’s inspection framework and PSI to analyze Java code.

- **Quick Fixes & Completion Contributor**
    - Provides quick fixes where possible (e.g. adding timeouts, suggesting
      safer API usage).
    - Optionally contributes small, context-aware completion templates related
      to concurrency and logging.

- **FlowLens Report Tool Window**
    - Displays aggregated findings from the most recent analysis.
    - Supports filtering, grouping, and navigation.

- **Backend Client**
    - Serializes a compact representation of findings.
    - Sends and receives data from the backend via HTTP.
    - Handles configuration (backend URL) and error conditions.

### 3.2.2 Backend Service (FlowLens Server)

Within the backend service:

- **REST API Layer**
    - Exposes endpoints to receive analysis reports and to request AI summaries.
    - Validates and deserializes incoming JSON payloads.

- **Analysis Report Processor**
    - Normalizes and prepares analysis data for LLM consumption.
    - Applies any lightweight server-side logic (e.g. coarse prioritization).

- **LLM Integration Module**
    - Converts analysis data into prompts for the LLM provider.
    - Calls the external API and parses responses into a structured internal
      format (`AISummary`, `AISuggestion` objects).

- **Persistence Layer (optional)**
    - Stores analysis runs and AI outputs for debugging, comparison, or future
      enhancements (e.g. historical views).

### 3.2.3 External LLM Provider

- **LLM API**
    - A hosted large language model accessible via HTTPS.
    - Accepts text prompts and returns text completions or structured responses.
    - Is abstracted behind the backend’s `LLM Integration Module` so that
      providers can be swapped with minimal changes.

---

## 3.3 Core Data Flows

This section outlines the primary interactions between the components.

### 3.3.1 Flow A – Local Static Analysis in the IDE

1. The user triggers a **FlowLens analysis** on a project or module.
2. The **Inspection Engine**:
    - Traverses the PSI (Java AST) for relevant files.
    - Applies each FlowLens inspection rule to detect code smells.
3. For each finding, the plugin:
    - Records metadata (file, line, smell type, severity, message).
    - Registers an inspection result in IntelliJ, which appears inline in the
      editor and in the standard inspections view.
4. When analysis completes, the plugin:
    - Aggregates all findings into a model used by the FlowLens Report tool
      window.
    - Updates the **FlowLens Report** UI so the user can browse findings.

This flow works entirely locally; it does not require the backend or LLM.

### 3.3.2 Flow B – AI-Augmented Summary & Suggestions

1. After an analysis run completes, the user:
    - Either clicks a “Request AI Summary” button or has a setting enabled
      that automatically requests one.

2. The **Backend Client** in the plugin:
    - Converts the current analysis run into a compact JSON payload:
        - High-level project/module info.
        - A curated subset of findings (type, location, short description,
          optional code snippets).
    - Sends this payload to the backend’s `POST /analysis` endpoint.

3. The **Backend Service**:
    - Validates the request and stores or buffers the analysis data.
    - Invokes the `LLM Integration Module`:
        - Builds a prompt describing the project context and key findings.
        - Calls the external LLM API.

4. The **LLM Provider**:
    - Processes the prompt and returns:
        - A natural-language summary of major issues and risks.
        - Optionally, a list of suggested refactorings or remediation steps.

5. The **Backend Service**:
    - Parses and structures the LLM response.
    - Returns a JSON response to the plugin containing:
        - A short summary.
        - A list of suggestions, each referencing relevant finding(s).

6. The **Plugin**:
    - Receives the response and associates suggestions with the current
      analysis run.
    - Updates the **FlowLens Report** tool window (e.g. “AI Suggestions” tab)
      so the user can read and act on the advice.

### 3.3.3 Flow C – Degraded Operation (Backend/LLM Unavailable)

1. The user triggers an analysis as usual.
2. Static analysis in the IDE runs as in **Flow A** and completes.
3. When the plugin attempts to:
    - Send results to the backend, or
    - Request an AI summary,

   it encounters an error (e.g. connection refused, timeout, 5xx response).

4. The **Backend Client**:
    - Logs the error.
    - Returns a failure status to the UI layer.

5. The **FlowLens Report** tool window:
    - Displays a small, non-blocking message such as:  
      “AI suggestions are currently unavailable. Static analysis results are
      still shown.”

6. The user continues to work with local findings and quick fixes; no AI
   suggestions are shown for this run.

---

## 3.4 Technology Stack Overview

While detailed technology choices are covered in later chapters, the main stack
can be summarized as:

- **IntelliJ Plugin**
    - Language: Java (with possible incremental use of Kotlin).
    - Platform: IntelliJ Platform SDK, built with Gradle.
    - Responsibilities: static analysis, UI, communication with backend.

- **Backend Service**
    - Language: Java (e.g. Java 17+).
    - Framework: Spring Boot.
    - Deployment: local JVM process or Docker container.
    - Responsibilities: REST API, analysis report handling, LLM integration.

- **LLM Provider**
    - External HTTP API (e.g. OpenAI or compatible service).
    - Accessed securely with API keys configured on the backend side.

---

## 3.5 System Boundaries

FlowLens explicitly defines the following system boundaries:

- **Inside the system**
    - IntelliJ plugin code and all custom inspections.
    - Backend service code and configuration.
    - Data contracts (JSON payloads) between plugin and backend.

- **Outside the system**
    - The IntelliJ Platform and JDK themselves.
    - The LLM provider implementation and infrastructure.
    - Any optional persistence or monitoring infrastructure not bundled with
      the FlowLens demo deployment.

These boundaries help clarify which parts are under FlowLens’ control and
which are external dependencies or integration points.

---
