#!/bin/bash
# Script pour lancer le backend facilement

cd "$(dirname "$0")/Back"

echo "=========================================="
echo "Compilation et démarrage du Backend..."
echo "=========================================="

# Compiler le projet
mvn clean package -DskipTests

# Vérifier si la compilation a réussi
if [ $? -eq 0 ]; then
    echo ""
    echo "=========================================="
    echo "✅ Compilation réussie!"
    echo "=========================================="
    echo ""
    echo "Démarrage de l'application..."
    java -jar target/cloud-web-api-1.0.0.jar
else
    echo ""
    echo "=========================================="
    echo "❌ Erreur de compilation!"
    echo "=========================================="
    exit 1
fi
