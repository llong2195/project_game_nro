package Dragon.jdbc.daos;

import Dragon.models.boss.Boss;
import Dragon.models.map.ItemMap;
import Dragon.models.player.Player;
import Dragon.utils.Logger;
import com.girlkun.database.GirlkunDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * BossRewardService - Service để xử lý reward khi giết boss
 * Thay thế cho việc hardcode reward trong từng boss class
 */
public class BossRewardService {

    private static BossRewardService instance;

    public static BossRewardService getInstance() {
        if (instance == null) {
            instance = new BossRewardService();
        }
        return instance;
    }

    /**
     * Process rewards khi boss bị giết
     */
    public void processRewards(Boss boss, Player plKill) {
        List<BossReward> rewards = getBossRewards((int) boss.id);

        for (BossReward reward : rewards) {
            if (shouldDropReward(reward)) {
                dropReward(boss, plKill, reward);
            }
        }
    }

    /**
     * Get rewards của boss từ database
     */
    private List<BossReward> getBossRewards(int bossId) {
        List<BossReward> rewards = new ArrayList<>();
        Connection con = null;

        try {
            con = GirlkunDB.getConnection();
            PreparedStatement ps = con.prepareStatement(
                    "SELECT item_id, quantity, drop_rate FROM boss_rewards WHERE boss_id = ? ORDER BY drop_rate DESC");
            ps.setInt(1, bossId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                BossReward reward = new BossReward();
                reward.itemId = rs.getShort("item_id");
                reward.quantity = rs.getInt("quantity");
                reward.dropRate = rs.getDouble("drop_rate");
                rewards.add(reward);
            }

            rs.close();
            ps.close();

        } catch (Exception e) {
            Logger.logException(BossRewardService.class, e);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    Logger.logException(BossRewardService.class, e);
                }
            }
        }

        return rewards;
    }

    /**
     * Check xem có nên drop reward không
     */
    private boolean shouldDropReward(BossReward reward) {
        return Dragon.utils.Util.isTrue((int) reward.dropRate, 100);
    }

    /**
     * Drop reward xuống map
     */
    private void dropReward(Boss boss, Player plKill, BossReward reward) {
        // Logic drop item xuống map
        Dragon.services.Service.gI().dropItemMap(
                boss.zone,
                new ItemMap(
                        boss.zone,
                        reward.itemId,
                        reward.quantity,
                        boss.location.x,
                        boss.location.y,
                        plKill.id));
    }

    /**
     * Inner class để chứa reward data
     */
    private static class BossReward {
        short itemId;
        int quantity;
        double dropRate;
    }
}
