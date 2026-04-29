🚀 SentinelAI
Understanding Logs through Past Knowledge and AI Reasoning
🌟 Vision

SentinelAI is an evolving system designed to explore how AI can assist in understanding complex logs and incidents.

The goal is simple yet powerful:

Combine past knowledge with intelligent reasoning to help identify root causes faster and more effectively.

This project is being developed step-by-step with a focus on clarity, modular design, and future extensibility.

🧠 Core Idea

Logs can be understood in two ways:

1. Memory (What has happened before?)
2. Reasoning (What is happening now?)

SentinelAI combines both through a Dual Capability Architecture:

Historical RCA → Fast, reliable, based on known incidents
AI/ML Analysis → Flexible, exploratory, for new problems
🏗️ Current System Features (2026)
🔹 SentinelAI-Core (Java, Spring Boot)
Accepts logs from the React UI (file or text)
Performs vector-based RCA using pgvector embeddings
Identifies similar past incidents
Returns known root cause when confidence is high
Calls Python AI Engine when deeper analysis is needed
Aggregates and returns results to the UI
External services configurable via application.yaml
🔹 SentinelAI-AI-Engine (Python, FastAPI)
Receives log analysis requests from Java
Provides a clean abstraction over multiple AI providers
Supports:
Ollama (local models)
HuggingFace (cloud models)
OpenAI (planned)
Routes requests dynamically to selected provider
Returns structured JSON responses
Designed to be modular and extensible
🔹 React UI
Upload log files or paste log text
Displays RCA results
Can show:
History-based result
AI-based analysis
Combined insights
🔄 System Flow (Current)
User uploads logs via UI

Java backend processes request

A. Vector-Based RCA
Convert logs into embeddings
Search pgvector database
If high-confidence match found → return result
B. AI/ML Log Analysis (Fallback)
Send logs to Python AI Engine
AI Engine selects provider (Ollama / HF / etc.)
Returns structured root cause analysis
Aggregation
Best available result is returned
Optionally both results can be shown
🧩 Architecture (Textual View)
User (React UI)
   |
   v
Java Backend (Spring Boot)
   |
   v
-------------------------------
|  Vector RCA (pgvector DB)   |
-------------------------------
   | (if no match)
   v
-------------------------------
|  Python AI Engine (FastAPI) |
|  - Provider Abstraction     |
|  - Ollama, HuggingFace, ...|
-------------------------------
   |
   v
Aggregated Results
   |
   v
User (React UI)
🔌 AI Gateway (Inside Python Engine)
AI Provider Layer
   |        |         |         |
   v        v         v         v
Ollama   HuggingFace OpenAI   Claude
(Local)    (Free)     (Future) (Future)
📦 Response Structure
{
  "errors": [],
  "rootCause": "",
  "suggestedFix": "",
  "confidence": 0.0,
  "provider": ""
}

Security 
data limit
RAG : filter response in user ways

JIRA : User action

watchdog


🌱 Design Principles
Keep logic modular and replaceable
Separate orchestration (Java) and AI reasoning (Python)
Prefer structured outputs over free text
Enable easy model switching
Build with future expansion in mind
🚀 Future Expansion

SentinelAI is designed as a foundation that can evolve gradually.

🔹 Near-Term Enhancements
Improved log parsing and grouping
Confidence-based routing between Vector RCA and AI analysis
Better aggregation of multiple results
🔹 Advanced Capabilities
Explainability layer (why this root cause?)
Multi-model comparison and fallback strategies
Learning from past AI outputs
🔹 Toward Agentic AI

Over time, SentinelAI can evolve into a more autonomous system:

Tool-based reasoning (logs, RCA, metrics as tools)
Multi-step investigation workflows
Context-aware analysis using memory
Iterative problem-solving instead of one-shot responses
🧰 Tech Stack
Java (Spring Boot)
Python (FastAPI)
PostgreSQL + pgvector
React
Ollama / HuggingFace / OpenAI APIs


✅ 1. Request size / context limit

For model: llama-3.3-70b-versatile
Current limits are:

Limit	Value
Context window	131072 tokens
Max output tokens	32768

run UI : npm start
http://localhost:3000/
