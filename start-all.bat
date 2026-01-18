@echo off
echo Demarrage des deux applications...
echo.

echo [1/2] Demarrage du backend...
start "Backend" cmd /c "cd /d d:\ITU_S5\WEB\Cloud_web\Back && mvn spring-boot:run"

timeout /t 10 /nobreak > nul

echo [2/2] Demarrage du frontend...
start "Frontend" cmd /c "cd /d d:\ITU_S5\WEB\Cloud_web\Front && npm start"

echo.
echo Applications demarrees !
echo - Backend: http://localhost:8080/api
echo - Frontend: http://localhost:3000
echo.
pause