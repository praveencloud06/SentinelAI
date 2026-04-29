# Dual Capability Architecture: SentinelAI

## Overview
Your application will support two complementary capabilities for log analysis and root cause detection:

### 1. Historical RCA (Vector-Based)
- **Purpose:** Quickly match new logs with past resolved cases using vector embeddings and similarity search.
- **Best for:** Known, recurring issues; fast suggestions based on resolution history.

### 2. Human-like Log Analysis (AI/ML)
- **Purpose:** Deeply analyze raw logs, extract errors, group scenarios, and use AI to suggest root causes—even for new/unseen issues.
- **Best for:** New, complex, or unknown issues; detailed investigation.

---

## System Flow

1. **User uploads a log file or submits log text via React UI.**
2. **Java backend receives the input and triggers both analysis flows:**
   - **A. Vector-Based RCA:**
     - Converts logs to embeddings.
     - Searches for similar cases in the resolution history (pgvector).
     - Returns matched root cause (if found).
   - **B. AI/ML Log Analysis:**
     - Sends logs to Python AI/ML service.
     - Extracts errors, groups scenarios, and uses Llama 3 for summarization/root cause suggestion.
     - Returns detailed findings.
3. **Results Aggregation:**
   - Java backend aggregates results from both flows.
   - Sends combined results to the React UI.
4. **UI Presentation:**
   - Shows both “History Match Result” and “AI Log Analysis Result” side by side or as a combined report.
   - Indicates which method found the root cause (history, AI/ML, or both).

---

## Architecture Diagram (Textual)

User (React UI)
    |
    v
Java Backend (Spring Boot)
    |                \
    v                 v
Vector RCA Flow   AI/ML Log Analysis Flow (Python Service)
    |                 |
    v                 v
Resolution DB     AI/ML Model (Llama 3, etc.)
    |                 |
    \_________________/
           |
           v
    Aggregated Results
           |
           v
      User (React UI)

---

## Benefits
- Handles both known and unknown issues.
- Fast answers and deep insights.
- Scalable and future-proof.

---

## Next Steps
- Implement both flows as separate modules/services.
- Design UI to present both results clearly.
- Add aggregation logic in backend.
- Test with real log files and scenarios.

---

This dual approach will make your application robust, user-friendly, and ready for future expansion.