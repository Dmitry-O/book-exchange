$ErrorActionPreference = 'Stop'

$composeFile = Join-Path $PSScriptRoot 'docker-compose.local.yml'

docker compose -f $composeFile up -d --force-recreate backend

if ($LASTEXITCODE -ne 0) {
    throw 'Backend container recreation failed.'
}

Write-Host 'Backend configuration reapplied. The frontend will refresh metadata automatically.'
