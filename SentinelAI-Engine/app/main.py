from fastapi import FastAPI, Body, Query
from app.router import get_provider
import json
import re

app = FastAPI()

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
        "errors": []
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

@app.post("/analyze")
def analyze(
    logs: str = Body(..., embed=True),
    provider: str = Query("groq", enum=["groq", "ollama", "huggingface", "openai"])
):
    try:
        llm = get_provider(provider)
        provider_result = llm.analyze(logs)
        return _to_rca_response(provider, provider_result)
    except Exception as e:
        # Always return a 200 with a structured body so callers (Java) don't fail the whole request.
        return {
            "provider": provider,
            "issue": "",
            "rootCause": "",
            "impactedService": "",
            "recommendedFix": "",
            "errors": [f"AI-Engine error: {type(e).__name__}: {e}"],
        }
