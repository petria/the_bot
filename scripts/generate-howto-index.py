#!/usr/bin/env python3
import argparse
import hashlib
import json
import re
import subprocess
from datetime import datetime, timezone
from pathlib import Path


ALLOWLIST = [
    "the_bot_web/frontend/src/App.tsx",
    "the_bot_web/frontend/src/permissions.ts",
    "the_bot_web/frontend/src/api",
    "the_bot_web/frontend/src/pages",
    "the_bot_web/src/main/java/org/freakz/web/controller",
    "the_bot_web/src/main/java/org/freakz/web/config",
    "the_bot_common/src/main/java/org/freakz/common/model",
    "the_bot_common/src/main/java/org/freakz/common/config",
    "docs",
]

EXTENSIONS = {".css", ".java", ".json", ".md", ".ts", ".tsx"}
MAX_CHUNK_CHARS = 3500


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace")


def normalize_text(text: str) -> str:
    text = re.sub(r"/\*.*?\*/", " ", text, flags=re.DOTALL)
    text = re.sub(r"\s+", " ", text)
    return text.strip()


def display_title(path: Path, repo_root: Path) -> str:
    relative = path.relative_to(repo_root).as_posix()
    stem = path.stem
    title = re.sub(r"([a-z0-9])([A-Z])", r"\1 \2", stem)
    title = title.replace("-", " ").replace("_", " ").strip()
    if "/frontend/src/pages/" in relative:
        return f"Web UI page: {title}"
    if "/frontend/src/api/" in relative:
        return f"Web UI API client: {title}"
    if "/controller/" in relative:
        return f"Bot web REST controller: {title}"
    if "/config/" in relative:
        return f"Configuration support: {title}"
    if relative.startswith("docs/"):
        return f"Documentation: {title}"
    return title


def area_for(path: Path, repo_root: Path) -> str:
    relative = path.relative_to(repo_root).as_posix()
    if "/frontend/src/pages/" in relative:
        return "web-ui"
    if "/frontend/src/api/" in relative:
        return "web-api-client"
    if "/controller/" in relative:
        return "web-rest-api"
    if "/config/" in relative:
        return "configuration"
    if relative.startswith("docs/"):
        return "documentation"
    return "source"


def extract_keywords(text: str, path: Path) -> list[str]:
    candidates = set()
    candidates.update(re.findall(r"[A-Za-z][A-Za-z0-9]*(?:Page|Controller|Service|Config|Command|Route|User|Channel|Connection)", text))
    candidates.update(re.findall(r"['\"]([A-Za-z][A-Za-z0-9 _./:-]{2,60})['\"]", text))
    candidates.update(re.findall(r"@(?:GetMapping|PostMapping|PutMapping|DeleteMapping)\(['\"]([^'\"]+)['\"]\)", text))
    candidates.update(re.findall(r"/api/[A-Za-z0-9_./{}:-]+", text))
    candidates.update(path.stem.replace("-", " ").replace("_", " ").split())

    normalized = []
    seen = set()
    for candidate in candidates:
        value = re.sub(r"\s+", " ", candidate).strip()
        if not value or len(value) > 80:
            continue
        key = value.lower()
        if key in seen:
            continue
        seen.add(key)
        normalized.append(value)
    return sorted(normalized, key=str.lower)[:40]


def chunk_text(text: str) -> list[str]:
    clean = normalize_text(text)
    if not clean:
        return []
    if len(clean) <= MAX_CHUNK_CHARS:
        return [clean]

    chunks = []
    start = 0
    while start < len(clean):
        end = min(start + MAX_CHUNK_CHARS, len(clean))
        if end < len(clean):
            split = clean.rfind(". ", start, end)
            if split > start + 1000:
                end = split + 1
        chunks.append(clean[start:end].strip())
        start = end
    return [chunk for chunk in chunks if chunk]


def git_tracked_files(repo_root: Path) -> set[str] | None:
    try:
        result = subprocess.run(
            ["git", "ls-files"],
            cwd=repo_root,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.DEVNULL,
            check=True,
        )
    except (FileNotFoundError, subprocess.CalledProcessError):
        return None
    return {line.strip() for line in result.stdout.splitlines() if line.strip()}


def iter_files(repo_root: Path, tracked_files: set[str] | None):
    for entry in ALLOWLIST:
        path = repo_root / entry
        if path.is_file() and path.suffix in EXTENSIONS:
            relative = path.relative_to(repo_root).as_posix()
            if tracked_files is None or relative in tracked_files:
                yield path
        elif path.is_dir():
            for child in sorted(path.rglob("*")):
                if child.is_file() and child.suffix in EXTENSIONS:
                    relative = child.relative_to(repo_root).as_posix()
                    if tracked_files is None or relative in tracked_files:
                        yield child


def build_index(repo_root: Path) -> dict:
    chunks = []
    tracked_files = git_tracked_files(repo_root)
    for path in sorted(set(iter_files(repo_root, tracked_files))):
        relative = path.relative_to(repo_root).as_posix()
        if "/target/" in relative or "/node_modules/" in relative or "/dist/" in relative:
            continue

        text = read_text(path)
        keywords = extract_keywords(text, path)
        for index, chunk in enumerate(chunk_text(text)):
            chunk_id = hashlib.sha256(f"{relative}:{index}:{chunk}".encode("utf-8")).hexdigest()[:16]
            chunks.append({
                "id": chunk_id,
                "title": display_title(path, repo_root),
                "area": area_for(path, repo_root),
                "sourcePath": relative,
                "chunkIndex": index,
                "keywords": keywords,
                "text": chunk,
            })

    return {
        "schemaVersion": 1,
        "generatedAt": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "generator": "scripts/generate-howto-index.py",
        "sourceFilter": "git-tracked-files",
        "allowlist": ALLOWLIST,
        "chunkCount": len(chunks),
        "chunks": chunks,
    }


def main():
    parser = argparse.ArgumentParser(description="Generate build-time howto index for bot-engine.")
    parser.add_argument("--repo-root", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()

    repo_root = Path(args.repo_root).resolve()
    output = Path(args.output).resolve()
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(build_index(repo_root), ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
