# Cloud Web

Application full-stack avec:
- Backend Spring Boot (`Back`)
- Frontend React/Vite (`Front`)
- PostgreSQL
- Serveur tuiles OSM (`overv/openstreetmap-tile-server`)

Le projet est orchestré via `docker-compose.yml`.

## Prérequis

- Docker
- Docker Compose

## Structure

```text
Cloud_web/
├── Back/                    # API Spring Boot
├── Front/                   # Interface React
├── Osm-server/data/         # Fichier .pbf OSM
├── sql/                     # Schéma et données SQL de référence
├── docker-compose.yml
└── .env / .env.example
```

## Configuration

1. Copier l'exemple d'environnement:

```bash
cp .env.example .env
```

2. Vérifier les variables principales:
- `SERVER_PORT` (backend, défaut `8080`)
- `FRONTEND_PORT` (frontend, défaut `5173`)
- `DB_*` (PostgreSQL)
- `OSM_PORT` (tile server, défaut `8090`)
- `FIREBASE_ENABLED` + `FIREBASE_SERVICE_ACCOUNT` si synchro Firebase activée

## Démarrage standard (online)

```bash
docker compose up -d
```

Services:
- Frontend: `http://localhost:5173`
- Backend API: `http://localhost:8080`
- OSM tiles: `http://localhost:8090`
- PostgreSQL: `localhost:5433`

## Important: OSM import

Le service `osm-import` peut échouer avec:
`service "osm-import" didn't complete successfully: exit 1`

Causes fréquentes:
1. Fichier absent: `Osm-server/data/madagascar.osm.pbf`
2. DNS/réseau indisponible pendant l'import (ex: `Temporary failure in name resolution`)
3. Volume OSM partiellement initialisé

Vérifications rapides:

```bash
ls -lh ./Osm-server/data/madagascar.osm.pbf
docker compose logs --tail=200 osm-import
docker run --rm busybox nslookup osmdata.openstreetmap.de
```

Réinitialisation OSM:

```bash
docker compose down
docker volume rm cloud_web_osm_data
docker compose up osm-import
docker compose up -d
```

## Endpoints utiles

- Santé API: `GET /api/auth/health`
- Signalements: `GET /api/signalements`
- Stats signalements: `GET /api/signalements/stats`
- Photos d'un signalement: `GET /api/signalements/{id}/photos`
- Sync Firebase (si activé): `POST /api/sync/firebase/refresh`

Base URL API locale: `http://localhost:8080/api`

## Vérification interface

- Carte visiteurs: `http://localhost:5173/Visiteurs`
  - affiche les stats (`points`, `surface`, `budget`, `avancement`)
  - affiche une barre de progression d'avancement
- Manager: `http://localhost:5173/manager`
  - affichage/édition des signalements
  - barre d'avancement global

Note: l'endpoint photos existe côté backend; l'affichage des photos dans l'UI dépend des écrans front branchés.

## Commandes utiles

```bash
# Voir l'état des conteneurs
docker compose ps

# Logs d'un service
docker compose logs -f backend
docker compose logs -f osm-import

# Rebuild propre
docker compose down
docker compose up -d --build
```
