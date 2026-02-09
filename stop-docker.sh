#!/bin/bash

echo "========================================"
echo "  Cloud Web - Arrêt des services Docker"
echo "========================================"
echo ""

echo "Arrêt de tous les services..."
docker-compose down

echo ""
echo "Services arrêtés avec succès!"
echo ""
