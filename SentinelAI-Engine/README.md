# SentinelAI-AI-Engine

A modular Python FastAPI service for log analysis using LLM providers (Ollama, HuggingFace, OpenAI).

## Usage
- Start the service (recommended): `python -m uvicorn app.main:app --reload`
- If you're using the bundled venv on Windows PowerShell:
  - `.\venv\Scripts\Activate.ps1`
  - `python -m uvicorn app.main:app --reload`
- POST /analyze with JSON: `{ "logs": "..." }` and optional `?provider=ollama|huggingface|openai`

## Providers
- Ollama (local, default)
- HuggingFace (cloud, free)
- OpenAI (future)

Configure HuggingFace token in `huggingface.py`.

##Run Application
python -m uvicorn app.main:app --reload


grow token: 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
