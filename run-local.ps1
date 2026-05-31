$ErrorActionPreference = 'Stop'

$env:JAVA_HOME = 'C:\Users\User\.jdks\openjdk-25.0.1'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

Get-Content .env | ForEach-Object {
    if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
        $name = $matches[1]
        $value = $matches[2].Trim('"')
        Set-Item -Path "Env:$name" -Value $value
    }
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker is not installed or is not available in PATH. Local mail/mysql/elasticsearch services need Docker.'
}

docker info | Out-Null
docker compose -f "$PSScriptRoot\compose.localmysql.yaml" up -d

.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=localmysql"
