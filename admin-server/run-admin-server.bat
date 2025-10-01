@echo off
echo Starting Standalone Admin Server...
echo Admin API will be available at: http://localhost:9090/admin
echo ===============================

REM Check if JAR exists
if not exist "StandaloneAdminServer.jar" (
    echo ❌ StandaloneAdminServer.jar not found!
    echo Please run build-admin.bat first
    pause
    exit /b 1
)

REM Run with correct classpath
java -cp "..\lib\*;StandaloneAdminServer.jar" admin.server.StandaloneAdminServer
pause
