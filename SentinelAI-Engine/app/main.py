"""
SentinelAI AI Engine — FastAPI application entry point.

Endpoints
---------
POST /analyze           – existing chat / RCA completion (unchanged)
POST /api/embeddings    – NEW text embedding generation
"""

import json
import logging
import re
import time

from fastapi import FastAPI, Body, Query, HTTPException, Request
from fastapi.responses import JSONResponse

from app.router import get_provider, get_embedding_provider
from app.schemas import EmbeddingRequest, EmbeddingResponse
from app.exceptions import EmbeddingNotSupportedError, EmbeddingProviderError

# ---------------------------------------------------------------------------
# Logging — structured, single-line per event, no sensitive data
# ---------------------------------------------------------------------------

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s %(message)s",
)
logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Application
# ---------------------------------------------------------------------------

app = FastAPI(
    title="SentinelAI AI Engine",
    description=(
        "Generic AI gateway providing chat completion and text embedding "
        "generation. Supports multiple AI providers with transparent routing."
    ),
    version="2.0.0",
)


# ---------------------------------------------------------------------------
# Helpers shared by the /analyze endpoint (existing logic — do not change)
# ---------------------------------------------------------------------------

def _strip_code_fences(text: str) -> str:
    if not text:
        return ""
    cleaned = text.strip()
    cleaned = re.sub(r"^```(?:json)?\s*", "", cleaned, flags=re.IGNORECASE)
    cleaned = re.sub(r"\s*```$", "", cleaned)
    return cleaned.strip()


def _to_rca_response(provider: str, provider_result: dict) -> dict:
    result_text = (provider_result or {}).get("result")
    error_text = (provider_result or {}).get("error")

    rca = {
        "provider": provider,
        "issue": "",
        "rootCause": "",
        "impactedService": "",
        "recommendedFix": "",
        "errors": [],
    }

    if error_text:
        rca["errors"].append(str(error_text))
        return rca

    if not result_text:
        rca["errors"].append("No result")
        return rca

    cleaned = _strip_code_fences(str(result_text))
    try:
        parsed = json.loads(cleaned)
        if isinstance(parsed, dict):
            rca["issue"] = str(parsed.get("issue", "") or "")
            rca["rootCause"] = str(parsed.get("rootCause", "") or "")
            rca["impactedService"] = str(parsed.get("impactedService", "") or "")
            rca["recommendedFix"] = str(parsed.get("recommendedFix", "") or "")
            return rca
    except Exception:
        pass

    # Non-JSON output: keep it as rootCause so UI still shows something.
    rca["rootCause"] = cleaned
    rca["errors"].append("Provider returned non-JSON output")
    return rca


# ---------------------------------------------------------------------------
# POST /analyze  — existing endpoint, completely unchanged
# ---------------------------------------------------------------------------

@app.post("/analyze")
def analyze(
    logs: str = Body(..., embed=True),
    provider: str = Query("groq", enum=["groq", "ollama", "huggingface", "openai"]),
):
    """
    Run AI-assisted root cause analysis on log data.

    This endpoint is unchanged from the original implementation.
    All existing clients continue to work without modification.
    """
    try:
        llm = get_provider(provider)
        provider_result = llm.analyze(logs)
        return _to_rca_response(provider, provider_result)
    except Exception as exc:
        # Always return a 200 with a structured body so callers (Java) don't
        # fail the whole request.
        return {
            "provider": provider,
            "issue": "",
            "rootCause": "",
            "impactedService": "",
            "recommendedFix": "",
            "errors": [f"AI-Engine error: {type(exc).__name__}: {exc}"],
        }


# ---------------------------------------------------------------------------
# POST /api/embeddings  — NEW endpoint
# ---------------------------------------------------------------------------

@app.post(
    "/api/embeddings",
    response_model=EmbeddingResponse,
    summary="Generate a text embedding vector",
    responses={
        200: {"description": "Embedding generated successfully"},
        400: {"description": "Invalid request — text is empty or missing"},
        422: {"description": "Embedding not supported by the requested provider"},
        502: {"description": "Provider returned an error or is unavailable"},
        503: {"description": "No embedding-capable provider is configured"},
    },
)
def create_embedding(
    body: EmbeddingRequest,
    provider: str | None = Query(
        default=None,
        description=(
            "AI provider to use for embedding generation. "
            "Defaults to the configured primary embedding provider. "
            "Supported: openai, ollama."
        ),
    ),
) -> EmbeddingResponse:
    """
    Generate a text embedding vector for the supplied text.

    The AI Engine is provider-agnostic — it delegates to whichever
    embedding-capable provider is configured or explicitly requested.
    It has no knowledge of GitHub, Jira, Confluence, or any Knowledge
    Service internals.

    **Request**
    ```json
    { "text": "Payment gateway timeout after deployment" }
    ```

    **Response**
    ```json
    {
        "model": "text-embedding-3-small",
        "dimensions": 1536,
        "embedding": [0.0123, -0.0842, ...]
    }
    ```
    """
    # Input is already validated by Pydantic (min_length=1), but guard
    # against whitespace-only strings which pass min_length.
    if not body.text.strip():
        raise HTTPException(
            status_code=400,
            detail="The 'text' field must not be empty or whitespace only.",
        )

    start = time.monotonic()
    selected_provider_name = provider or "auto"
    logger.info(
        "Embedding request received | provider=%s text_length=%d",
        selected_provider_name,
        len(body.text),
    )

    # Provider selection
    try:
        embedding_provider = get_embedding_provider(provider)
    except EmbeddingNotSupportedError as exc:
        logger.warning(
            "No embedding provider available | requested=%s error=%s",
            selected_provider_name,
            exc,
        )
        raise HTTPException(status_code=503, detail=str(exc))

    # Embedding generation
    try:
        result = embedding_provider.embed(body.text)
    except EmbeddingNotSupportedError as exc:
        logger.warning(
            "Embedding not supported | provider=%s error=%s",
            embedding_provider.__class__.__name__,
            exc,
        )
        raise HTTPException(status_code=422, detail=str(exc))
    except EmbeddingProviderError as exc:
        duration_ms = (time.monotonic() - start) * 1000
        logger.error(
            "Embedding provider error | provider=%s duration_ms=%.0f error=%s",
            embedding_provider.__class__.__name__,
            duration_ms,
            exc,
        )
        raise HTTPException(status_code=502, detail=str(exc))
    except Exception as exc:
        duration_ms = (time.monotonic() - start) * 1000
        logger.error(
            "Unexpected embedding error | provider=%s duration_ms=%.0f error=%s",
            embedding_provider.__class__.__name__,
            duration_ms,
            exc,
        )
        raise HTTPException(
            status_code=502,
            detail=f"Unexpected error during embedding generation: {type(exc).__name__}: {exc}",
        )

    duration_ms = (time.monotonic() - start) * 1000
    logger.info(
        "Embedding complete | provider=%s model=%s dimensions=%d duration_ms=%.0f",
        embedding_provider.__class__.__name__,
        result.get("model", "unknown"),
        result.get("dimensions", 0),
        duration_ms,
    )

    return EmbeddingResponse(
        model=result["model"],
        dimensions=result["dimensions"],
        embedding=result["embedding"],
    )
