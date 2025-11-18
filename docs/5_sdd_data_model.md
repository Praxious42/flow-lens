# 5. Data Model

This chapter defines the core data model for FlowLens. It focuses on the
**domain concepts** shared between the IntelliJ plugin and the backend
service, and the **JSON contracts** used for communication.

The goals of the data model are:

- To represent analysis results in a way that is:
    - IDE-friendly (fine-grained locations, severity, descriptions).
    - Backend-friendly (compact, serializable, extensible).
- To make it easy to:
    - Add new smell types.
    - Evolve AI suggestions without breaking existing clients.

---

## 5.1 Core Domain Concepts

At a high level, FlowLens deals with the following concepts:

- **AnalysisRun** – a single execution of FlowLens analysis on a project/module.
- **Finding** – a single code smell detected by an inspection.
- **CodeLocation** – the position of a `Finding` in source code.
- **AiSummary** – a high-level, human-readable summary of an analysis run.
- **AiSuggestion** – a concrete recommendation linked to one or more findings.

The IntelliJ plugin maintains in-memory representations of these concepts,
while the backend uses similar structures for processing and (optionally)
persistence.

---

## 5.2 AnalysisRun

### 5.2.1 Concept

An **AnalysisRun** represents one complete run of FlowLens analysis over a
scope (project or module). It groups together all findings and related metadata.

### 5.2.2 Key Fields (Logical)

- `id` – Unique identifier for the analysis run.
- `scopeType` – `"PROJECT"` or `"MODULE"`.
- `scopeName` – Name of the project or module analyzed.
- `timestamp` – When the analysis was performed (UTC or ISO-8601).
- `findings` – List of `Finding` objects produced in this run.
- `metrics` (optional) – Basic metrics, e.g. total files scanned, analysis
  duration.

Example (logical Java-style model):

```java
class AnalysisRun {
    String id;
    ScopeType scopeType;       // PROJECT, MODULE
    String scopeName;
    Instant timestamp;
    List<Finding> findings;
    AnalysisMetrics metrics;   // optional
}

class AnalysisMetrics {
    int filesScanned;
    int totalFindings;
    long analysisDurationMillis;
}
```
## 5.3 Finding
### 5.3.1 Concept

A **Finding** represents a single identified issue in the user’s code, such as
a blocking call or a missing timeout. It includes:

- What kind of smell it is.
- Where in the code it occurs.
- How severe it is.
- A short, human-readable description.

### 5.3.2 Key Fields

- id – Unique identifier within an analysis run.
- type – Machine-readable code for the smell, e.g.:
  - "BLOCKING_CALL_IN_HOT_PATH"
  - "MISSING_TIMEOUT_ON_HTTP_CALL"
  - "INEFFICIENT_COLLECTION_USAGE"

- severity – e.g. "INFO", "WARNING", "ERROR" (aligned with plugin severity).
- message – Concise description shown to the user.
- location – A CodeLocation object.
- context (optional) – Short contextual data, such as:
    - Method or class name.
    - Framework hints (e.g. annotation names).
- codeSnippet (optional) – A short excerpt of code around the issue.

### 5.3.3 CodeLocation

The CodeLocation contains enough information to:
- Navigate to the problem in the IDE.
- Provide limited context to the backend/LLM.

Typical fields:
- filePath – Project-relative or logical path (src/main/java/...).
- lineStart, lineEnd – Line numbers (1-based).
- columnStart, columnEnd – Columns (optional, but useful for precision).
- symbolName (optional) – e.g. method or field name.
- packageName (optional) – Java package.
- Example (logical model):
```java
class Finding {
    String id;
    FindingType type;
    Severity severity;
    String message;
    CodeLocation location;
    Map<String, String> context;  // e.g. {"method": "handleRequest", "annotation": "@Transactional"}
    String codeSnippet;           // optional, limited length
}

class CodeLocation {
    String filePath;
    int lineStart;
    int lineEnd;
    Integer columnStart;          // nullable
    Integer columnEnd;            // nullable
    String symbolName;            // optional
    String packageName;           // optional
}
```

## 5.4 AiSummary and AiSuggestion
### 5.4.1 AiSummary

An **AiSummary** is a high-level, human-readable description of the key issues
found in an analysis run. It is meant to be displayed prominently in the
FlowLens Report tool window.

Key fields:
- analysisId – The AnalysisRun.id this summary refers to.
- title – Short headline (e.g. “3 critical performance risks detected”).
- body – A few paragraphs of explanatory text.

```java
class AiSummary {
    String analysisId;
    String title;
    String body;
}
```

### 5.4.2 AiSuggestion

An **AiSuggestion** is a more targeted recommendation, typically linked to one
or more findings.

Key fields:
- id – Unique identifier for the suggestion.
- analysisId – The AnalysisRun.id this suggestion belongs to.
- relatedFindingIds – IDs of findings that this suggestion addresses.
- category – e.g. "PERFORMANCE", "RESILIENCE", "CONCURRENCY".
- title – Short, actionable label (e.g. “Add timeouts to external HTTP calls”).
- description – Human-readable description of the suggested change.
- priority – e.g. "HIGH", "MEDIUM", "LOW" (used for ordering).

```java
class AiSuggestion {
    String id;
    String analysisId;
    List<String> relatedFindingIds;
    String category;
    String title;
    String description;
    Priority priority;  // HIGH, MEDIUM, LOW
}
```

## 5.5 Plugin ↔ Backend JSON Contracts

The plugin and backend communicate via JSON over HTTP. This section defines
the expected payloads.

### 5.5.1 Analysis Report Request (Plugin → Backend)

Endpoint (example): ```POST /api/analysis```

Request body:
```json
{
  "analysisId": "a5c1f2b9-1234-4cde-90ab-ef5678901234",
  "scopeType": "PROJECT",
  "scopeName": "payment-service",
  "timestamp": "2025-11-18T20:15:30Z",
  "metrics": {
    "filesScanned": 128,
    "totalFindings": 12,
    "analysisDurationMillis": 4230
  },
  "findings": [
    {
      "id": "F-001",
      "type": "BLOCKING_CALL_IN_HOT_PATH",
      "severity": "WARNING",
      "message": "Blocking call `Thread.sleep` in method marked as performance-critical.",
      "location": {
        "filePath": "src/main/java/com/example/payment/PaymentService.java",
        "lineStart": 42,
        "lineEnd": 44,
        "columnStart": 13,
        "columnEnd": 27,
        "symbolName": "processPayment",
        "packageName": "com.example.payment"
      },
      "context": {
        "method": "processPayment",
        "annotation": "@Transactional"
      },
      "codeSnippet": "Thread.sleep(5000); // TODO: remove before production"
    },
    {
      "id": "F-002",
      "type": "MISSING_TIMEOUT_ON_HTTP_CALL",
      "severity": "WARNING",
      "message": "HTTP call without explicit timeout.",
      "location": {
        "filePath": "src/main/java/com/example/payment/Client.java",
        "lineStart": 88,
        "lineEnd": 88
      },
      "context": {
        "httpClient": "RestTemplate"
      },
      "codeSnippet": "restTemplate.getForObject(url, Response.class);"
    }
  ]
}
```
Notes:
- ```analysisId``` is generated by the plugin and reused for subsequent AI requests.
- ```codeSnippet``` fields should be size-limited to avoid sending entire files.


## 5.5.2 AI Summary Request (Plugin → Backend)

Depending on the design, the plugin either:
- Requests AI summarization as part of the POST /analysis call, or
- Makes a separate call like POST /api/analysis/{analysisId}/summary.

A simple separate-request body:
```json
{
  "analysisId": "a5c1f2b9-1234-4cde-90ab-ef5678901234",
  "maxFindings": 10
}
```

5.5.3 AI Summary & Suggestions Response (Backend → Plugin)

Example response body from ```POST /api/analysis/{analysisId}/summary```:
```json
{
  "analysisId": "a5c1f2b9-1234-4cde-90ab-ef5678901234",
  "summary": {
    "title": "Focus on blocking calls and missing timeouts",
    "body": "FlowLens detected several potential performance and resilience risks. The most critical issues are blocking calls in methods that are likely to run under load, and HTTP calls that do not specify timeouts..."
  },
  "suggestions": [
    {
      "id": "S-001",
      "analysisId": "a5c1f2b9-1234-4cde-90ab-ef5678901234",
      "relatedFindingIds": ["F-001"],
      "category": "PERFORMANCE",
      "title": "Remove or refactor blocking calls in hot paths",
      "description": "Consider replacing Thread.sleep in processPayment(...) with a non-blocking wait mechanism or moving this delay out of the request handling path.",
      "priority": "HIGH"
    },
    {
      "id": "S-002",
      "analysisId": "a5c1f2b9-1234-4cde-90ab-ef5678901234",
      "relatedFindingIds": ["F-002"],
      "category": "RESILIENCE",
      "title": "Introduce timeouts for HTTP calls",
      "description": "Configure your RestTemplate (or equivalent client) withreasonable connect and read timeouts to avoid hanging threadswhen the upstream service is slow or unavailable.",
      "priority": "MEDIUM"
    }
  ]
}
```

The plugin uses:

- ```summary``` to populate the AI summary area.

- ```suggestions``` to show a prioritized, clickable list in the FlowLens Report.


## 5.6 Extensibility Considerations
### 5.6.1 Adding New Smell Types

To add a new smell type:

1. Introduce a new FindingType constant (e.g. "BLOCKING_DB_CALL").
2. Implement a corresponding inspection rule in the plugin.
3. Optionally, teach the backend/LLM prompt logic to:
   - Recognize the new type.
   - Provide richer text for it.

Existing contracts and clients remain valid because:
- type is a string; unknown values can be handled gracefully.
- The plugin can render unknown types as generic findings.

### 5.6.2 Evolving AI Suggestions

AI suggestions can evolve by:
- Adding new optional fields to AiSuggestion:
- For example, codeExample or estimatedEffort.
- Adding new suggestion categories.

Backward compatibility:
- New fields should be optional and defaulted on the plugin side.
- The plugin should ignore unknown fields in the JSON payload.

### 5.7 Summary

The FlowLens data model centers around a small set of domain concepts:
- ```AnalysisRun``` groups findings from a single analysis.
- ```Finding``` captures individual issues and their locations in code.
- ```AiSummary``` and AiSuggestion augment static analysis with AI-generated
insight.
-By keeping the model simple, explicit, and extensible, FlowLens can:
-Evolve to support more inspections and AI features.
-Maintain a stable, understandable contract between the plugin and backend.

Offer a clean story to explain in design discussions and interviews.