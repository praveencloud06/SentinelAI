"""
Request / response schemas for the SentinelAI AI Engine.

EmbeddingRequest and EmbeddingResponse are intentionally independent of
the existing analyze/RCA models so neither side needs to know about the other.
"""

from pydantic import BaseModel, Field
from typing import List


class EmbeddingRequest(BaseModel):
    """Request body for POST /api/embeddings."""

    text: str = Field(
        ...,
        min_length=1,
        description="The text to embed. Must not be empty.",
        examples=["Payment gateway timeout after deployment"],
    )


class EmbeddingResponse(BaseModel):
    """Successful response from POST /api/embeddings."""

    model: str = Field(
        ...,
        description="The embedding model used to generate the vector.",
        examples=["text-embedding-3-small"],
    )
    dimensions: int = Field(
        ...,
        description="Length of the returned embedding vector.",
        examples=[1536],
    )
    embedding: List[float] = Field(
        ...,
        description="The embedding vector as a list of floats.",
    )
