# Builds the KFUPM Sorter (Windows and Mac edition) installer on this PC. Nothing is uploaded.
#   1. uses a JDK 21+ if one is installed, otherwise downloads Eclipse Temurin 21 (adoptium.net) once
#   2. compiles the app and runs the self-test
#   3. jpackage makes "KFUPM Sorter Desktop.exe" with Java built in
#   4. Inno Setup wraps it into build\installer\KFUPM-Sorter-Windows-Setup.exe
# Started by "Build Windows installer.bat". Writes build\build-log.txt.

$ErrorActionPreference = 'Continue'   # native tools report through exit codes
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
New-Item -ItemType Directory -Force build | Out-Null
$log = Join-Path $root 'build\build-log.txt'
Start-Transcript -Path $log -Force | Out-Null

function Step($t) { Write-Host ''; Write-Host "== $t" -ForegroundColor Cyan }
function Fail($t) { Write-Host ''; Write-Host "FAILED: $t" -ForegroundColor Red; Stop-Transcript | Out-Null; exit 1 }

try {
    # ---------------------------------------------------------------- JDK
    Step 'Looking for Java (JDK 21 or newer)'
    $jdk = $null
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
    $cache = Join-Path $env:LOCALAPPDATA 'KFUPM Sorter Build\jdk'
    if (Test-Path $cache) { $candidates += (Get-ChildItem $cache -Directory | ForEach-Object { $_.FullName }) }
    foreach ($base in @("$env:ProgramFiles\Eclipse Adoptium", "$env:ProgramFiles\Java", "$env:ProgramFiles\Microsoft")) {
        if (Test-Path $base) { $candidates += (Get-ChildItem $base -Directory | ForEach-Object { $_.FullName }) }
    }
    foreach ($c in $candidates) {
        $jp = Join-Path $c 'bin\jpackage.exe'
        $rel = Join-Path $c 'release'
        if ((Test-Path $jp) -and (Test-Path $rel)) {
            $v = (Select-String -Path $rel -Pattern 'JAVA_VERSION="(\d+)' | Select-Object -First 1).Matches[0].Groups[1].Value
            if ([int]$v -ge 21) { $jdk = $c; break }
        }
    }
    if (-not $jdk) {
        Write-Host 'No JDK 21 found. Downloading Eclipse Temurin 21 from adoptium.net (about 190 MB, one time only)...'
        New-Item -ItemType Directory -Force $cache | Out-Null
        $zip = Join-Path $env:TEMP 'temurin21-jdk.zip'
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        $ProgressPreference = 'SilentlyContinue'
        Invoke-WebRequest -UseBasicParsing -Uri 'https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse' -OutFile $zip -ErrorAction Stop
        Expand-Archive -Path $zip -DestinationPath $cache -Force -ErrorAction Stop
        Remove-Item $zip -Force
        $jdk = (Get-ChildItem $cache -Directory | Where-Object { Test-Path (Join-Path $_.FullName 'bin\jpackage.exe') } | Select-Object -First 1).FullName
        if (-not $jdk) { Fail 'The JDK download did not contain jpackage.exe.' }
    }
    Write-Host "Using JDK: $jdk"
    $bin = Join-Path $jdk 'bin'
    $version = ((Get-Content build.sh | Select-String '^VERSION=(.+)$').Matches[0].Groups[1].Value).Trim()

    # ---------------------------------------------------------------- compile + self-test
    Step 'Compiling'
    foreach ($d in 'build\classes', 'build\test', 'build\jar', 'build\image', 'build\installer') {
        if (Test-Path $d) { Remove-Item -Recurse -Force $d }
        New-Item -ItemType Directory -Force $d | Out-Null
    }
    $src = Get-ChildItem -Recurse src\main\java -Filter *.java | ForEach-Object { '"' + $_.FullName.Replace('\','/') + '"' }
    Set-Content -Path build\sources.txt -Value $src -Encoding ASCII
    & "$bin\javac.exe" -encoding UTF-8 -d build\classes '@build\sources.txt'
    if ($LASTEXITCODE) { Fail 'javac' }
    Copy-Item -Recurse src\main\resources\* build\classes\ -ErrorAction Stop

    Step 'Self-test'
    $tsrc = Get-ChildItem -Recurse src\test\java -Filter *.java | ForEach-Object { '"' + $_.FullName.Replace('\','/') + '"' }
    Set-Content -Path build\tests.txt -Value $tsrc -Encoding ASCII
    & "$bin\javac.exe" -encoding UTF-8 -cp build\classes -d build\test '@build\tests.txt'
    if ($LASTEXITCODE) { Fail 'javac (tests)' }
    $tdata = Join-Path $env:TEMP ('kfupm-selftest-' + [guid]::NewGuid())
    & "$bin\java.exe" '-Djava.awt.headless=true' "-Dkfupmsorter.data=$tdata" -cp 'build\classes;build\test' io.github.kal429.kfupmsorter.SelfTest
    $testExit = $LASTEXITCODE
    Remove-Item -Recurse -Force $tdata -ErrorAction SilentlyContinue
    if ($testExit) { Fail "self-test: $testExit check(s) failed" }

    & "$bin\jar.exe" --create --file build\jar\kfupm-sorter-desktop.jar --main-class io.github.kal429.kfupmsorter.Main -C build\classes .
    if ($LASTEXITCODE) { Fail 'jar' }

    # ---------------------------------------------------------------- jpackage
    Step 'Packaging KFUPM Sorter Desktop.exe with Java built in'
    & "$bin\jpackage.exe" --type app-image --name 'KFUPM Sorter Desktop' --input build\jar --main-jar kfupm-sorter-desktop.jar `
        --app-version $version --vendor 'KFUPM Sorter (student project)' --icon packaging\app.ico `
        --description 'Keeps your Downloads folder sorted by KFUPM course' `
        --add-modules java.base,java.desktop,java.net.http `
        --jlink-options '--strip-debug --no-header-files --no-man-pages --compress=zip-6' `
        --dest build\image
    if ($LASTEXITCODE) { Fail 'jpackage' }
    $exe = 'build\image\KFUPM Sorter Desktop\KFUPM Sorter Desktop.exe'
    $p = Start-Process -FilePath $exe -ArgumentList '--preview' -Wait -PassThru -WindowStyle Hidden
    Write-Host "Quick check of the packaged app: exit code $($p.ExitCode)"
    if ($p.ExitCode -ne 0) { Fail 'the packaged app did not start' }

    # ---------------------------------------------------------------- Inno Setup
    Step 'Building the installer (Inno Setup)'
    $iscc = @("$env:LOCALAPPDATA\Programs\Inno Setup 6\ISCC.exe", "${env:ProgramFiles(x86)}\Inno Setup 6\ISCC.exe", "$env:ProgramFiles\Inno Setup 6\ISCC.exe") |
            Where-Object { Test-Path $_ } | Select-Object -First 1
    if (-not $iscc) { Fail 'Inno Setup 6 is not installed (winget install JRSoftware.InnoSetup), then run this again.' }
    & $iscc /Qp packaging\windows.iss
    if ($LASTEXITCODE) { Fail 'Inno Setup' }

    $out = Resolve-Path 'build\installer\KFUPM-Sorter-Windows-Setup.exe'
    Step "Done: $out"
    Stop-Transcript | Out-Null
    Start-Process explorer.exe -ArgumentList "/select,`"$out`""
    exit 0
}
catch {
    Fail $_.Exception.Message
}
