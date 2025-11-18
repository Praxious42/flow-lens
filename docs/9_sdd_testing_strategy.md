# 9. Testing Strategy

This chapter describes how FlowLens will be tested to ensure that:

- Core features work as intended (inspections, quick fixes, AI integration).
- Non-functional requirements (performance, resilience, usability) are
  reasonably validated.
- The system remains maintainable and safe to change.

The focus is on **what** to test and **how**, not on specific test tool
implementations.

---

## 9.1 Testing Goals

- Verify correctness of:
    - Static analysis and findings (`Finding`, `AnalysisRun`).
    - Quick fixes and small refactorings.
    - Plugin ↔ backend JSON contracts.
    - Backend logic for AI prompt creation and response parsing.

- Reduce risk of regressions when:
    - Adding new inspections.
    - Changing the data model.
    - Switching or updating the LLM provider.

- Provide **confidence for demos/interviews**:
    - Core flows (Ch. 6) should behave predictably on realistic sample projects.

---

## 9.2 Test Levels

FlowLens will use several complementary test levels:

1. **Unit tests**
    - Small, focused, in-memory tests of logic in isolation.

2. **Component / Integration tests**
    - Plugin ↔ backend contract tests.
    - Backend + mock LLM tests.

3. **Manual / Exploratory tests**
    - Hands-on testing in IntelliJ IDEA with sample projects.

4. **(Optional) End-to-end tests**
    - Scripted scenarios that exercise the whole stack locally.

---

## 9.3 Unit Testing

### 9.3.1 Plugin: Inspections & Data Model

**Scope**

- Inspection logic (e.g. “detect blocking calls in hot paths”).
- Mapping from PSI elements to `Finding` objects.
- Basic behavior of `AnalysisResultModel`.

**Approach**

- Use IntelliJ’s test framework (or simplified PSI setups) to:
    - Load small Java test files.
    - Run FlowLens inspections.
    - Assert that:
        - The correct number of findings is produced.
        - Each finding has expected type, severity, and location.

**Example cases**

- Blocking call detection:
    - Method annotated with `@Transactional` containing `Thread.sleep` → 1 finding.
    - Method without annotations using `Thread.sleep` → 0 or lower-severity finding
      (depending on rule definition).

- Missing timeout on HTTP call:
    - `RestTemplate.getForObject(...)` with no configured timeout → finding.
    - HTTP client configured with timeouts → no finding.

**What makes them valuable**

- Catch logic regressions early when modifying or adding inspections.
- Provide executable examples of the intended behavior of each rule.

### 9.3.2 Plugin: Quick Fixes

**Scope**

- Behavior of `LocalQuickFix` implementations.

**Approach**

- For each quick fix:
    - Given a Java snippet containing a finding, apply the quick fix programmatically.
    - Assert that the resulting PSI/text:
        - Compiles syntactically.
        - Reflects the intended change (e.g. added timeout argument, replaced type).

**Example cases**

- `AddTimeoutQuickFix`:
    - Before: `restTemplate.getForObject(url, Response.class);`
    - After: `restTemplate.getForObject(url, Response.class, timeoutConfig);`
      (or similar, depending on design).

### 9.3.3 Backend: Services & LLM Integration (with Mocks)

**Scope**

- `AnalysisService`:
    - Handling of incoming reports.
    - Selection and prioritization of findings for AI.
- `LlmClient`:
    - Prompt construction.
    - Parsing of synthetic/mock LLM responses.

**Approach**

- Use plain unit tests (without starting full Spring context where possible).
- Mock dependencies:
    - For `AnalysisService`, mock `LlmClient`.
    - For `LlmClient`, use pre-canned “LLM responses” to test parsing.

**Example cases**

- `AnalysisService.generateSummary(run)`:
    - Given an `AnalysisRun` with multiple findings, verify:
        - Only top N by severity are included in prompt inputs.
        - The returned `AiSummary` and `AiSuggestion` objects are mapped correctly
          from mocked LLM results.

- `LlmClient.callLlm(...)`:
    - Given a known `AnalysisRun`, confirm:
        - Prompt string contains expected smell types and snippets.
        - Parsing logic correctly handles:
            - Structured JSON in the response.
            - Basic error conditions (e.g. missing fields) gracefully.

---

## 9.4 Integration Testing

### 9.4.1 Plugin ↔ Backend JSON Contract

**Scope**

- Correctness of JSON payloads between plugin and backend:
    - `AnalysisReport` request.
    - `AiSummaryResponse` response.

**Approach**

- Create small integration tests that:
    - Instantiate `AnalysisRun` + `Finding` in plugin-side test code.
    - Serialize to JSON and send
