package admin.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Standalone Admin Web Server để quản lý NRO Game Server từ bên ngoài
 * Chạy độc lập với game server, có thể restart/monitor game server
 */
public class StandaloneAdminServer {
    
    private static final int ADMIN_PORT = 9090;
    private static final String GAME_SERVER_DIR = "../"; // Đường dẫn đến game server
    private static final String GAME_START_SCRIPT = "./start-server.sh";
    private static final String GAME_STOP_SCRIPT = "./stop-server.sh";
    
    private static HttpServer server;
    private static Process gameServerProcess;
    private static Gson gson = new Gson();
    
    public static void main(String[] args) {
        try {
            startAdminServer();
            System.out.println("=== STANDALONE ADMIN SERVER ===");
            System.out.println("Admin Server started on port: " + ADMIN_PORT);
            System.out.println("Admin endpoint: http://localhost:" + ADMIN_PORT + "/admin");
            System.out.println("Game Server Directory: " + new File(GAME_SERVER_DIR).getAbsolutePath());
            System.out.println("===============================");
            
            // Keep server running
            Thread.currentThread().join();
            
        } catch (Exception e) {
            System.err.println("Failed to start Standalone Admin Server: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void startAdminServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(ADMIN_PORT), 0);
        server.createContext("/admin", new AdminHandler());
        server.createContext("/status", new StatusHandler());
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
    }
    
    static class AdminHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Set CORS headers
            setCorsHeaders(exchange);
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
            
            try {
                // Read request body
                String requestBody = readRequestBody(exchange);
                Map<String, String> requestData = parseJsonRequest(requestBody);
                String command = requestData.get("command");
                String data = requestData.get("data");
                
                System.out.println("[AdminServer] Received command: " + command + 
                                 (data != null ? " with data: " + data : ""));
                
                // Process command
                String response = processCommand(command, data);
                
                System.out.println("[AdminServer] Response: " + 
                                 (response.length() > 100 ? response.substring(0, 100) + "..." : response));
                
                sendResponse(exchange, 200, response);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "Server error: " + e.getMessage());
            }
        }
    }
    
    static class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            
            String status = getAdminServerStatus();
            sendResponse(exchange, 200, status);
        }
    }
    
    private static String processCommand(String command, String data) {
        switch (command.toLowerCase()) {
            case "start-game":
                return startGameServer();
                
            case "stop-game":
                return stopGameServer();
                
            case "restart-game":
                return restartGameServer(data);
                
            case "game-status":
                return getGameServerStatus();
                
            case "game-logs":
                return getGameServerLogs(data);
                
            case "admin-status":
                return getAdminServerStatus();
                
            case "health-check":
                return performHealthCheck();
                
            case "force-kill":
                return forceKillGameServer();
                
            default:
                return "Error: Unknown command '" + command + "'";
        }
    }
    
    private static String startGameServer() {
        try {
            if (isGameServerRunning()) {
                return "Game server is already running";
            }
            
            ProcessBuilder builder = new ProcessBuilder();
            String os = System.getProperty("os.name").toLowerCase();
            
            if (os.contains("win")) {
                builder.command("cmd", "/c", "start-server.bat");
            } else {
                builder.command("./start-server.sh");
            }
            
            builder.directory(new File(GAME_SERVER_DIR));
            builder.redirectOutput(new File("game-server.log"));
            builder.redirectError(new File("game-server-error.log"));
            
            gameServerProcess = builder.start();
            
            return "Game server started successfully (PID: " + gameServerProcess.pid() + ")";
            
        } catch (Exception e) {
            return "Error starting game server: " + e.getMessage();
        }
    }
    
    private static String stopGameServer() {
        try {
            if (!isGameServerRunning()) {
                return "Game server is not running";
            }
            
            String os = System.getProperty("os.name").toLowerCase();
            
            // Kill processes multiple ways to ensure success
            if (os.contains("win")) {
                // Windows
                Runtime.getRuntime().exec("taskkill /f /im java.exe");
            } else {
                // Linux/Mac: Multiple kill strategies
                Runtime.getRuntime().exec("pkill -f 'gradle.*run'");
                Runtime.getRuntime().exec("pkill -f 'Dragon.server.ServerManager'");
                Runtime.getRuntime().exec("fuser -k 13579/tcp"); // Kill process using port 13579
                Runtime.getRuntime().exec("fuser -k 8080/tcp");  // Kill process using port 8080
            }
            
            // Wait and verify
            Thread.sleep(3000);
            
            // Reset process reference
            gameServerProcess = null;
            
            // Verify server is actually stopped
            if (isGameServerRunning()) {
                return "Game server stop initiated, but may still be running. Please check manually.";
            }
            
            return "Game server stopped successfully";
            
        } catch (Exception e) {
            return "Error stopping game server: " + e.getMessage();
        }
    }
    
    private static String restartGameServer(String delayData) {
        try {
            int delay = 5; // default 5 seconds
            if (delayData != null && !delayData.trim().isEmpty()) {
                try {
                    delay = Integer.parseInt(delayData.trim());
                } catch (NumberFormatException e) {
                    return "Error: Invalid delay format. Use number of seconds.";
                }
            }
            
            // Stop game server
            String stopResult = stopGameServer();
            if (stopResult.contains("Error")) {
                return stopResult;
            }
            
            // Wait before restart
            Thread.sleep(delay * 1000);
            
            // Start game server
            String startResult = startGameServer();
            
            return "Game server restarted. Stop: " + stopResult + ", Start: " + startResult;
            
        } catch (Exception e) {
            return "Error restarting game server: " + e.getMessage();
        }
    }
    
    private static String getGameServerStatus() {
        try {
            boolean isRunning = isGameServerRunning();
            long pid = gameServerProcess != null ? gameServerProcess.pid() : -1;
            
            JsonObject status = new JsonObject();
            status.addProperty("running", isRunning);
            status.addProperty("pid", pid);
            status.addProperty("uptime", getGameServerUptime());
            status.addProperty("log_size", getLogFileSize());
            
            return gson.toJson(status);
            
        } catch (Exception e) {
            return "Error getting game server status: " + e.getMessage();
        }
    }
    
    private static String getAdminServerStatus() {
        try {
            JsonObject status = new JsonObject();
            status.addProperty("admin_server", "running");
            status.addProperty("admin_port", ADMIN_PORT);
            status.addProperty("game_server_running", isGameServerRunning());
            status.addProperty("uptime", getAdminServerUptime());
            status.addProperty("java_version", System.getProperty("java.version"));
            status.addProperty("os", System.getProperty("os.name"));
            
            return gson.toJson(status);
            
        } catch (Exception e) {
            return "Error getting admin server status: " + e.getMessage();
        }
    }
    
    private static String performHealthCheck() {
        try {
            JsonObject health = new JsonObject();
            
            // Check admin server
            health.addProperty("admin_server_status", "healthy");
            health.addProperty("admin_port_open", isPortOpen("localhost", ADMIN_PORT));
            
            // Check game server
            boolean gameRunning = isGameServerRunning();
            health.addProperty("game_server_running", gameRunning);
            health.addProperty("game_port_13579", isPortOpen("localhost", 13579));
            health.addProperty("game_port_8080", isPortOpen("localhost", 8080));
            
            // System info
            health.addProperty("java_version", System.getProperty("java.version"));
            health.addProperty("os", System.getProperty("os.name"));
            health.addProperty("memory_free", Runtime.getRuntime().freeMemory());
            health.addProperty("memory_total", Runtime.getRuntime().totalMemory());
            
            // Overall status
            health.addProperty("overall_status", gameRunning ? "healthy" : "game_server_down");
            
            return gson.toJson(health);
            
        } catch (Exception e) {
            return "Error performing health check: " + e.getMessage();
        }
    }
    
    private static String forceKillGameServer() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            
            if (os.contains("win")) {
                // Windows: Nuclear option
                Runtime.getRuntime().exec("taskkill /f /im java.exe");
                Runtime.getRuntime().exec("taskkill /f /im gradlew.bat");
            } else {
                // Linux/Mac: Nuclear option
                Runtime.getRuntime().exec("pkill -9 -f 'gradle'");
                Runtime.getRuntime().exec("pkill -9 -f 'java.*Dragon'");
                Runtime.getRuntime().exec("pkill -9 -f 'ServerManager'");
                Runtime.getRuntime().exec("fuser -k 13579/tcp");
                Runtime.getRuntime().exec("fuser -k 8080/tcp");
            }
            
            Thread.sleep(2000);
            gameServerProcess = null;
            
            return "Force kill completed. All game server processes terminated.";
            
        } catch (Exception e) {
            return "Error during force kill: " + e.getMessage();
        }
    }
    
    private static String getGameServerLogs(String lines) {
        try {
            int numLines = 50; // default
            if (lines != null && !lines.trim().isEmpty()) {
                try {
                    numLines = Integer.parseInt(lines.trim());
                } catch (NumberFormatException e) {
                    // Use default
                }
            }
            
            File logFile = new File(GAME_SERVER_DIR + "game-server.log");
            if (!logFile.exists()) {
                return "Log file not found";
            }
            
            // Read last N lines
            return readLastLines(logFile, numLines);
            
        } catch (Exception e) {
            return "Error reading game server logs: " + e.getMessage();
        }
    }
    
    // Helper methods
    private static boolean isGameServerRunning() {
        return isPortOpen("127.0.0.1", 13579) || isPortOpen("127.0.0.1", 8080);
    }
    
    private static boolean isPortOpen(String host, int port) {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, port), 2000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    private static String getGameServerUptime() {
        // Implementation for uptime calculation
        return "N/A";
    }
    
    private static String getAdminServerUptime() {
        // Implementation for admin server uptime
        return "N/A";
    }
    
    private static long getLogFileSize() {
        try {
            File logFile = new File(GAME_SERVER_DIR + "game-server.log");
            return logFile.exists() ? logFile.length() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    private static String readLastLines(File file, int numLines) {
        try {
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                java.util.List<String> lines = new java.util.ArrayList<>();
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
                int start = Math.max(0, lines.size() - numLines);
                for (int i = start; i < lines.size(); i++) {
                    content.append(lines.get(i)).append("\n");
                }
            }
            return content.toString();
        } catch (Exception e) {
            return "Error reading file: " + e.getMessage();
        }
    }
    
    private static void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
    
    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
            return body.toString();
        }
    }
    
    private static Map<String, String> parseJsonRequest(String jsonString) {
        Map<String, String> result = new HashMap<>();
        try {
            JsonObject json = gson.fromJson(jsonString, JsonObject.class);
            if (json.has("command")) {
                result.put("command", json.get("command").getAsString());
            }
            if (json.has("data")) {
                result.put("data", json.get("data").getAsString());
            }
        } catch (Exception e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
        }
        return result;
    }
    
    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}
