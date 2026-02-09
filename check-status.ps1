Write-Host "`n=== ETAT DES SERVICES CLOUD WEB ===" -ForegroundColor Cyan
Write-Host ""

$services = @("cloudweb-postgres", "cloudweb-backend", "cloudweb-frontend", "osm_import", "osm_server")

foreach ($service in $services) {
    $status = docker inspect $service --format='{{.State.Status}}' 2>$null
    if ($status) {
        $color = if ($status -eq "running") { "Green" } elseif ($status -eq "exited") { "Red" } else { "Yellow" }
        Write-Host "  $service : " -NoNewline
        Write-Host $status -ForegroundColor $color
    }
}

Write-Host "`n=== PORTS ===" -ForegroundColor Cyan
Write-Host "  Frontend : http://localhost:5173"
Write-Host "  Backend  : http://localhost:8080"
Write-Host "  Database : localhost:5433"
Write-Host "  OSM      : http://localhost:8090"
Write-Host ""
