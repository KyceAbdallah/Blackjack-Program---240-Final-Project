$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$buildDir = Join-Path $root 'build'
$mainOut = Join-Path $buildDir 'main'

New-Item -ItemType Directory -Force -Path $mainOut | Out-Null

$sources = Get-ChildItem -Path (Join-Path $root 'src') -Filter *.java | ForEach-Object { $_.FullName }
javac -d $mainOut $sources

Write-Host "Compiled application classes to $mainOut"
