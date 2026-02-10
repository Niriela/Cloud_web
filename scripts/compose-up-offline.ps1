param(
    [string]$ArchivePath = "$PSScriptRoot/../docker-images-cache/cloud_web_stack.tar"
)

$ErrorActionPreference = "Stop"

& "$PSScriptRoot/import-docker-images.ps1" -ArchivePath $ArchivePath

$RootDir = (Resolve-Path "$PSScriptRoot/..").Path
Push-Location $RootDir
try {
    docker compose up -d --pull never
} finally {
    Pop-Location
}
