import requests
import json
import os
from .base import LLMProvider


class OpenRouterProvider(LLMProvider):
    def __init__(self, model=None):
        # Load config from the common providers_config.json file
        config_path = os.path.join(os.path.dirname(__file__), "providers_config.json")

        with open(config_path, "r") as f:
            config = json.load(f)

        openrouter_config = config.get("openrouter", {})

        api_key = (
            os.getenv("OPENROUTER_API_KEY")
            or openrouter_config.get("api_key")
        )

        self.model = model or openrouter_config.get(
            "model",
            "openai/gpt-oss-20b:free"
        )

        self.api_url = "https://openrouter.ai/api/v1/chat/completions"

        self.headers = {
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",

            # Optional but recommended by OpenRouter
            "HTTP-Referer": "http://localhost",
            "X-Title": "SentinelAI"
        }

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
                    )
                }
            ]
        }

        try:

            response = requests.post(
                self.api_url,
                headers=self.headers,
                json=payload,
                timeout=60
            )

            if response.status_code != 200:
                return {
                    "provider": "openrouter",
                    "error": f"OpenRouter API error {response.status_code}: {response.text}"
                }

            result = response.json()

            return {
                "provider": "openrouter",
                "model": self.model,
                "result": result["choices"][0]["message"]["content"]
            }

        except Exception as e:
            return {
                "provider": "openrouter",
                "error": f"{type(e).__name__}: {e}"
            }