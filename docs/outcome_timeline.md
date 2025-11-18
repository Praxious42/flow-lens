# Outcomes & Timeline

This document captures the **target outcomes** for the FlowLens project and a
**realistic 3-month timeline** to complete them, based on ~11 hours/week of
focused work.

It can be used as:

- An internal project plan.
- A high-level roadmap you can show or reference in interviews.

---

## 1. High-Level Outcomes

By the end of ~3 months, the goal is to have:

1. **A working IntelliJ IDEA plugin (FlowLens)**
    - Custom inspections for Java code.
    - At least one working quick fix.
    - A FlowLens Report tool window showing aggregated findings.
    - Optional: a small completion contributor.

2. **A working backend service**
    - Spring Boot REST API that accepts analysis reports.
    - Integration with one LLM provider to generate summaries & suggestions.
    - Dockerized backend and basic config/docs.

3. **End-to-end AI-assisted workflow**
    - Run analysis in IDE → send report to backend → get AI summary →
      display in FlowLens tool window.

4. **Documentation & design artifacts**
    - `README.md` explaining what FlowLens does and how to run it.
    - `SYSTEM_DESIGN.md` (the “bible”) with architecture, data model, flows, etc.
    - Basic deployment & testing instructions.

5. **Demonstration setup**
    - At least one small sample Java project with known “smells” to demo.
    - A prepared, repeatable demo flow:
        - Run analysis
        - Show findings
        - Request AI summary
        - Apply a quick fix

6. **Evidence of engineering practices**
    - A reasonable set of unit tests (inspections + backend logic).
    - Simple build scripts (`./gradlew buildPlugin`, `./mvnw test`, etc.).
    - Optional: lightweight CI pipeline (GitHub Actions or similar).

---

## 2. Outcomes Breakdown (Core vs Stretch)

### 2.1 Core Outcomes (Must-Have)

These are the **minimum** you want finished before the interview:

- **Plugin**
    - 2–3 meaningful inspections (e.g. blocking calls, missing timeouts,
      one collection/concurrency smell).
    - 1–2 quick fixes.
    - FlowLens Report tool window with grouping + navigation.
    - Manual configuration of backend URL.

- **Backend**
    - `POST /api/analysis` endpoint accepting AnalysisRun JSON.
    - `POST /api/analysis/{analysisId}/summary` (or similar) returning
      `AiSummary` + `AiSuggestion` objects.
    - Integration with one LLM provider (real or mocked for local dev).
    - Run as Spring Boot JAR or Docker container.

- **Docs & Tests**
    - README with instructions & screenshots.
    - SYSTEM_DESIGN with main chapters (1–10).
    - Unit tests for:
        - At least one inspection.
        - Backend summary generation (with mock LLM).

### 2.2 Stretch Outcomes (Nice-to-Have)

If time permits:

- Publish plugin to the **JetBrains Marketplace** (even as “alpha”).
- Add:
    - More inspections (DB, thread pools).
    - A small completion contributor (e.g. logging or async template).
- Implement simple persistence in the backend (store last N AnalysisRuns).
- Add:
    - GitHub Actions / CI to build & test automatically.
    - A minimal metrics endpoint (e.g. analysis counts).

---

## 3. Assumptions

- Time: ~**11 hours/week** for ~**12 weeks** → ~130 hours total.
- You are already comfortable with:
    - Java, Spring Boot.
    - Basic Docker usage.
- You are **new to IntelliJ plugin development** and LLM integration, so the
  timeline includes learning time.

---

## 4. Timeline Overview (12 Weeks)

### Month 1 – Foundations (Weeks 1–4)

**Goal:** Basic IntelliJ plugin with one real inspection, plus initial design docs.

---

#### Week 1 – Setup & Skeletons (≈ 8–11 hours)

**Outcomes**

- IntelliJ plugin project created with Gradle template.
- “Hello World” action and empty FlowLens Report tool window.
- Backend Spring Boot project skeleton created.

**Tasks**

- Create plugin project:
    - Configure Gradle, plugin XML, basic action.
- Create minimal `FlowLensToolWindowFactory` showing placeholder UI.
- Create backend project:
    - Basic Spring Boot app with `/api/health` endpoint.
- Start `SYSTEM_DESIGN.md` and plug in chapters 1–2 (already drafted).

---

#### Week 2 – First Inspection (≈ 8–11 hours)

**Outcomes**

- One working inspection that detects a simple pattern (e.g. `Thread.sleep`).
- Inline warnings appear in IntelliJ.

**Tasks**

- Implement `BlockingCallInspection` with basic PSI traversal.
- Register inspection so it appears in IntelliJ’s inspection settings.
- Test manually on a tiny Java project.
- Add unit test for this inspection if possible.

---

#### Week 3 – Inspection Set & Tool Window Wiring (≈ 8–11 hours)

**Outcomes**

- 2–3 inspections implemented (blocking calls, missing HTTP timeout, one
  collection/concurrency issue).
- Findings aggregated and visible in the FlowLens Report tool window.

**Tasks**

- Implement:
    - `MissingTimeoutInspection`.
    - One more inspection (collection or concurrency).
- Design `AnalysisResultModel` inside plugin.
- Wire analysis action → analysis model → FlowLens Report UI.
- Update SYSTEM_DESIGN (Architecture & Data Model chapters, 4–5).

---

#### Week 4 – Quick Fixes & Polish (≈ 8–11 hours)

**Outcomes**

- At least one quick fix works end-to-end.
- FlowLens Report allows navigation to code.

**Tasks**

- Implement `AddTimeoutQuickFix` or similar.
- Implement one more quick fix if time allows.
- Add navigation from tool window entries to code.
- Begin `Testing Strategy` and `Non-Functional Design` chapters.
- Manual testing on a slightly larger sample project.

---

### Month 2 – Backend & Integration (Weeks 5–8)

**Goal:** Backend running, plugin can send analysis data and receive stubbed AI responses.

---

#### Week 5 – Backend Core (≈ 8–11 hours)

**Outcomes**

- Backend accepts `POST /api/analysis` with AnalysisRun JSON.
- Request validates and maps into domain objects.

**Tasks**

- Define DTOs for `AnalysisReportRequest`, `AiSummaryResponse`.
- Implement `AnalysisController` + `AnalysisService` (registration only).
- Add simple validation + error handling.
- Create integration test for `POST /api/analysis`.

---

#### Week 6 – Plugin ↔ Backend Wiring (≈ 8–11 hours)

**Outcomes**

- Plugin can send analysis reports to the backend.
- Backend responds with a stubbed/fake AI summary.
- FlowLens Report displays this stub summary.

**Tasks**

- Implement `FlowLensBackendClient` in plugin.
- Add settings for backend URL (hardcoded or via simple config UI).
- Implement fake AI summary logic in backend:
    - Return canned summary/suggestions without calling real LLM.
- Wire FlowLens Report to show `AiSummary` and suggestion list.

---

#### Week 7 – Real LLM Integration (≈ 8–11 hours)

**Outcomes**

- Backend calls a real LLM provider.
- AI summaries reflect the actual findings.

**Tasks**

- Implement `LlmClient` abstraction and one real implementation.
- Add prompt-building logic based on AnalysisRun and top findings.
- Handle timeouts and basic error mapping.
- Add unit tests with mock LLM for prompt and parsing.
- Update docs (`Data Model`, `Key Flows`) to reflect real AI path.

---

#### Week 8 – Refine UX & Stability (≈ 8–11 hours)

**Outcomes**

- More stable plugin behavior.
- Usable “AI Suggestions” tab with proper error messages.

**Tasks**

- Improve FlowLens Report UI:
    - Clear sections for static findings vs AI suggestions.
    - Loading / error states for AI requests.
- Improve UX:
    - Button to “Request AI Summary”.
    - Non-intrusive notifications on failure.
- Manual testing across multiple sample projects.

---

### Month 3 – Hardening, Deployment & Polish (Weeks 9–12)

**Goal:** Production-like polish: Docker backend, better docs, tests, and a smooth demo story.

---

#### Week 9 – Dockerization & Config (≈ 8–11 hours)

**Outcomes**

- Backend runs via Docker and (optionally) `docker-compose`.
- Clear configuration instructions.

**Tasks**

- Write Dockerfile for backend.
- Create `docker-compose.yml` (even if just the backend).
- Add environment variable-based config for LLM API key, timeouts.
- Update `Deployment & Operations` chapter with exact commands.

---

#### Week 10 – Testing & Performance (≈ 8–11 hours)

**Outcomes**

- Core tests in place:
    - Inspections.
    - Backend services.
    - Plugin ↔ backend contract.
- Basic performance sanity checks.

**Tasks**

- Add/expand unit tests for:
    - At least two inspections.
    - Backend analysis + summary generation.
- Add integration test for `POST /api/analysis/{analysisId}/summary`.
- Run analysis on a medium sample project and:
    - Note timings.
    - Adjust scope/heuristics if needed.

---

#### Week 11 – Documentation & Demo Prep (≈ 8–11 hours)

**Outcomes**

- README + SYSTEM_DESIGN polished and consistent with implementation.
- Demo script prepared.

**Tasks**

- Update README with:
    - Installation steps.
    - Backend setup.
    - Example screenshots (tool window, AI suggestions).
- Ensure SYSTEM_DESIGN chapters 1–10 are aligned with actual behavior.
- Create a “demo scenario” document with:
    - Steps to run backend & IDE.
    - Steps to run analysis and show AI summary.
    - Key talking points (design decisions, trade-offs).

---

#### Week 12 – Stretch Work & Buffer (≈ 8–11 hours)

**Outcomes**

- Use remaining time for stretch goals and final stabilization.

**Tasks (choose based on remaining time)**

- Try publishing plugin to JetBrains Marketplace.
- Add one more high-impact inspection.
- Improve completion contributor.
- Add minimal CI (GitHub Actions) that:
    - Builds plugin and backend.
    - Runs tests.
- Final round of manual testing and small bug fixes.

---

## 5. Success Criteria

By the end of this timeline, you should be able to:

1. **Show a live demo** of FlowLens:
    - Run analysis, view findings, use the tool window, request AI summary.
2. **Walk through the design** using SYSTEM_DESIGN:
    - Architecture, data model, flows, non-functional design.
3. **Discuss trade-offs and future work**:
    - Inspections scope, AI design, plugin/backend boundaries.
4. **Point to a repo** that:
    - Builds successfully.
    - Has tests.
    - Has clear docs.

That combination positions FlowLens as a **serious, JetBrains-aligned project**
and gives you a strong story for an IntelliJ IDEA Java team interview.
