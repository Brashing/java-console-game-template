#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="$ROOT/out"
rm -rf "$OUT"
mkdir -p "$OUT"
find "$ROOT/src" -name "*.java" > "$ROOT/.sources"
javac -encoding UTF-8 -d "$OUT" @"$ROOT/.sources"
echo "Build OK. Run ./run.sh"
