#!/bin/bash

echo "========================================"
echo "  Cloud Web - Nettoyage Docker"
echo "========================================"
echo ""
echo "ATTENTION: Cette opération va supprimer:"
echo "  - Tous les conteneurs"
echo "  - Tous les volumes (données de la base)"
echo "  - Toutes les images"
echo ""
read -p "Êtes-vous sûr de vouloir continuer? (o/N): " confirm

if [ "$confirm" != "o" ] && [ "$confirm" != "O" ]; then
    echo "Opération annulée."
    exit 0
fi

echo ""
echo "[1/3] Arrêt et suppression des conteneurs et volumes..."
docker-compose down -v

echo ""
echo "[2/3] Suppression des images..."
docker-compose down --rmi all

echo ""
echo "[3/3] Nettoyage général de Docker..."
docker system prune -f

echo ""
echo "========================================"
echo "  Nettoyage terminé!"
echo "========================================"
echo ""
