package Dragon.jdbc.daos;

import Dragon.card.Card;
import Dragon.card.OptionCard;
import com.girlkun.database.GirlkunDB;
import com.girlkun.result.GirlkunResultSet;
import Dragon.consts.ConstPlayer;
import Dragon.data.DataGame;
import Dragon.models.Template.ArchivementTemplate;
import Dragon.models.clan.Clan;
import Dragon.models.clan.ClanMember;
import Dragon.models.item.Item;
import Dragon.models.item.ItemTime;
import Dragon.models.npc.specialnpc.MabuEgg;
import Dragon.models.npc.specialnpc.BillEgg;
import Dragon.models.npc.specialnpc.MagicTree;
import Dragon.models.player.Enemy;
import Dragon.models.player.Friend;
import Dragon.models.player.Fusion;
import Dragon.models.player.Pet;
import Dragon.models.player.Player;
import Dragon.models.skill.Skill;
import Dragon.models.task.TaskMain;
import com.girlkun.network.server.GirlkunSessionManager;
import com.girlkun.network.session.ISession;
import Dragon.server.Client;
import Dragon.server.Manager;
import Dragon.server.ServerManager;
import Dragon.server.ServerNotify;
import Dragon.server.io.MySession;
import Dragon.server.model.AntiLogin;
import Dragon.services.ClanService;
import Dragon.services.IntrinsicService;
import Dragon.services.ItemService;
import Dragon.services.MapService;
import Dragon.services.Service;
import Dragon.services.TaskService;
import Dragon.utils.Logger;
import Dragon.utils.SkillUtil;
import Dragon.utils.TimeUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import Dragon.utils.Util;
import java.util.Calendar;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class GodGK {

    public static List<OptionCard> loadOptionCard(JSONArray json) {
        List<OptionCard> ops = new ArrayList<>();
        try {
            for (int i = 0; i < json.size(); i++) {
                JSONObject ob = (JSONObject) json.get(i);
                if (ob != null) {
                    ops.add(new OptionCard(Integer.parseInt(ob.get("id").toString()),
                            Integer.parseInt(ob.get("param").toString()), Byte.parseByte(ob.get("active").toString())));
                }
            }
        } catch (Exception e) {

        }
        return ops;
    }

    public static Boolean baotri = false;

    public static synchronized Player login(MySession session, AntiLogin al) {
        Player player = null;
        GirlkunResultSet rs = null;
        GirlkunResultSet rsAcc = null;
        try {
            rsAcc = GirlkunDB.executeQuery(
                    "select * from account where username = ? and password = ? limit 1",
                    session.uu, session.pp);
            if (!rsAcc.first()) {
                Service.gI().sendThongBaoOK(session, "Thông tin tài khoản hoặc mật khẩu không chính xác");
                al.wrong();
                return null;
            }

            session.userId = rsAcc.getInt("id");
            try {
                session.isAdmin = rsAcc.getBoolean("is_admin") == true;
            } catch (Exception ignored) {
                session.isAdmin = false;
            }
            try {
                Timestamp lastLogout = rsAcc.getTimestamp("last_time_logout");
                Timestamp lastOff = rsAcc.getTimestamp("last_time_off");
                session.lastTimeLogout = lastLogout != null ? lastLogout.getTime()
                        : (System.currentTimeMillis() - 86400000);
                session.lastTimeOff = lastOff != null ? lastOff.getTime() : (System.currentTimeMillis() - 86400000);
            } catch (Exception ignored) {
                session.lastTimeLogout = System.currentTimeMillis() - 86400000;
                session.lastTimeOff = System.currentTimeMillis() - 86400000;
            }
            session.actived = true;
            session.mtvgtd = false;
            session.vip1d = false;
            session.vip2d = false;
            session.vip3d = false;
            session.vip4d = false;
            session.vip5d = false;
            session.vip6d = false;
            session.tongnap = 0;
            session.vnd = rsAcc.getInt("vnd");
            session.mocnap = 0;
            session.gioithieu = 0;
            session.goldBar = 0;
            session.bdPlayer = 0.0;

            al.reset();

            Player existingPlayer = Client.gI().getPlayerByUser(session.userId);
            if (existingPlayer != null) {
                Logger.log("LOGIN: User " + session.userId + " already online, kicking old session");
                existingPlayer.getSession().disconnect();
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Load player by correct account_id
            rs = GirlkunDB.executeQuery("select * from player where account_id = ? limit 1", session.userId);
            if (!rs.first()) {
                Service.gI().switchToCreateChar(session);
                DataGame.sendDataItemBG(session);
                DataGame.sendVersionGame(session);
                DataGame.sendTileSetInfo(session);
                Service.gI().sendMessage(session, -93, "1630679752231_-93_r");
                DataGame.updateData(session);
            } else {
                player = PlayerDataLoader.loadPlayer(rs, PlayerDataLoader.LoadType.FULL_LOGIN);

                long now = System.currentTimeMillis();
                long thoiGianOffline = now - session.lastTimeOff;
                player.timeoff = thoiGianOffline /= 60000;
                player.totalPlayerViolate = 0;
            }
        } catch (Exception e) {
            Logger.error("LOGIN: Failed for user=" + session.uu + " - " + e.getMessage());
            if (player != null) {
                player.dispose();
            }
            player = null;
            Logger.logException(GodGK.class, e);
        } finally {
            if (rs != null) {
                rs.dispose();
            }
            if (rsAcc != null) {
                rsAcc.dispose();
            }
        }
        return player;
    }

    // Compatibility methods for PlayerDataLoader
    public static void SetPlayer(Player pl) {
        // no-op for compatibility
    }

    public static void SetPlayer(Pet pet) {
        // no-op for compatibility
    }

    public static Player loadById(int id) {
        Player player = null;
        GirlkunResultSet rs = null;
        if (Client.gI().getPlayer(id) != null) {
            player = Client.gI().getPlayer(id);
            return player;
        }
        try {
            rs = GirlkunDB.executeQuery("select * from player where id = ? limit 1", id);
            if (rs.first()) {
                player = PlayerDataLoader.loadPlayer(rs, PlayerDataLoader.LoadType.FULL_BY_ID);
            }
        } catch (Exception e) {
            if (player != null) {
                player.dispose();
            }
            player = null;
            Logger.logException(GodGK.class, e);
        } finally {
            if (rs != null) {
                rs.dispose();
            }
        }
        return player;
    }

    public static Player loadByIdSieuHang(int id) {
        Player player = null;
        GirlkunResultSet rs = null;
        if (Client.gI().getPlayer(id) != null) {
            player = Client.gI().getPlayer(id);
            return player;
        }
        try {
            rs = GirlkunDB.executeQuery("select * from player where id = ? limit 1", id);
            if (rs.first()) {
                player = PlayerDataLoader.loadPlayer(rs, PlayerDataLoader.LoadType.SIEU_HANG_ONLY);
            }
        } catch (Exception e) {
            if (player != null) {
                player.dispose();
            }
            player = null;
            Logger.logException(GodGK.class, e);
        } finally {
            if (rs != null) {
                rs.dispose();
            }
        }
        return player;
    }
}
