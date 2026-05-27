import asyncio
import io
from functools import lru_cache

from app.config import settings

# Maps full language names (lowercase) to ISO 639-1 codes used by Whisper.
_WHISPER_LANG: dict[str, str] = {
    "english": "en",
    "german": "de",
    "french": "fr",
    "spanish": "es",
    "italian": "it",
    "portuguese": "pt",
    "japanese": "ja",
    "chinese": "zh",
    "mandarin": "zh",
    "korean": "ko",
    "dutch": "nl",
    "polish": "pl",
    "russian": "ru",
    "arabic": "ar",
}


def _to_whisper_lang(language: str) -> str | None:
    """Convert a full language name or BCP-47 tag to a Whisper ISO 639-1 code.
    Returns None to let Whisper auto-detect if the language is unrecognised."""
    code = _WHISPER_LANG.get(language.lower())
    if code:
        return code
    # If already a short code (e.g. "de"), pass it through
    if len(language) <= 3:
        return language.lower()
    return None


@lru_cache(maxsize=1)
def _get_whisper():
    from faster_whisper import WhisperModel

    return WhisperModel(settings.whisper_model, device="cpu", compute_type="int8")


def _transcribe_local(audio_bytes: bytes, language: str) -> str:
    model = _get_whisper()
    lang_code = _to_whisper_lang(language)
    segments, _ = model.transcribe(io.BytesIO(audio_bytes), language=lang_code)
    return " ".join(s.text.strip() for s in segments)


async def transcribe(audio_bytes: bytes, language: str) -> str:
    """Transcribe audio bytes to text using the configured STT backend.

    Uses faster-whisper locally (Ollama mode) or OpenAI Whisper API (OpenAI mode).
    """
    if settings.llm_provider == "openai":
        import openai

        client = openai.AsyncOpenAI(api_key=settings.llm_api_key)
        lang_code = _to_whisper_lang(language)
        result = await client.audio.transcriptions.create(
            model="whisper-1",
            file=("audio.wav", io.BytesIO(audio_bytes)),
            language=lang_code,
        )
        return result.text

    return await asyncio.to_thread(_transcribe_local, audio_bytes, language)
