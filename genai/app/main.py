import asyncio
import os
from contextlib import asynccontextmanager

from fastapi import FastAPI, APIRouter, Depends, Response
from fastapi.security import HTTPBearer
from prometheus_client import Gauge
from prometheus_fastapi_instrumentator import Instrumentator

from app.llm import llm_configuration_status
from app.middleware.auth import add_auth_middleware
from app.middleware.error_handler import add_error_handlers
from app.routers.exercises import router as exercises_router
from app.routers.listening import router as listening_router
from app.routers.rag import router as rag_router
from app.routers.speaking import router as speaking_router
from app.routers.writing import router as writing_router


bearer_auth = HTTPBearer(auto_error=False)
application_info = Gauge(
    "application_info",
    "Deployed application version information.",
    ("service", "version"),
)
application_info.labels(
    service="genai-service", version=os.getenv("APP_VERSION", "unknown")
).set(1)


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Preload speech recognition so readiness means speaking requests can be
    # transcribed. Only preload TTS when its model files are explicitly provided;
    # otherwise Kokoro may perform a large runtime download.
    from app.config import settings

    if settings.prewarm_models:
        from app.stt.client import _get_whisper

        await asyncio.to_thread(_get_whisper)
        if settings.kokoro_model_path and settings.kokoro_voices_path:
            from app.tts.client import _get_kokoro

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
add_auth_middleware(app)
Instrumentator().instrument(app).expose(
    app, endpoint="/metrics", include_in_schema=False
)

api_v1 = APIRouter(prefix="/api/v1/genai", dependencies=[Depends(bearer_auth)])
api_v1.include_router(exercises_router)
api_v1.include_router(writing_router)
api_v1.include_router(speaking_router)
api_v1.include_router(listening_router)
api_v1.include_router(rag_router)
app.include_router(api_v1)


@app.get(
    "/health",
    tags=["ops"],
    responses={
        503: {
            "description": "LLM provider is not configured",
            "content": {"application/json": {"schema": {}}},
        }
    },
)
async def health(response: Response):
    llm_status = llm_configuration_status()
    if not llm_status["configured"]:
        response.status_code = 503
        return {"status": "degraded"}
    return {"status": "ok"}


@app.get("/live", tags=["ops"], include_in_schema=False)
async def live():
    return {"status": "ok"}
