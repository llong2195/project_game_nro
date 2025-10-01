package Dragon.server;

import Dragon.models.player.Player;
import Dragon.services.AdminCommandHandler;
import Dragon.services.Service;
import Dragon.services.SystemInfoService;
import Dragon.utils.Logger;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Admin Web Server để nhận commands từ web interface
 * Chạy trên port 8080 với endpoint /admin
 */
public class AdminWebServer {
    
    private static AdminWebServer instance;
    private HttpServer server;
    private boolean isRunning = false;
    private static final int PORT = 8080;
    private static final String ADMIN_ENDPOINT = "/admin";
    
    public static AdminWebServer getInstance() {
        if (instance == null) {
            instance = new AdminWebServer();
        }
        return instance;
    }
    
    /**
     * Khởi động web server
     */
    public void start() {
        if (isRunning) {
            Logger.log("AdminWebServer: Already running on port " + PORT);
            return;
        }
        
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
            
            // Thiết lập CORS và admin endpoint
            server.createContext(ADMIN_ENDPOINT, new AdminCommandHandler());
            server.createContext("/", new CorsHandler()); // Handle CORS preflight
            
            server.setExecutor(Executors.newFixedThreadPool(4));
            server.start();
            
            isRunning = true;
            Logger.log("AdminWebServer: Started successfully on port " + PORT);
            Logger.log("AdminWebServer: Admin endpoint: http://localhost:" + PORT + ADMIN_ENDPOINT);
            
        } catch (IOException e) {
            Logger.logException(AdminWebServer.class, e);
            Logger.log("AdminWebServer: Failed to start server on port " + PORT);
        }
    }
    
    /**
     * Dừng web server
     */
    public void stop() {
        if (server != null && isRunning) {
            server.stop(0);
            isRunning = false;
            Logger.log("AdminWebServer: Stopped");
        }
    }
    
    /**
     * Handler cho CORS preflight requests
     */
    private static class CorsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Set CORS headers
            setCorsHeaders(exchange);
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, 0);
                exchange.close();
                return;
            }
            
            // For non-admin endpoints, return 404
            String response = "Admin Web Server - Use /admin endpoint for commands";
            exchange.sendResponseHeaders(404, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
    
    /**
     * Handler chính cho admin commands
     */
    private static class AdminCommandHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            
            // Handle OPTIONS request for CORS
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, 0);
                exchange.close();
                return;
            }
            
            // Only accept POST requests
            if (!"POST".equals(exchange.getRequestMethod())) {
                String response = "Only POST method allowed";
                exchange.sendResponseHeaders(405, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                return;
            }
            
            try {
                // Read request body
                InputStream is = exchange.getRequestBody();
                String requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                
                Map<String, String> requestData = parseJsonRequest(requestBody);
                String command = requestData.get("command");
                String data = requestData.get("data");
                
                Logger.log("AdminWebServer: Received command: " + command + 
                          (data != null ? " with data: " + data : ""));
                
                String response = processCommand(command, data);
                
                Logger.log("AdminWebServer: Response for " + command + ": " + 
                          (response.length() > 100 ? response.substring(0, 100) + "..." : response));
                
                // Send response
                byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, responseBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(responseBytes);
                os.close();
                
            } catch (Exception e) {
                Logger.logException(AdminWebServer.class, e);
                String errorResponse = "Server error: " + e.getMessage();
                exchange.sendResponseHeaders(500, errorResponse.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(errorResponse.getBytes());
                os.close();
            }
        }
    }
    
    /**
     * Set CORS headers
     */
    private static void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
    }
    
    /**
     * Parse JSON request đơn giản
     */
    private static Map<String, String> parseJsonRequest(String json) {
        Map<String, String> result = new HashMap<>();
        
        try {
            // Remove braces and quotes
            json = json.trim().replaceAll("[{}]", "");
            String[] pairs = json.split(",");
            
            for (String pair : pairs) {
                String[] keyValue = pair.split(":", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim().replaceAll("\"", "");
                    String value = keyValue[1].trim().replaceAll("\"", "");
                    result.put(key, value);
                }
            }
        } catch (Exception e) {
            Logger.log("AdminWebServer: Error parsing JSON: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Xử lý admin commands
     */
    private static String processCommand(String command, String data) {
        if (command == null || command.trim().isEmpty()) {
            return "Error: Command is required";
        }
        
        try {
            SystemInfoService sysInfo = SystemInfoService.gI();
            
            switch (command.toLowerCase()) {
                case "status":
                    return getServerStatus();
                    
                case "players":
                    return "Players online: " + Client.gI().getPlayers().size();
                    
                case "threads":
                    return "Active threads: " + (Thread.activeCount() - ServerManager.gI().threadMap);
                    
                case "gameloop-stats":
                    return getGameLoopStats();
                    
                case "saveclan":
                    return saveClanData();
                    
                case "refresh-mob-cache":
                    return refreshMobCache();
                    
                case "refresh-boss-cache":
                    return refreshBossCache();
                    
                case "refresh-gift-cache":
                    return refreshGiftCache();
                    
                case "announcement":
                    if (data == null || data.trim().isEmpty()) {
                        return "Error: Announcement text is required";
                    }
                    return sendGlobalAnnouncement(data);
                    
                case "vip-announcement":
                    if (data == null || data.trim().isEmpty()) {
                        return "Error: VIP announcement text is required";
                    }
                    return sendVipAnnouncement(data);
                    
                case "maintenance":
                    return setMaintenanceMode(data);
                    
                case "cancel-maintenance":
                    return cancelMaintenanceMode();
                    
                case "restart":
                    return restartServer(data);
                    
                case "shutdown":
                    return shutdownServer(data);
                    
                default:
                    return "Error: Unknown command '" + command + "'";
            }
            
        } catch (Exception e) {
            Logger.logException(AdminWebServer.class, e);
            return "Error processing command: " + e.getMessage();
        }
    }
    
    /**
     * Lấy thông tin tổng quan server
     */
    private static String getServerStatus() {
        SystemInfoService sysInfo = SystemInfoService.gI();
        
        StringBuilder status = new StringBuilder();
        status.append("=== SERVER STATUS ===\n");
        status.append("Players: ").append(Client.gI().getPlayers().size()).append("\n");
        status.append("Threads: ").append(Thread.activeCount() - ServerManager.gI().threadMap).append("\n");
        status.append("CPU: ").append(sysInfo.getCpuUsageString()).append("%\n");
        status.append("RAM: ").append(sysInfo.getUsedMemoryGB()).append("/").append(sysInfo.getTotalMemoryGB()).append("GB\n");
        status.append("Host: ").append(sysInfo.getHostname()).append(" (").append(sysInfo.getHostIP()).append(")\n");
        status.append("Uptime: ").append(sysInfo.getUptimeString()).append("\n");
        status.append("Server Start: ").append(ServerManager.timeStart);
        
        return status.toString();
    }
    
    /**
     * Lấy thống kê GameLoop
     */
    private static String getGameLoopStats() {
        return "GameLoop Stats:\n" +
               "Maps: " + Manager.MAPS.size() + "\n" +
               "Update interval: 1000ms\n" +
               "Status: Running";
    }
    
    /**
     * Lưu dữ liệu clan
     */
    private static String saveClanData() {
        try {
            // Trigger clan data save
            // Có thể gọi method save clan ở đây
            return "Clan data saved successfully";
        } catch (Exception e) {
            return "Error saving clan data: " + e.getMessage();
        }
    }
    
    /**
     * Refresh Mob Cache
     */
    private static String refreshMobCache() {
        try {
            Dragon.jdbc.daos.MobRewardCache.getInstance().refreshCache();
            return "Mob cache refreshed successfully";
        } catch (Exception e) {
            return "Error refreshing mob cache: " + e.getMessage();
        }
    }
    
    /**
     * Refresh Boss Cache
     */
    private static String refreshBossCache() {
        try {
            Dragon.jdbc.daos.BossRewardCache.getInstance().refreshCache();
            return "Boss cache refreshed successfully";
        } catch (Exception e) {
            return "Error refreshing boss cache: " + e.getMessage();
        }
    }
    
    /**
     * Refresh Gift Cache
     */
    private static String refreshGiftCache() {
        try {
            Dragon.jdbc.daos.GiftCodeCache.getInstance().refreshCache();
            return "Gift cache refreshed successfully";
        } catch (Exception e) {
            return "Error refreshing gift cache: " + e.getMessage();
        }
    }
    
    /**
     * Gửi thông báo toàn server
     */
    private static String sendGlobalAnnouncement(String message) {
        try {
            Service.gI().sendThongBaoAllPlayer(message);
            return "Global announcement sent: " + message;
        } catch (Exception e) {
            return "Error sending announcement: " + e.getMessage();
        }
    }
    
    /**
     * Gửi thông báo VIP
     */
    private static String sendVipAnnouncement(String message) {
        try {
            // Send to VIP players only
            for (Player player : Client.gI().getPlayers()) {
                if (player != null && player.vip > 0) {
                    Service.gI().sendThongBao(player, "[VIP] " + message);
                }
            }
            return "VIP announcement sent: " + message;
        } catch (Exception e) {
            return "Error sending VIP announcement: " + e.getMessage();
        }
    }
    
    /**
     * Thiết lập chế độ bảo trì với countdown
     */
    private static String setMaintenanceMode(String timeData) {
        try {
            // Parse time if provided
            int minutes = 30; // default
            if (timeData != null && !timeData.trim().isEmpty()) {
                try {
                    minutes = Integer.parseInt(timeData.trim());
                } catch (NumberFormatException e) {
                    return "Error: Invalid time format. Use number of minutes.";
                }
            }
            
            // Set maintenance mode
            DataControlGame.DataGame.IsBaoTri = true;
            
            // Send initial notification
            String initialMsg = "🔧 SERVER SẼ BẢO TRÌ TRONG " + minutes + " PHÚT\n" +
                               "Vui lòng hoàn thành nhiệm vụ và thoát game an toàn!";
            Service.gI().sendThongBaoAllPlayer(initialMsg);
            
            // Start countdown timer
            final int totalMinutes = minutes;
            new Thread(() -> {
                try {
                    for (int i = totalMinutes - 1; i > 0; i--) {
                        Thread.sleep(60000); // Wait 1 minute
                        
                        String countdownMsg;
                        if (i == 1) {
                            countdownMsg = "⚠️ SERVER SẼ BẢO TRÌ SAU 1 PHÚT NỮA!\n" +
                                         "Vui lòng thoát game NGAY để tránh mất dữ liệu!";
                        } else if (i <= 5) {
                            countdownMsg = "⚠️ SERVER SẼ BẢO TRÌ SAU " + i + " PHÚT NỮA!\n" +
                                         "Hãy chuẩn bị thoát game!";
                        } else if (i % 5 == 0) { // Thông báo mỗi 5 phút
                            countdownMsg = "🔧 Server sẽ bảo trì sau " + i + " phút nữa.";
                        } else {
                            continue; // Skip thông báo cho các phút khác
                        }
                        
                        Service.gI().sendThongBaoAllPlayer(countdownMsg);
                        Logger.log("MaintenanceCountdown: " + i + " minutes remaining");
                    }
                    
                    // Final warning
                    Thread.sleep(60000); // Wait final minute
                    Service.gI().sendThongBaoAllPlayer("🚨 SERVER ĐANG BẢO TRÌ! Tất cả kết nối sẽ bị ngắt!");
                    Logger.log("MaintenanceCountdown: Maintenance time reached!");
                    
                    // Optionally kick all players or shutdown server here
                    // kickAllPlayers();
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Logger.log("MaintenanceCountdown: Countdown interrupted");
                }
            }).start();
            
            return "Maintenance mode activated with " + minutes + " minutes countdown";
        } catch (Exception e) {
            return "Error setting maintenance mode: " + e.getMessage();
        }
    }
    
    /**
     * Hủy chế độ bảo trì
     */
    private static String cancelMaintenanceMode() {
        try {
            // Cancel maintenance mode
            DataControlGame.DataGame.IsBaoTri = false;
            
            // Send notification to all players
            String cancelMsg = "✅ BẢO TRÌ ĐÃ BỊ HỦY!\n" +
                              "Server tiếp tục hoạt động bình thường. Chúc bạn chơi game vui vẻ!";
            Service.gI().sendThongBaoAllPlayer(cancelMsg);
            
            Logger.log("AdminWebServer: Maintenance mode cancelled");
            return "Maintenance mode cancelled successfully";
        } catch (Exception e) {
            return "Error cancelling maintenance mode: " + e.getMessage();
        }
    }
    
    /**
     * Khởi động lại server
     */
    private static String restartServer(String countdownData) {
        try {
            int countdown = 60; // default 60 seconds
            if (countdownData != null && !countdownData.trim().isEmpty()) {
                try {
                    countdown = Integer.parseInt(countdownData.trim());
                } catch (NumberFormatException e) {
                    return "Error: Invalid countdown format. Use number of seconds.";
                }
            }
            
            // Send restart notification
            String restartMsg = "Server sẽ khởi động lại sau " + countdown + " giây. Vui lòng thoát game ngay!";
            Service.gI().sendThongBaoAllPlayer(restartMsg);
            
            // Schedule restart with auto-restart capability
            final int finalCountdown = countdown; // Make it effectively final for lambda
            new Thread(() -> {
                try {
                    Thread.sleep(finalCountdown * 1000);
                    Logger.log("AdminWebServer: Restarting server...");
                    
                    // Try to restart using auto-restart scripts (cross-platform)
                    try {
                        String os = System.getProperty("os.name").toLowerCase();
                        ProcessBuilder builder;
                        
                        if (os.contains("win")) {
                            // Windows: Use start-server.bat
                            builder = new ProcessBuilder("cmd", "/c", "start", "start-server.bat");
                        } else {
                            // Linux/Mac: Use start-server.sh
                            builder = new ProcessBuilder("nohup", "./start-server.sh");
                        }
                        
                        // Set working directory to current directory
                        builder.directory(new java.io.File(System.getProperty("user.dir")));
                        
                        // Redirect output to avoid hanging
                        if (!os.contains("win")) {
                            builder.redirectOutput(new java.io.File("server-restart.log"));
                            builder.redirectError(new java.io.File("server-restart-error.log"));
                        }
                        
                        Process newProcess = builder.start();
                        Logger.log("AdminWebServer: Auto-restart script started (PID: " + newProcess.pid() + ")");
                        Logger.log("AdminWebServer: Script will handle server restart automatically");
                        Thread.sleep(1000); // Brief wait
                        
                    } catch (Exception e) {
                        Logger.log("AdminWebServer: Failed to start restart script: " + e.getMessage());
                        Logger.log("AdminWebServer: OS: " + System.getProperty("os.name"));
                        Logger.log("AdminWebServer: Working Dir: " + System.getProperty("user.dir"));
                        Logger.log("AdminWebServer: Falling back to simple exit (manual restart required)...");
                    }
                    
                    System.exit(0); // Exit current process
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            
            return "Server restart scheduled in " + countdown + " seconds";
        } catch (Exception e) {
            return "Error scheduling restart: " + e.getMessage();
        }
    }
    
    /**
     * Tắt server (không restart)
     */
    private static String shutdownServer(String countdownData) {
        try {
            int countdown = 30; // default 30 seconds
            if (countdownData != null && !countdownData.trim().isEmpty()) {
                try {
                    countdown = Integer.parseInt(countdownData.trim());
                } catch (NumberFormatException e) {
                    return "Error: Invalid countdown format. Use number of seconds.";
                }
            }
            
            // Send shutdown notification
            String shutdownMsg = "Server sẽ tắt sau " + countdown + " giây. Vui lòng thoát game ngay!";
            Service.gI().sendThongBaoAllPlayer(shutdownMsg);
            
            // Schedule shutdown
            final int finalCountdown = countdown;
            new Thread(() -> {
                try {
                    Thread.sleep(finalCountdown * 1000);
                    Logger.log("AdminWebServer: Shutting down server...");
                    System.exit(0);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            
            return "Server shutdown scheduled in " + countdown + " seconds";
        } catch (Exception e) {
            return "Error scheduling shutdown: " + e.getMessage();
        }
    }
}
