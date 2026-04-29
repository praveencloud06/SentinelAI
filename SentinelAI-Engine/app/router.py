
from app.providers.ollama import OllamaProvider
from app.providers.huggingface import HuggingFaceProvider
from app.providers.openai import OpenAIProvider
from app.providers.groq import GroqProvider


PROVIDERS = {
    "groq": GroqProvider(),
    "ollama": OllamaProvider(),
    "huggingface": HuggingFaceProvider(),
    "openai": OpenAIProvider()
}

def get_provider(name: str):
    return PROVIDERS.get(name, GroqProvider())
