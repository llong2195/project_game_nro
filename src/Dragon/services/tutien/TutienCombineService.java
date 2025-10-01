package Dragon.services.tutien;

import Dragon.models.item.Item;
import Dragon.models.player.Player;
import Dragon.server.Manager;
import Dragon.services.*;
import Dragon.utils.Util;

public class TutienCombineService {

    private static TutienCombineService instance;

    public static TutienCombineService gI() {
        if (instance == null) {
            instance = new TutienCombineService();
        }
        return instance;
    }

    public void combineTanDanFragment(Player player) {
        if (player.combineNew.itemsCombine.size() != 2) {
            Service.gI().sendThongBao(player, "Cần chọn 99 mảnh tàn đan và 99 công thức!");
            return;
        }
        // Tìm mảnh tàn đan và công thức
        Item fragment = null;
        Item congThuc = null;
        for (Item item : player.combineNew.itemsCombine) {
            if (item.template.id == 1805) {
                fragment = item;
            } else if (item.template.id == 1804) {
                congThuc = item;
            }
        }

        if (fragment == null || congThuc == null) {
            Service.gI().sendThongBao(player, "Cần 99 " + (fragment != null ? fragment.template.name : "Mảnh Tàn Đan")
                    + " và 99 " + (congThuc != null ? congThuc.template.name : "Công Thức Đan Dược") + "!");
            return;
        }

        int totalCombines = 0;
        int cost = 100_000_000;

        // Ghép theo số lần được chỉ định trong dapdo
        while (player.combineNew.dapdo > 0) {
            // Kiểm tra điều kiện cho mỗi lần ghép
            if (fragment.quantity < 99) {
                Service.gI().sendThongBao(player, "Không đủ " + fragment.template.name + "! Cần 99 cho mỗi lần ghép.");
                break;
            }

            if (congThuc.quantity < 99) {
                Service.gI().sendThongBao(player, "Không đủ " + congThuc.template.name + "! Cần 99 cho mỗi lần ghép.");
                break;
            }

            if (player.inventory.gold < cost) {
                Service.gI().sendThongBao(player, "Không đủ Gold! Cần " + Util.numberToMoney(cost) + " cho mỗi lần ghép.");
                break;
            }

            if (InventoryServiceNew.gI().getCountEmptyBag(player) < 1) {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
                break;
            }

            // Thực hiện ghép
            player.inventory.gold -= cost;
            InventoryServiceNew.gI().subQuantityItemsBag(player, fragment, 99);
            InventoryServiceNew.gI().subQuantityItemsBag(player, congThuc, 99);

            Item newDan = new Item();
            newDan.template = Dragon.server.Manager.ITEM_TEMPLATES.get(1806);
            newDan.quantity = 1;
            InventoryServiceNew.gI().addItemBag(player, newDan);

            totalCombines++;
            player.combineNew.dapdo--;
        }

        if (totalCombines > 0) {
            Service.gI().sendMoney(player);
            InventoryServiceNew.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Ghép đan thành công " + totalCombines + " lần! Nhận được " + totalCombines + " Đan Tu Tiên");
            Dragon.services.func.CombineServiceNew.gI().sendEffectSuccessCombine(player);
        }

        player.combineNew.itemsCombine.clear();
        Dragon.services.func.CombineServiceNew.gI().reOpenItemCombine(player);
    }

    public void upgradeTutienDan(Player player) {
        if (player.combineNew.itemsCombine.size() != 2) {
            Service.gI().sendThongBao(player, "Cần chọn đan Tu Tiên và công thức!");
            return;
        }

        Item dan = null;
        Item congThuc = null;

        for (Item item : player.combineNew.itemsCombine) {
            if (item.template.id >= 1806 && item.template.id <= 1815) {
                dan = item;
            } else if (item.template.id == 1804) {
                congThuc = item;
            }
        }

        if (dan == null || congThuc == null) {
            Service.gI().sendThongBao(player, "Cần đan Tu Tiên và công thức!");
            return;
        }

        int currentLevel = dan.template.id - 1805;
        if (currentLevel >= 10) {
            Service.gI().sendThongBao(player, "Đan đã đạt cấp tối đa!");
            return;
        }

        int totalUpgrades = 0;
        int successCount = 0;
        int failCount = 0;
        int requiredQuantity = 99;
        int cost = 100_000_000;

        int upgradeTimes = player.combineNew.dapdo > 0 ? player.combineNew.dapdo : 1;
        
        while (upgradeTimes > 0) {
            if (dan.quantity < requiredQuantity) {
                Service.gI().sendThongBao(player, "Không đủ " + dan.template.name + "! Cần 99 cho mỗi lần nâng cấp.");
                break;
            }

            if (congThuc.quantity < requiredQuantity) {
                Service.gI().sendThongBao(player, "Không đủ " + congThuc.template.name + "! Cần 99 cho mỗi lần nâng cấp.");
                break;
            }

            if (player.inventory.gold < cost) {
                Service.gI().sendThongBao(player, "Không đủ Gold! Cần " + Util.numberToMoney(cost) + " cho mỗi lần nâng cấp.");
                break;
            }

            if (InventoryServiceNew.gI().getCountEmptyBag(player) < 1) {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
                break;
            }

            // Thực hiện nâng cấp
            player.inventory.gold -= cost;
            InventoryServiceNew.gI().subQuantityItemsBag(player, dan, requiredQuantity);
            InventoryServiceNew.gI().subQuantityItemsBag(player, congThuc, requiredQuantity);

            // Kiểm tra thành công
            int targetLevel = currentLevel + 1;
            int successRate = getSuccessRateForLevel(targetLevel);
            
            if (Util.nextInt(1, 100) <= successRate) {
                // Thành công - tạo đan cấp cao hơn
                int newDanId = 1805 + targetLevel;
                if (newDanId <= 1815) {
                    Item newDan = new Item();
                    newDan.template = Manager.ITEM_TEMPLATES.get(newDanId);
                    newDan.quantity = 1;
                    InventoryServiceNew.gI().addItemBag(player, newDan);
                    successCount++;
                }
            } else {
                failCount++;
            }

            totalUpgrades++;
            upgradeTimes--;
        }

        if (totalUpgrades > 0) {
            Service.gI().sendMoney(player);
            InventoryServiceNew.gI().sendItemBags(player);
            
            String message = "Nâng cấp hoàn tất! ";
            message += "Thành công: " + successCount + ", Thất bại: " + failCount;
            Service.gI().sendThongBao(player, message);

            if (successCount > 0) {
                Dragon.services.func.CombineServiceNew.gI().sendEffectSuccessCombine(player);
            } else {
                Dragon.services.func.CombineServiceNew.gI().sendEffectFailCombine(player);
                
            }
        }
        player.combineNew.dapdo = 0;
        player.combineNew.itemsCombine.clear();
        Dragon.services.func.CombineServiceNew.gI().reOpenItemCombine(player);
       
    }

    public void showInfoCombineTanDanFragment(Player player) {
        showInfoCombineTanDanFragment(player, null);
    }

    public void showInfoCombineTanDanFragment(Player player, Dragon.models.npc.Npc npc) {
        if (player.combineNew.itemsCombine.size() == 2) {
            Item fragment = null;
            Item congThuc = null;

            for (Item item : player.combineNew.itemsCombine) {
                if (item.template.id == 1805) {
                    fragment = item;
                } else if (item.template.id == 1804) {
                    congThuc = item;
                }
            }

            if (fragment != null && congThuc != null) {
                int maxCombines = Math.min(fragment.quantity / 99, congThuc.quantity / 99);
                
                String npcSay = "|7|Ghép Mảnh Tàn Đan\n\n";
                npcSay += "|2|Mảnh Tàn Đan: " + fragment.template.name + " (x" + fragment.quantity + ")\n";
                npcSay += "|2|Công Thức: " + congThuc.template.name + " (x" + congThuc.quantity + ")\n\n";
                npcSay += "|1|Chi phí: 100M Gold/lần\n";
                npcSay += "|3|Tỉ lệ thành công: 100%\n";
                npcSay += "|6|Có thể ghép tối đa: " + maxCombines + " lần\n\n";
                npcSay += "|6|Kết quả: "
                        + (Manager.ITEM_TEMPLATES.get(1806) != null ? Manager.ITEM_TEMPLATES.get(1806).name
                        : "Đan Tu Tiên");

                Dragon.models.npc.Npc currentNpc = npc != null ? npc
                        : Dragon.services.func.CombineServiceNew.gI()
                                .getNpcByType(Dragon.services.func.CombineServiceNew.COMBINE_TAN_DAN_FRAGMENT);

                if (maxCombines >= 100) {
                    currentNpc.createOtherMenu(player, Dragon.consts.ConstNpc.MENU_START_COMBINE, npcSay, 
                        "Ghép x1", "Ghép x10", "Ghép x100");
                } else if (maxCombines >= 10) {
                    currentNpc.createOtherMenu(player, Dragon.consts.ConstNpc.MENU_START_COMBINE, npcSay, 
                        "Ghép x1", "Ghép x10");
                } else if (maxCombines >= 1) {
                    currentNpc.createOtherMenu(player, Dragon.consts.ConstNpc.MENU_START_COMBINE, npcSay, 
                        "Ghép x1");
                } else {
                    currentNpc.createOtherMenu(player, Dragon.consts.ConstNpc.IGNORE_MENU,
                            "Cần ít nhất 99 Mảnh Tàn Đan và 99 Công Thức Đan Dược", "Đóng");
                }
            } else {
                Dragon.models.npc.Npc currentNpc = npc != null ? npc
                        : Dragon.services.func.CombineServiceNew.gI()
                                .getNpcByType(Dragon.services.func.CombineServiceNew.COMBINE_TAN_DAN_FRAGMENT);
                currentNpc.createOtherMenu(player, Dragon.consts.ConstNpc.IGNORE_MENU,
                        "Cần 99 Mảnh Tàn Đan và 99 Công Thức Đan Dược", "Đóng");
            }
        } else {
            Dragon.models.npc.Npc currentNpc = npc != null ? npc
                    : Dragon.services.func.CombineServiceNew.gI()
                            .getNpcByType(Dragon.services.func.CombineServiceNew.COMBINE_TAN_DAN_FRAGMENT);
            currentNpc.createOtherMenu(player, Dragon.consts.ConstNpc.IGNORE_MENU,
                    "Cần chọn 2 item: Mảnh Tàn Đan và Công Thức", "Đóng");
        }
    }

    public void showInfoUpgradeTutienDan(Player player) {
        showInfoUpgradeTutienDan(player, null);
    }

    public void showInfoUpgradeTutienDan(Player player, Dragon.models.npc.Npc npc) {

        if (player.combineNew.itemsCombine.size() == 2) {
            Item dan = null;
            Item congThuc = null;

            for (Item item : player.combineNew.itemsCombine) {
                if (item.template.id >= 1806 && item.template.id <= 1815) {
                    dan = item;
                } else if (item.template.id == 1804) {
                    congThuc = item;
                }
            }

            if (dan != null && congThuc != null) {
                int currentLevel = dan.template.id - 1805; 
                int targetLevel = currentLevel + 1;

                if (currentLevel >= 10) {
                    Dragon.services.func.CombineServiceNew.gI()
                            .getNpcByType(Dragon.services.func.CombineServiceNew.UPGRADE_TUTIEN_DAN)
                            .createOtherMenu(player, Dragon.consts.ConstNpc.IGNORE_MENU,
                                    "Đan đã đạt cấp tối đa!", "Đóng");
                    return;
                }

                // Tính số lượng tối đa có thể nâng cấp
                int maxUpgrades = Math.min(dan.quantity / 99, congThuc.quantity / 99);
                
                int requiredQuantity = 99;
                int cost = 100_000_000;
                int successRate = getSuccessRateForLevel(targetLevel);
                int newDanId = 1805 + targetLevel;

                String npcSay = "|7|Nâng Cấp Đan Tu Tiên\n\n";
                npcSay += "|2|Đan hiện tại: " + dan.template.name + " (Cấp " + currentLevel + ")\n";
                npcSay += "|2|Công thức: " + congThuc.template.name + " (x" + congThuc.quantity + ")\n\n";
                npcSay += "|1|Chi phí: " + Util.numberToMoney(cost) + " Gold/lần\n";
                npcSay += "|3|Tỉ lệ thành công: " + successRate + "%\n";
                npcSay += "|6|Có thể nâng cấp tối đa: " + maxUpgrades + " lần\n\n";
                npcSay += "|6|Kết quả: "
                        + (Manager.ITEM_TEMPLATES.get(newDanId) != null ? Manager.ITEM_TEMPLATES.get(newDanId).name
                        : "Đan Tu Tiên cấp " + targetLevel);

                Dragon.models.npc.Npc currentNpc = npc != null ? npc
                        : Dragon.services.func.CombineServiceNew.gI()
                                .getNpcByType(Dragon.services.func.CombineServiceNew.UPGRADE_TUTIEN_DAN);

                // Tạo menu options dựa trên số lượng có thể nâng cấp
                if (maxUpgrades >= 100) {
                    currentNpc.createOtherMenu(player, Dragon.consts.ConstNpc.MENU_START_COMBINE, npcSay, 
                        "Nâng cấp x1", "Nâng cấp x10", "Nâng cấp x100");
                } else if (maxUpgrades >= 10) {
                    currentNpc.createOtherMenu(player, Dragon.consts.ConstNpc.MENU_START_COMBINE, npcSay, 
                        "Nâng cấp x1", "Nâng cấp x10");
                } else if (maxUpgrades >= 1) {
                    currentNpc.createOtherMenu(player, Dragon.consts.ConstNpc.MENU_START_COMBINE, npcSay, 
                        "Nâng cấp x1");
                } else {
                    currentNpc.createOtherMenu(player, Dragon.consts.ConstNpc.IGNORE_MENU,
                            "Cần ít nhất 99 đan và 99 công thức", "Đóng");
                }
            } else {
                Dragon.models.npc.Npc currentNpc = npc != null ? npc
                        : Dragon.services.func.CombineServiceNew.gI()
                                .getNpcByType(Dragon.services.func.CombineServiceNew.UPGRADE_TUTIEN_DAN);
                currentNpc.createOtherMenu(player, Dragon.consts.ConstNpc.IGNORE_MENU,
                        "Cần đan Tu Tiên và công thức", "Đóng");
            }
        } else {
            Dragon.models.npc.Npc currentNpc = npc != null ? npc
                    : Dragon.services.func.CombineServiceNew.gI()
                            .getNpcByType(Dragon.services.func.CombineServiceNew.UPGRADE_TUTIEN_DAN);
            currentNpc.createOtherMenu(player, Dragon.consts.ConstNpc.IGNORE_MENU,
                    "Cần chọn 2 item: Đan Tu Tiên và Công Thức", "Đóng");
        }
    }

    private int getSuccessRateForLevel(int level) {
        return Math.max(10, 100 - (level * 10)); 
    }

}
