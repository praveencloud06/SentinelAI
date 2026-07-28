import os
import json
import time
import logging
import requests

from .base import LLMProvider
from app.exceptions import EmbeddingProviderError

logger = logging.getLogger(__name__)


class OllamaProvider(LLMProvider):
    """
    Ollama provider — supports both chat completion (analyze) and
    text embedding generation (embed) using locally hosted models.

    Ollama embedding API:  POST /api/embeddings
    Ollama generate  API:  POST /api/generate

    Configuration priority:
      1. Constructor arguments
      2. Environment variables  (OLLAMA_BASE_URL, OLLAMA_MODEL, OLLAMA_EMBEDDING_MODEL)
      3. Hard-coded defaults
    """

    def __init__(
        self,
        endpoint: str | None = None,
        model: str | None = None,
        embedding_model: str | None = None,
    ):
        base_url: str = (
            os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
        ).rstrip("/")

        # Chat / generation
        self.model: str = (
            model
            or os.getenv("OLLAMA_MODEL", "llama3:8b")
        )
        self.endpoint: str = endpoint or f"{base_url}/api/generate"

        # Embedding — uses a separate lightweight embedding model by default.
        # nomic-embed-text is the recommended Ollama embedding model.
        self.embedding_model: str = (
            embedding_model
            or os.getenv("OLLAMA_EMBEDDING_MODEL", "nomic-embed-text")
        )
        self._embedding_endpoint: str = f"{base_url}/api/embeddings"

    # ------------------------------------------------------------------
    # Chat / RCA completion (existing contract — do not change signature)
    # ------------------------------------------------------------------

    def analyze(self, logs: str) -> dict:
        """Send a log analysis prompt to the local Ollama generate API."""
        payload = {
            "model": self.model,
            "prompt": (
                "Analyze these logs and respond ONLY as JSON with keys: "
                "issue, rootCause, impactedService, recommendedFix.\n\n"
                f"Logs:\n{logs}"
            ),
            "stream": False,
        }
        try:
            resp = requests.post(self.endpoint, json=payload, timeout=60)
            resp.raise_for_status()
            result = resp.json()
            return {"provider": "ollama", "result": result.get("response", "")}
        except Exception as exc:
            return {"provider": "ollama", "error": f"{type(exc).__name__}: {exc}"}

    # ------------------------------------------------------------------
    # Text embedding generation (new capability)
    # ------------------------------------------------------------------

    def embed(self, text: str) -> dict:
        """
        Generate a text embedding using the local Ollama /api/embeddings API.

        Requires an embedding-capable model such as nomic-embed-text to be
        pulled in Ollama:  ollama pull nomic-embed-text

        Returns:
            dict with keys: model (str), dimensions (int), embedding (list[float])

        Raises:
            EmbeddingProviderError – on API errors, network failures, or unexpected responses.
        """
        payload = {
            "model": self.embedding_model,
            "prompt": text,
        }

        start = time.monotonic()
        logger.info(
            "Embedding request | provider=ollama model=%s text_length=%d",
            self.embedding_model,
            len(text),
        )

        try:
            resp = requests.post(
                self._embedding_endpoint,
                json=payload,
                timeout=30,
            )
        except requests.exceptions.Timeout:
            logger.error(
                "Embedding timeout | provider=ollama model=%s duration_ms=%.0f",
                self.embedding_model,
                (time.monotonic() - start) * 1000,
            )
            raise EmbeddingProviderError(
                f"Ollama embedding request timed out. "
                f"Is Ollama running at {self._embedding_endpoint}?"
            )
        except requests.exceptions.ConnectionError as exc:
            logger.error(
                "Embedding connection error | provider=ollama error=%s duration_ms=%.0f",
                exc,
                (time.monotonic() - start) * 1000,
            )
            raise EmbeddingProviderError(
                f"Cannot connect to Ollama at {self._embedding_endpoint}. "
                "Ensure Ollama is running locally."
            )
        except requests.exceptions.RequestException as exc:
            logger.error(
                "Embedding network error | provider=ollama error=%s duration_ms=%.0f",
                exc,
                (time.monotonic() - start) * 1000,
            )
            raise EmbeddingProviderError(
                f"Network error communicating with Ollama: {exc}"
            )

        duration_ms = (time.monotonic() - start) * 1000

        if resp.status_code != 200:
            logger.error(
                "Embedding failed | provider=ollama model=%s status=%d duration_ms=%.0f",
                self.embedding_model,
                resp.status_code,
                duration_ms,
            )
            raise EmbeddingProviderError(
                f"Ollama returned {resp.status_code}: {resp.text}. "
                f"Ensure the model '{self.embedding_model}' is pulled "
                "(run: ollama pull nomic-embed-text)."
            )

        try:
            body = resp.json()
            vector: list[float] = body["embedding"]
            if not isinstance(vector, list) or len(vector) == 0:
                raise ValueError("embedding field is empty or not a list")
        except (KeyError, ValueError) as exc:
            logger.error(
                "Embedding invalid response | provider=ollama model=%s duration_ms=%.0f",
                self.embedding_model,
                duration_ms,
            )
            raise EmbeddingProviderError(
                f"Unexpected response structure from Ollama embeddings API: {exc}"
            )

        dimensions = len(vector)
        logger.info(
            "Embedding success | provider=ollama model=%s dimensions=%d duration_ms=%.0f",
            self.embedding_model,
            dimensions,
            duration_ms,
        )

        return {
            "model": self.embedding_model,
            "dimensions": dimensions,
            "embedding": vector,
        }
