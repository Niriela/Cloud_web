#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ARCHIVE_PATH="${1:-$ROOT_DIR/docker-images-cache/cloud_web_stack.tar}"

"$ROOT_DIR/scripts/import-docker-images.sh" "$ARCHIVE_PATH"

cd "$ROOT_DIR"
docker compose up -d --pull never
