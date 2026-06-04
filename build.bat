@echo off
setlocal

set "OUT=divera247-poller.jar"
set "JAR_EXE="

for %%J in (jar.exe) do set "JAR_EXE=%%~$PATH:J"

if not defined JAR_EXE if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jar.exe" set "JAR_EXE=%JAVA_HOME%\bin\jar.exe"
if not defined JAR_EXE if exist "C:\Program Files\Android\Android Studio\jbr\bin\jar.exe" set "JAR_EXE=C:\Program Files\Android\Android Studio\jbr\bin\jar.exe"
if not defined JAR_EXE if exist "C:\Program Files\Java\jdk-11\bin\jar.exe" set "JAR_EXE=C:\Program Files\Java\jdk-11\bin\jar.exe"
if not defined JAR_EXE if exist "C:\Program Files\Java\jdk-17\bin\jar.exe" set "JAR_EXE=C:\Program Files\Java\jdk-17\bin\jar.exe"

if not defined JAR_EXE (
  echo Fehler: jar.exe wurde nicht gefunden.
  echo Bitte installiere ein JDK oder setze JAVA_HOME.
  exit /b 1
)

if exist "%OUT%" del "%OUT%"

"%JAR_EXE%" cfm "%OUT%" META-INF\MANIFEST.MF ^
  -C . com ^
  -C . de ^
  -C . javazoom ^
  -C . junit ^
  -C . org ^
  -C . META-INF ^
  -C . Log4j-charsets.properties ^
  -C . Log4j-events.dtd ^
  -C . Log4j-events.xsd ^
  -C . Log4j-levels.xsd ^
  -C . log4j2.xml

if errorlevel 1 (
  echo Fehler: Build fehlgeschlagen.
  exit /b 1
)

echo Fertig: %OUT% wurde erstellt.
