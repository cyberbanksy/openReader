#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_DIR="$(mktemp -d "${TMPDIR:-/tmp}/openreader-apple.XXXXXX")"
trap 'rm -rf "$BUILD_DIR"' EXIT

rsync -a --delete --exclude '.build' "$ROOT/native/apple/" "$BUILD_DIR/"
(
  cd "$BUILD_DIR"
  swift test
)
