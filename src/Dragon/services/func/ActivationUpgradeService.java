package Dragon.services.func;

import Dragon.consts.ConstNpc;
import Dragon.models.item.Item;
import Dragon.models.item.Item.ItemOption;
import Dragon.models.npc.Npc;
import Dragon.models.player.Player;
import Dragon.models.player.SetClothes;
import Dragon.server.Manager;
import Dragon.services.InventoryServiceNew;
import Dragon.services.Service;
import Dragon.services.func.UpdateItem;
import Dragon.services.EffectSkillService;
import Dragon.utils.Util;


public class ActivationUpgradeService {
    
    private static ActivationUpgradeService instance;

    public static final int[][] SET_KICH_HOAT = {
        {127,141},{128,140},{129,139}, // Trái Đất
        {130,142},{131,143},{132,144}, // Namek 
        {133,136},{134,137},{135,138}, // Xayda
    };

    public static ActivationUpgradeService gI() {
        if (instance == null) {
            instance = new ActivationUpgradeService();
        }
        return instance;
    }

    public void ShowMenuSetKichHoat(Player player, Npc npc) {
        if (player.combineNew.itemsCombine.size() == 1) {
            Item item = player.combineNew.itemsCombine.get(0);
            
            if (player.point_kill_mobs < 99) {
                npc.createOtherMenu(player, ConstNpc.IGNORE_MENU, 
                        "Bạn cần ít nhất 99 điểm giết quái để kích hoạt SKH\n" +
                        "Hiện tại bạn có: " + player.point_kill_mobs + " điểm", 
                        "Đóng");
                return;
            }
            
            if (item == null || !item.isNotNullItem()) {
                npc.createOtherMenu(player, ConstNpc.IGNORE_MENU, 
                        "Item không hợp lệ để kích hoạt SKH", 
                        "Đóng");
                return;
            }
            
            if (item.template.type >= 5) {
                npc.createOtherMenu(player, ConstNpc.IGNORE_MENU, 
                        "Chỉ có thể kích hoạt SKH cho trang bị\n" +
                        "(Áo, Quần, Giày, Găng, Rada)", 
                        "Đóng");
                return;
            }
            
            // Kiểm tra item đã có set kích hoạt chưa
            if (hasActivationSet(item)) {
                npc.createOtherMenu(player, ConstNpc.IGNORE_MENU, 
                        "Trang bị này đã được kích hoạt SKH rồi!\n" +
                        "Không thể kích hoạt thêm lần nữa.", 
                        "Đóng");
                return;
            }
            String npcSay = "\n|2| " + item.template.name + "\n";
            npcSay += "\n|7|Chọn loại Set Kích Hoạt cho trang bị này";
            npcSay += "\n|7|Chi phí: 99 điểm giết quái";
            npcSay += "\n|1|Bạn hiện có: " + player.point_kill_mobs + " điểm\n";
            String[] setNames = getSetNamesByGender(player.gender);
            npc.createOtherMenu(player, 214, npcSay, setNames);
        } else {
            npc.createOtherMenu(player, ConstNpc.IGNORE_MENU, 
                    "Hãy đưa cho ta 1 trang bị để kích hoạt SKH", 
                    "Đóng");
        }
    }
    
    
    private String[] getSetNamesByGender(byte gender) {
        String[] setNames = new String[4]; // 3 sets + Đóng
        
        // Lấy tên từ option templates
        for (int i = 0; i < 3; i++) {
            int[] setOptions = getSetOptions(gender, i);
            if (setOptions != null && setOptions.length >= 1) {
                // Lấy tên từ option template đầu tiên của set
                String optionName = getOptionTemplateName(setOptions[0]);
                setNames[i] = optionName != null ? optionName : ("Set " + (i + 1));
            } else {
                setNames[i] = "Set " + (i + 1);
            }
        }
        setNames[3] = "Đóng";
        
        return setNames;
    }
    
    /**
     * Lấy tên option từ template
     */
    private String getOptionTemplateName(int optionId) {
        try {
            return Manager.ITEM_OPTION_TEMPLATES.get(optionId).name;
        } catch (Exception e) {
            return null;
        }
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
    
 
    public void processSetActivation(Player player, int setIndex) {
        if (player.combineNew.itemsCombine.size() != 1) {
            Service.gI().sendThongBao(player, "Hãy đặt đúng 1 trang bị để kích hoạt");
            return;
        }
        
        if (player.point_kill_mobs < 99) {
            Service.gI().sendThongBao(player, "Không đủ điểm giết quái (cần 99 điểm)");
            return;
        }
        
        Item item = player.combineNew.itemsCombine.get(0);
        
        // Kiểm tra chỉ áp dụng cho trang bị body (type < 5)
        if (item.template.type >= 5) {
            Service.gI().sendThongBao(player, "Chỉ có thể kích hoạt SKH cho trang bị!");
            return;
        }
        
        // Kiểm tra lại item đã có set kích hoạt chưa
        if (hasActivationSet(item)) {
            Service.gI().sendThongBao(player, "Trang bị này đã được kích hoạt SKH rồi!");
            return;
        }
        
        // Trừ điểm trước khi thực hiện
        player.point_kill_mobs -= 99;
        
        // Hiệu ứng kích hoạt
        EffectSkillService.gI().sendEffectPlayer(player, player, (byte) 1, (byte) 21); // Effect kích hoạt
        
        if (Util.isTrue(50, 100)) {
            // Thành công
            addSKHOptionToItem(player, item, setIndex);
            InventoryServiceNew.gI().sendItemBags(player);
            String setName = getSetName(player.gender, setIndex);
            Service.gI().sendThongBao(player, "Thành công! Đã kích hoạt " + setName + " cho " + item.template.name);
            CombineServiceNew.gI().sendEffectSuccessCombine(player); // Effect thành công
        } else {
            // Thất bại
            Service.gI().sendThongBao(player, "Thất bại! Kích hoạt SKH không thành công. Vật phẩm không bị mất.");
            CombineServiceNew.gI().sendEffectFailCombine(player); // Effect thất bại
        }
        
        CombineServiceNew.gI().reOpenItemCombine(player);
    }
    private void addSKHOptionToItem(Player player, Item item, int setIndex) {
        byte gender = player.gender;
        
        int[] setOptions = getSetOptions(gender, setIndex);
        
        if (setOptions != null && setOptions.length == 2) {
            int setOptionId = setOptions[0];    // ID option set (127-135)
            int bonusOptionId = setOptions[1];  // ID option bonus (136-144)
            int setParam = getDefaultSetParam(setOptionId);
            int bonusParam = getDefaultBonusParam(bonusOptionId);
            
            item.itemOptions.add(new ItemOption(setOptionId, setParam));
            item.itemOptions.add(new ItemOption(bonusOptionId, bonusParam));
        }
    }
    
    private int getDefaultSetParam(int optionId) {
        return 1;
    }
    private int getDefaultBonusParam(int optionId) {
        switch (optionId) {
            case 136: return SetClothes.BONUS_DAMAGE_KAKAROT;       // Kakarot
            case 137: return SetClothes.BONUS_DAMAGE_CADIC;         // Cadic  
            case 138: return SetClothes.BONUS_DAMAGE_NAPPA;         // Nappa
            case 139: return SetClothes.BONUS_STUN_THIENXINHANG;    // Thiên Tâm Hàng
            case 140: return SetClothes.BONUS_DAMAGE_KIRIN;         // Kirin
            case 141: return SetClothes.BONUS_DAMAGE_SONGOKU;       // Songoku
            case 142: return SetClothes.BONUS_DAMAGE_PICOLO;        // Picolo
            case 143: return SetClothes.BONUS_DAMAGE_OCTIEU;        // Ốc Tiêu
            case 144: return SetClothes.BONUS_DAMAGE_PIKKORO_DAIMAO; // Pikkoro Đại Ma Vương
            default: return 100; // fallback
        }
    }
    
    private int[] getSetOptions(byte gender, int setIndex) {
        if (setIndex < 0 || setIndex >= 3) return null;
        
        switch (gender) {
            case 0: // Trái Đất
                return SET_KICH_HOAT[setIndex];
            case 1: // Namek  
                return SET_KICH_HOAT[setIndex + 3];
            case 2: // Xayda
                return SET_KICH_HOAT[setIndex + 6];
            default:
                return null;
        }
    }
    
    /**
     * Lấy tên set
     */
    private String getSetName(byte gender, int setIndex) {
        String genderName = "";
        switch (gender) {
            case 0: genderName = "Trái Đất"; break;
            case 1: genderName = "Namek"; break;
            case 2: genderName = "Xayda"; break;
        }
        return "Set Kích Hoạt " + (setIndex + 1) + " (" + genderName + ")";
    }
}
