from .base import LLMProvider

class OpenAIProvider(LLMProvider):
    def analyze(self, logs: str) -> dict:
        # Stub for future implementation
        return {"provider": "openai", "result": "Not implemented"}
