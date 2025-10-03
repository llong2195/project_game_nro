package Dragon.jdbc.helpers;

import com.girlkun.database.GirlkunDB;
import Dragon.utils.Logger;


public class DatabaseAutoMigration {
    
    private static boolean isInitialized = false;
    
    public enum PlayerColumn {
        // Basic info
        ACCOUNT_ID("account_id", "INT NOT NULL"),
        NAME("name", "VARCHAR(50) NOT NULL"),
        HEAD("head", "TINYINT DEFAULT 0"),
        GENDER("gender", "TINYINT DEFAULT 0"),
        HAVE_TENNIS_SPACE_SHIP("have_tennis_space_ship", "TINYINT DEFAULT 0"),
        
        // Game data - JSON fields
        DATA_INVENTORY("data_inventory", "TEXT DEFAULT '[]'"),
        DATA_LOCATION("data_location", "TEXT DEFAULT '[]'"),
        DATA_POINT("data_point", "TEXT DEFAULT '[]'"),
        DATA_MAGIC_TREE("data_magic_tree", "TEXT DEFAULT '[]'"),
        ITEMS_BODY("items_body", "TEXT DEFAULT '[]'"),
        ITEMS_BAG("items_bag", "TEXT DEFAULT '[]'"),
        ITEMS_BOX("items_box", "TEXT DEFAULT '[]'"),
        ITEMS_BOX_LUCKY_ROUND("items_box_lucky_round", "TEXT DEFAULT '[]'"),
        FRIENDS("friends", "TEXT DEFAULT '[]'"),
        ENEMIES("enemies", "TEXT DEFAULT '[]'"),
        DATA_INTRINSIC("data_intrinsic", "TEXT DEFAULT '[]'"),
        DATA_ITEM_TIME("data_item_time", "TEXT DEFAULT '[]'"),
        DATA_TASK("data_task", "TEXT DEFAULT '[]'"),
        DATA_MABU_EGG("data_mabu_egg", "TEXT DEFAULT '[]'"),
        DATA_CHARM("data_charm", "TEXT DEFAULT '[]'"),
        SKILLS("skills", "TEXT DEFAULT '[]'"),
        SKILLS_SHORTCUT("skills_shortcut", "TEXT DEFAULT '[]'"),
        PET("pet", "TEXT DEFAULT '[]'"),
        DATA_BLACK_BALL("data_black_ball", "TEXT DEFAULT '[]'"),
        DATA_SIDE_TASK("data_side_task", "TEXT DEFAULT '[]'"),
        DATA_CARD("data_card", "TEXT DEFAULT '[]'"),
        BILL_DATA("bill_data", "TEXT DEFAULT '[]'"),
        DATA_ITEM_TIME_SIEU_CAP("data_item_time_sieu_cap", "TEXT DEFAULT '[]'"),
        DATA_OFFTRAIN("data_offtrain", "TEXT DEFAULT '[]'"),
        THU_TRIEUHOI("thu_trieuhoi", "TEXT DEFAULT '[]'"),
        DATA_CAI_TRANG_SEND("data_cai_trang_send", "TEXT DEFAULT '[]'"),
        
        // Points & Stats
        POINTBOSS("pointboss", "INT DEFAULT 0"),
        DATAARCHIERMENT("dataarchierment", "TEXT DEFAULT '[\"[-1,0]\"]'"),
        RESETSKILL("resetskill", "INT DEFAULT 1"),
        POINTCAUCA("pointcauca", "INT DEFAULT 0"),
        LASTDOANHTRAI("lastdoanhtrai", "INT DEFAULT 0"),
        RUONGITEMC2("ruongitemc2", "INT DEFAULT 0"),
        CUONGNOC2("cuongnoc2", "INT DEFAULT 0"),
        BOHUYETC2("bohuyetc2", "INT DEFAULT 0"),
        BOKHIC2("bokhic2", "INT DEFAULT 0"),
        DABAOVE("dabaove", "INT DEFAULT 0"),
        DANGUSAC("dangusac", "INT DEFAULT 0"),
        DOTHANLINH("dothanlinh", "INT DEFAULT 0"),
        
        // New statistics columns
        TOTAL_KILL_MOBS("total_kill_mobs", "INT DEFAULT 0"),
        TOTAL_KILL_BOSS("total_kill_boss", "INT DEFAULT 0"),
        PVP_WINS("pvp_wins", "INT DEFAULT 0"),
        PVP_LOSSES("pvp_losses", "INT DEFAULT 0"),
        
        // Time tracking
        LAST_LOGIN_TIME("last_login_time", "BIGINT DEFAULT 0"),
        TOTAL_PLAY_TIME("total_play_time", "BIGINT DEFAULT 0");
        
        private final String columnName;
        private final String sqlDefinition;
        
        PlayerColumn(String columnName, String sqlDefinition) {
            this.columnName = columnName;
            this.sqlDefinition = sqlDefinition;
        }
        
        public String getColumnName() {
            return columnName;
        }
        
        public String getSqlDefinition() {
            return sqlDefinition;
        }
    }
    
   
    public static void initializeOnServerStart() {
        if (isInitialized) {
            return;
        }
        
        try {
            checkPlayerTable();
            isInitialized = true;
        } catch (Exception e) {
        }
    }
    
  

    private static void checkPlayerTable() {
        int createdCount = 0;
        
        for (PlayerColumn column : PlayerColumn.values()) {
            if (tryCreateColumn("player", column.getColumnName(), column.getSqlDefinition())) {
                createdCount++;
            }
        }
    }
    
 
    private static boolean tryCreateColumn(String tableName, String columnName, String definition) {
        try {
            String alterSQL = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition;
            GirlkunDB.executeUpdate(alterSQL);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public static boolean isInitialized() {
        return isInitialized;
    }
}
