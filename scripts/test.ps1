$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$buildDir = Join-Path $root 'build'
$mainOut = Join-Path $buildDir 'main'
$testOut = Join-Path $buildDir 'test'

New-Item -ItemType Directory -Force -Path $mainOut | Out-Null
New-Item -ItemType Directory -Force -Path $testOut | Out-Null

$mainSources = Get-ChildItem -Path (Join-Path $root 'src') -Filter *.java | ForEach-Object { $_.FullName }
$testSources = Get-ChildItem -Path (Join-Path $root 'test') -Filter *.java | ForEach-Object { $_.FullName }

javac -d $mainOut $mainSources
javac -cp $mainOut -d $testOut $testSources
java -cp "$mainOut;$testOut" ProjectTestRunner
