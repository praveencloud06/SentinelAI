from abc import ABC, abstractmethod

class LLMProvider(ABC):
    @abstractmethod
    def analyze(self, logs: str) -> dict:
        pass
