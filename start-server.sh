#!/bin/bash

# NRO Server Auto-Restart Script
# Sử dụng script này để server có thể tự động restart từ web admin

echo "=== NRO Server Auto-Restart Script ==="
echo "Server sẽ tự động restart khi bị tắt từ admin web"
echo "Để dừng hoàn toàn, nhấn Ctrl+C"
echo "=========================================="

# Infinite loop để auto-restart
while true; do
    echo "[$(date)] Starting NRO Server..."
    
    # Chạy server
    ./gradlew run
    
    # Lấy exit code
    EXIT_CODE=$?
    
    echo "[$(date)] Server stopped with exit code: $EXIT_CODE"
    
    # Nếu exit code = 0 (restart từ admin), thì restart
    if [ $EXIT_CODE -eq 0 ]; then
        echo "[$(date)] Server restart requested from admin. Restarting in 3 seconds..."
        sleep 3
        # Kill any remaining processes on ports to avoid conflicts
        pkill -f "gradle.*run" 2>/dev/null || true
        sleep 2
    else
        # Nếu exit code khác 0 (lỗi), hỏi có restart không
        echo "[$(date)] Server stopped with error. Restart? (y/n)"
        read -t 10 -n 1 REPLY
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            echo "[$(date)] Auto-restart cancelled by user"
            break
        fi
        echo "[$(date)] Restarting server in 5 seconds..."
        sleep 5
    fi
done

echo "[$(date)] Server shutdown completed"
