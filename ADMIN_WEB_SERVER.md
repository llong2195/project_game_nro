# Admin Web Server

Backend web server để nhận admin commands từ web interface cho game NRO.

## Tính năng

### 🚀 Auto Start
- Tự động khởi động cùng với game server
- Chạy trên port **8080**
- Endpoint: `http://localhost:8080/admin`

### 📊 Server Status Commands
- `status` - Hiển thị tổng quan server (Players, CPU, RAM, Host, Uptime)
- `players` - Số người chơi online
- `threads` - Số threads đang hoạt động
- `gameloop-stats` - Thống kê GameLoop

### 💾 Data Management Commands
- `saveclan` - Lưu dữ liệu clan
- `refresh-mob-cache` - Refresh Mob Reward Cache
- `refresh-boss-cache` - Refresh Boss Reward Cache
- `refresh-gift-cache` - Refresh Gift Code Cache

### 📢 Announcement Commands
- `announcement` - Gửi thông báo toàn server
- `vip-announcement` - Gửi thông báo riêng cho VIP

### 🔧 Server Management Commands
- `maintenance` - Thiết lập chế độ bảo trì với countdown từng phút + block login
- `cancel-maintenance` - Hủy chế độ bảo trì và cho phép login lại
- `restart` - Khởi động lại server (với countdown)

## Cách sử dụng

### 1. Khởi động Server

#### Cách 1: Chạy thường (không auto-restart)

**Linux/Mac:**
```bash
./gradlew run
```

**Windows:**
```cmd
gradlew.bat run
```

#### Cách 2: Chạy với auto-restart (Khuyến nghị cho VPS)

**Linux/Mac:**
```bash
# Sử dụng script auto-restart để có thể restart từ web admin
./start-server.sh
```

**Windows:**
```cmd
REM Sử dụng batch script auto-restart
start-server.bat
```

**⚠️ QUAN TRỌNG**: 
- **Để restart từ web admin hoạt động**, BẮT BUỘC phải chạy server bằng script auto-restart
- Nếu chạy `gradlew run` trực tiếp, restart sẽ chỉ tắt server mà không khởi động lại
- AdminWebServer sẽ tự động gọi đúng script tương ứng với OS

**Restart Command Flow**:
```
Web Admin → restart command → Server countdown → Call script → Exit → Script restart
```

### 2. Frontend Integration
- Tích hợp với Next.js frontend
- API endpoint: `http://localhost:8080/admin`

### 3. Next.js Integration Example
```javascript
// API call example for Next.js
const sendAdminCommand = async (command, data = null) => {
  try {
    const response = await fetch('http://localhost:8080/admin', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        command: command,
        data: data
      })
    });
    
    const result = await response.text();
    return result;
  } catch (error) {
    console.error('Admin command error:', error);
    throw error;
  }
};

// Usage examples
const status = await sendAdminCommand('status');
const playerCount = await sendAdminCommand('players');
const announcement = await sendAdminCommand('announcement', 'Server maintenance in 30 minutes');
```

## API Format

### Request
```json
POST /admin
Content-Type: application/json

{
  "command": "tên_command",
  "data": "dữ_liệu_tùy_chọn"
}
```

### Response
- **Success**: Plain text với kết quả
- **Error**: Plain text với thông báo lỗi
- **HTTP Status**: 200 (success), 405 (method not allowed), 500 (server error)

## Ví dụ Commands

### 1. Lấy Server Status
```bash
curl -X POST http://localhost:8080/admin \
  -H "Content-Type: application/json" \
  -d '{"command":"status"}'
```

### 2. Gửi Thông Báo
```bash
curl -X POST http://localhost:8080/admin \
  -H "Content-Type: application/json" \
  -d '{"command":"announcement","data":"Server sẽ bảo trì 30 phút"}'
```

### 3. Refresh Cache
```bash
curl -X POST http://localhost:8080/admin \
  -H "Content-Type: application/json" \
  -d '{"command":"refresh-mob-cache"}'
```

### 4. Bảo Trì Server
```bash
# Bảo trì 30 phút với countdown
curl -X POST http://localhost:8080/admin \
  -H "Content-Type: application/json" \
  -d '{"command":"maintenance","data":"30"}'

# Hủy bảo trì
curl -X POST http://localhost:8080/admin \
  -H "Content-Type: application/json" \
  -d '{"command":"cancel-maintenance"}'
```

#### Maintenance Features:
- **Block Login**: Người chơi không thể đăng nhập khi server bảo trì
- **Countdown Timer**: Thông báo đếm ngược từng phút
- **Smart Notifications**: 
  - Mỗi 5 phút: Thông báo bình thường
  - 5 phút cuối: Thông báo cảnh báo
  - 1 phút cuối: Thông báo khẩn cấp
- **Auto Messages**: Tự động gửi thông báo cho tất cả players

## Cross-Platform Support

### 🖥️ Supported Platforms
- ✅ **Linux** (Ubuntu, CentOS, Debian, etc.)
- ✅ **Windows** (Windows 10, Windows Server)
- ✅ **macOS** (Intel & Apple Silicon)

### 🔄 Auto-Restart Features
- **OS Detection**: Tự động detect Windows/Linux/Mac
- **Process Management**: Sử dụng ProcessBuilder cross-platform
- **Script Support**: 
  - Linux/Mac: `start-server.sh` (với nohup)
  - Windows: `start-server.bat` (với start command)
- **Restart Flow**:
  1. Web admin gửi restart command
  2. Server countdown và thông báo players
  3. Server gọi auto-restart script
  4. Server hiện tại tắt (exit 0)
  5. Script tự động start server mới

### 📁 File Structure
```
project_game_nro/
├── start-server.sh      # Linux/Mac auto-restart script
├── start-server.bat     # Windows auto-restart script
├── gradlew              # Linux/Mac gradle wrapper
├── gradlew.bat          # Windows gradle wrapper
└── src/Dragon/server/AdminWebServer.java
```

## Cấu hình

### Thay đổi Port
Sửa trong `AdminWebServer.java`:
```java
private static final int PORT = 8080; // Đổi port ở đây
```

### Thêm Authentication
Để production, nên thêm authentication:
```java
// Trong AdminCommandHandler.handle()
String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
if (!isValidAuth(authHeader)) {
    exchange.sendResponseHeaders(401, 0);
    return;
}
```

### CORS Configuration
Server đã enable CORS cho tất cả origins:
```java
exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
```

## Troubleshooting

### Lỗi "Address already in use"
- Port 8080 đã được sử dụng
- Đổi PORT trong AdminWebServer.java
- Hoặc kill process đang dùng port 8080

### Lỗi CORS
- Đã được xử lý trong server
- Nếu vẫn lỗi, kiểm tra browser console

### Commands không hoạt động
- Kiểm tra server logs
- Đảm bảo command name đúng
- Kiểm tra JSON format

### Kết nối timeout
- Kiểm tra firewall
- Đảm bảo server đang chạy
- Thử với localhost thay vì IP

## Security Notes

⚠️ **Quan trọng cho Production:**

1. **Chỉ chạy trên localhost** hoặc internal network
2. **Thêm authentication** cho admin commands
3. **Validate input** để tránh injection
4. **Rate limiting** để tránh spam
5. **HTTPS** cho production
6. **Firewall rules** để block external access

## Logs

Server sẽ log tất cả commands:
```
AdminWebServer: Received command: status
AdminWebServer: Received command: announcement with data: Hello players
```

## Tích hợp với Game

Server đã được tích hợp sẵn vào:
- `ServerManager.java` - Auto start
- `AdminCommandHandler.java` - Xử lý admin commands
- `SystemInfoService.java` - Lấy thông tin hệ thống

Không cần cấu hình thêm, chỉ cần start game server là web server sẽ tự động chạy.
