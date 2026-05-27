import asyncio
from contextlib import asynccontextmanager

from fastapi import FastAPI, APIRouter

from app.config import settings
from app.middleware.error_handler import add_error_handlers
from app.routers.exercises import router as exercises_router
from app.routers.listening import router as listening_router
from app.routers.speaking import router as speaking_router
from app.routers.writing import router as writing_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    # PREWARM_MODELS=true pre-loads STT/TTS at startup to avoid cold-start latency
    # on the first request. Disabled by default because the initial model download
    # (Whisper ~150 MB, kokoro-onnx ~300 MB) would otherwise delay container startup.
    if settings.prewarm_models:
        from app.stt.client import _get_whisper
        from app.tts.client import _get_kokoro

        await asyncio.to_thread(_get_whisper)
        if settings.tts_enabled:
            await asyncio.to_thread(_get_kokoro)
    yield


app = FastAPI(
    title="GenAI Language Learning Service",
    description=(
        "LLM-powered microservice for the language learning platform. "
        "Provides exercise generation, writing evaluation, and speaking practice via LangChain "
        "with configurable backends (Ollama or OpenAI). "
        "All routes are prefixed with `/api/v1/genai/`.\n\n"
        "Interactive docs: `/docs` — Alternative docs: `/redoc`"
    ),
    version="0.1.0",
    servers=[{"url": "http://localhost:8084", "description": "GenAI Service (local)"}],
    lifespan=lifespan,
)

add_error_handlers(app)

api_v1 = APIRouter(prefix="/api/v1/genai")
api_v1.include_router(exercises_router)
api_v1.include_router(writing_router)
api_v1.include_router(speaking_router)
api_v1.include_router(listening_router)
app.include_router(api_v1)


@app.get("/health", tags=["ops"])
async def health():
    return {"status": "ok", "provider": settings.llm_provider}
