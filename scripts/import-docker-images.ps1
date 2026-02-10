param(
    [string]$ArchivePath = "$PSScriptRoot/../docker-images-cache/cloud_web_stack.tar"
)

$ErrorActionPreference = "Stop"

$ResolvedArchivePath = (Resolve-Path -LiteralPath $ArchivePath -ErrorAction SilentlyContinue)
if (-not $ResolvedArchivePath) {
    throw "Archive introuvable: $ArchivePath`nGenerez-la d'abord avec: ./scripts/export-docker-images.ps1"
}

Write-Host "Chargement des images depuis: $($ResolvedArchivePath.Path)"
docker load -i $ResolvedArchivePath.Path

Write-Host "Chargement termine."
