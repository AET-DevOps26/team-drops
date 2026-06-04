FROM python:3.13-slim

RUN pip install --no-cache-dir uv

WORKDIR /app

COPY genai/pyproject.toml genai/uv.lock* ./

RUN uv sync --frozen --no-dev

ENV PATH="/app/.venv/bin:$PATH"

COPY genai/ .
COPY RAG ./RAG
COPY ["RAG doc DB", "./RAG doc DB"]

EXPOSE 8084

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8084"]
