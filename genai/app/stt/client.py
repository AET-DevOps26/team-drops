import asyncio
import io
import threading

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

_whisper_lock = threading.Lock()
_whisper_instance = None


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


def _get_whisper():
    global _whisper_instance
    if _whisper_instance is not None:
        return _whisper_instance
    with _whisper_lock:
        if _whisper_instance is not None:
            return _whisper_instance
        from faster_whisper import WhisperModel

        _whisper_instance = WhisperModel(
            settings.whisper_model, device=settings.whisper_device, compute_type="int8"
        )
        return _whisper_instance


def _transcribe_local(audio_bytes: bytes, language: str) -> str:
    model = _get_whisper()
    lang_code = _to_whisper_lang(language)
    segments, _ = model.transcribe(io.BytesIO(audio_bytes), language=lang_code)
    return " ".join(s.text.strip() for s in segments)


async def transcribe(audio_bytes: bytes, language: str) -> str:
    """Transcribe audio bytes to text using the configured STT backend.

    Uses faster-whisper locally by default, or OpenAI Whisper API when
    STT_PROVIDER=openai. This is intentionally separate from LLM_PROVIDER
    because OpenAI-compatible LLM gateways usually do not implement audio APIs.
    """
    if settings.stt_provider.lower() == "openai":
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
