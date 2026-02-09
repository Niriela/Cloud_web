#!/bin/bash

echo "========================================"
echo "  Cloud Web - Démarrage avec Docker"
echo "========================================"
echo ""

# Vérifier si .env existe
if [ ! -f .env ]; then
    echo "[INFO] Création du fichier .env depuis .env.example..."
    cp .env.example .env
    echo ""
fi

echo "[1/4] Arrêt des conteneurs existants..."
docker-compose down
echo ""

echo "[2/4] Construction des images Docker..."
docker-compose build
echo ""

echo "[3/4] Démarrage de tous les services..."
docker-compose up -d
echo ""

echo "[4/4] Affichage des logs..."
echo ""
echo "========================================"
echo "  Services démarrés avec succès!"
echo "========================================"
echo ""
echo "URLs d'accès:"
echo "  - Frontend:  http://localhost:5173"
echo "  - Backend:   http://localhost:8080"
echo "  - Database:  localhost:5433"
echo "  - OSM Tiles: http://localhost:8090"
echo ""
echo "Pour voir les logs:"
echo "  docker-compose logs -f"
echo ""
echo "Pour arrêter:"
echo "  docker-compose down"
echo ""

# Afficher les logs en temps réel
docker-compose logs -f
