FROM python:3.13-slim

RUN pip install --no-cache-dir uv

WORKDIR /app

COPY genai/pyproject.toml genai/uv.lock* ./

RUN uv sync --frozen --no-dev

ENV PATH="/app/.venv/bin:$PATH"

# Keep speech recognition independent from runtime internet access. The Rancher
# deployment points WHISPER_MODEL at this image-bundled CTranslate2 model.
RUN python -c "from huggingface_hub import snapshot_download; snapshot_download('Systran/faster-whisper-tiny', local_dir='/opt/models/faster-whisper-tiny')"

COPY genai/ .
COPY RAG ./RAG
COPY ["RAG doc DB", "./RAG doc DB"]

EXPOSE 8084

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8084"]
