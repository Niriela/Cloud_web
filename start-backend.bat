@echo off
REM Script pour lancer le backend facilement

cd /d "%~dp0Back"

echo.
echo ==========================================
echo Compilation et demarrage du Backend...
echo ==========================================
echo.

REM Compiler le projet
echo Compilation en cours...
call mvn clean package -DskipTests

REM Vérifier si la compilation a réussi
if %ERRORLEVEL% EQU 0 (
    echo.
    echo ==========================================
    echo Compilation reussie!
    echo ==========================================
    echo.
    echo Demarrage de l'application...
    echo API sera disponible sur: http://localhost:8080/api
    echo.
    java -jar target\cloud-web-api-1.0.0.jar
) else (
    echo.
    echo ==========================================
    echo Erreur de compilation!
    echo ==========================================
    pause
    exit /b 1
)