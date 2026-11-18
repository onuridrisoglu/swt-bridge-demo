@echo off
rem Builds everything and runs the Vaadin app with WarehousePro bridged into it
rem on http://localhost:8890. Every user's SWT application runs in this JVM,
rem on a hidden desktop of its own.
rem Extra JVM options can be passed as arguments, e.g.
rem   start-vaadin.bat -Dswingbridge.swt.debugInput=true
rem Can be double-clicked, also on a shared folder: the window stays open when
rem the app stops.
setlocal
rem pushd, not cd: it also works when this folder is a UNC path.
pushd "%~dp0"

rem One H2 database for the host and every user's SWT application, on the
rem local disk: H2's file locking does not work on a shared folder.
set DATA=%LOCALAPPDATA%\warehouse-demo-swt
if not exist "%DATA%" mkdir "%DATA%"
set DB=%DATA%\warehousedb

call mvnw.cmd -B -ntp install -DskipTests || goto end

cd vaadin-app\target
java "-Dwarehouse.db=%DB%" %* -jar app.jar

:end
popd
pause
