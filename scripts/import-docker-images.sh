#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ARCHIVE_PATH="${1:-$ROOT_DIR/docker-images-cache/cloud_web_stack.tar}"

if [ ! -f "$ARCHIVE_PATH" ]; then
  echo "Archive introuvable: $ARCHIVE_PATH"
  echo "Generez-la d'abord avec: ./scripts/export-docker-images.sh"
  exit 1
fi

echo "Chargement des images depuis: $ARCHIVE_PATH"
docker load -i "$ARCHIVE_PATH"

echo "Chargement termine."
