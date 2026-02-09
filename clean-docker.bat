@echo off
echo ========================================
echo  Cloud Web - Nettoyage Docker
echo ========================================
echo.
echo ATTENTION: Cette operation va supprimer:
echo  - Tous les conteneurs
echo  - Tous les volumes (donnees de la base)
echo  - Toutes les images
echo.
set /p confirm="Etes-vous sur de vouloir continuer? (O/N): "

if /i "%confirm%" NEQ "O" (
    echo Operation annulee.
    pause
    exit /b
)

echo.
echo [1/3] Arret et suppression des conteneurs et volumes...
docker-compose down -v

echo.
echo [2/3] Suppression des images...
docker-compose down --rmi all

echo.
echo [3/3] Nettoyage general de Docker...
docker system prune -f

echo.
echo ========================================
echo  Nettoyage termine!
echo ========================================
echo.
pause
