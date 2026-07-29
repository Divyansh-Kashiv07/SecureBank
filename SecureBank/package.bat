@echo off
REM ============================================================
REM  SecureBank — Windows Executable Packaging Script
REM  Uses jpackage (JDK 14+) to create a self-contained EXE
REM ============================================================

REM PREREQUISITES:
REM   1. JDK 17+ installed (jpackage is included)
REM   2. WiX Toolset v3.0+ installed (for .exe installer)
REM      Download from: https://wixtoolset.org/releases/
REM   3. Fat JAR already built: mvn package
REM      The JAR should be at target\securebank-1.0-SNAPSHOT.jar

REM --name        : Application name (shown in Start Menu, Desktop shortcut)
REM --input       : Directory containing the fat JAR and any resources
REM --main-jar    : The specific JAR file to run
REM --main-class  : Fully qualified main class (entry point)
REM --dest        : Where to save the output installer
REM --type        : exe = Windows installer, msi = MSI installer, app-image = portable folder
REM --win-shortcut : Creates a desktop shortcut
REM --win-menu     : Adds an entry to the Start Menu
REM --win-dir-chooser : Lets user pick install directory
REM --icon         : Application icon (must be .ico format for Windows)
REM --app-version  : Version number for the installer
REM --description  : Description shown in Add/Remove Programs
REM --vendor       : Your name/org shown in Add/Remove Programs

jpackage ^
  --name "SecureBank" ^
  --input "target" ^
  --main-jar "securebank-1.0-SNAPSHOT.jar" ^
  --main-class "com.securebank.main.Main" ^
  --dest "dist" ^
  --type exe ^
  --win-shortcut ^
  --win-menu ^
  --win-dir-chooser ^
  --app-version "1.0.0" ^
  --description "SecureBank Cooperative Banking Management System" ^
  --vendor "NIET SecureBank Team" ^
  --java-options "--enable-native-access=ALL-UNNAMED"

echo.
echo ============================================================
echo  Build complete! Check the 'dist' folder for SecureBank.exe
echo ============================================================
pause
