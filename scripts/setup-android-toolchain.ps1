# Installs a user-level Android build toolchain (no admin rights needed):
#   JDK 21 (Microsoft Build of OpenJDK), Android command-line tools, SDK 35, build-tools, platform-tools, Gradle.
# Running it accepts the Android SDK license (https://developer.android.com/studio/terms).
# Default location: %USERPROFILE%\tools. Override with -ToolsDir.
param(
    [string]$ToolsDir = (Join-Path $env:USERPROFILE "tools"),
    [string]$GradleVersion = "8.11.1",
    [string]$CmdlineToolsBuild = "13114758"
)
$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$downloads = Join-Path $ToolsDir "_downloads"
New-Item -ItemType Directory -Force $downloads | Out-Null

function Get-Zip([string]$url, [string]$name) {
    $zip = Join-Path $downloads $name
    if (-not (Test-Path $zip)) {
        Write-Host "Downloading $url"
        Invoke-WebRequest -Uri $url -OutFile $zip
    }
    return $zip
}

# JDK 21
$jdkHome = Join-Path $ToolsDir "jdk-21"
if (-not (Test-Path (Join-Path $jdkHome "bin\java.exe"))) {
    $zip = Get-Zip "https://aka.ms/download-jdk/microsoft-jdk-21-windows-x64.zip" "jdk21.zip"
    $tmp = Join-Path $downloads "jdk-extract"
    Remove-Item -Recurse -Force $tmp -ErrorAction SilentlyContinue
    Expand-Archive $zip $tmp
    Move-Item (Get-ChildItem $tmp -Directory | Select-Object -First 1).FullName $jdkHome
    Remove-Item -Recurse -Force $tmp
}

# Android command-line tools (sdkmanager expects them under cmdline-tools\latest)
$sdkRoot = Join-Path $ToolsDir "android-sdk"
$cmdlineLatest = Join-Path $sdkRoot "cmdline-tools\latest"
if (-not (Test-Path (Join-Path $cmdlineLatest "bin\sdkmanager.bat"))) {
    $zip = Get-Zip "https://dl.google.com/android/repository/commandlinetools-win-${CmdlineToolsBuild}_latest.zip" "cmdline-tools.zip"
    $tmp = Join-Path $downloads "cmdline-extract"
    Remove-Item -Recurse -Force $tmp -ErrorAction SilentlyContinue
    Expand-Archive $zip $tmp
    New-Item -ItemType Directory -Force (Split-Path $cmdlineLatest) | Out-Null
    Move-Item (Join-Path $tmp "cmdline-tools") $cmdlineLatest
    Remove-Item -Recurse -Force $tmp
}

# Gradle (only used once to generate the project's Gradle wrapper)
$gradleHome = Join-Path $ToolsDir "gradle-$GradleVersion"
if (-not (Test-Path (Join-Path $gradleHome "bin\gradle.bat"))) {
    $zip = Get-Zip "https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip" "gradle.zip"
    Expand-Archive $zip $ToolsDir
}

# SDK packages
$env:JAVA_HOME = $jdkHome
$env:ANDROID_HOME = $sdkRoot
$sdkmanager = Join-Path $cmdlineLatest "bin\sdkmanager.bat"
# PowerShell pipes do not reach the .bat's stdin reliably, so feed the answers through cmd redirection.
$yes = Join-Path $downloads "yes.txt"
Set-Content -Path $yes -Value (@("y") * 30) -Encoding ascii
cmd /c "`"$sdkmanager`" --sdk_root=`"$sdkRoot`" --licenses < `"$yes`"" | Out-Null
& $sdkmanager --sdk_root=$sdkRoot "platform-tools" "platforms;android-35" "build-tools;35.0.0"
# sdkmanager exits 0 even when it skips packages, so check for the files instead.
foreach ($p in @("platform-tools\adb.exe", "platforms\android-35\android.jar", "build-tools\35.0.0\aapt2.exe")) {
    if (-not (Test-Path (Join-Path $sdkRoot $p))) { throw "Android SDK package missing: $p" }
}

# Persist for new shells (user scope only)
[Environment]::SetEnvironmentVariable("JAVA_HOME", $jdkHome, "User")
[Environment]::SetEnvironmentVariable("ANDROID_HOME", $sdkRoot, "User")

Write-Host ""
Write-Host "JAVA_HOME    = $jdkHome"
Write-Host "ANDROID_HOME = $sdkRoot"
Write-Host "Gradle       = $gradleHome"
