#!/bin/bash

echo "Démarrage des deux applications..."
echo

echo "[1/2] Démarrage du backend..."
cd /home/vanille/Documents/S5/Mr Rojo/Cloud_web/Back || exit
mvn spring-boot:run &
BACK_PID=$!

sleep 10

echo "[2/2] Démarrage du frontend..."
cd /home/vanille/Documents/S5/Mr Rojo/Cloud_web/Front || exit
npm start &
FRONT_PID=$!

echo
echo "Applications démarrées !"
echo "- Backend  : http://localhost:8083/api"
echo "- Frontend : http://localhost:3000"
echo
echo "Backend PID  : $BACK_PID"
echo "Frontend PID : $FRONT_PID"

read -p "Appuyez sur Entrée pour quitter..."
