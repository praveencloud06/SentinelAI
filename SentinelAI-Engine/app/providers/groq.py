import requests
import json
import os
from .base import LLMProvider

class GroqProvider(LLMProvider):
    def __init__(self, model=None):
        # Load config from the common providers_config.json file
        config_path = os.path.join(os.path.dirname(__file__), 'providers_config.json')
        with open(config_path, 'r') as f:
            config = json.load(f)
        groq_config = config.get('groq', {})
        api_key = os.getenv("GROQ_API_KEY") or groq_config.get('api_key')
        model_name = model or groq_config.get('model', 'llama3-8b-8192')
        self.model = model_name
        self.api_url = "https://api.groq.com/openai/v1/chat/completions"
        self.headers = {"Authorization": f"Bearer {api_key}"}

    def analyze(self, logs: str) -> dict:
        payload = {
            "model": self.model,
            "messages": [
                {
                    "role": "user",
                    "content": (
                        "Analyze these logs and respond ONLY as JSON with keys: "
                        "issue, rootCause, impactedService, recommendedFix.\n\n"
                        f"Logs:\n{logs}"
                    ),
                }
            ]
        }
        try:
            resp = requests.post(self.api_url, headers=self.headers, json=payload, timeout=60)
            if resp.status_code != 200:
                return {"provider": "groq", "error": f"Groq API error {resp.status_code}: {resp.text}"}
            result = resp.json()
            return {"provider": "groq", "result": result["choices"][0]["message"]["content"]}
        except Exception as e:
            return {"provider": "groq", "error": f"{type(e).__name__}: {e}"}
