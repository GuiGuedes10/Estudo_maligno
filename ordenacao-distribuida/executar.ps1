# Exemplos: .\executar.ps1 Trabalhador 3001 1
#           .\executar.ps1 Coordenador "9,3,7,1" localhost:3001
$ErrorActionPreference = 'Stop'
$argumentosJava = $args
Push-Location $PSScriptRoot
try {
    $java = $null
    $jdkLocal = Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot '..\.tools') -Directory -Filter 'jdk*' -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($jdkLocal) { $java = Join-Path $jdkLocal.FullName 'bin\java.exe' }
    if (-not $java) { $java = Get-Command java -ErrorAction SilentlyContinue }
    if (-not $java) { throw 'Instale um JDK 17 ou superior.' }
    if (-not (Test-Path -LiteralPath 'out\Coordenador.class')) { throw 'Execute .\compilar.ps1 primeiro.' }
    if ($argumentosJava.Count -eq 0) { $argumentosJava = @('Coordenador') }
    & $java -cp out @argumentosJava
    if ($LASTEXITCODE -ne 0) { throw "Programa encerrado com codigo $LASTEXITCODE." }
} finally { Pop-Location }
