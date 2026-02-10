param(
    [string]$ArchiveDir = "$PSScriptRoot/../docker-images-cache"
)

$ErrorActionPreference = "Stop"

$RootDir = (Resolve-Path "$PSScriptRoot/..").Path
$ArchiveDirResolved = (Resolve-Path -LiteralPath $ArchiveDir -ErrorAction SilentlyContinue)
if (-not $ArchiveDirResolved) {
    New-Item -ItemType Directory -Path $ArchiveDir -Force | Out-Null
    $ArchiveDirResolved = (Resolve-Path -LiteralPath $ArchiveDir).Path
}
$ArchivePath = Join-Path $ArchiveDirResolved "cloud_web_stack.tar"

$images = docker compose -f (Join-Path $RootDir "docker-compose.yml") config --images | Sort-Object -Unique
if (-not $images -or $images.Count -eq 0) {
    throw "Aucune image detectee dans docker-compose.yml"
}

Write-Host "Images detectees:"
$images | ForEach-Object { Write-Host " - $_" }

Write-Host "Pull des images..."
$images | ForEach-Object { docker pull $_ | Out-Null }

Write-Host "Creation de l'archive: $ArchivePath"
docker save -o $ArchivePath $images

Write-Host "Archive creee avec succes."
