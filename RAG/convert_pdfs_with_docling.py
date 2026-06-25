from __future__ import annotations

import argparse
import csv
import hashlib
import json
import multiprocessing
import re
import sys
import unicodedata
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from pypdf import PdfReader


DEFAULT_DB_ROOT = Path(__file__).resolve().parents[1] / "RAG doc DB"
DEFAULT_CHUNK_SIZE = 3500
DEFAULT_CHUNK_OVERLAP = 450
TABLE_HEADER = (
    "| Status | PDF | Source URL | Markdown | JSON | Chunks | Notes |\n"
    "| --- | --- | --- | --- | --- | --- | --- |\n"
)


@dataclass
class DataRow:
    status: str
    pdf: str
    source_url: str = ""
    markdown: str = ""
    json_path: str = ""
    chunks: str = ""
    notes: str = ""


def main() -> int:
    parser = argparse.ArgumentParser(
        description=(
            "Convert topic PDFs in RAG doc DB with Docling, store Markdown and "
            "per-PDF JSON, and rebuild corpus/chunks.json."
        )
    )
    parser.add_argument("--db-root", type=Path, default=DEFAULT_DB_ROOT)
    parser.add_argument(
        "--topic",
        action="append",
        help="Topic folder name under the RAG document DB. Repeat to process multiple topics.",
    )
    parser.add_argument("--chunk-size", type=int, default=DEFAULT_CHUNK_SIZE)
    parser.add_argument("--chunk-overlap", type=int, default=DEFAULT_CHUNK_OVERLAP)
    parser.add_argument(
        "--docling-timeout",
        type=int,
        default=45,
        help="Seconds to wait for Docling on one PDF before using the pypdf fallback.",
    )
    parser.add_argument("--force", action="store_true", help="Retransfer PDFs even when outputs are current.")
    args = parser.parse_args()

    db_root = args.db_root.resolve()
    topics = args.topic or _list_topics(db_root)
    if not topics:
        print(f"No RAG topics found under {db_root}", file=sys.stderr)
        return 1

    for topic in topics:
        stats = process_topic(
            db_root / topic,
            chunk_size=args.chunk_size,
            chunk_overlap=args.chunk_overlap,
            docling_timeout=args.docling_timeout,
            force=args.force,
        )
        print(
            f"{topic}: {stats['pdf_count']} PDFs, {stats['converted']} converted, "
            f"{stats['skipped']} skipped, {stats['chunk_count']} chunks"
        )
    return 0


def process_topic(
    topic_dir: Path,
    *,
    chunk_size: int,
    chunk_overlap: int,
    docling_timeout: int,
    force: bool,
) -> dict[str, int]:
    if not topic_dir.is_dir():
        raise FileNotFoundError(f"Topic folder not found: {topic_dir}")

    corpus_dir = topic_dir / "corpus"
    markdown_dir = topic_dir / "markdown"
    corpus_dir.mkdir(parents=True, exist_ok=True)
    markdown_dir.mkdir(parents=True, exist_ok=True)

    rows = _load_or_create_data_rows(topic_dir)
    rows_by_pdf = {row.pdf: row for row in rows}
    pdfs = sorted(path for path in topic_dir.glob("*.pdf") if path.is_file())
    manifest_urls = _read_manifest_urls(topic_dir / "manifest.tsv")

    all_chunks: list[dict[str, Any]] = []
    converted = 0
    skipped = 0

    for pdf_path in pdfs:
        row = rows_by_pdf.setdefault(
            pdf_path.name,
            DataRow(status="PENDING", pdf=pdf_path.name, source_url=manifest_urls.get(pdf_path.name, "")),
        )
        if not row.source_url:
            row.source_url = manifest_urls.get(pdf_path.name, "")

        md_path = markdown_dir / f"{pdf_path.stem}.md"
        json_path = corpus_dir / f"{pdf_path.stem}.json"
        fingerprint = _fingerprint(pdf_path)

        if not force and _is_current(json_path, fingerprint) and md_path.exists():
            payload = json.loads(json_path.read_text(encoding="utf-8"))
            chunks = payload.get("chunks", [])
            skipped += 1
            _mark_row(row, md_path, json_path, len(chunks), "TRANSFERRED", "Skipped; outputs are current.")
        else:
            payload = _convert_pdf(
                pdf_path,
                chunk_size=chunk_size,
                chunk_overlap=chunk_overlap,
                docling_timeout=docling_timeout,
                fingerprint=fingerprint,
            )
            md_path.write_text(payload["markdown"], encoding="utf-8")
            json_path.write_text(
                json.dumps(payload, ensure_ascii=False, indent=2, default=str),
                encoding="utf-8",
            )
            converted += 1
            note = payload.get("converter_note") or f"Converted with {payload['converter']}."
            _mark_row(row, md_path, json_path, len(payload["chunks"]), "TRANSFERRED", note)
            chunks = payload["chunks"]

        all_chunks.extend(chunks)

    rows = sorted(rows_by_pdf.values(), key=lambda item: item.pdf.lower())
    _write_data_list(topic_dir / "data_list.md", topic_dir.name, rows)
    _write_aggregate_corpus(
        corpus_dir / "chunks.json",
        topic_dir,
        pdfs,
        all_chunks,
        chunk_size=chunk_size,
        chunk_overlap=chunk_overlap,
    )
    return {
        "pdf_count": len(pdfs),
        "converted": converted,
        "skipped": skipped,
        "chunk_count": len(all_chunks),
    }


def _convert_pdf(
    pdf_path: Path,
    *,
    chunk_size: int,
    chunk_overlap: int,
    docling_timeout: int,
    fingerprint: dict[str, Any],
) -> dict[str, Any]:
    converted_at = datetime.now(timezone.utc).isoformat()
    document_json: dict[str, Any] = {}
    docling_result, docling_note = _convert_with_docling_timeout(pdf_path, docling_timeout)
    if docling_result is not None:
        markdown = docling_result["markdown"]
        document_json = docling_result["document"]
        converter_name = "docling"
        converter_note = ""
    else:
        markdown, document_json = _fallback_pdf_to_markdown(pdf_path)
        converter_name = "pypdf"
        converter_note = f"{docling_note}; converted with pypdf fallback."

    if converter_name == "docling":
        pages = _page_texts(pdf_path)
    else:
        pages = document_json.get("pages", [])
    chunks = _chunk_markdown(
        markdown,
        source=pdf_path.name,
        pages=pages,
        chunk_size=chunk_size,
        chunk_overlap=chunk_overlap,
    )

    return {
        "source": pdf_path.name,
        "converter": converter_name,
        "converter_note": converter_note,
        "converted_at": converted_at,
        "fingerprint": fingerprint,
        "markdown_path": f"../markdown/{pdf_path.stem}.md",
        "markdown": markdown,
        "document": document_json,
        "chunks": chunks,
    }


def _convert_with_docling_timeout(
    pdf_path: Path, timeout_seconds: int
) -> tuple[dict[str, Any] | None, str]:
    queue: multiprocessing.Queue = multiprocessing.Queue(maxsize=1)
    process = multiprocessing.Process(
        target=_docling_worker,
        args=(str(pdf_path), queue),
        daemon=True,
    )
    process.start()
    process.join(timeout_seconds)
    if process.is_alive():
        process.terminate()
        process.join(5)
        return None, f"Docling timed out after {timeout_seconds}s"
    if queue.empty():
        return None, f"Docling exited with code {process.exitcode}"
    status, payload = queue.get()
    if status == "ok":
        return payload, ""
    return None, payload


def _docling_worker(pdf_path: str, queue: multiprocessing.Queue) -> None:
    try:
        from docling.document_converter import DocumentConverter

        result = DocumentConverter().convert(Path(pdf_path))
        document = result.document
        markdown = _normalize_text(document.export_to_markdown(), preserve_lines=True)
        payload = {
            "markdown": markdown,
            "document": {
                "format": "docling",
                "text_characters": len(markdown),
            },
        }
        queue.put(("ok", payload))
    except Exception as exc:
        queue.put(("error", f"Docling failed ({type(exc).__name__})"))


def _fallback_pdf_to_markdown(pdf_path: Path) -> tuple[str, dict[str, Any]]:
    pages = _page_texts(pdf_path)
    sections = []
    for page in pages:
        text = _normalize_text(str(page["text"]), preserve_lines=True)
        if text:
            sections.append(f"## Page {page['page']}\n\n{text}")
    return "\n\n".join(sections).strip() + "\n", {"pages": pages}


def _page_texts(pdf_path: Path) -> list[dict[str, Any]]:
    reader = PdfReader(str(pdf_path))
    pages = []
    for page_number, page in enumerate(reader.pages, start=1):
        text = _normalize_text(page.extract_text() or "", preserve_lines=True)
        if text:
            pages.append({"page": page_number, "text": text})
    return pages


def _chunk_markdown(
    markdown: str,
    *,
    source: str,
    pages: list[dict[str, Any]],
    chunk_size: int,
    chunk_overlap: int,
) -> list[dict[str, Any]]:
    chunks: list[dict[str, Any]] = []
    text = _normalize_text(markdown)
    if not text:
        return chunks

    start = 0
    while start < len(text):
        end = min(start + chunk_size, len(text))
        if end < len(text):
            boundary = max(text.rfind(". ", start, end), text.rfind("\n", start, end))
            if boundary > start + int(chunk_size * 0.55):
                end = boundary + 1
        chunk_text = text[start:end].strip()
        if chunk_text:
            chunks.append(
                {
                    "source": source,
                    "page": _guess_page(chunk_text, pages),
                    "chunk_index": len(chunks),
                    "text": chunk_text,
                }
            )
        if end >= len(text):
            break
        start = max(end - chunk_overlap, start + 1)
    return chunks


def _guess_page(chunk_text: str, pages: list[dict[str, Any]]) -> int:
    sample = chunk_text[:220].lower()
    if not sample:
        return 1
    best_page = 1
    best_score = 0
    for page in pages:
        page_text = str(page.get("text", "")).lower()
        score = sum(1 for token in sample.split()[:20] if token and token in page_text)
        if score > best_score:
            best_score = score
            best_page = int(page.get("page") or 1)
    return best_page


def _load_or_create_data_rows(topic_dir: Path) -> list[DataRow]:
    path = topic_dir / "data_list.md"
    if path.exists():
        rows = _read_data_list(path)
        if rows:
            return rows
    manifest_urls = _read_manifest_urls(topic_dir / "manifest.tsv")
    return [
        DataRow(status="PENDING", pdf=pdf_path.name, source_url=manifest_urls.get(pdf_path.name, ""))
        for pdf_path in sorted(topic_dir.glob("*.pdf"))
    ]


def _read_data_list(path: Path) -> list[DataRow]:
    rows: list[DataRow] = []
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.startswith("|") or line.startswith("| ---") or " PDF " in line:
            continue
        cells = [cell.strip() for cell in line.strip("|").split("|")]
        if len(cells) < 7:
            continue
        rows.append(
            DataRow(
                status=_unescape_cell(cells[0]),
                pdf=_unescape_cell(cells[1]),
                source_url=_unescape_cell(cells[2]),
                markdown=_unescape_cell(cells[3]),
                json_path=_unescape_cell(cells[4]),
                chunks=_unescape_cell(cells[5]),
                notes=_unescape_cell(cells[6]),
            )
        )
    return rows


def _write_data_list(path: Path, topic: str, rows: list[DataRow]) -> None:
    content = [
        f"# Data list: {topic}\n\n",
        "This file lists the PDF sources used for the topic and whether each file has been transferred into Markdown and JSON corpus files.\n\n",
        TABLE_HEADER,
    ]
    for row in rows:
        content.append(
            "| "
            + " | ".join(
                _escape_cell(value)
                for value in (
                    row.status,
                    row.pdf,
                    row.source_url,
                    row.markdown,
                    row.json_path,
                    row.chunks,
                    row.notes,
                )
            )
            + " |\n"
        )
    path.write_text("".join(content), encoding="utf-8")


def _write_aggregate_corpus(
    path: Path,
    topic_dir: Path,
    pdfs: list[Path],
    chunks: list[dict[str, Any]],
    *,
    chunk_size: int,
    chunk_overlap: int,
) -> None:
    payload = {
        "topic": topic_dir.name,
        "chunk_size": chunk_size,
        "chunk_overlap": chunk_overlap,
        "documents": [_engine_fingerprint(pdf_path) for pdf_path in pdfs],
        "chunks": chunks,
    }
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def _mark_row(
    row: DataRow,
    md_path: Path,
    json_path: Path,
    chunk_count: int,
    status: str,
    notes: str,
) -> None:
    row.status = status
    row.markdown = _relative_output(md_path)
    row.json_path = _relative_output(json_path)
    row.chunks = str(chunk_count)
    row.notes = notes


def _relative_output(path: Path) -> str:
    return f"{path.parent.name}/{path.name}"


def _read_manifest_urls(path: Path) -> dict[str, str]:
    if not path.exists():
        return {}
    with path.open(newline="", encoding="utf-8") as handle:
        reader = csv.DictReader(handle, delimiter="\t")
        return {
            row.get("filename", ""): row.get("url", "")
            for row in reader
            if row.get("filename") and row.get("url")
        }


def _is_current(json_path: Path, fingerprint: dict[str, Any]) -> bool:
    if not json_path.exists():
        return False
    try:
        payload = json.loads(json_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return False
    return payload.get("fingerprint") == fingerprint and bool(payload.get("chunks"))


def _fingerprint(path: Path) -> dict[str, Any]:
    stat = path.stat()
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return {
        "filename": path.name,
        "size": stat.st_size,
        "modified_ns": stat.st_mtime_ns,
        "sha256": digest.hexdigest(),
    }


def _engine_fingerprint(path: Path) -> dict[str, Any]:
    stat = path.stat()
    return {
        "filename": path.name,
        "size": stat.st_size,
        "modified_ns": stat.st_mtime_ns,
    }


def _normalize_text(text: str, *, preserve_lines: bool = False) -> str:
    cleaned = "".join(
        char
        if char in "\n\t" or not unicodedata.category(char).startswith("C")
        else " "
        for char in text
    )
    if preserve_lines:
        cleaned = re.sub(r"[ \t]+", " ", cleaned)
        cleaned = re.sub(r"\n{3,}", "\n\n", cleaned)
        return cleaned.strip()
    return re.sub(r"\s+", " ", cleaned).strip()


def _escape_cell(value: str) -> str:
    return str(value).replace("|", "\\|").replace("\n", " ")


def _unescape_cell(value: str) -> str:
    return value.replace("\\|", "|").strip()


def _list_topics(db_root: Path) -> list[str]:
    if not db_root.exists():
        return []
    return sorted(child.name for child in db_root.iterdir() if child.is_dir())


if __name__ == "__main__":
    raise SystemExit(main())
