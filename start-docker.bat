@echo off
echo ========================================
echo  Cloud Web - Demarrage avec Docker
echo ========================================
echo.

REM Verifier si .env existe
if not exist .env (
    echo [INFO] Creation du fichier .env depuis .env.example...
    copy .env.example .env
    echo.
)

echo [1/4] Arret des conteneurs existants...
docker-compose down
echo.

echo [2/4] Construction des images Docker...
docker-compose build
echo.

echo [3/4] Demarrage de tous les services...
docker-compose up -d
echo.

echo [4/4] Affichage des logs...
echo.
echo ========================================
echo  Services demarres avec succes!
echo ========================================
echo.
echo URLs d'acces:
echo - Frontend:  http://localhost:5173
echo - Backend:   http://localhost:8080
echo - Database:  localhost:5433
echo - OSM Tiles: http://localhost:8090
echo.
echo Pour voir les logs:
echo   docker-compose logs -f
echo.
echo Pour arreter:
echo   docker-compose down
echo.

REM Afficher les logs en temps reel
docker-compose logs -f
