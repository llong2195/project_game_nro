package Dragon.services;

import Dragon.data.DataGame;
import Dragon.jdbc.daos.GodGK;
import Dragon.models.boss.BossManager;
import Dragon.models.item.Item;
import Dragon.models.shop.ItemShop;
import Dragon.models.player.Player;
import Dragon.models.shop.Shop;
import Dragon.server.Client;
import Dragon.server.Manager;
import Dragon.server.ServerManager;
import com.girlkun.network.io.Message;
import Dragon.services.func.ChangeMapService;
import Dragon.services.func.Input;
import Dragon.utils.Logger;
import Dragon.utils.Util;
import com.girlkun.network.session.ISession;
import com.girlkun.network.server.GirlkunSessionManager;

public class AdminCommandHandler {
    
    private static AdminCommandHandler instance;
    
    public static AdminCommandHandler gI() {
        if (instance == null) {
            instance = new AdminCommandHandler();
        }
        return instance;
    }
    
    /**
     * Xử lý các lệnh admin
     * @param player Player thực hiện lệnh
     * @param text Nội dung lệnh
     * @return true nếu đã xử lý lệnh admin, false nếu không phải lệnh admin
     */
    public boolean handleAdminCommand(Player player, String text) {
        if (player.getSession() == null || !player.isAdmin()) {
            return false;
        }
        
        // Các lệnh admin cơ bản
        if (handleBasicCommands(player, text)) {
            return true;
        }
        
        // Các lệnh với tham số
        if (handleParameterCommands(player, text)) {
            return true;
        }
        
        // Các lệnh phức tạp
        if (handleComplexCommands(player, text)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Xử lý các lệnh admin cơ bản (không có tham số)
     */
    private boolean handleBasicCommands(Player player, String text) {
        switch (text) {
            case "enddt":
                player.clan.doanhTrai = null;
                return true;
                
            case "load":
                Manager.loadPart();
                DataGame.updateData(player.getSession());
                return true;
                
            case "r": // hồi all skill, Ki
                Service.getInstance().releaseCooldownSkill(player);
                return true;
                
            case "mob":
                System.err.print(Service.getInstance().DataMobReward);
                return true;
                
            case "skillxd":
                SkillService.gI().learSkillSpecial(player, Dragon.models.skill.Skill.LIEN_HOAN_CHUONG);
                return true;
                
            case "skilltd":
                SkillService.gI().learSkillSpecial(player, Dragon.models.skill.Skill.SUPER_KAME);
                return true;
                
            case "skillnm":
                SkillService.gI().learSkillSpecial(player, Dragon.models.skill.Skill.LIEN_HOAN, (byte) 7);
                SkillService.gI().learSkillSpecial(player, Dragon.models.skill.Skill.MA_PHONG_BA);
                return true;
                
            case "bktne":
                Service.gI().showthanthu(player);
                return true;
                
            case "client":
                Client.gI().show(player);
                return true;
                
            case "vt":
                Service.gI().sendThongBao(player, player.location.x + " - " + player.location.y + "\n"
                        + player.zone.map.yPhysicInTop(player.location.x, player.location.y));
                return true;
                
            case "hs":
                player.nPoint.setFullHpMpDame();
                PlayerService.gI().sendInfoHpMp(player);
                Service.gI().sendThongBao(player, "Quyền năng trị liệu\n");
                return true;
                
            case "m":
                Service.gI().sendThongBao(player, "Map " + player.zone.map.mapName + " (" + player.zone.map.mapId + ")");
                return true;
                
            case "a":
                BossManager.gI().showListBoss(player);
                return true;
                
            case "b":
                sendBossMessage(player, 0);
                return true;
                
            case "c":
                sendBossMessage(player, 2);
                return true;
                
            case "ad":
                showAdminMenu(player);
                return true;
                
            case "bot":
                showBotMenu(player);
                return true;
                
            case "dtu":
                PetService.gI().createNormalPet(player, (byte) 2);
                return true;
                
            case "item":
                Input.gI().createFormSenditem1(player);
                return true;
                
            case "keyz":
                Input.gI().createFormGiveItem(player);
                return true;
                
            case "key":
                Input.gI().createFormSenditem1(player);
                return true;
                
            case "thread":
                Service.gI().sendThongBao(player, "Current thread: " + (Thread.activeCount() - ServerManager.gI().threadMap));
                return true;
                
                
            default:
                return false;
        }
    }
    
    /**
     * Xử lý các lệnh có tham số
     */
    private boolean handleParameterCommands(Player player, String text) {
        if (text.startsWith("set_")) {
            handleSetPowerCommand(player, text);
            return true;
        }
        
        if (text.startsWith("i")) {
            handleItemCommand(player, text);
            return true;
        }
        
        if (text.startsWith("notify")) {
            String message = text.replace("notify ", "");
            Service.gI().sendThongBaoAllPlayer(message);
            return true;
        }
        
        if (text.startsWith("upp")) {
            handlePetPowerUp(player, text);
            return true;
        }
        
        if (text.startsWith("up")) {
            handlePlayerPowerUp(player, text);
            return true;
        }
        
        if (text.startsWith("m") && text.length() > 1) {
            handleMapCommand(player, text);
            return true;
        }
        
        if (text.startsWith("it ")) {
            handleItemRangeCommand(player, text);
            return true;
        }
        
        if (text.startsWith("s")) {
            handleSpeedCommand(player, text);
            return true;
        }
        
        return false;
    }
    
    /**
     * Xử lý các lệnh phức tạp
     */
    private boolean handleComplexCommands(Player player, String text) {
        // Có thể thêm các lệnh phức tạp khác ở đây
        return false;
    }
    
    /**
     * Xử lý lệnh set power
     */
    private void handleSetPowerCommand(Player player, String text) {
        try {
            String[] args = text.split("_");
            double powerToAdd = Double.parseDouble(args[1]);
            
            player.nPoint.power = powerToAdd;
            player.nPoint.tiemNang = powerToAdd;
            player.nPoint.hpg = powerToAdd;
            player.nPoint.dameg = powerToAdd;
            player.nPoint.mpg = powerToAdd;
            player.nPoint.defg = powerToAdd;
            player.nPoint.hpMax = powerToAdd;
            player.nPoint.mpMax = powerToAdd;
            player.nPoint.setHp(powerToAdd);
            player.nPoint.setMp(powerToAdd);
            player.nPoint.setDame(powerToAdd);
            
            Service.gI().point(player);
            Service.gI().sendThongBao(player,
                    "Bạn vừa tự cộng cho mình " + Util.powerToString((long) powerToAdd) + " sức mạnh.");
            PlayerService.gI().sendInfoHpMp(player);
        } catch (NumberFormatException e) {
            Service.gI().sendThongBao(player, "Số không hợp lệ. Dùng: setpoint_ hoặc setpoin_ [số sức mạnh]");
        } catch (Exception e) {
            Service.gI().sendThongBao(player, "Cú pháp không hợp lệ. Dùng: setpoint_ hoặc setpoin_ [sức mạnh tùy chọn]");
        }
    }
    
    /**
     * Xử lý lệnh item
     */
    private void handleItemCommand(Player player, String text) {
        System.out.println("Item: " + text);
        try {
            String[] item = text.replace("i", "").split(" ");
            Item it = ItemService.gI().createNewItem((short) Short.parseShort(item[0]));
            
            if (it != null && item.length == 1) {
                InventoryServiceNew.gI().addItemBag(player, it);
                InventoryServiceNew.gI().sendItemBags(player);
                Service.gI().sendThongBao(player, "Đã nhận được " + it.template.name);
            } else if (it != null && item.length == 2 && Client.gI().getPlayer(String.valueOf(item[1])) == null) {
                it.quantity = Integer.parseInt(item[1]);
                InventoryServiceNew.gI().addItemBag(player, it);
                InventoryServiceNew.gI().sendItemBags(player);
                Service.gI().sendThongBao(player,
                        "Đã nhận được x" + Integer.valueOf(item[1]) + " " + it.template.name);
            } else if (it != null && item.length == 2 && Client.gI().getPlayer(String.valueOf(item[1])) != null) {
                String name = String.valueOf(item[1]);
                InventoryServiceNew.gI().addItemBag(Client.gI().getPlayer(name), it);
                InventoryServiceNew.gI().sendItemBags(Client.gI().getPlayer(name));
                Service.gI().sendThongBao(player, "Đã buff " + it.template.name + " đến player " + name);
                Service.gI().sendThongBao(Client.gI().getPlayer(name), "Đã nhận được " + it.template.name);
            } else if (it != null && item.length == 3 && Client.gI().getPlayer(String.valueOf(item[2])) != null) {
                String name = String.valueOf(item[2]);
                it.quantity = Integer.parseInt(item[1]);
                InventoryServiceNew.gI().addItemBag(Client.gI().getPlayer(name), it);
                InventoryServiceNew.gI().sendItemBags(Client.gI().getPlayer(name));
                Service.gI().sendThongBao(player, "Đã buff x" + Integer.valueOf(item[1]) + " "
                        + it.template.name + " đến player " + name);
                Service.gI().sendThongBao(Client.gI().getPlayer(name),
                        "Đã nhận được x" + Integer.valueOf(item[1]) + " " + it.template.name);
            } else {
                Service.gI().sendThongBao(player, "Không tìm thấy player");
            }
        } catch (NumberFormatException e) {
            Service.gI().sendThongBao(player, "Không tìm thấy player");
        }
    }
    
    /**
     * Xử lý lệnh tăng sức mạnh pet
     */
    private void handlePetPowerUp(Player player, String text) {
        try {
            long power = Long.parseLong(text.replaceAll("upp", ""));
            Service.gI().addSMTN(player.pet, (byte) 2, power, false);
        } catch (Exception e) {
            // Handle exception
        }
    }
    
    /**
     * Xử lý lệnh tăng sức mạnh player
     */
    private void handlePlayerPowerUp(Player player, String text) {
        try {
            long power = Long.parseLong(text.replaceAll("up", ""));
            Service.gI().addSMTN(player, (byte) 2, power, false);
        } catch (Exception e) {
            // Handle exception
        }
    }
    
    /**
     * Xử lý lệnh chuyển map
     */
    private void handleMapCommand(Player player, String text) {
        try {
            int mapId = Integer.parseInt(text.replace("m", ""));
            ChangeMapService.gI().changeMapInYard(player, mapId, -1, -1);
            Service.gI().sendThongBao(player, "|7|" + player.name + " đã dịch chuyển tức thời đến: "
                    + player.zone.map.mapName + " (" + player.zone.map.mapId + ")");
        } catch (Exception e) {
            // Handle exception
        }
    }
    
    /**
     * Xử lý lệnh item range
     */
    private void handleItemRangeCommand(Player player, String text) {
        String[] itemRange = text.replace("it ", "").split(" ");
        
        if (itemRange.length == 2) {
            int startItemId = Integer.parseInt(itemRange[0]);
            int endItemId = Integer.parseInt(itemRange[1]);
            
            for (int itemId = startItemId; itemId <= endItemId; itemId++) {
                Item item = ItemService.gI().createNewItem((short) itemId);
                ItemShop it = new Shop().getItemShop(itemId);
                
                if (it != null && !it.options.isEmpty()) {
                    item.itemOptions.addAll(it.options);
                }
                
                InventoryServiceNew.gI().addItemBag(player, item);
            }
            
            InventoryServiceNew.gI().sendItemBags(player);
            Service.getInstance().sendThongBao(player, "Đã lấy các món đồ từ kho đủ!");
        }
    }
    
    /**
     * Xử lý lệnh speed
     */
    private void handleSpeedCommand(Player player, String text) {
        try {
            player.nPoint.speed = (byte) Integer.parseInt(text.substring(1));
            Service.gI().point(player);
        } catch (Exception e) {
            // Handle exception
        }
    }
    
    /**
     * Gửi boss message
     */
    private void sendBossMessage(Player player, int type) {
        Message msg;
        try {
            msg = new Message(52);
            msg.writer().writeByte(type);
            msg.writer().writeInt((int) player.id);
            if (type == 2) {
                msg.writer().writeInt((int) player.zone.getHumanoids().get(1).id);
            }
            Service.gI().sendMessAllPlayerInMap(player, msg);
            msg.cleanup();
        } catch (Exception e) {
            // Handle exception
        }
    }
    
    /**
     * Hiển thị menu admin với format thông tin chi tiết
     */
    
    public void showUpdateCache(Player player){
        SystemInfoService systemInfo = SystemInfoService.gI();
        int playerCount = Client.gI().getPlayers().size();
        int threadCount = Thread.activeCount() - ServerManager.gI().threadMap;
        int sessionCount = GirlkunSessionManager.gI().getSessions().size();

        String menuText = "=== MENU CẬP NHẬT CACHE ===\n" +
                "Players: " + playerCount + " | Thread: " + threadCount + " | Session: " + sessionCount + "\n" +
                "CPU: " + systemInfo.getCpuUsageString() + "/100% | RAM: " + systemInfo.getUsedMemoryGB() + "/" + systemInfo.getTotalMemoryGB() + "GB\n" +
                "Host: " + systemInfo.getHostname() + " (" + systemInfo.getHostIP() + ")\n" +
                "Khởi động: " + ServerManager.timeStart;

        NpcService.gI().createMenuConMeo(player, 21588, 21587,
                menuText,
                "Cập nhật Boss Cache", "Cập nhật Gift Cache", "Cập nhật Shop Cache", "Cập nhật Task Cache", "Cập nhật Mob Cache", "Tải lại tất cả Cache");
    }
     
    public void showAdminMenu(Player player) {
        SystemInfoService systemInfo = SystemInfoService.gI();
        // Lấy thông tin cơ bản
        int playerCount = Client.gI().getPlayers().size();
        int threadCount = Thread.activeCount() - ServerManager.gI().threadMap;
        int sessionCount = GirlkunSessionManager.gI().getSessions().size();
        
        // Tạo menu text theo format yêu cầu
        String menuText = "=== THÔNG TIN SERVER ===\n" +
                "Players: " + playerCount + " | Thread: " + threadCount + " | Session: " + sessionCount + "\n" +
                "CPU: " + systemInfo.getCpuUsageString() + "/100% | RAM: " + systemInfo.getUsedMemoryGB() + "/" + systemInfo.getTotalMemoryGB() + "GB\n" +
                "Host: " + systemInfo.getHostname() + " (" + systemInfo.getHostIP() + ")\n" +
                "OS: " + systemInfo.getOSName() + " " + systemInfo.getOSVersion() + " (" + systemInfo.getArchitecture() + ")\n" +
                "Java: " + systemInfo.getJavaVersion() + " (" + systemInfo.getJavaVendor() + ")\n" +
                "CPU Cores: " + systemInfo.getCPUCores() + 
                " | Usage: " + systemInfo.getCpuUsageString() + "%\n" +
                "JVM Memory: " + systemInfo.getJVMUsedMemoryMB() + "/" + systemInfo.getJVMTotalMemoryMB() + "MB\n" +
                "Khởi động: " + ServerManager.timeStart;
        
        NpcService.gI().createMenuConMeo(player, Dragon.consts.ConstNpc.MENU_ADMIN, 21587,
                menuText,
                "Menu Admin", "Call Boss", "Buff Item", "GIFTCODE", "Update Cache", "Đóng");
    }
    
 
    private void showBotMenu(Player player) {
        NpcService.gI().createMenuConMeo(player, 206783, 206783, "|7| Menu bot\n"
                + "Player Online : " + Client.gI().getPlayers().size() + "\n"
                + "Bot Online : " + Dragon.Bot.BotManager.gI().bot.size(),
                "Bot\nPem Quái", "Bot\nBán Item", "Bot\nSăn Boss", "Đóng");
    }
}
