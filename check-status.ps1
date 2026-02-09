Write-Host "`n=== ETAT DES SERVICES CLOUD WEB ===" -ForegroundColor Cyan
Write-Host ""

$services = @("cloudweb-postgres", "cloudweb-backend", "cloudweb-frontend", "osm_import", "osm_server")

foreach ($service in $services) {
    $state = docker inspect $service --format='{{.State.Status}}|{{.State.ExitCode}}' 2>$null
    if ($state) {
        $parts = $state -split '\|'
        $status = $parts[0]
        $exitCode = if ($parts.Count -gt 1) { [int]$parts[1] } else { -1 }

        if ($service -eq "osm_import" -and $status -eq "exited" -and $exitCode -eq 0) {
            $status = "completed"
        }

        $color = if ($status -eq "running" -or $status -eq "completed") { "Green" } elseif ($status -eq "exited") { "Red" } else { "Yellow" }
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
