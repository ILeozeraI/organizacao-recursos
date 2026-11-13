@REM Maven Wrapper simplificado para Windows (PowerShell).
@REM Baixa a versao fixa do Maven definida em .mvn\wrapper\maven-wrapper.properties
@echo off
setlocal
set "BASEDIR=%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$p = Get-Content '%BASEDIR%.mvn\wrapper\maven-wrapper.properties' | Where-Object { $_ -like 'distributionUrl=*' };" ^
  "$url = $p.Substring(16); $name = [IO.Path]::GetFileName($url) -replace '-bin.zip','';" ^
  "$dest = Join-Path $env:USERPROFILE ('.m2\wrapper\dists\' + $name);" ^
  "if (-not (Test-Path (Join-Path $dest 'bin\mvn.cmd'))) {" ^
  "  Write-Host ('[mvnw] Baixando ' + $name + ' ...');" ^
  "  $tmp = $dest + '.tmp'; New-Item -ItemType Directory -Force -Path $tmp | Out-Null;" ^
  "  Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile (Join-Path $tmp 'maven.zip');" ^
  "  Expand-Archive -Force (Join-Path $tmp 'maven.zip') $tmp;" ^
  "  if (Test-Path $dest) { Remove-Item -Recurse -Force $dest };" ^
  "  Move-Item (Join-Path $tmp $name) $dest; Remove-Item -Recurse -Force $tmp }" ^
  "; Set-Content -Path (Join-Path $env:TEMP 'mvnw-home.txt') -Value $dest"
if errorlevel 1 exit /b 1
set /p MVN_HOME=<"%TEMP%\mvnw-home.txt"
call "%MVN_HOME%\bin\mvn.cmd" -f "%BASEDIR%pom.xml" %*
