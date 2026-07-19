FROM python:3.13-slim

RUN pip install --no-cache-dir uv

WORKDIR /app

COPY genai/pyproject.toml genai/uv.lock* ./

RUN uv sync --frozen --no-dev

ENV PATH="/app/.venv/bin:$PATH"

# Keep speech recognition independent from runtime internet access. The Rancher
# deployment points WHISPER_MODEL at this image-bundled CTranslate2 model.
RUN python -c "from huggingface_hub import snapshot_download; snapshot_download('Systran/faster-whisper-tiny', local_dir='/opt/models/faster-whisper-tiny')"

# Listening exercises always require spoken audio. Bundle Kokoro so production
# never depends on a large runtime download from GitHub on the first request.
RUN python -c "from pathlib import Path; from urllib.request import urlretrieve; model_dir = Path('/opt/models/kokoro'); model_dir.mkdir(parents=True, exist_ok=True); base = 'https://github.com/thewh1teagle/kokoro-onnx/releases/download/model-files-v1.0'; urlretrieve(f'{base}/kokoro-v1.0.onnx', model_dir / 'kokoro-v1.0.onnx'); urlretrieve(f'{base}/voices-v1.0.bin', model_dir / 'voices-v1.0.bin')"

COPY genai/ .
COPY RAG ./RAG
COPY ["RAG doc DB", "./RAG doc DB"]

EXPOSE 8084

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8084"]
