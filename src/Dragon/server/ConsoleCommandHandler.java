package Dragon.server;

import Dragon.jdbc.daos.BossRewardService;
import Dragon.jdbc.daos.GiftCodeCache;
import Dragon.jdbc.daos.MobRewardService;
import Dragon.server.netty.NettyServerManager;
import Dragon.models.item.Item;
import Dragon.models.player.Player;
import Dragon.services.ClanService;
import Dragon.services.InventoryServiceNew;
import Dragon.services.Service;
import Dragon.utils.Logger;
import Dragon.utils.Util;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ConsoleCommandHandler {

    private static ConsoleCommandHandler instance;

    public static ConsoleCommandHandler gI() {
        if (instance == null) instance = new ConsoleCommandHandler();
        return instance;
    }

    public void handle(String line) {
        if (line == null) return;
        try {
            Logger.log(Logger.YELLOW, "Console command activated: " + line + "\n");
            if (line.equals("savekigui")) {
            }
            if (line.equals("baotri")) {
                Maintenance.gI().start(2);
            }
            if (line.equals("lavie")) {
                ClanService.gI().saveclan();
            } else if (line.equals("online")) {
                List<String> lines = new ArrayList<>();
                ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
                ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds());
                System.out.println("Danh sách tên các luồng đang chạy:");
                lines.add("Danh sách tên các luồng đang chạy:");
                for (ThreadInfo threadInfo : threadInfos) {
                    lines.add(threadInfo.getThreadName());
                    System.out.println(threadInfo.getThreadName());
                }
                Path file = Paths.get("DataThread.txt");
                try {
                    Files.write(file, lines, StandardCharsets.UTF_8);
                } catch (IOException ex) {
                    java.util.logging.Logger.getLogger(ServerManager.class.getName()).log(Level.SEVERE, null, ex);
                }
                Logger.log(Logger.PURPLE, "Thread" + (Thread.activeCount() - ServerManager.gI().threadMap) + "\nOnline:" +
                        Client.gI().getPlayers().size());

            } else if (line.equals("nplayer")) {
                Logger.error("Player in game: " + Client.gI().getPlayers().size() + "\n");
            } else if (line.equals("admin")) {
                new Thread(() -> {
                    Client.gI().close();
                }, "adminThread").start();
            } else if (line.startsWith("bang")) {
                new Thread(() -> {
                    try {
                        ClanService.gI().close();
                        Logger.error("Save " + Manager.CLANS.size() + " bang");
                    } catch (Exception e) {
                        e.printStackTrace();
                        Logger.error("Thông báo: lỗi lưu dữ liệu bang hội.\n");
                    }
                }, "bangThread").start();
            } else if (line.startsWith("a")) {
                String a = line.replace("a ", "");
                Service.gI().sendThongBaoAllPlayer(a);
            } else if (line.startsWith("tb")) {
                String a = line.replace("tb ", "");
                Service.gI().sendBangThongBaoAllPlayervip(a);
            } else if (line.startsWith("qua")) {
                try {
                    List<Item.ItemOption> ios = new ArrayList<>();
                    String[] pagram1 = line.split("=")[1].split("-");
                    String[] pagram2 = line.split("=")[2].split("-");
                    if (pagram1.length == 4 && pagram2.length % 2 == 0) {
                        Player p = Client.gI().getPlayer(Integer.parseInt(pagram1[0]));
                        if (p != null) {
                            for (int i = 0; i < pagram2.length; i += 2) {
                                ios.add(new Item.ItemOption(Integer.parseInt(pagram2[i]),
                                        Integer.parseInt(pagram2[i + 1])));
                            }
                            Item i = Util.sendDo(Integer.parseInt(pagram1[2]), Integer.parseInt(pagram1[3]), ios);
                            i.quantity = Integer.parseInt(pagram1[1]);
                            InventoryServiceNew.gI().addItemBag(p, i);
                            InventoryServiceNew.gI().sendItemBags(p);
                            Service.gI().sendThongBao(p, "Admin trả đồ. anh em thông cảm nhé...");
                        } else {
                            System.out.println("Người chơi không online");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Lỗi quà");
                }
            } else if (line.equals("refreshmobcache")) {
                try {
                    Dragon.utils.Logger.log("ServerManager: Refreshing Mob Reward Cache...");
                    MobRewardService.getInstance().refreshCache();
                    Dragon.utils.Logger.log("ServerManager: Mob Reward Cache refreshed successfully!");
                    Dragon.utils.Logger.log(
                            "ServerManager: " + MobRewardService.getInstance().getCacheStats());
                } catch (Exception e) {
                    e.printStackTrace();
                    Dragon.utils.Logger.log("ServerManager: Failed to refresh Mob Reward Cache!");
                }
            } else if (line.equals("mobcachestats")) {
                try {
                    Dragon.utils.Logger.log(
                            "ServerManager: " + MobRewardService.getInstance().getCacheStats());
                } catch (Exception e) {
                    e.printStackTrace();
                    Dragon.utils.Logger.log("ServerManager: Failed to get cache stats!");
                }
            } else if (line.equals("refreshbosscache")) {
                try {
                    Dragon.utils.Logger.log("ServerManager: Refreshing Boss Reward Cache...");
                    BossRewardService.getInstance().refreshCache();
                    Dragon.utils.Logger.log("ServerManager: Boss Reward Cache refreshed successfully!");
                    Dragon.utils.Logger.log(
                            "ServerManager: " + BossRewardService.getInstance().getCacheStats());
                } catch (Exception e) {
                    e.printStackTrace();
                    Dragon.utils.Logger.log("ServerManager: Failed to refresh Boss Reward Cache!");
                }
            } else if (line.equals("bosscachestats")) {
                try {
                    Dragon.utils.Logger.log(
                            "ServerManager: " + BossRewardService.getInstance().getCacheStats());
                } catch (Exception e) {
                    e.printStackTrace();
                    Dragon.utils.Logger.log("ServerManager: Failed to get boss cache stats!");
                }
            } else if (line.equals("refreshgiftcache")) {
                try {
                    Dragon.utils.Logger.log("ServerManager: Refreshing Gift Code Cache...");
                    GiftCodeCache.getInstance().refreshCache();
                    Dragon.utils.Logger.log("ServerManager: Gift Code Cache refreshed successfully!");
                    Dragon.utils.Logger.log(
                            "ServerManager: " + GiftCodeCache.getInstance().getCacheStats());
                } catch (Exception e) {
                    e.printStackTrace();
                    Dragon.utils.Logger.log("ServerManager: Failed to refresh Gift Code Cache!");
                }
            } else if (line.equals("giftcachestats")) {
                try {
                    Dragon.utils.Logger.log(
                            "ServerManager: " + GiftCodeCache.getInstance().getCacheStats());
                } catch (Exception e) {
                    e.printStackTrace();
                    Dragon.utils.Logger.log("ServerManager: Failed to get gift code cache stats!");
                }
            } else if (line.equals("reloadgiftcodes")) {
                try {
                    Dragon.utils.Logger.log("ServerManager: Reloading Gift Codes from database...");
                    GiftCodeCache.getInstance().clearCache();
                    GiftCodeCache.getInstance().initializeCache();
                    Dragon.utils.Logger.log("ServerManager: Gift Codes reloaded successfully!");
                    Dragon.utils.Logger.log(
                            "ServerManager: " + GiftCodeCache.getInstance().getCacheStats());
                } catch (Exception e) {
                    e.printStackTrace();
                    Dragon.utils.Logger.log("ServerManager: Failed to reload Gift Codes!");
                }
            } else if (line.equals("gameloopstats")) {
                try {
                    Dragon.utils.Logger.log("ServerManager: GameLoopManager Performance Stats:");
                    Dragon.utils.Logger.log(GameLoopManager.getInstance().getPerformanceStats());
                } catch (Exception e) {
                    e.printStackTrace();
                    Dragon.utils.Logger.log("ServerManager: Failed to get GameLoop stats!");
                }
            } else if (line.equals("netty")) {
                try {
                    Dragon.utils.Logger.log("ServerManager: Netty Server Manager:");
                    Dragon.utils.Logger.log(NettyServerManager.getInstance().getStats());
                } catch (Exception e) {
                    e.printStackTrace();
                    Dragon.utils.Logger.log("ServerManager: Failed to get Netty stats!");
                }
            } else if (line.equals("enablenetty")) {
                try {
                    Dragon.utils.Logger.log("ServerManager: Enabling Netty mode...");
                    NettyServerManager.getInstance().enableNettyMode();
                    Dragon.utils.Logger.log("ServerManager: Netty mode enabled! Restart server to use Netty.");
                } catch (Exception e) {
                    e.printStackTrace();
                    Dragon.utils.Logger.log("ServerManager: Failed to enable Netty mode!");
                }
            } else if (line.equals("disablenetty")) {
                try {
                    Dragon.utils.Logger.log("ServerManager: Disabling Netty mode...");
                    NettyServerManager.getInstance().disableNettyMode();
                    Dragon.utils.Logger.log(
                            "ServerManager: Traditional mode enabled! Restart server to use traditional server.");
                } catch (Exception e) {
                    e.printStackTrace();
                    Dragon.utils.Logger.log("ServerManager: Failed to disable Netty mode!");
                }
            } else if (line.equals("startnetty")) {
                try {
                    Dragon.utils.Logger.log("ServerManager: Starting Netty server...");
                    NettyServerManager.getInstance().startNettyServer(ServerManager.PORT);
                    Dragon.utils.Logger.log("ServerManager: Netty server started!");
                } catch (Exception e) {
                    e.printStackTrace();
                    Dragon.utils.Logger.log("ServerManager: Failed to start Netty server!");
                }
            } else if (line.equals("stopnetty")) {
                try {
                    Dragon.utils.Logger.log("ServerManager: Stopping Netty server...");
                    NettyServerManager.getInstance().stopNettyServer();
                    Dragon.utils.Logger.log("ServerManager: Netty server stopped!");
                } catch (Exception e) {
                    e.printStackTrace();
                    Dragon.utils.Logger.log("ServerManager: Failed to stop Netty server!");
                }
            } else if (line.equals("closeallnetty")) {
                try {
                    Dragon.utils.Logger.log("ServerManager: Force closing all Netty connections...");
                    NettyServerManager.getInstance().forceCloseAllConnections();
                    Dragon.utils.Logger.log("ServerManager: All Netty connections closed!");
                } catch (Exception e) {
                    e.printStackTrace();
                    Dragon.utils.Logger.log("ServerManager: Failed to close Netty connections!");
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
