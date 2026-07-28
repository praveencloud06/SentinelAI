import os
import json
import time
import logging
import requests

from .base import LLMProvider
from app.exceptions import EmbeddingNotSupportedError, EmbeddingProviderError

logger = logging.getLogger(__name__)


class OpenAIProvider(LLMProvider):
    """
    OpenAI provider — supports both chat completion (analyze) and
    text embedding generation (embed).

    Configuration priority:
      1. Environment variables  (OPENAI_API_KEY, OPENAI_BASE_URL, OPENAI_EMBEDDING_MODEL)
      2. providers_config.json  openai section
      3. Hard-coded defaults
    """

    # Default model dimensions for known OpenAI embedding models.
    _KNOWN_DIMENSIONS: dict[str, int] = {
        "text-embedding-3-small": 1536,
        "text-embedding-3-large": 3072,
        "text-embedding-ada-002": 1536,
    }

    def __init__(self, model: str | None = None):
        config_path = os.path.join(os.path.dirname(__file__), "providers_config.json")
        with open(config_path, "r") as f:
            config = json.load(f)

        openai_cfg = config.get("openai", {})

        self.api_key: str = (
            os.getenv("OPENAI_API_KEY")
            or openai_cfg.get("api_key", "")
        )
        self.base_url: str = (
            os.getenv("OPENAI_BASE_URL")
            or openai_cfg.get("base_url", "https://api.openai.com/v1")
        ).rstrip("/")

        # Chat / completion model
        self.model: str = model or openai_cfg.get("model", "gpt-4o-mini")

        # Embedding model — separate from the chat model
        self.embedding_model: str = (
            os.getenv("OPENAI_EMBEDDING_MODEL")
            or openai_cfg.get("embedding_model", "text-embedding-3-small")
        )

        self._headers: dict[str, str] = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json",
        }

    # ------------------------------------------------------------------
    # Chat / RCA completion (existing contract — do not change signature)
    # ------------------------------------------------------------------

    def analyze(self, logs: str) -> dict:
        """Send a log analysis prompt to the OpenAI chat completions API."""
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
            ],
        }
        try:
            resp = requests.post(
                f"{self.base_url}/chat/completions",
                headers=self._headers,
                json=payload,
                timeout=60,
            )
            if resp.status_code != 200:
                return {
                    "provider": "openai",
                    "error": f"OpenAI API error {resp.status_code}: {resp.text}",
                }
            result = resp.json()
            return {
                "provider": "openai",
                "result": result["choices"][0]["message"]["content"],
            }
        except Exception as exc:
            return {"provider": "openai", "error": f"{type(exc).__name__}: {exc}"}

    # ------------------------------------------------------------------
    # Text embedding generation (new capability)
    # ------------------------------------------------------------------

    def embed(self, text: str) -> dict:
        """
        Generate a text embedding using the OpenAI /v1/embeddings API.

        Returns:
            dict with keys: model (str), dimensions (int), embedding (list[float])

        Raises:
            EmbeddingProviderError – on API errors, network failures, or unexpected responses.
        """
        if not self.api_key:
            raise EmbeddingProviderError(
                "OpenAI API key is not configured. "
                "Set the OPENAI_API_KEY environment variable."
            )

        payload = {
            "model": self.embedding_model,
            "input": text,
        }

        start = time.monotonic()
        logger.info(
            "Embedding request | provider=openai model=%s text_length=%d",
            self.embedding_model,
            len(text),
        )

        try:
            resp = requests.post(
                f"{self.base_url}/embeddings",
                headers=self._headers,
                json=payload,
                timeout=30,
            )
        except requests.exceptions.Timeout:
            logger.error(
                "Embedding timeout | provider=openai model=%s duration_ms=%.0f",
                self.embedding_model,
                (time.monotonic() - start) * 1000,
            )
            raise EmbeddingProviderError(
                "OpenAI embedding request timed out after 30 seconds."
            )
        except requests.exceptions.RequestException as exc:
            logger.error(
                "Embedding network error | provider=openai error=%s duration_ms=%.0f",
                exc,
                (time.monotonic() - start) * 1000,
            )
            raise EmbeddingProviderError(
                f"Network error communicating with OpenAI: {exc}"
            )

        duration_ms = (time.monotonic() - start) * 1000

        if resp.status_code != 200:
            logger.error(
                "Embedding failed | provider=openai model=%s status=%d duration_ms=%.0f",
                self.embedding_model,
                resp.status_code,
                duration_ms,
            )
            raise EmbeddingProviderError(
                f"OpenAI API returned {resp.status_code}: {resp.text}"
            )

        try:
            body = resp.json()
            vector: list[float] = body["data"][0]["embedding"]
        except (KeyError, IndexError, ValueError) as exc:
            logger.error(
                "Embedding invalid response | provider=openai model=%s duration_ms=%.0f",
                self.embedding_model,
                duration_ms,
            )
            raise EmbeddingProviderError(
                f"Unexpected response structure from OpenAI embeddings API: {exc}"
            )

        dimensions = len(vector)
        logger.info(
            "Embedding success | provider=openai model=%s dimensions=%d duration_ms=%.0f",
            self.embedding_model,
            dimensions,
            duration_ms,
        )

        return {
            "model": self.embedding_model,
            "dimensions": dimensions,
            "embedding": vector,
        }
