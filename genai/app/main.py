from fastapi import FastAPI, APIRouter

from app.config import settings
from app.middleware.error_handler import add_error_handlers
from app.routers.exercises import router as exercises_router
from app.routers.writing import router as writing_router

app = FastAPI(
    title="GenAI Language Learning Service",
    description=(
        "LLM-powered microservice for the language learning platform. "
        "Provides exercise generation and writing evaluation via LangChain "
        "with configurable backends (Ollama or OpenAI). "
        "All routes are prefixed with `/api/v1/genai/`.\n\n"
        "Interactive docs: `/docs` — Alternative docs: `/redoc`"
    ),
    version="0.1.0",
    servers=[{"url": "http://localhost:8084", "description": "GenAI Service (local)"}],
)

add_error_handlers(app)

api_v1 = APIRouter(prefix="/api/v1/genai")
api_v1.include_router(exercises_router)
api_v1.include_router(writing_router)
app.include_router(api_v1)


@app.get("/health", tags=["ops"])
async def health():
    return {"status": "ok", "provider": settings.llm_provider}
