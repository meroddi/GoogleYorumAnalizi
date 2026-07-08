from __future__ import annotations

import os
from functools import lru_cache
from pathlib import Path

import streamlit as st


def _normalize_secret_value(raw_value: str) -> str:
    value = raw_value.strip()
    if len(value) >= 2 and value[0] == value[-1] and value[0] in {'"', "'"}:
        return value[1:-1]
    return value


@lru_cache(maxsize=None)
def _read_flat_secret_file(path_str: str) -> dict[str, str]:
    path = Path(path_str)
    data: dict[str, str] = {}
    if not path.exists():
        return data

    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue

        key, value = line.split("=", 1)
        key = key.strip()
        if not key or key.startswith("["):
            continue

        data[key] = _normalize_secret_value(value)

    return data


def _candidate_secret_paths(current_file: str) -> list[Path]:
    script_dir = Path(current_file).resolve().parent
    candidates = [
        Path.cwd() / ".streamlit" / "secrets.toml",
        script_dir / ".streamlit" / "secrets.toml",
        script_dir.parent / ".streamlit" / "secrets.toml",
    ]

    seen: set[Path] = set()
    ordered: list[Path] = []
    for candidate in candidates:
        resolved = candidate.resolve()
        if resolved not in seen:
            seen.add(resolved)
            ordered.append(resolved)
    return ordered


def get_secret(name: str, current_file: str, default: str = "") -> str:
    try:
        value = st.secrets.get(name)
        if value not in (None, ""):
            return str(value)
    except Exception:
        pass

    env_value = os.getenv(name)
    if env_value:
        return env_value

    for secret_path in _candidate_secret_paths(current_file):
        value = _read_flat_secret_file(str(secret_path)).get(name)
        if value not in (None, ""):
            return str(value)

    return default
