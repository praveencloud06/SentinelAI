"""
Custom exceptions for the SentinelAI AI Engine.

Kept in a single module so every layer (providers, router, endpoint)
can import from one place without circular imports.
"""


class EmbeddingNotSupportedError(NotImplementedError):
    """
    Raised when a provider does not support text embedding generation.

    This is an intentional, informative error — not an unexpected failure.
    The endpoint translates this into a clear HTTP 422 response so callers
    know they need to switch to a provider that supports embeddings.
    """


class EmbeddingProviderError(RuntimeError):
    """
    Raised when a provider that supports embeddings encounters a runtime
    failure during embedding generation (network error, bad response, etc.).

    The endpoint translates this into an HTTP 502 response.
    """
