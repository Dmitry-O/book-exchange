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

$env:RUNTIME_ENV = 'test'
$env:APP_DEMO_EMAIL_SANDBOX_ENABLED = 'false'

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker is not installed or is not available in PATH. Testcontainers needs Docker to run integration tests.'
}

docker info | Out-Null

& .\mvnw.cmd -q clean verify
$mavenExitCode = $LASTEXITCODE

$surefireReportFiles = Get-ChildItem 'target\surefire-reports\TEST-*.xml' -ErrorAction SilentlyContinue
$failsafeReportFiles = Get-ChildItem 'target\failsafe-reports\TEST-*.xml' -ErrorAction SilentlyContinue

if ($surefireReportFiles -or $failsafeReportFiles) {
    $surefireReports = @($surefireReportFiles |
        Select-Xml -XPath '/testsuite' |
        ForEach-Object { $_.Node })

    $failsafeReports = @($failsafeReportFiles |
        Select-Xml -XPath '/testsuite' |
        ForEach-Object { $_.Node })

    $surefireTests = ($surefireReports | Measure-Object tests -Sum).Sum
    $surefireFailures = ($surefireReports | Measure-Object failures -Sum).Sum
    $surefireErrors = ($surefireReports | Measure-Object errors -Sum).Sum
    $surefireSkipped = ($surefireReports | Measure-Object skipped -Sum).Sum

    $failsafeTests = ($failsafeReports | Measure-Object tests -Sum).Sum
    $failsafeFailures = ($failsafeReports | Measure-Object failures -Sum).Sum
    $failsafeErrors = ($failsafeReports | Measure-Object errors -Sum).Sum
    $failsafeSkipped = ($failsafeReports | Measure-Object skipped -Sum).Sum

    $summary = [pscustomobject]@{
        UnitTests           = $surefireTests
        IntegrationTests    = $failsafeTests
        TotalTests          = ($surefireTests + $failsafeTests)
        TotalFailures       = ($surefireFailures + $failsafeFailures)
        TotalErrors         = ($surefireErrors + $failsafeErrors)
        TotalSkipped        = ($surefireSkipped + $failsafeSkipped)
    }

    Write-Host ''
    Write-Host 'Test summary:'
    $summary | Format-List | Out-Host
}

exit $mavenExitCode
