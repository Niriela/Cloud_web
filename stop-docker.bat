@echo off
echo ========================================
echo  Cloud Web - Arret des services Docker
echo ========================================
echo.

echo Arret de tous les services...
docker-compose down

echo.
echo Services arretes avec succes!
echo.
pause
