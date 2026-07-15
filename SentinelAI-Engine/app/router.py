
from app.providers.ollama import OllamaProvider
from app.providers.huggingface import HuggingFaceProvider
from app.providers.openai import OpenAIProvider
from app.providers.groq import GroqProvider
from app.providers.openrouter import OpenRouterProvider


PROVIDERS = {
    "groq": GroqProvider(),
    "ollama": OllamaProvider(),
    "huggingface": HuggingFaceProvider(),
    "openai": OpenAIProvider(),
    "openrouter": OpenRouterProvider()
}

def get_provider(name: str):
    return PROVIDERS.get(name, GroqProvider())
