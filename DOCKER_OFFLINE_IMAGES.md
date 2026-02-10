# Utiliser Docker sans retélécharger les images

Ce projet fournit des scripts pour exporter/importer les images Docker du `docker-compose.yml`
dans une archive `.tar`.

## 1) Exporter les images en `.tar`

```bash
./scripts/export-docker-images.sh
```

PowerShell:

```powershell
./scripts/export-docker-images.ps1
```

Archive créée par défaut:

`docker-images-cache/cloud_web_stack.tar`

## 2) Recharger les images depuis le `.tar`

```bash
./scripts/import-docker-images.sh
```

PowerShell:

```powershell
./scripts/import-docker-images.ps1
```

## 3) Lancer le projet en mode offline (sans pull)

```bash
./scripts/compose-up-offline.sh
```

PowerShell:

```powershell
./scripts/compose-up-offline.ps1
```

Le script recharge les images puis exécute:

`docker compose up -d --pull never`

## Variante manuelle

Vous pouvez aussi lancer directement:

```bash
docker load -i docker-images-cache/cloud_web_stack.tar
docker compose up -d --pull never
```
