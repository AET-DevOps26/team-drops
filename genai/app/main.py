from fastapi import FastAPI

from app.config import settings
from app.routers.exercises import router as exercises_router
from app.routers.writing import router as writing_router

app = FastAPI(
    title="GenAI Language Learning Service",
    description=(
        "AI-powered microservice for the language learning platform. "
        "Provides exercise generation, speaking evaluation, and writing evaluation "
        "powered by LangChain with configurable LLM backends (Ollama or OpenAI).\n\n"
        "Interactive docs: `/docs` — Alternative docs: `/redoc`"
    ),
    version="0.1.0",
)

app.include_router(exercises_router)
app.include_router(writing_router)


@app.get("/health", tags=["ops"])
async def health():
    return {"status": "ok", "provider": settings.llm_provider}
