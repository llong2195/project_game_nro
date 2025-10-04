package Dragon.services;

import Dragon.consts.ConstPlayer;
import Dragon.models.player.NewPet;
import Dragon.models.player.Pet;
import Dragon.models.player.Player;
import static Dragon.services.PetService.Thu_TrieuHoi;
import Dragon.services.func.ChangeMapService;
import Dragon.utils.PetFusionBonus;
import Dragon.utils.SkillUtil;
import Dragon.utils.Util;
import Dragon.De2.Thu_TrieuHoi;

public class PetService {

    private static PetService i;

    public static PetService gI() {
        if (i == null) {
            i = new PetService();
        }
        return i;
    }
    
    /**
     * Generic method to create any pet using PetBonusType enum
     * @param player The player
     * @param petType Pet type from PetBonusType enum
     * @param isChange Whether to change existing pet or create new
     * @param gender Pet gender (optional, random if not specified)
     * @param limitPower Limit power (optional)
     */
    public void createPet(Player player, PetFusionBonus.PetBonusType petType, boolean isChange, Byte gender, Byte limitPower) {
        new Thread(() -> {
            try {
                byte finalLimitPower = 1;
                
                // Handle existing pet if changing
                if (isChange && player.pet != null) {
                    finalLimitPower = player.pet.nPoint.limitPower;
                    if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
                        player.pet.unFusion();
                    }
                    ChangeMapService.gI().exitMap(player.pet);
                    player.pet.dispose();
                    player.pet = null;
                }
                
                // Override limitPower if specified
                if (limitPower != null) {
                    finalLimitPower = limitPower;
                }
                
                // Create pet based on type
                if (isVipPet(petType)) {
                    createVipPetInternal(player, petType, gender != null ? gender : (byte) Util.nextInt(0, 2));
                } else {
                    createNormalPetInternal(player, petType, gender != null ? gender : (byte) Util.nextInt(0, 2));
                }
                
                // Set limit power
                if (player.pet != null) {
                    player.pet.nPoint.limitPower = finalLimitPower;
                }
                
                Thread.sleep(1000);
                
                // Send appropriate message
                String message = getPetCreationMessage(petType);
                Service.getInstance().chatJustForMe(player, player.pet, message);
                
            } catch (Exception e) {
                // Handle exception
            }
        }).start();
    }
    
    /**
     * Check if pet type is VIP (high-tier pets)
     */
    private boolean isVipPet(PetFusionBonus.PetBonusType petType) {
        return petType == PetFusionBonus.PetBonusType.ANDROID_21 ||
               petType == PetFusionBonus.PetBonusType.FU ||
               petType == PetFusionBonus.PetBonusType.KID_BILL ||
               petType == PetFusionBonus.PetBonusType.GOKU_SSJ4;
    }
    
    /**
     * Create VIP pet with high stats
     */
    private void createVipPetInternal(Player player, PetFusionBonus.PetBonusType petType, byte gender) {
        Pet pet = new Pet(player);
        pet.name = "$" + petType.getDisplayName();
        pet.gender = gender;
        pet.id = -player.id;
        pet.nPoint.power = 1500000;
        pet.typePet = petType.getPetType();
        pet.nPoint.stamina = 1000;
        pet.nPoint.maxStamina = 1000;
        pet.nPoint.hpg = 5000;
        pet.nPoint.mpg = 5000;
        pet.nPoint.dameg = 320;
        pet.nPoint.defg = 250;
        pet.nPoint.critg = 25;
        
        for (int i = 0; i < 7; i++) {
            pet.inventory.itemsBody.add(ItemService.gI().createItemNull());
        }
        pet.playerSkill.skills.add(SkillUtil.createSkill(Util.nextInt(0, 2) * 2, 1));
        for (int i = 0; i < 4; i++) {
            pet.playerSkill.skills.add(SkillUtil.createEmptySkill());
        }
        pet.nPoint.setFullHpMp();
        player.pet = pet;
    }
    
    /**
     * Create normal pet with appropriate stats based on type
     */
    private void createNormalPetInternal(Player player, PetFusionBonus.PetBonusType petType, byte gender) {
        // Use existing createNewPet methods based on pet type
        switch (petType) {
            case NORMAL:
                createNewPet(player, false, false, false, gender);
                break;
            case MABU:
                createNewPet(player, true, false, false, gender);
                break;
            case BERUS:
                createNewPet(player, false, true, false, gender);
                break;
            case BROLY:
                createNewPet1(player, true, false, false, gender);
                break;
            case UBB:
                createNewPet1(player, false, true, false, gender);
                break;
            case XEN_CON:
                createNewPet1(player, false, false, true, gender);
                break;
            default:
                createNewPet(player, false, false, false, gender); // Fallback to normal
                break;
        }
    }
    
    /**
     * Get appropriate creation message for pet type
     */
    private String getPetCreationMessage(PetFusionBonus.PetBonusType petType) {
        switch (petType) {
            case NORMAL:
                return "Xin hãy thu nhận làm đệ tử";
            case MABU:
                return "Oa oa oa...";
            case BERUS:
            case BROLY:
            case UBB:
            case XEN_CON:
                return "Thần hủy diệt hiện thân tất cả quỳ xuống...";
            case ANDROID_21:
            case FU:
            case KID_BILL:
            case GOKU_SSJ4:
                return "Đệ tử vip vãi nồi đây...";
            default:
                return "Xin hãy thu nhận làm đệ tử";
        }
    }
    

    public void createVipPet(Player player, PetFusionBonus.PetBonusType petType, boolean isChange, byte gender) {
        byte limitPower = isChange && player.pet != null ? player.pet.nPoint.limitPower : 1;
        createPet(player, petType, isChange, gender, limitPower);
    }

    public void createAndroid21Vip(Player player, boolean isChange, byte gender) {
        createVipPet(player, PetFusionBonus.PetBonusType.ANDROID_21, isChange, gender);
    }

    public void createFuVip(Player player, boolean isChange, byte gender) {
        createVipPet(player, PetFusionBonus.PetBonusType.FU, isChange, gender);
    }

    public void createKidbillVip(Player player, boolean isChange, byte gender) {
        createVipPet(player, PetFusionBonus.PetBonusType.KID_BILL, isChange, gender);
    }

    public void createGokuSSJ4Vip(Player player, boolean isChange, byte gender) {
        createVipPet(player, PetFusionBonus.PetBonusType.GOKU_SSJ4, isChange, gender);
    }

    // Simplified pet creation methods using the generic createPet method
    public void createNormalPet(Player player, int gender, byte... limitPower) {
        byte limit = (limitPower != null && limitPower.length > 0) ? limitPower[0] : 1;
        createPet(player, PetFusionBonus.PetBonusType.NORMAL, false, (byte) gender, limit);
    }

    public void createNormalPet(Player player, byte... limitPower) {
        byte limit = (limitPower != null && limitPower.length > 0) ? limitPower[0] : 1;
        createPet(player, PetFusionBonus.PetBonusType.NORMAL, false, null, limit);
    }

    public void createMabuPet(Player player, byte... limitPower) {
        byte limit = (limitPower != null && limitPower.length > 0) ? limitPower[0] : 1;
        createPet(player, PetFusionBonus.PetBonusType.MABU, false, null, limit);
    }

    public void createMabuPet(Player player, int gender, byte... limitPower) {
        byte limit = (limitPower != null && limitPower.length > 0) ? limitPower[0] : 1;
        createPet(player, PetFusionBonus.PetBonusType.MABU, false, (byte) gender, limit);
    }

    public void createBerusPet(Player player, byte... limitPower) {
        byte limit = (limitPower != null && limitPower.length > 0) ? limitPower[0] : 1;
        createPet(player, PetFusionBonus.PetBonusType.BERUS, false, null, limit);
    }

    public void createBerusPet(Player player, int gender, byte... limitPower) {
        byte limit = (limitPower != null && limitPower.length > 0) ? limitPower[0] : 1;
        createPet(player, PetFusionBonus.PetBonusType.BERUS, false, (byte) gender, limit);
    }

    public void createBrolyPet(Player player, byte... limitPower) {
        byte limit = (limitPower != null && limitPower.length > 0) ? limitPower[0] : 1;
        createPet(player, PetFusionBonus.PetBonusType.BROLY, false, null, limit);
    }

    public void createBrolyPet(Player player, int gender, byte... limitPower) {
        byte limit = (limitPower != null && limitPower.length > 0) ? limitPower[0] : 1;
        createPet(player, PetFusionBonus.PetBonusType.BROLY, false, (byte) gender, limit);
    }

    public void creatUbbPet(Player player, byte... limitPower) {
        byte limit = (limitPower != null && limitPower.length > 0) ? limitPower[0] : 1;
        createPet(player, PetFusionBonus.PetBonusType.UBB, false, null, limit);
    }

    public void creatUbbPet(Player player, int gender, byte... limitPower) {
        byte limit = (limitPower != null && limitPower.length > 0) ? limitPower[0] : 1;
        createPet(player, PetFusionBonus.PetBonusType.UBB, false, (byte) gender, limit);
    }

    public void creatXenConPet(Player player, byte... limitPower) {
        byte limit = (limitPower != null && limitPower.length > 0) ? limitPower[0] : 1;
        createPet(player, PetFusionBonus.PetBonusType.XEN_CON, false, null, limit);
    }

    public void creatXenConPet(Player player, int gender, byte... limitPower) {
        byte limit = (limitPower != null && limitPower.length > 0) ? limitPower[0] : 1;
        createPet(player, PetFusionBonus.PetBonusType.XEN_CON, false, (byte) gender, limit);
    }

    public void createPicPet(Player player, byte... limitPower) {
        byte limit = (limitPower != null && limitPower.length > 0) ? limitPower[0] : 1;
        createPet(player, PetFusionBonus.PetBonusType.BROLY, false, null, limit); // Note: Pic uses BROLY type
    }

    public void createPicPet(Player player, int gender, byte... limitPower) {
        byte limit = (limitPower != null && limitPower.length > 0) ? limitPower[0] : 1;
        createPet(player, PetFusionBonus.PetBonusType.BROLY, false, (byte) gender, limit); // Note: Pic uses BROLY type
    }

    public void changeNormalPet(Player player, int gender) {
        createPet(player, PetFusionBonus.PetBonusType.NORMAL, true, (byte) gender, null);
    }

    public void changeNormalPet(Player player) {
        createPet(player, PetFusionBonus.PetBonusType.NORMAL, true, null, null);
    }

    public void changeMabuPet(Player player) {
        createPet(player, PetFusionBonus.PetBonusType.MABU, true, null, null);
    }

    public void changeMabuPet(Player player, int gender) {
        createPet(player, PetFusionBonus.PetBonusType.MABU, true, (byte) gender, null);
    }

    public void changeBerusPet(Player player) {
        createPet(player, PetFusionBonus.PetBonusType.BERUS, true, null, null);
    }

    public void changeBerusPet(Player player, int gender) {
        createPet(player, PetFusionBonus.PetBonusType.BERUS, true, (byte) gender, null);
    }

    public void changeBrolyPet(Player player) {
        createPet(player, PetFusionBonus.PetBonusType.BROLY, true, null, null);
    }

    public void changeBrolyPet(Player player, int gender) {
        createPet(player, PetFusionBonus.PetBonusType.BROLY, true, (byte) gender, null);
    }

    public void changeUbbPet(Player player) {
        createPet(player, PetFusionBonus.PetBonusType.UBB, true, null, null);
    }

    public void changeUbbPet(Player player, int gender) {
        createPet(player, PetFusionBonus.PetBonusType.UBB, true, (byte) gender, null);
    }

    public void changeXenConPet(Player player) {
        createPet(player, PetFusionBonus.PetBonusType.XEN_CON, true, null, null);
    }

    public void changeXenConPet(Player player, int gender) {
        createPet(player, PetFusionBonus.PetBonusType.XEN_CON, true, (byte) gender, null);
    }

    public void changePicPet(Player player) {
        createPet(player, PetFusionBonus.PetBonusType.BROLY, true, null, null); // Note: Pic uses BROLY type
    }

    public void changePicPet(Player player, int gender) {
        createPet(player, PetFusionBonus.PetBonusType.BROLY, true, (byte) gender, null); // Note: Pic uses BROLY type
    }

    public void changeNamePet(Player player, String name) {
        try {
            if (!InventoryServiceNew.gI().isExistItemBag(player, 400)) {
                Service.getInstance().sendThongBao(player, "Bạn Cần Thẻ Đặt Tên Đệ Tử, Mua Tại Bardock");
                return;
            } else if (Util.haveSpecialCharacter(name)) {
                Service.getInstance().sendThongBao(player, "Tên Không Được Chứa Ký Tự Đặc Biệt");
                return;
            } else if (name.length() > 10) {
                Service.getInstance().sendThongBao(player, "Tên Quá Dài");
                return;
            }
            ChangeMapService.gI().exitMap(player.pet);
            player.pet.name = "$" + name.toLowerCase().trim();
            InventoryServiceNew.gI().subQuantityItemsBag(player, InventoryServiceNew.gI().findItemBag(player, 400), 1);
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    Service.getInstance().chatJustForMe(player, player.pet, "Cảm Ơn Sư Phụ Đã Đặt Cho Con Tên " + name);
                } catch (Exception e) {

                }
            }).start();
        } catch (Exception ex) {

        }
    }

    private int[] getDataPetNormal() {
        long[] hpmp = {1700, 1800, 1900, 2000, 2100, 2200};
        int[] petData = new int[5];
        petData[0] = Util.nextInt(40, 105) * 20; //hp
        petData[1] = Util.nextInt(40, 105) * 20; //mp
        petData[2] = Util.nextInt(20, 45); //dame
        petData[3] = Util.nextInt(9, 50); //def
        petData[4] = Util.nextInt(0, 2); //crit
        return petData;
    }

    private int[] getDataPetMabu() {
        long[] hpmp = {1700, 1800, 1900, 2000, 2100, 2200};
        int[] petData = new int[5];
        petData[0] = Util.nextInt(40, 105) * 20; // hp
        petData[1] = Util.nextInt(40, 105) * 20; // mp
        petData[2] = Util.nextInt(50, 120); // dame
        petData[3] = Util.nextInt(9, 50); // def
        petData[4] = Util.nextInt(0, 2); // crit
        return petData;
    }

    private int[] getDataPetBerus() {
        long[] hpmp = {1700, 1800, 1900, 2000, 2100, 2200};
        int[] petData = new int[5];
        petData[0] = Util.nextInt(40, 110) * 20; //hp
        petData[1] = Util.nextInt(40, 110) * 20; //mp
        petData[2] = Util.nextInt(50, 130); //dame
        petData[3] = Util.nextInt(9, 50); //def
        petData[4] = Util.nextInt(0, 2); //crit
        return petData;
    }

    private int[] getDataPetPic() {
        long[] hpmp = {2000, 2100, 2200, 2300, 2400, 2500};
        int[] petData = new int[5];
        petData[0] = Util.nextInt(40, 125) * 20; //hp
        petData[1] = Util.nextInt(40, 125) * 20; //mp
        petData[2] = Util.nextInt(80, 160); //dame
        petData[3] = Util.nextInt(10, 60); //def
        petData[4] = Util.nextInt(2, 5); //crit
        return petData;
    }

    private int[] getDataXencon() {
        long[] hpmp = {2000, 2100, 2200, 2300, 2400, 2500};
        int[] petData = new int[5];
        petData[0] = Util.nextInt(40, 125) * 20; //hp
        petData[1] = Util.nextInt(40, 125) * 20; //mp
        petData[2] = Util.nextInt(80, 160); //dame
        petData[3] = Util.nextInt(10, 60); //def
        petData[4] = Util.nextInt(2, 5); //crit
        return petData;
    }

    private int[] getDataPetKaido() {
        int[] hpmp = {2000, 2100, 2200, 2300, 2400, 2500};
        int[] petData = new int[5];
        petData[0] = Util.nextInt(40, 115) * 20; //hp
        petData[1] = Util.nextInt(40, 115) * 20; //mp
        petData[2] = Util.nextInt(70, 140); //dame
        petData[3] = Util.nextInt(9, 50); //def
        petData[4] = Util.nextInt(0, 2); //crit
        return petData;
    }

    private void createNewPet(Player player, boolean isMabu, boolean isBerus, boolean isPic,
            byte... gender) {
        int[] data = isBerus ? isMabu ? isPic ? getDataPetBerus() : getDataPetMabu() : getDataPetPic()
                : getDataPetNormal();
        Pet pet = new Pet(player);
        pet.name = "$" + (isMabu ? "Mabư" : isBerus ? "Berus" : isPic ? "Pic" : "Đệ tử");
        pet.gender = (gender != null && gender.length != 0) ? gender[0] : (byte) Util.nextInt(0, 2);
        pet.id = -player.id;
        pet.nPoint.power = isMabu || isBerus || isPic ? 1500000 : 2000;
        pet.typePet = (byte) (isMabu ? ConstPlayer.PET_MABU : isBerus ? ConstPlayer.PET_BERUS : isPic ? ConstPlayer.PET_BROLY : ConstPlayer.PET_NORMAL);
        pet.nPoint.stamina = 1000;
        pet.nPoint.maxStamina = 1000;
        pet.nPoint.hpg = data[0];
        pet.nPoint.mpg = data[1];
        pet.nPoint.dameg = data[2];
        pet.nPoint.defg = data[3];
        pet.nPoint.critg = data[4];
        for (int i = 0; i < 7; i++) {
            pet.inventory.itemsBody.add(ItemService.gI().createItemNull());
        }
        pet.playerSkill.skills.add(SkillUtil.createSkill(Util.nextInt(0, 2) * 2, 1));
        for (int i = 0; i < 4; i++) {
            pet.playerSkill.skills.add(SkillUtil.createEmptySkill());
        }
        pet.nPoint.setFullHpMp();
        player.pet = pet;
    }

    private void createNewPet1(Player player, boolean isBroly, boolean isUbb, boolean isXencon, byte... gender) {
        int[] data = isBroly ? isUbb ? isXencon ? getDataPetKaido() : getDataPetPic() : getDataPetBerus() : getDataPetNormal();
        Pet pet = new Pet(player);
        pet.name = "$" + (isBroly ? "Broly" : isUbb ? "Ubb" : isXencon ? "Xên Con" : "Đệ tử");
        pet.gender = (gender != null && gender.length != 0) ? gender[0] : (byte) Util.nextInt(0, 2);
        pet.id = -player.id;
        pet.nPoint.power = isBroly || isUbb || isXencon ? 1500000 : 2000;
        pet.typePet = (byte) (isBroly ? ConstPlayer.PET_BROLY : isUbb ? ConstPlayer.PET_UBB : isXencon ? ConstPlayer.PET_XEN_CON : ConstPlayer.PET_NORMAL);
        pet.nPoint.stamina = 1000;
        pet.nPoint.maxStamina = 1000;
        pet.nPoint.hpg = data[0];
        pet.nPoint.mpg = data[1];
        pet.nPoint.dameg = data[2];
        pet.nPoint.defg = data[3];
        pet.nPoint.critg = data[4];
        for (int i = 0; i < 7; i++) {
            pet.inventory.itemsBody.add(ItemService.gI().createItemNull());
        }
        pet.playerSkill.skills.add(SkillUtil.createSkill(Util.nextInt(0, 2) * 2, 1));
        for (int i = 0; i < 3; i++) {
            pet.playerSkill.skills.add(SkillUtil.createEmptySkill());
        }
        pet.nPoint.setFullHpMp();
        player.pet = pet;
    }

    public static void Pet2(Player pl, int h, int b, int l) {
        if (pl.newpet != null) {
            pl.newpet.dispose();
        }
        pl.newpet = new NewPet(pl, (short) h, (short) b, (short) l);
        pl.newpet.name = pl.inventory.itemsBody.get(7).template.name;
        pl.newpet.gender = pl.gender;
        pl.newpet.nPoint.tiemNang = 1;
        pl.newpet.nPoint.power = 80000000000L;
        pl.newpet.nPoint.limitPower = 10;
        pl.newpet.nPoint.hpg = 5000;
        pl.newpet.nPoint.mpg = 5000;
        pl.newpet.nPoint.hp = 5000;
        pl.newpet.nPoint.mp = 5000;
        pl.newpet.nPoint.dameg = 1;
        pl.newpet.nPoint.defg = 1;
        pl.newpet.nPoint.critg = 1;
        pl.newpet.nPoint.stamina = 1;
        pl.newpet.nPoint.setBasePoint();
        pl.newpet.nPoint.setFullHpMp();
    }

    public static void Thu_TrieuHoi(Player pl) {
        if (pl.TrieuHoipet != null) {
            pl.TrieuHoipet.dispose();
        }
        pl.TrieuHoipet = new Thu_TrieuHoi(pl);
        pl.TrieuHoipet.name = "$" + "[" + pl.NameThanthu(pl.CapBacThan) + "] " + pl.TenThan;
        pl.TrieuHoipet.gender = pl.gender;
        pl.TrieuHoipet.nPoint.tiemNang = 1;
        pl.TrieuHoipet.nPoint.power = 1;
        pl.TrieuHoipet.nPoint.limitPower = 1;
        pl.TrieuHoipet.nPoint.hpg = pl.MauThan;
        pl.TrieuHoipet.nPoint.mpg = 500000000;
        pl.TrieuHoipet.nPoint.hp = pl.MauThan;
        pl.TrieuHoipet.nPoint.mp = 500000000;
        pl.TrieuHoipet.nPoint.dameg = pl.DameThan;
        pl.TrieuHoipet.nPoint.defg = 1;
        pl.TrieuHoipet.nPoint.critg = 1;
        pl.TrieuHoipet.nPoint.stamina = 10000;
        pl.TrieuHoipet.nPoint.maxStamina = 10000;
        pl.TrieuHoipet.playerSkill.skills.add(SkillUtil.createSkill(17, 7));
        pl.TrieuHoipet.nPoint.setBasePoint();
        pl.TrieuHoipet.nPoint.setFullHpMp();
    }

    //--------------------------------------------------------------------------
}
