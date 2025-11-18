# 10. Open Questions & Future Work

This chapter captures known limitations, design decisions that are still open,
and ideas for future enhancements to FlowLens. It serves both as:

- A **backlog** of potential improvements.
- A **conversation starter** for design and interview discussions.

---

## 10.1 Known Limitations (Initial Version)

### 10.1.1 Limited Inspection Coverage

- Current inspections focus on:
    - Blocking calls in performance-sensitive contexts.
    - HTTP calls without explicit timeouts.
    - A small subset of collection/concurrency issues.
- Many other important areas are not yet covered, e.g.:
    - Database access patterns (N+1 queries, long-running transactions).
    - Thread pool misconfiguration.
    - Blocking operations in reactive or async APIs.
    - Resource leaks (connections, streams).

**Impact:**  
FlowLens is intentionally narrow; it should be presented as a focused PoC
rather than a comprehensive static analysis tool.

---

### 10.1.2 Java-Only Support

- Initial implementation targets **Java** only.
- No support for:
    - Kotlin.
    - Other JVM languages.
    - Non-JVM languages.

**Impact:**  
Mixed-language projects (Java + Kotlin) will only see FlowLens results on the
Java side. This is acceptable for the first iteration but limits adoption and
completeness.

---

### 10.1.3 No Incremental / Continuous Analysis

- Analysis is **on-demand**:
    - Project/module-level run.
    - File-level (optional) re-run.
- There is no:
    - Continuous, low-latency incremental analysis across the entire project.
    - Smart caching of findings across runs beyond simple in-memory state.

**Impact:**  
On larger projects or frequent re-runs, performance could degrade compared to a
fully incremental engine. For the initial scope, this is acceptable.

---

### 10.1.4 Minimal Persistence & History

- The backend:
    - May store analysis runs in memory only.
    - Does not provide a robust historical view of:
        - How findings evolve over time.
        - Comparison between runs (before/after refactoring).

**Impact:**  
Users cannot easily see progress trends or compare current and previous
analyses. This is out of scope for the first version but valuable long-term.

---

### 10.1.5 Simplified AI Integration

- LLM prompt/response handling is intentionally simple:
    - Single provider.
    - Basic prompt template.
    - Limited error classification.
- No sophisticated:
    - Safety filters.
    - Multiple prompt strategies.
    - Re-ranking or merging of AI suggestions.

**Impact:**  
AI suggestions are “best-effort” and may occasionally be:
- Too generic.
- Not fully aligned with the project’s architecture or style.

---

## 10.2 Open Design Questions

These are areas where multiple reasonable options exist and the current design
may take a simple approach, leaving room for future refinement.

### 10.2.1 How Deep Should the Static Analysis Go?

**Question:**  
To what extent should FlowLens attempt deeper analysis (e.g. interprocedural
dataflow, whole-program analysis) versus staying lightweight and heuristic-based?

**Current stance:**

- Start with **local, heuristic inspections** for clarity and speed.
- Consider deeper analyses only if:
    - There is a clear, high-value use case.
    - Performance remains acceptable.

### 10.2.2 Where to Draw the Boundary Between Plugin and Backend Logic?

**Question:**  
Which responsibilities belong in the plugin vs. the backend when it comes to:

- Prioritizing findings.
- Grouping related smells.
- Generating human-readable explanations?

**Current stance:**

- Plugin is responsible for **raw findings** and IDE UX.
- Backend is responsible for:
    - **Aggregation** and **AI summarization**.
    - Light prioritization.

This boundary could shift if the backend grows into a richer analysis platform.

---

### 10.2.3 Single vs Multiple LLM Providers

**Question:**  
Should FlowLens:

- Stick to a single LLM provider, or
- Support multiple providers (e.g. user-selectable), or
- Abstract them behind a generic “provider-agnostic” configuration?

**Current stance:**

- Code is structured with a `LlmClient` abstraction, but:
    - Only one real implementation may be provided initially.
- Multi-provider support is left as future work.

---

### 10.2.4 Level of User Control Over AI Data

**Question:**  
How much configurability should users have over what is sent to the backend/LLM?

Options include:

- Global on/off toggle for AI.
- Fine-grained toggles:
    - Include/exclude code snippets.
    - Redact class/method names.
- Per-project configuration vs. global.

**Current stance:**

- Provide a **global toggle** for AI usage.
- Always send **minimal necessary snippets**.
- More granular controls are noted as future enhancements.

---

### 10.2.5 Handling Very Large Projects

**Question:**  
How should FlowLens behave on very large codebases (hundreds of modules,
millions of LOC)?

Potential strategies:

- Strict scope limits (e.g. module-only analysis).
- Sampling (analyze a subset of modules/files).
- Incremental analysis with persistent caches.

**Current stance:**

- Operate best on small to medium projects and selected modules.
- Document performance expectations and scope recommendations.
- Leave extreme scale optimizations as future work.

---

## 10.3 Future Enhancements

### 10.3.1 Expanded Inspection Library

- Add more inspections, such as:
    - Database access and transaction patterns.
    - Thread pool usage and configuration.
    - Blocking calls in reactive streams or async handlers.
    - Potential deadlocks and shared mutable state patterns.

- Provide **rulesets**:
    - “Performance-focused”
    - “Resilience-focused”
    - “Cloud-ready baseline”

---

### 10.3.2 Kotlin and Multi-Language Support

- Extend inspections to **Kotlin** projects using Kotlin PSI/analysis APIs.
- Support mixed Java/Kotlin modules, where similar smells are detected in both.
- Eventually, consider basic support for:
    - Scala or other JVM languages (stretch goal).

---

### 10.3.3 Richer AI Interactions

- Move beyond one-shot summary to:
    - Interactive chat-like refinement within the FlowLens tool window.
    - “What-if” questions about a specific subset of findings or files.
- Generate:
    - More structured refactoring plans (e.g. per-module/epic).
    - Code examples that the user can insert or adapt.
- Explore:
    - Different prompting strategies.
    - Safety filters and validation of AI suggestions.

---

### 10.3.4 Historical Trends & Dashboards

- Persist analysis runs and:
    - Track evolution of:
        - Total findings.
        - High-severity issues.
        - Specific categories (performance, resilience, concurrency).
- Provide UI (IDE or web) to:
    - Compare “before/after” analyses.
    - Visualize trends over time.

---

### 10.3.5 Improved Deployment Options

- Provide:
    - A pre-built Docker image for the backend on a registry.
    - A “one-command” `docker-compose` setup for demos.
- Optional:
    - A small public demo backend (secured, rate-limited) for:
        - Quick onboarding.
        - Evaluation without local backend setup.

---

### 10.3.6 Integration with Other Tools

- **CI Integration**
    - CLI or headless mode for FlowLens server to analyze projects during CI.
    - Export results as:
        - JSON reports.
        - SARIF or other standard formats.

- **JetBrains Ecosystem**
    - Potential integration with Qodana or other JetBrains tooling (conceptual).
    - Deeper embedding of AI suggestions alongside existing IntelliJ inspections.

---

## 10.4 Interview & Design Discussion Hooks

For interviews or design reviews, the items in this chapter can be used as
intentional talking points, e.g.:

- “If I had more time, I’d expand inspections into database and thread-pool
  patterns because…”
- “I deliberately chose to keep analysis heuristic-based for now, but a natural
  extension would be…”
- “The current AI integration is minimal; here’s how I’d evolve it to support
  multiple models and richer interactions…”

This shows not only that FlowLens has a concrete implementation, but that its
design was made with **future evolution in mind**.

---

## 10.5 Summary

FlowLens v1 is deliberately scoped and focused:

- It solves a **slice** of the overall “cloud-ready Java analysis + AI
  assistance” problem.
- It leaves room for meaningful future extensions in:
    - Inspections.
    - Language support.
    - AI richness.
    - Scale and operational maturity.

These open questions and future work items document where the system could go
next, and provide a roadmap for continued improvement.
