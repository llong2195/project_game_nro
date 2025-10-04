package Dragon.utils;

import Dragon.consts.ConstPlayer;
import Dragon.models.player.Pet;
import Dragon.models.player.Player;

public class PetFusionBonus {
    
    
    /**
     * Pet Fusion Bonus Enum - Type Safe and Easy to Read
     * All fusion types (PORATA, PORATA2, PORATA3, PORATA4) use the same bonus percentages
     */
    public enum PetBonusType {
        NORMAL      (ConstPlayer.PET_NORMAL,     0,  "Đệ tử thường"),
        MABU        (ConstPlayer.PET_MABU,       5,  "Mabư"),
        BERUS       (ConstPlayer.PET_BERUS,      10, "Berus"),
        BROLY       (ConstPlayer.PET_BROLY,      13, "Broly"),
        UBB         (ConstPlayer.PET_UBB,        15, "Ubb"),
        XEN_CON     (ConstPlayer.PET_XEN_CON,    18, "Xên Con"),
        ANDROID_21  (ConstPlayer.PET_ANDROID_21, 20, "Android 21 Majin Form"),
        FU          (ConstPlayer.PET_FU,         25, "Fu"),
        KID_BILL    (ConstPlayer.PET_KID_BILL,   30, "Kid Bill"),
        GOKU_SSJ4   (ConstPlayer.PET_GOKU_SSJ4,  40, "Goku SSJ4 - Strongest!");
        
        private final byte petType;
        private final int bonusPercent;
        private final String displayName;
        
        PetBonusType(byte petType, int bonusPercent, String displayName) {
            this.petType = petType;
            this.bonusPercent = bonusPercent;
            this.displayName = displayName;
        }
        
        public byte getPetType() { return petType; }
        public int getBonusPercent() { return bonusPercent; }
        public String getDisplayName() { return displayName; }
        
        /**
         * Get PetBonusType by pet type constant
         */
        public static PetBonusType getByPetType(byte petType) {
            for (PetBonusType type : values()) {
                if (type.petType == petType) {
                    return type;
                }
            }
            return NORMAL; // Default fallback
        }
    }
    
    /**
     * Checks if the fusion type is supported
     * @param fusionType Fusion type constant
     * @return true if supported, false otherwise
     */
    private static boolean isSupportedFusionType(byte fusionType) {
        return fusionType == ConstPlayer.HOP_THE_PORATA ||
               fusionType == ConstPlayer.HOP_THE_PORATA2 ||
               fusionType == ConstPlayer.HOP_THE_PORATA3 ||
               fusionType == ConstPlayer.HOP_THE_PORATA4;
    }
    
    /**
     * Gets the bonus percentage for a specific pet type and fusion type
     * @param petType Pet type constant
     * @param fusionType Fusion type constant
     * @return Bonus percentage (0 if no bonus)
     */
    private static int getBonusPercent(byte petType, byte fusionType) {
        // Check if fusion type is supported
        if (!isSupportedFusionType(fusionType)) {
            return 0;
        }
        
        // Get bonus from enum
        PetBonusType bonusType = PetBonusType.getByPetType(petType);
        return bonusType.getBonusPercent();
    }
    
    /**
     * Calculates and applies HP bonus for pet based on fusion type
     * @param player The player (must be a pet)
     * @param currentHpMax Current HP max value
     * @return New HP max value with bonus applied
     */
    public static double applyHpFusionBonus(Player player, double currentHpMax) {
        if (!player.isPet) {
            return currentHpMax;
        }
        
        Pet pet = (Pet) player;
        int bonusPercent = getBonusPercent(pet.typePet, pet.master.fusion.typeFusion);
        
        if (bonusPercent > 0) {
            return currentHpMax + (currentHpMax * bonusPercent / 100.0);
        }
        
        return currentHpMax;
    }
    
    /**
     * Calculates and applies MP bonus for pet based on fusion type
     * @param player The player (must be a pet)
     * @param currentMpMax Current MP max value
     * @return New MP max value with bonus applied
     */
    public static double applyMpFusionBonus(Player player, double currentMpMax) {
        if (!player.isPet) {
            return currentMpMax;
        }
        
        Pet pet = (Pet) player;
        int bonusPercent = getBonusPercent(pet.typePet, pet.master.fusion.typeFusion);
        
        if (bonusPercent > 0) {
            return currentMpMax + (currentMpMax * bonusPercent / 100.0);
        }
        
        return currentMpMax;
    }
    
    /**
     * Calculates and applies Damage bonus for pet based on fusion type
     * @param player The player (must be a pet)
     * @param currentDamage Current damage value
     * @return New damage value with bonus applied
     */
    public static double applyDamageFusionBonus(Player player, double currentDamage) {
        if (!player.isPet) {
            return currentDamage;
        }
        
        Pet pet = (Pet) player;
        int bonusPercent = getBonusPercent(pet.typePet, pet.master.fusion.typeFusion);
        
        if (bonusPercent > 0) {
            return currentDamage + (currentDamage * bonusPercent / 100.0);
        }
        
        return currentDamage;
    }
    
    /**
     * Gets the HP bonus percentage for a specific pet type and fusion type
     * @param petType Pet type constant
     * @param fusionType Fusion type constant
     * @return Bonus percentage (0 if no bonus)
     */
    public static int getHpBonusPercent(byte petType, byte fusionType) {
        return getBonusPercent(petType, fusionType);
    }
    
    /**
     * Gets the MP bonus percentage for a specific pet type and fusion type
     * @param petType Pet type constant
     * @param fusionType Fusion type constant
     * @return Bonus percentage (0 if no bonus)
     */
    public static int getMpBonusPercent(byte petType, byte fusionType) {
        return getBonusPercent(petType, fusionType);
    }
    
    /**
     * Gets the Damage bonus percentage for a specific pet type and fusion type
     * @param petType Pet type constant
     * @param fusionType Fusion type constant
     * @return Bonus percentage (0 if no bonus)
     */
    public static int getDamageBonusPercent(byte petType, byte fusionType) {
        return getBonusPercent(petType, fusionType);
    }
}
