import requests
from .base import LLMProvider

class OllamaProvider(LLMProvider):
    def __init__(self, endpoint="http://localhost:11434/api/generate", model="llama3:8b"):
        self.endpoint = endpoint
        self.model = model

    def analyze(self, logs: str) -> dict:
        payload = {
            "model": self.model,
            "prompt": (
                "Analyze these logs and respond ONLY as JSON with keys: "
                "issue, rootCause, impactedService, recommendedFix.\n\n"
                f"Logs:\n{logs}"
            ),
            "stream": False
        }
        try:
            resp = requests.post(self.endpoint, json=payload, timeout=60)
            resp.raise_for_status()
            result = resp.json()
            return {"provider": "ollama", "result": result.get("response", "")}
        except Exception as e:
            return {"provider": "ollama", "error": f"{type(e).__name__}: {e}"}
