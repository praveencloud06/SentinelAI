from abc import ABC, abstractmethod


class LLMProvider(ABC):
    """
    Abstract base class for all AI providers.

    All providers MUST implement analyze() for chat/RCA completion.
    Providers that support text embedding generation MAY override embed().
    The default embed() implementation raises EmbeddingNotSupportedError so
    that existing providers require zero changes.
    """

    @abstractmethod
    def analyze(self, logs: str) -> dict:
        """Run log analysis / RCA chat completion. Must be implemented by every provider."""
        pass

    def embed(self, text: str) -> dict:
        """
        Generate a text embedding vector.

        Override this method in providers that support embedding generation
        (e.g. OpenAI, Ollama).  Providers that do not support embeddings
        intentionally raise EmbeddingNotSupportedError rather than returning
        a silent failure so callers get a clear, actionable error.

        Returns a dict with keys:
            model      (str)        – embedding model name used
            dimensions (int)        – length of the vector
            embedding  (list[float]) – the vector itself

        Raises:
            EmbeddingNotSupportedError – when the provider does not support embeddings.
        """
        # Import here to avoid a circular dependency at module load time.
        from app.exceptions import EmbeddingNotSupportedError
        raise EmbeddingNotSupportedError(
            f"Provider '{self.__class__.__name__}' does not support embedding generation."
        )

    def supports_embeddings(self) -> bool:
        """
        Returns True if this provider overrides embed() with a real implementation.
        Used by the router to select a capable provider for embedding requests.
        """
        return type(self).embed is not LLMProvider.embed
