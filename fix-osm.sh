#!/bin/bash
echo "Correction de la commande OSM..."

# Arrêter le conteneur problématique
docker compose stop osm-import 2>/dev/null || true
docker rm osm_import 2>/dev/null || true

# Tester la bonne commande
docker run --rm \
  -v ./Osm-server/data/madagascar.osm.pbf:/data/region.osm.pbf \
  -v cloudweb_osm_data:/data/database \
  overv/openstreetmap-tile-server \
  /bin/sh -c "
    if [ -f /data/database/planet-import-complete ]; then 
      echo 'OSM déjà importé, passage en mode serveur.'
      exec /run.sh run
    else 
      echo 'Lancement de l import...'
      exec /run.sh import
    fi
  "