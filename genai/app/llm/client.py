from functools import lru_cache

from langchain_core.language_models.chat_models import BaseChatModel

from app.config import settings


class LLMConfigurationError(Exception):
    def __init__(self, message: str):
        self.message = message
        super().__init__(message)


def llm_configuration_status() -> dict:
    provider = settings.llm_provider.lower()
    configured = provider != "openai" or bool(settings.llm_api_key.strip())
    model = settings.llm_model or (
        "gpt-4o-mini" if provider == "openai" else settings.ollama_model
    )

    return {
        "provider": provider,
        "configured": configured,
        "model": model,
        "base_url_configured": bool(
            settings.llm_base_url.strip()
            if provider == "openai"
            else settings.ollama_base_url.strip()
        ),
        "api_key_present": bool(settings.llm_api_key.strip()),
        "request_timeout_seconds": settings.llm_request_timeout_seconds,
    }


def get_structured_llm(schema):
    llm = get_llm()
    if settings.llm_provider.lower() == "openai":
        if not settings.llm_base_url.strip():
            return llm.with_structured_output(schema, method="json_schema", strict=True)
        return llm.with_structured_output(schema, method="json_schema")
    return llm.with_structured_output(schema, method="json_schema")


@lru_cache(maxsize=1)
def get_llm() -> BaseChatModel:
    provider = settings.llm_provider.lower()

    if provider == "openai":
        if not settings.llm_api_key.strip():
            raise LLMConfigurationError("LLM provider openai requires LLM_API_KEY.")

        from langchain_openai import ChatOpenAI

        return ChatOpenAI(
            model=settings.llm_model or "gpt-4o-mini",
            api_key=settings.llm_api_key,
            base_url=settings.llm_base_url or None,
            temperature=0,
            timeout=settings.llm_request_timeout_seconds,
        )

    from langchain_ollama import ChatOllama

    return ChatOllama(
        model=settings.ollama_model,
        base_url=settings.ollama_base_url,
        temperature=0,
    )
