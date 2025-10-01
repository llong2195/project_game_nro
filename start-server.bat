@echo off
REM NRO Server Auto-Restart Script for Windows
REM Sử dụng script này để server có thể tự động restart từ web admin

echo === NRO Server Auto-Restart Script (Windows) ===
echo Server sẽ tự động restart khi bị tắt từ admin web
echo Để dừng hoàn toàn, nhấn Ctrl+C
echo ================================================

:restart_loop
echo [%date% %time%] Starting NRO Server...

REM Chạy server
call gradlew.bat run

REM Lấy exit code
set EXIT_CODE=%ERRORLEVEL%

echo [%date% %time%] Server stopped with exit code: %EXIT_CODE%

REM Nếu exit code = 0 (restart từ admin), thì restart
if %EXIT_CODE% == 0 (
    echo [%date% %time%] Server restart requested from admin. Restarting in 3 seconds...
    timeout /t 3 /nobreak >nul
    goto restart_loop
) else (
    REM Nếu exit code khác 0 (lỗi), hỏi có restart không
    echo [%date% %time%] Server stopped with error. Restart? (y/n)
    choice /t 10 /c yn /d n /m "Auto restart in 10 seconds or press y/n"
    if errorlevel 2 (
        echo [%date% %time%] Auto-restart cancelled by user
        goto end
    )
    echo [%date% %time%] Restarting server in 5 seconds...
    timeout /t 5 /nobreak >nul
    goto restart_loop
)

:end
echo [%date% %time%] Server shutdown completed
pause
