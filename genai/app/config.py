from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import Field
from pathlib import Path


def default_rag_doc_db_path() -> str:
    config_path = Path(__file__).resolve()
    for parent in [Path.cwd(), *config_path.parents]:
        candidate = parent / "RAG doc DB"
        if candidate.exists():
            return str(candidate)
    return str(config_path.parents[1] / "RAG doc DB")


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env", extra="ignore", populate_by_name=True
    )

    llm_provider: str = Field(default="openai", alias="LLM_PROVIDER")
    llm_api_key: str = Field(default="", alias="LLM_API_KEY")
    llm_model: str = Field(default="", alias="LLM_MODEL")
    llm_base_url: str = Field(default="", alias="LLM_BASE_URL")
    llm_request_timeout_seconds: int = Field(
        default=90, ge=1, alias="LLM_REQUEST_TIMEOUT_SECONDS"
    )
    rag_learning_plan_max_repair_attempts: int = Field(
        default=3,
        ge=0,
        le=5,
        alias="RAG_LEARNING_PLAN_MAX_REPAIR_ATTEMPTS",
    )

    ollama_base_url: str = Field(
        default="http://host.docker.internal:11434", alias="OLLAMA_BASE_URL"
    )
    ollama_model: str = Field(default="qwen3:1.7b", alias="OLLAMA_MODEL")

    mongo_url: str = Field(default="mongodb://localhost:27017", alias="MONGO_URL")
    rag_doc_db_path: str = Field(
        default_factory=default_rag_doc_db_path,
        alias="RAG_DOC_DB_PATH",
    )

    whisper_model: str = Field(default="base", alias="WHISPER_MODEL")
    whisper_device: str = Field(default="cpu", alias="WHISPER_DEVICE")
    stt_provider: str = Field(default="local", alias="STT_PROVIDER")
    tts_enabled: bool = Field(default=True, alias="TTS_ENABLED")
    prewarm_models: bool = Field(default=False, alias="PREWARM_MODELS")

    # Explicit paths to kokoro-onnx model files.
    # Download from https://github.com/thewh1teagle/kokoro-onnx/releases and set these.
    # If unset and TTS is enabled, the first /speaking/evaluate request will download them.
    kokoro_model_path: str = Field(default="", alias="KOKORO_MODEL_PATH")
    kokoro_voices_path: str = Field(default="", alias="KOKORO_VOICES_PATH")

    auth_enabled: bool = Field(default=False, alias="AUTH_ENABLED")
    keycloak_issuer_uri: str = Field(default="", alias="KEYCLOAK_ISSUER_URI")
    keycloak_jwks_uri: str = Field(default="", alias="KEYCLOAK_JWKS_URI")
    keycloak_audience: str = Field(default="", alias="KEYCLOAK_AUDIENCE")


settings = Settings()
