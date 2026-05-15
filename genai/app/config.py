from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import Field


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env", extra="ignore", populate_by_name=True
    )

    llm_provider: str = Field(default="ollama", alias="LLM_PROVIDER")
    llm_api_key: str = Field(default="", alias="LLM_API_KEY")
    llm_model: str = Field(default="", alias="LLM_MODEL")

    ollama_base_url: str = Field(
        default="http://host.docker.internal:11434", alias="OLLAMA_BASE_URL"
    )
    ollama_model: str = Field(default="llama3", alias="OLLAMA_MODEL")

    mongo_url: str = Field(default="mongodb://localhost:27017", alias="MONGO_URL")


settings = Settings()
