@echo off
echo === Building Standalone Admin Server ===

REM Create build directories
if not exist build\classes mkdir build\classes
if not exist build\lib mkdir build\lib

REM Download Gson dependency if not exists
if not exist build\lib\gson.jar (
    echo Downloading Gson library...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar' -OutFile 'build\lib\gson.jar'"
    if %ERRORLEVEL% neq 0 (
        echo ❌ Failed to download Gson library!
        pause
        exit /b 1
    )
)

REM Compile Java source
echo Compiling Java source...
javac -cp "build\lib\*" -d build\classes StandaloneAdminServer.java
if %ERRORLEVEL% neq 0 (
    echo ❌ Compilation failed!
    pause
    exit /b 1
)
echo ✅ Compilation successful!

REM Create JAR file
echo Creating JAR file...
cd build\classes
jar -cf ..\standalone-admin-server.jar admin\
cd ..\..

echo ✅ Build completed!

echo.
echo Admin API will be available at: http://localhost:9090/admin
echo.

set /p choice="Do you want to start the Admin Server now? (Y/N): "
if /i "%choice%"=="Y" (
    echo.
    echo Starting Admin Server...
    echo.
    java -cp "build\lib\gson.jar;build\standalone-admin-server.jar" admin.server.StandaloneAdminServer
) else (
    echo.
    echo You can run the server later with: build-admin.bat
    pause
)
