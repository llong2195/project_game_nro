# NRO Admin API Documentation

**API để quản lý NRO Game Server từ Next.js**

## 🎯 API Endpoint

**Base URL**: `http://localhost:9090/admin`

**Method**: `POST`

**Headers**: `Content-Type: application/json`

**Features**: 
- ✅ Cross-platform (Windows, Linux, macOS)
- ✅ Zero dependencies (Pure Java)
- ✅ Real-time monitoring
- ✅ CORS enabled

### 1. **start-game** - Khởi động Game Server

**Request:**
```javascript
{
  "command": "start-game"
}
```

**Response:**
```
"Game server started successfully (PID: 12345)"
// hoặc
"Game server is already running"  ⚠️ Cảnh báo khi start 2 lần
// hoặc  
"Error starting game server: [error message]"
```

### 2. **stop-game** - Dừng Game Server

**Request:**
```javascript
{
  "command": "stop-game"
}
```

**Response:**
```
"Game server stopped successfully"
// hoặc
"Game server is not running"  ⚠️ Khi server đã tắt rồi
// hoặc
"Game server stop initiated, but may still be running. Please check manually."
// hoặc
"Error stopping game server: [error message]"
```

### 3. **restart-game** - Restart Game Server

**Request:**
```javascript
{
  "command": "restart-game",
  "data": "10"  // delay in seconds (optional, default: 5)
}
```

**Response:**
```
"Game server restarted. Stop: [stop_result], Start: [start_result]"
// hoặc
"Error restarting game server: [error message]"
```

### 4. **game-status** - Trạng thái Game Server

**Request:**
```javascript
{
  "command": "game-status"
}
```

**Response:**
```json
{
  "running": true,
  "pid": 12345,
  "uptime": "N/A",
  "log_size": 1024
}
```

### 5. **game-logs** - Xem Logs Game Server

**Request:**
```javascript
{
  "command": "game-logs",
  "data": "100"  // số dòng logs (optional, default: 50)
}
```

**Response:**
```
"[2025-10-01 20:00:00] Server started...\n[2025-10-01 20:00:01] Loading data...\n..."
// hoặc
"Log file not found"
// hoặc
"Error reading game server logs: [error message]"
```

### 6. **admin-status** - Trạng thái Admin Server

**Request:**
```javascript
{
  "command": "admin-status"
}
```

**Response:**
```json
{
  "admin_server": "running",
  "admin_port": 9090,
  "game_server_running": true,
  "uptime": "N/A",
  "java_version": "17.0.16",
  "os": "Linux"
}
```

### 7. **health-check** - Kiểm tra sức khỏe hệ thống

**Request:**
```javascript
{
  "command": "health-check"
}
```

**Response:**
```json
{
  "admin_server_status": "healthy",
  "admin_port_open": true,
  "game_server_running": true,
  "game_port_13579": true,
  "game_port_8080": true,
  "java_version": "17.0.16",
  "os": "Linux",
  "memory_free": 377984336,
  "memory_total": 394264576,
  "overall_status": "healthy"
}
```

### 8. **force-kill** - Force Kill Game Server

**Request:**
```javascript
{
  "command": "force-kill"
}
```

**Response:**
"Force kill completed. All game server processes terminated."
// hoặc
"Error during force kill: [error message]"

### Basic Usage Example
```javascript
const adminApi = async (command, data = null) => {
  const response = await fetch(`${process.env.NEXT_PUBLIC_ADMIN_API_URL}/admin`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ command, data })
  });
  
  const result = await response.text();
  
  // Try parse JSON, fallback to text
  try {
    return JSON.parse(result);
  } catch {
    return result;
  }
};

// Usage Examples
const startServer = () => adminApi('start-game');
const stopServer = () => adminApi('stop-game');
const restartServer = (delay = 5) => adminApi('restart-game', delay.toString());
const getStatus = () => adminApi('game-status');
const getLogs = (lines = 50) => adminApi('game-logs', lines.toString());
const healthCheck = () => adminApi('health-check');
const forceKill = () => adminApi('force-kill');

### Error Handling Example
```javascript
const handleServerStart = async () => {
  try {
    const result = await startServer();
    
    if (result.includes('already running')) {
      showWarning('⚠️ Server đã chạy rồi!');
    } else if (result.includes('successfully')) {
      showSuccess('✅ Server khởi động thành công!');
      const pid = result.match(/PID: (\d+)/)?.[1];
      console.log('Server PID:', pid);
    } else if (result.includes('Error')) {
      showError('❌ Lỗi: ' + result);
    }
  } catch (error) {
    showError('❌ Network error: ' + error.message);
  }
};

const handleHealthCheck = async () => {
  const health = await healthCheck();
  
  if (health.overall_status === 'healthy') {
    console.log('✅ System healthy');
    console.log(`Memory: ${Math.round(health.memory_free/1024/1024)}MB free`);
  } else {
    console.log('⚠️ Game server down');
  }
};
```

## 📋 Quick Reference

### Response Status Indicators
| Response Contains | Meaning | UI Action |
|-------------------|---------|-----------|
| `"successfully"` | ✅ Success | Show success message |
| `"already running"` | ⚠️ Warning | Show warning toast |
| `"is not running"` | ℹ️ Info | Show info message |
| `"Error"` | ❌ Error | Show error alert |
| `"Force kill completed"` | ⚡ Force action | Show force action result |

### Port Information
| Service | Port | Purpose |
|---------|------|---------|
| **Admin Server** | 9090 | API endpoint for Next.js |
| **Game Server** | 13579 | Player connections |
| **Game Web Admin** | 8080 | Built-in web interface |

### Command Summary
| Command | Purpose | Response Type |
|---------|---------|---------------|
| `start-game` | Khởi động server | Text |
| `stop-game` | Dừng server | Text |
| `restart-game` | Restart server | Text |
| `game-status` | Trạng thái server | JSON |
| `game-logs` | Xem logs | Text |
| `admin-status` | Trạng thái admin | JSON |
| `health-check` | Kiểm tra hệ thống | JSON |
| `force-kill` | Force kill server | Text |

**🚀 Ready for Next.js integration!**

**Built with Pure Java - Zero Dependencies - Cross Platform** ⚡
