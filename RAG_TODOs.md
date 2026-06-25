# RAG TODOs

## Current status

- RAG document folders exist under `RAG doc DB/`.
- Topic corpora exist under each topic's `corpus/chunks.json`.
- Per-PDF JSON files exist under each topic's `corpus/`.
- Markdown exports exist under each topic's `markdown/`.
- GenAI exposes RAG endpoints:
  - `GET /api/v1/genai/rag/topics`
  - `POST /api/v1/genai/rag/topics/{topic}/corpus`
  - `POST /api/v1/genai/rag/query`
- Docker stack was started during local testing and `genai-service` listed RAG topics.
- Later Docker Desktop daemon access failed locally with `Docker Desktop is unable to start`, so live end-to-end retesting needs Docker to be healthy again.
- Ollama model `llama3.2:1b` was manually pulled and is available.

## Remaining backend work

- Fix `ollama-pull` in `docker-compose.yml`.
  - Current behavior: the one-shot container exits with `unknown command "sh" for "ollama"`.
  - Expected behavior: compose should pull `${OLLAMA_MODEL:-llama3.2:1b}` automatically.
  - Likely fix: override the entrypoint or command so the container actually runs shell syntax instead of passing `sh` to the `ollama` binary.

- Verify full RAG query latency.
  - `GET /api/v1/genai/rag/topics` works.
  - `POST /api/v1/genai/rag/query` connected but did not return inside a short local curl timeout during testing.
  - Check whether the delay is model warmup, Ollama generation time, retrieval, or an app-level timeout/logging issue.

- Add server-side timeout/logging around LLM invocation.
  - Log topic, top_k, retrieved chunk count, selected model, and elapsed time.
  - Return a useful 502/504-style error if Ollama does not answer in time.

- Decide whether corpus generation should use the new Docling conversion script or the older `RAG/engine.py` pypdf path.
  - Current retrieval reads the aggregate `corpus/chunks.json`.
  - Per-PDF JSON files are not used directly by retrieval.
  - If Docling output should be the official pipeline, route rebuilds through `RAG/convert_pdfs_with_docling.py` or port that logic into `RAG/engine.py`.

- Add tests for RAG corpus loading.
  - Test topic listing.
  - Test stale-corpus detection.
  - Test retrieval reads `corpus/chunks.json`.
  - Test query endpoint error behavior when Ollama is unavailable.

## Remaining frontend work

- Confirm the RAG UI against a successful live query after Docker/GenAI is healthy again.
  - The UI now loads topics from GenAI and sends questions to the RAG query endpoint.
  - It needs end-to-end browser verification once the backend query returns reliably.

- Replace hardcoded fallback topic descriptions with backend metadata.
  - Current backend returns topic names only.
  - Add topic metadata such as display title, description, language, document count, and chunk count.

- Add a proper loading and timeout experience.
  - RAG answers can take a long time with local Ollama.
  - The UI should explain slow first responses and allow retry/cancel.

- Add source inspection.
  - Current RAG response includes sources.
  - The UI should let users expand source text and show source PDF/page/chunk clearly.

- Decide final product behavior.
  - Current backend supports question answering from RAG.
  - The old UI text said "Generate learning plan", but there is no backend endpoint that turns a RAG answer into a saved learning plan with lessons/exercises.
  - If the product goal is plan generation, add a backend endpoint that creates a learning plan from RAG context, then connect the frontend form to that endpoint.

## Nice-to-have improvements

- Add a small admin/debug page for rebuilding a topic corpus.
- Add corpus freshness indicators in the UI.
- Add document/source counts to the topic cards.
- Add a smoke-test script for both topics:
  - list topics
  - ask one question per topic
  - assert a non-empty answer and at least one source
- Add CI checks that generated corpora are valid JSON and match current PDFs.
