$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    $javac = $null
    $jdkLocal = Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot '..\.tools') -Directory -Filter 'jdk*' -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($jdkLocal) { $javac = Join-Path $jdkLocal.FullName 'bin\javac.exe' }
    if (-not $javac) { $javac = Get-Command javac -ErrorAction SilentlyContinue }
    if (-not $javac) { throw 'Instale um JDK 17 ou superior e adicione a pasta bin ao PATH.' }
    New-Item -ItemType Directory -Force -Path out | Out-Null
    $fontes = Get-ChildItem -LiteralPath src -Filter '*.java' | ForEach-Object { $_.FullName }
    & $javac -encoding UTF-8 --release 17 -d out $fontes
    if ($LASTEXITCODE -ne 0) { throw 'Falha na compilacao.' }
    Write-Host 'Compilacao concluida. Classes na pasta out.'
} finally { Pop-Location }
