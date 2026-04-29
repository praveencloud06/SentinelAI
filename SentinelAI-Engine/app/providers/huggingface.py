

import requests
from .base import LLMProvider
from huggingface_hub import InferenceClient
import json
import os



class HuggingFaceProvider(LLMProvider):
    def __init__(self, model="facebook/bart-large-cnn"):
        self.model = model
        self.api_url = f"https://api-inference.huggingface.co/models/{model}"
        token = os.getenv("HUGGINGFACE_TOKEN")
        if not token:
            # Load token from a config file in the project folder
            config_path = os.path.join(os.path.dirname(__file__), '..', 'hf_token.json')
            with open(config_path, 'r') as f:
                config = json.load(f)
            token = config.get('HUGGINGFACE_TOKEN')
        self.headers = {"Authorization": f"Bearer {token}"}
        self.client = InferenceClient(token=token)

    def analyze(self, logs: str) -> dict:
        # Check if the model is currently deployed (warm)
        try:
            deployed_models = self.client.list_deployed_models()
            if self.model not in deployed_models:
                return {"provider": "huggingface", "error": f"Model '{self.model}' is not currently deployed/warm. Please try again later or choose another model."}
        except Exception as e:
            return {"provider": "huggingface", "error": f"{type(e).__name__}: {e}"}
        payload = {
            "inputs": (
                "Analyze these logs and respond ONLY as JSON with keys: "
                "issue, rootCause, impactedService, recommendedFix.\n\n"
                f"Logs:\n{logs}"
            )
        }
        try:
            resp = requests.post(self.api_url, headers=self.headers, json=payload, timeout=60)
            resp.raise_for_status()
            result = resp.json()
            return {"provider": "huggingface", "result": result[0]["generated_text"] if result else ""}
        except Exception as e:
            return {"provider": "huggingface", "error": f"{type(e).__name__}: {e}"}
