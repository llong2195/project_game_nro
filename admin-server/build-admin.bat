@echo off
echo === Building Standalone Admin Server ===

REM Compile Java source
echo Compiling Java source...
javac -cp "..\lib\*" -d build StandaloneAdminServer.java
if %ERRORLEVEL% neq 0 (
    echo ❌ Compilation failed!
    pause
    exit /b 1
)
echo ✅ Compilation successful!

REM Create JAR file
echo Creating JAR file...
if not exist build mkdir build
cd build
jar -cf ..\StandaloneAdminServer.jar admin\server\*.class
cd ..
echo ✅ Build completed!

echo.
echo To run the admin server:
echo   Windows:   run-admin-server.bat
echo   Linux/Mac: ./run-admin-server.sh
echo.
echo Admin API will be available at: http://localhost:9090/admin
pause
