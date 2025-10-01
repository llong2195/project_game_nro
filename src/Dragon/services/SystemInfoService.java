package Dragon.services;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

import java.text.DecimalFormat;

/**
 * Service để lấy thông tin hệ thống sử dụng OSHI
 * Cross-platform và hiệu suất cao hơn JMX
 */
public class SystemInfoService {
    
    private static SystemInfoService instance;
    private static SystemInfo systemInfo;
    private static HardwareAbstractionLayer hardware;
    private static OperatingSystem os;
    private static CentralProcessor processor;
    private static GlobalMemory memory;
    
    // Cache CPU ticks để tính CPU usage
    private static long[] prevTicks;
    private static long lastUpdateTime = 0;
    private static double cachedCpuUsage = 0.0;
    
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");
    private static final long CPU_UPDATE_INTERVAL = 1000; // 1 second
    
    public static SystemInfoService gI() {
        if (instance == null) {
            instance = new SystemInfoService();
            initializeOSHI();
        }
        return instance;
    }
    
    /**
     * Khởi tạo OSHI components
     */
    private static void initializeOSHI() {
        try {
            systemInfo = new SystemInfo();
            hardware = systemInfo.getHardware();
            os = systemInfo.getOperatingSystem();
            processor = hardware.getProcessor();
            memory = hardware.getMemory();
            
            // Initialize CPU ticks
            prevTicks = processor.getSystemCpuLoadTicks();
            
        } catch (Exception e) {
            System.err.println("Failed to initialize OSHI: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Lấy thông tin CPU usage (%)
     * Cached để tránh gọi quá thường xuyên
     */
    public double getCpuUsage() {
        long currentTime = System.currentTimeMillis();
        
        // Update CPU usage mỗi giây để tránh overhead
        if (currentTime - lastUpdateTime > CPU_UPDATE_INTERVAL) {
            try {
                long[] currentTicks = processor.getSystemCpuLoadTicks();
                cachedCpuUsage = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100.0;
                prevTicks = currentTicks;
                lastUpdateTime = currentTime;
            } catch (Exception e) {
                // Fallback to 0 if error
                cachedCpuUsage = 0.0;
            }
        }
        
        return cachedCpuUsage;
    }
    
    /**
     * Lấy CPU usage dạng string đã format
     */
    public String getCpuUsageString() {
        return DECIMAL_FORMAT.format(getCpuUsage());
    }
    
    /**
     * Lấy tổng RAM (bytes)
     */
    public long getTotalMemory() {
        try {
            return memory.getTotal();
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Lấy RAM khả dụng (bytes)
     */
    public long getAvailableMemory() {
        try {
            return memory.getAvailable();
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Lấy RAM đã sử dụng (bytes)
     */
    public long getUsedMemory() {
        return getTotalMemory() - getAvailableMemory();
    }
    
    /**
     * Lấy RAM đã sử dụng dạng GB string
     */
    public String getUsedMemoryGB() {
        double usedGB = (double) getUsedMemory() / (1024 * 1024 * 1024);
        return DECIMAL_FORMAT.format(usedGB);
    }
    
    /**
     * Lấy tổng RAM dạng GB string
     */
    public String getTotalMemoryGB() {
        double totalGB = (double) getTotalMemory() / (1024 * 1024 * 1024);
        return DECIMAL_FORMAT.format(totalGB);
    }
    
    /**
     * Lấy phần trăm RAM đã sử dụng
     */
    public double getMemoryUsagePercent() {
        long total = getTotalMemory();
        if (total == 0) return 0.0;
        return ((double) getUsedMemory() / total) * 100.0;
    }
    
    /**
     * Lấy thông tin OS
     */
    public String getOSInfo() {
        try {
            return os.getFamily() + " " + os.getVersionInfo().getVersion();
        } catch (Exception e) {
            return "Unknown OS";
        }
    }
    
    /**
     * Lấy thông tin CPU
     */
    public String getCPUInfo() {
        try {
            return processor.getProcessorIdentifier().getName();
        } catch (Exception e) {
            return "Unknown CPU";
        }
    }
    
    /**
     * Lấy số core CPU
     */
    public int getCPUCores() {
        try {
            return processor.getLogicalProcessorCount();
        } catch (Exception e) {
            return Runtime.getRuntime().availableProcessors(); // Fallback
        }
    }
    
    /**
     * Lấy hostname của máy
     */
    public String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "Unknown Host";
        }
    }
    
    /**
     * Lấy IP address của host
     */
    public String getHostIP() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "Unknown IP";
        }
    }
    
    /**
     * Lấy thông tin Java version (chỉ số version)
     */
    public String getJavaVersion() {
        return System.getProperty("java.version");
    }
    
    /**
     * Lấy Java vendor
     */
    public String getJavaVendor() {
        return System.getProperty("java.vendor");
    }
    
    /**
     * Lấy OS name
     */
    public String getOSName() {
        return System.getProperty("os.name");
    }
    
    /**
     * Lấy OS version
     */
    public String getOSVersion() {
        return System.getProperty("os.version");
    }
    
    /**
     * Lấy Java Runtime version
     */
    public String getJavaRuntime() {
        return System.getProperty("java.runtime.name") + " " + System.getProperty("java.runtime.version");
    }
    
    /**
     * Lấy JVM info
     */
    public String getJVMInfo() {
        return System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version");
    }
    
    /**
     * Lấy architecture
     */
    public String getArchitecture() {
        return System.getProperty("os.arch");
    }
    
    /**
     * Lấy uptime của hệ thống (giây)
     */
    public long getSystemUptime() {
        try {
            return os.getSystemUptime();
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Format uptime thành string dễ đọc
     */
    public String getUptimeString() {
        long uptimeSeconds = getSystemUptime();
        
        long days = uptimeSeconds / 86400;
        long hours = (uptimeSeconds % 86400) / 3600;
        long minutes = (uptimeSeconds % 3600) / 60;
        long seconds = uptimeSeconds % 60;
        
        StringBuilder uptime = new StringBuilder();
        if (days > 0) uptime.append(days).append("d ");
        if (hours > 0) uptime.append(hours).append("h ");
        if (minutes > 0) uptime.append(minutes).append("m ");
        uptime.append(seconds).append("s");
        
        return uptime.toString();
    }
    
    /**
     * Lấy thông tin JVM memory
     */
    public String getJVMMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        double maxMB = maxMemory / (1024.0 * 1024.0);
        double usedMB = usedMemory / (1024.0 * 1024.0);
        
        return String.format("JVM: %sMB / %sMB", 
                DECIMAL_FORMAT.format(usedMB), 
                DECIMAL_FORMAT.format(maxMB));
    }
    
    /**
     * Lấy JVM used memory (MB)
     */
    public String getJVMUsedMemoryMB() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double usedMB = usedMemory / (1024.0 * 1024.0);
        return DECIMAL_FORMAT.format(usedMB);
    }
    
    /**
     * Lấy JVM total memory (MB)
     */
    public String getJVMTotalMemoryMB() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        double maxMB = maxMemory / (1024.0 * 1024.0);
        return DECIMAL_FORMAT.format(maxMB);
    }
    
    /**
     * Lấy số process đang chạy
     */
    public int getProcessCount() {
        try {
            return os.getProcessCount();
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Lấy số thread đang chạy
     */
    public int getThreadCount() {
        try {
            return os.getThreadCount();
        } catch (Exception e) {
            return Thread.activeCount(); // Fallback
        }
    }
    
    /**
     * Lấy thông tin hệ thống đầy đủ dạng string
     */
    public String getSystemInfoString() {
        StringBuilder info = new StringBuilder();
        
        try {
            info.append("=== HOST INFORMATION ===\n");
            info.append("Hostname: ").append(getHostname()).append("\n");
            info.append("OS: ").append(getOSInfo()).append(" (").append(getArchitecture()).append(")\n");
            info.append("Uptime: ").append(getUptimeString()).append("\n\n");
            
            info.append("=== HARDWARE ===\n");
            info.append("CPU: ").append(getCPUInfo()).append("\n");
            info.append("CPU Cores: ").append(getCPUCores()).append(" cores\n");
            info.append("CPU Usage: ").append(getCpuUsageString()).append("%\n");
            info.append("RAM: ").append(getUsedMemoryGB()).append("GB / ").append(getTotalMemoryGB()).append("GB");
            info.append(" (").append(DECIMAL_FORMAT.format(getMemoryUsagePercent())).append("%)\n\n");
            
            info.append("=== JAVA RUNTIME ===\n");
            info.append("Java: ").append(getJavaVersion()).append("\n");
            info.append("Runtime: ").append(getJavaRuntime()).append("\n");
            info.append("JVM: ").append(getJVMInfo()).append("\n");
            info.append("Memory: ").append(getJVMMemoryInfo()).append("\n\n");
            
            info.append("=== SYSTEM LOAD ===\n");
            info.append("Processes: ").append(getProcessCount()).append("\n");
            info.append("Threads: ").append(getThreadCount());
            
        } catch (Exception e) {
            info.append("Error getting system info: ").append(e.getMessage());
        }
        
        return info.toString();
    }
    
    /**
     * Lấy thông tin ngắn gọn cho admin menu
     */
    public String getCompactSystemInfo() {
        try {
            return String.format("Host: %s | CPU: %s%% (%d cores) | RAM: %s/%sGB | Java: %s", 
                    getHostname(),
                    getCpuUsageString(),
                    getCPUCores(),
                    getUsedMemoryGB(),
                    getTotalMemoryGB(),
                    System.getProperty("java.version"));
        } catch (Exception e) {
            return "System info unavailable";
        }
    }
    
    /**
     * Refresh tất cả thông tin (force update)
     */
    public void refresh() {
        lastUpdateTime = 0; // Force CPU update
        try {
            memory = hardware.getMemory(); // Refresh memory info
        } catch (Exception e) {
            // Ignore errors
        }
    }
}
