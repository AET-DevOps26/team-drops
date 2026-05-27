import asyncio
import base64
import io
import wave
from functools import lru_cache

import numpy as np

from app.config import settings

# Maps full language names (lowercase) to BCP-47 tags used by kokoro-onnx.
_KOKORO_LANG: dict[str, str] = {
    "english": "en-us",
    "german": "de",
    "french": "fr-fr",
    "spanish": "es",
    "italian": "it",
    "portuguese": "pt-br",
    "japanese": "ja",
    "chinese": "zh",
    "mandarin": "zh",
    "korean": "ko",
}

# Default voices per BCP-47 tag (af_heart works for most languages as a fallback).
_KOKORO_VOICES: dict[str, str] = {
    "en-us": "af_heart",
    "en-gb": "bf_emma",
    "de": "af_heart",
    "fr-fr": "af_heart",
    "es": "ef_dora",
    "it": "af_heart",
    "pt-br": "pf_dora",
    "ja": "jf_alpha",
    "zh": "af_heart",
    "ko": "af_heart",
}


def _to_kokoro_lang(language: str) -> str:
    return _KOKORO_LANG.get(language.lower(), "en-us")


@lru_cache(maxsize=1)
def _get_kokoro():
    from kokoro_onnx import Kokoro

    model_path = settings.kokoro_model_path
    voices_path = settings.kokoro_voices_path

    if not model_path or not voices_path:
        import urllib.request
        from pathlib import Path

        cache_dir = Path.home() / ".cache" / "kokoro_onnx"
        cache_dir.mkdir(parents=True, exist_ok=True)

        _BASE = "https://github.com/thewh1teagle/kokoro-onnx/releases/download/model-files-v1.0"

        def _download(filename: str) -> str:
            dest = cache_dir / filename
            if not dest.exists():
                urllib.request.urlretrieve(f"{_BASE}/{filename}", dest)
            return str(dest)

        model_path = _download("kokoro-v1.0.onnx")
        voices_path = _download("voices-v1.0.bin")

    return Kokoro(model_path, voices_path)


def _numpy_to_wav_b64(audio: np.ndarray, sample_rate: int) -> str:
    audio_int16 = (np.clip(audio, -1.0, 1.0) * 32767).astype(np.int16)
    buf = io.BytesIO()
    with wave.open(buf, "wb") as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(sample_rate)
        wf.writeframes(audio_int16.tobytes())
    return base64.b64encode(buf.getvalue()).decode()


def _synthesize_local(text: str, language: str) -> str:
    lang = _to_kokoro_lang(language)
    voice = _KOKORO_VOICES.get(lang, "af_heart")
    kokoro = _get_kokoro()
    samples, sample_rate = kokoro.create(text, voice=voice, speed=1.0, lang=lang)
    return _numpy_to_wav_b64(samples, sample_rate)


async def synthesize(text: str, language: str) -> str:
    """Synthesize text to speech, returning base64-encoded WAV audio.

    Uses kokoro-onnx locally (Ollama mode) or OpenAI TTS API (OpenAI mode).
    """
    if settings.llm_provider == "openai":
        import openai

        client = openai.AsyncOpenAI(api_key=settings.llm_api_key)
        response = await client.audio.speech.create(
            model="tts-1",
            voice="alloy",
            input=text,
            response_format="wav",
        )
        return base64.b64encode(response.content).decode()

    return await asyncio.to_thread(_synthesize_local, text, language)
