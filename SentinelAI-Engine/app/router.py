"""
Provider routing for the SentinelAI AI Engine.

get_provider()           – existing function, unchanged, used by /analyze.
get_embedding_provider() – new function, used by /api/embeddings.

The two functions are intentionally separate so the embedding path can
select a provider independently of the chat/RCA path without any risk
of breaking existing callers.
"""

import json
import os
import logging

from app.providers.ollama import OllamaProvider
from app.providers.huggingface import HuggingFaceProvider
from app.providers.openai import OpenAIProvider
from app.providers.groq import GroqProvider
from app.providers.openrouter import OpenRouterProvider
from app.exceptions import EmbeddingNotSupportedError

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Provider registry — used by both routing functions
# ---------------------------------------------------------------------------

PROVIDERS = {
    "groq": GroqProvider(),
    "ollama": OllamaProvider(),
    "huggingface": HuggingFaceProvider(),
    "openai": OpenAIProvider(),
    "openrouter": OpenRouterProvider(),
}


# ---------------------------------------------------------------------------
# Existing chat / RCA routing — DO NOT CHANGE
# ---------------------------------------------------------------------------

def get_provider(name: str):
    """
    Return the provider instance for the given name.
    Falls back to GroqProvider when the name is not recognised.
    This function is used exclusively by the /analyze endpoint and
    must remain fully backward compatible.
    """
    return PROVIDERS.get(name, GroqProvider())


# ---------------------------------------------------------------------------
# Embedding provider routing — NEW
# ---------------------------------------------------------------------------

def _load_embedding_config() -> dict:
    """Load the embedding section from providers_config.json."""
    config_path = os.path.join(
        os.path.dirname(__file__), "providers", "providers_config.json"
    )
    try:
        with open(config_path, "r") as f:
            config = json.load(f)
        return config.get("embedding", {})
    except Exception as exc:
        logger.warning("Could not load embedding config: %s", exc)
        return {}


def get_embedding_provider(requested_provider: str | None = None):
    """
    Return a provider instance that supports embedding generation.

    Selection order:
      1. Explicit ``requested_provider`` query param (if supplied and capable).
      2. ``embedding.provider`` value from providers_config.json.
      3. ``embedding.fallback_provider`` from providers_config.json.
      4. Any provider in the registry that supports embeddings.

    Raises:
        EmbeddingNotSupportedError – when no capable provider can be found.
    """
    embedding_cfg = _load_embedding_config()

    # Build the candidate list in priority order, deduplicated.
    candidates: list[str] = []
    if requested_provider:
        candidates.append(requested_provider.lower())
    configured_primary = embedding_cfg.get("provider", "openai")
    configured_fallback = embedding_cfg.get("fallback_provider", "ollama")
    for name in (configured_primary, configured_fallback):
        if name and name not in candidates:
            candidates.append(name)

    # Try each candidate in order.
    for name in candidates:
        provider = PROVIDERS.get(name)
        if provider is None:
            logger.debug("Embedding candidate '%s' not found in registry.", name)
            continue
        if provider.supports_embeddings():
            logger.debug("Selected embedding provider: %s", name)
            return provider
        logger.debug(
            "Provider '%s' does not support embeddings, trying next.", name
        )

    # Last resort: any registered provider that supports embeddings.
    for name, provider in PROVIDERS.items():
        if provider.supports_embeddings():
            logger.info(
                "Falling back to provider '%s' for embedding generation.", name
            )
            return provider

    raise EmbeddingNotSupportedError(
        "No configured provider supports embedding generation. "
        "Enable OpenAI (set OPENAI_API_KEY) or start Ollama with an "
        "embedding model (ollama pull nomic-embed-text)."
    )
