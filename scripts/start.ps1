$ErrorActionPreference = 'Stop'
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location $ProjectRoot

if (-not (Test-Path -LiteralPath '.env')) {
  Copy-Item -LiteralPath '.env.example' -Destination '.env'
  Write-Host 'Created .env from .env.example. Review its local MySQL settings before continuing.'
}

Get-Content -LiteralPath '.env' | ForEach-Object {
  if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
    [Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2], 'Process')
  }
}
if ([string]::IsNullOrWhiteSpace($env:MYSQL_CLIENT)) { $env:MYSQL_CLIENT = 'mysql' }

if ($env:MYSQL_DATABASE -ne 'threattrace') {
  throw 'The bundled schema and seed scripts target the threattrace database. Set MYSQL_DATABASE=threattrace in .env.'
}

if ($env:JAVA_HOME) {
  $javaExecutable = Join-Path $env:JAVA_HOME 'bin\java.exe'
  if (-not (Test-Path -LiteralPath $javaExecutable)) {
    throw "JAVA_HOME does not point to a JDK with bin\java.exe: $env:JAVA_HOME"
  }
} else {
  $javaCommand = Get-Command 'java' -ErrorAction SilentlyContinue
  if (-not $javaCommand) {
    throw 'Java 17+ is required. Install a JDK and ensure java.exe is on PATH (or JAVA_HOME is configured). Maven itself is provided by the project wrapper.'
  }
  $javaExecutable = $javaCommand.Source
}
$javaStdoutFile = New-TemporaryFile
$javaStderrFile = New-TemporaryFile
try {
  $javaVersionProcess = Start-Process -FilePath $javaExecutable -ArgumentList '-version' `
    -RedirectStandardOutput $javaStdoutFile.FullName -RedirectStandardError $javaStderrFile.FullName `
    -NoNewWindow -Wait -PassThru
  $javaVersionOutput = (Get-Content -LiteralPath $javaStdoutFile.FullName -Raw) +
    (Get-Content -LiteralPath $javaStderrFile.FullName -Raw)
} finally {
  Remove-Item -LiteralPath $javaStdoutFile.FullName, $javaStderrFile.FullName -Force -ErrorAction SilentlyContinue
}
if ($javaVersionProcess.ExitCode -ne 0) {
  throw "java.exe -version failed with exit code $($javaVersionProcess.ExitCode): $javaVersionOutput"
}
if ($javaVersionOutput -notmatch 'version "(?<major>\d+)') {
  throw "Could not determine the Java version. Output from java -version: $javaVersionOutput"
}
if ([int]$Matches['major'] -lt 17) {
  throw "Java 17+ is required, but java -version reported major version $($Matches['major']). Install or select a newer JDK."
}
if (-not (Test-Path -LiteralPath 'mvnw.cmd')) {
  throw 'The Maven Wrapper script mvnw.cmd is missing from the project root.'
}

$mysqlCommand = Get-Command $env:MYSQL_CLIENT -ErrorAction SilentlyContinue
if (-not $mysqlCommand -and (Test-Path -LiteralPath $env:MYSQL_CLIENT)) {
  $mysqlPath = (Resolve-Path -LiteralPath $env:MYSQL_CLIENT).Path
} elseif ($mysqlCommand) {
  $mysqlPath = $mysqlCommand.Source
} else {
  throw "Could not find MySQL client '$($env:MYSQL_CLIENT)'. Add mysql.exe to PATH or set MYSQL_CLIENT to its full path in .env."
}

if (-not (Get-Command 'python' -ErrorAction SilentlyContinue)) {
  throw 'Python 3.10+ is required. Install Python and ensure python is on PATH.'
}

$env:MYSQL_PWD = $env:MYSQL_PASSWORD
try {
  $mysqlArgs = @(
    '--protocol=TCP',
    "--host=$env:MYSQL_HOST",
    "--port=$env:MYSQL_PORT",
    "--user=$env:MYSQL_USER",
    "--database=$env:MYSQL_DATABASE",
    '--connect-timeout=5'
  )

  Write-Host "Checking local MySQL at $($env:MYSQL_HOST):$($env:MYSQL_PORT)/$($env:MYSQL_DATABASE)..."
  'SELECT VERSION();' | & $mysqlPath @mysqlArgs
  if ($LASTEXITCODE -ne 0) {
    throw 'Could not connect to local MySQL. Start the MySQL Server Windows service and check MYSQL_HOST, MYSQL_PORT, MYSQL_USER, and MYSQL_PASSWORD in .env.'
  }

  foreach ($sqlFile in @('database/schema.sql', 'database/seed.sql')) {
    Write-Host "Applying $sqlFile (safe to rerun)..."
    Get-Content -LiteralPath $sqlFile -Raw | & $mysqlPath @mysqlArgs
    if ($LASTEXITCODE -ne 0) {
      throw "MySQL returned an error while applying $sqlFile. Confirm the account can create tables, views, triggers, and stored procedures in the threattrace database."
    }
  }
} finally {
  Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
}

Write-Host 'Starting local Python analytics service...'
$analytics = Start-Process -FilePath 'python' -ArgumentList 'analytics/service.py' -WorkingDirectory $ProjectRoot -PassThru -WindowStyle Hidden
try {
  .\mvnw.cmd spring-boot:run
} finally {
  Stop-Process -Id $analytics.Id -ErrorAction SilentlyContinue
}
