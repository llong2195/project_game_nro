package Dragon.services.func;

import Dragon.consts.ConstNpc;
import Dragon.models.item.Item;
import Dragon.models.item.Item.ItemOption;
import Dragon.models.npc.Npc;
import Dragon.models.player.Player;
import Dragon.server.Manager;
import Dragon.services.InventoryServiceNew;
import Dragon.services.Service;
import Dragon.services.EffectSkillService;
import Dragon.utils.Util;

/**
 * Service xử lý nâng cấp Set Kích Hoạt
 * Sử dụng điểm săn quái để tăng param của SKH options
 */
public class SetKichHoatUpgradeService {
    
    private static SetKichHoatUpgradeService instance;
    
    // Cost cho mỗi lần nâng cấp
    private static final int UPGRADE_COST_MOBS = 500; // 500 điểm săn quái
    private static final int UPGRADE_COST_BOSS = 100; // 100 điểm giết boss (ít hơn)
    private static final int MAX_UPGRADE_LEVEL = 10; // Tối đa +10 levels
    
    // Config tăng param cho từng set (option ID → param increase per level)
    private static final java.util.Map<Integer, Integer> SET_PARAM_CONFIG = new java.util.HashMap<Integer, Integer>() {{
        // Trái Đất sets
        put(127, 15); // Set KI 1 - Songoku: +15% mỗi cấp
        put(128, 20); // Set KI 2 - Kirin: +20% mỗi cấp  
        put(129, 25); // Set KI 3 - Thiên Tâm Hàng: +25% mỗi cấp
        
        // Namek sets
        put(130, 10); // Set KI 1 - Picolo: +10% mỗi cấp
        put(131, 12); // Set KI 2 - Ốc Tiêu: +12% mỗi cấp
        put(132, 18); // Set KI 3 - Pikkoro Đại Ma Vương: +18% mỗi cấp
        
        // Xayda sets  
        put(133, 8);  // Set KI 1 - Kakarot: +8% mỗi cấp
        put(134, 14); // Set KI 2 - Cadic: +14% mỗi cấp
        put(135, 22); // Set KI 3 - Nappa: +22% mỗi cấp
    }};
    
    public static SetKichHoatUpgradeService gI() {
        if (instance == null) {
            instance = new SetKichHoatUpgradeService();
        }
        return instance;
    }
    
    // Getter methods for external access
    public static int getUpgradeCostMobs() {
        return UPGRADE_COST_MOBS;
    }
    
    public static int getUpgradeCostBoss() {
        return UPGRADE_COST_BOSS;
    }
    
    public static int getMaxUpgradeLevel() {
        return MAX_UPGRADE_LEVEL;
    }
    
    /**
     * Tính tỉ lệ thành công theo cấp độ
     * Cấp càng cao thì tỉ lệ càng thấp
     */
    private static int getSuccessRateByLevel(int level) {
        switch (level) {
            case 0: return 90; // +0 → +1: 90%
            case 1: return 85; // +1 → +2: 85%
            case 2: return 80; // +2 → +3: 80%
            case 3: return 75; // +3 → +4: 75%
            case 4: return 70; // +4 → +5: 70%
            case 5: return 60; // +5 → +6: 60%
            case 6: return 50; // +6 → +7: 50%
            case 7: return 40; // +7 → +8: 40%
            case 8: return 30; // +8 → +9: 30%
            case 9: return 20; // +9 → +10: 20%
            default: return 10; // fallback
        }
    }
    
    /**
     * Hiển thị menu nâng cấp SKH
     */
    public void showUpgradeMenu(Player player, Npc npc) {
        if (player.combineNew.itemsCombine.size() == 1) {
            Item item = player.combineNew.itemsCombine.get(0);
            
            // Kiểm tra có đủ điểm không (ít nhất 1 trong 2 loại)
            boolean canUseMobs = player.point_kill_mobs >= UPGRADE_COST_MOBS;
            boolean canUseBoss = player.point_kill_boss >= UPGRADE_COST_BOSS;
            
            if (!canUseMobs && !canUseBoss) {
                npc.createOtherMenu(player, ConstNpc.IGNORE_MENU, 
                        "Không đủ điểm để nâng cấp SKH!\n" +
                        "Cần: " + UPGRADE_COST_MOBS + " điểm săn quái HOẶC " + UPGRADE_COST_BOSS + " điểm giết boss\n" +
                        "Bạn có: " + player.point_kill_mobs + " điểm săn quái, " + player.point_kill_boss + " điểm giết boss", 
                        "Đóng");
                return;
            }
            
            if (item == null || !item.isNotNullItem()) {
                npc.createOtherMenu(player, ConstNpc.IGNORE_MENU, 
                        "Item không hợp lệ để nâng cấp SKH", 
                        "Đóng");
                return;
            }
            
            // Kiểm tra item có SKH không
            if (!hasActivationSet(item)) {
                npc.createOtherMenu(player, ConstNpc.IGNORE_MENU, 
                        "Trang bị này chưa được kích hoạt SKH!\n" +
                        "Hãy kích hoạt SKH trước khi nâng cấp.", 
                        "Đóng");
                return;
            }
            
            int currentLevel = getCurrentUpgradeLevel(item);
            if (currentLevel >= MAX_UPGRADE_LEVEL) {
                npc.createOtherMenu(player, ConstNpc.IGNORE_MENU, 
                        "Trang bị này đã đạt cấp nâng cấp tối đa!\n" +
                        "Cấp hiện tại: +" + currentLevel, 
                        "Đóng");
                return;
            }
            
            int paramIncrease = getParamIncreaseForItem(item);
            int successRate = getSuccessRateByLevel(currentLevel);
            String npcSay = "\n|2| " + item.template.name + "\n";
            npcSay += "\n|7|Nâng cấp Set Kích Hoạt";
            npcSay += "\n|7|Cấp hiện tại: +" + currentLevel + "/" + MAX_UPGRADE_LEVEL;
            npcSay += "\n|7|Tăng param: +" + paramIncrease + "% mỗi cấp";
            npcSay += "\n|7|Tỉ lệ thành công: " + successRate + "%";
            npcSay += "\n|1|Bạn có: " + player.point_kill_mobs + " điểm săn quái, " + player.point_kill_boss + " điểm giết boss\n";
            
            // Tạo menu với các lựa chọn dựa trên điểm có sẵn
            java.util.List<String> options = new java.util.ArrayList<>();
            if (canUseMobs) {
                options.add("Dùng " + UPGRADE_COST_MOBS + " điểm săn quái");
            }
            if (canUseBoss) {
                options.add("Dùng " + UPGRADE_COST_BOSS + " điểm giết boss");
            }
            options.add("Đóng");
            
            npc.createOtherMenu(player, 215, npcSay, options.toArray(new String[0]));
        } else {
            npc.createOtherMenu(player, ConstNpc.IGNORE_MENU, 
                    "Hãy đưa cho ta 1 trang bị có SKH để nâng cấp", 
                    "Đóng");
        }
    }
    
    /**
     * Xử lý menu selection cho nâng cấp SKH
     */
    public void handleUpgradeMenuSelection(Player player, int select) {
        if (player.combineNew.itemsCombine.size() != 1) {
            Service.gI().sendThongBao(player, "Hãy đặt đúng 1 trang bị để nâng cấp");
            return;
        }
        
        Item item = player.combineNew.itemsCombine.get(0);
        boolean canUseMobs = player.point_kill_mobs >= UPGRADE_COST_MOBS;
        boolean canUseBoss = player.point_kill_boss >= UPGRADE_COST_BOSS;
        
        // Xác định loại điểm dựa trên select và availability
        if (canUseMobs && canUseBoss) {
            // Cả 2 đều có sẵn
            if (select == 0) {
                processUpgrade(player, true); // Dùng điểm săn quái
            } else if (select == 1) {
                processUpgrade(player, false); // Dùng điểm giết boss
            }
            // select == 2 = Đóng
        } else if (canUseMobs && !canUseBoss) {
            // Chỉ có điểm săn quái
            if (select == 0) {
                processUpgrade(player, true); // Dùng điểm săn quái
            }
            // select == 1 = Đóng
        } else if (!canUseMobs && canUseBoss) {
            // Chỉ có điểm giết boss
            if (select == 0) {
                processUpgrade(player, false); // Dùng điểm giết boss
            }
            // select == 1 = Đóng
        }
    }
    private void processUpgrade(Player player, boolean useMobPoints) {
        if (player.combineNew.itemsCombine.size() != 1) {
            Service.gI().sendThongBao(player, "Hãy đặt đúng 1 trang bị để nâng cấp");
            return;
        }
        int cost;
        String pointType;
        if (useMobPoints) {
            if (player.point_kill_mobs < UPGRADE_COST_MOBS) {
                Service.gI().sendThongBao(player, "Không đủ điểm săn quái (cần " + UPGRADE_COST_MOBS + " điểm)");
                return;
            }
            cost = UPGRADE_COST_MOBS;
            pointType = "điểm săn quái";
        } else {
            if (player.point_kill_boss < UPGRADE_COST_BOSS) {
                Service.gI().sendThongBao(player, "Không đủ điểm giết boss (cần " + UPGRADE_COST_BOSS + " điểm)");
                return;
            }
            cost = UPGRADE_COST_BOSS;
            pointType = "điểm giết boss";
        }
        
        Item item = player.combineNew.itemsCombine.get(0);
        
        if (!hasActivationSet(item)) {
            Service.gI().sendThongBao(player, "Trang bị này chưa có SKH!");
            return;
        }
        
        int currentLevel = getCurrentUpgradeLevel(item);
        if (currentLevel >= MAX_UPGRADE_LEVEL) {
            Service.gI().sendThongBao(player, "Trang bị đã đạt cấp nâng cấp tối đa!");
            return;
        }
        
        // Trừ điểm theo loại được chọn
        if (useMobPoints) {
            player.point_kill_mobs -= cost;
        } else {
            player.point_kill_boss -= cost;
        }
        
     
        
        int successRate = getSuccessRateByLevel(currentLevel);
        
        if (Util.isTrue(successRate, 100)) {
            upgradeSKHOptions(item);
            int newLevel = getCurrentUpgradeLevel(item);
            InventoryServiceNew.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Thành công! SKH đã được nâng cấp lên +" + newLevel + " (Dùng " + cost + " " + pointType + ", tỉ lệ: " + successRate + "%)");
            CombineServiceNew.gI().sendEffectSuccessCombine(player);
        } else {
            
            Service.gI().sendThongBao(player, "Thất bại! Nâng cấp SKH không thành công. (Dùng " + cost + " " + pointType + ", tỉ lệ: " + successRate + "%)");
            CombineServiceNew.gI().sendEffectFailCombine(player);
        }
        
        CombineServiceNew.gI().reOpenItemCombine(player);
    }
    
  
    private boolean hasActivationSet(Item item) {
        if (item == null || item.itemOptions == null) return false;
        
        for (Item.ItemOption option : item.itemOptions) {
            if (option.optionTemplate.id >= 127 && option.optionTemplate.id <= 135) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Lấy param increase cho item dựa trên set option ID
     */
    private int getParamIncreaseForItem(Item item) {
        if (item == null || item.itemOptions == null) return 10; // fallback
        
        for (Item.ItemOption option : item.itemOptions) {
            if (option.optionTemplate.id >= 127 && option.optionTemplate.id <= 135) {
                return SET_PARAM_CONFIG.getOrDefault(option.optionTemplate.id, 10);
            }
        }
        return 10; // fallback
    }
    
    private int getCurrentUpgradeLevel(Item item) {
        if (item == null || item.itemOptions == null) return 0;
        
        for (Item.ItemOption option : item.itemOptions) {
            if (option.optionTemplate.id >= 127 && option.optionTemplate.id <= 135) {
                return option.param - 1;
            }
        }
        return 0;
    }
    private void upgradeSKHOptions(Item item) {
        if (item == null || item.itemOptions == null) return;
        
        int paramIncrease = getParamIncreaseForItem(item);
        
        for (Item.ItemOption option : item.itemOptions) {
            if (option.optionTemplate.id >= 127 && option.optionTemplate.id <= 135) {
                option.param += 1; // Tăng level hiển thị
            }
            else if (option.optionTemplate.id >= 136 && option.optionTemplate.id <= 144) {
                option.param += paramIncrease; // Tăng param theo config của set
            }
        }
    }
}
