@echo off
echo Starting Standalone Admin Server...
java -cp "build\lib\gson.jar;build\standalone-admin-server.jar" admin.server.StandaloneAdminServer
pause
