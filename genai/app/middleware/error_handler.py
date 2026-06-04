from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.llm import LLMConfigurationError


def _body(code: str, message: str, details=None) -> dict:
    b = {"code": code, "message": message}
    if details is not None:
        b["details"] = details
    return b


def add_error_handlers(app: FastAPI) -> None:

    @app.exception_handler(LLMConfigurationError)
    async def llm_config_exc(
        request: Request, exc: LLMConfigurationError
    ) -> JSONResponse:
        return JSONResponse(
            status_code=503,
            content=_body("LLM_NOT_CONFIGURED", exc.message),
        )

    @app.exception_handler(HTTPException)
    async def http_exc(request: Request, exc: HTTPException) -> JSONResponse:
        return JSONResponse(
            status_code=exc.status_code,
            content=_body(f"HTTP_{exc.status_code}", str(exc.detail)),
        )

    @app.exception_handler(RequestValidationError)
    async def validation_exc(
        request: Request, exc: RequestValidationError
    ) -> JSONResponse:
        return JSONResponse(
            status_code=422,
            content=_body(
                "VALIDATION_ERROR", "Request validation failed", exc.errors()
            ),
        )

    @app.exception_handler(Exception)
    async def unhandled_exc(request: Request, exc: Exception) -> JSONResponse:
        return JSONResponse(
            status_code=500,
            content=_body("INTERNAL_ERROR", "An unexpected error occurred"),
        )
