#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ARCHIVE_DIR="${1:-$ROOT_DIR/docker-images-cache}"
ARCHIVE_PATH="$ARCHIVE_DIR/cloud_web_stack.tar"

mkdir -p "$ARCHIVE_DIR"

mapfile -t IMAGES < <(cd "$ROOT_DIR" && docker compose config --images | sort -u)

if [ "${#IMAGES[@]}" -eq 0 ]; then
  echo "Aucune image detectee dans docker-compose.yml"
  exit 1
fi

echo "Images detectees:"
printf ' - %s\n' "${IMAGES[@]}"

echo "Pull des images..."
for image in "${IMAGES[@]}"; do
  docker pull "$image"
done

echo "Creation de l'archive: $ARCHIVE_PATH"
docker save -o "$ARCHIVE_PATH" "${IMAGES[@]}"

echo "Archive creee avec succes."
