package Dragon.jdbc.daos;

import Dragon.card.Card;
import Dragon.card.OptionCard;
import com.girlkun.database.GirlkunDB;
import com.girlkun.result.GirlkunResultSet;
import Dragon.consts.ConstPlayer;
import Dragon.data.DataGame;
import Dragon.models.Template.ArchivementTemplate;
//import Dragon.models.ThanhTich.ThanhTich;
//import Dragon.models.ThanhTich.ThanhTichPlayer;
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
                    ops.add(new OptionCard(Integer.parseInt(ob.get("id").toString()), Integer.parseInt(ob.get("param").toString()), Byte.parseByte(ob.get("active").toString())));
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
        try {
            rs = GirlkunDB.executeQuery("SELECT * FROM account WHERE username = ? AND password = ?", session.uu, session.pp);
            if (rs.first()) {
                session.userId = rs.getInt("account.id");
                session.isAdmin = rs.getBoolean("is_admin");
                session.lastTimeLogout = rs.getTimestamp("last_time_logout").getTime();
                session.lastTimeOff = rs.getTimestamp("last_time_off").getTime();
                session.actived = rs.getBoolean("active");
                session.mtvgtd = rs.getBoolean("mtvgt");
                session.vip1d = rs.getBoolean("vip1");
                session.vip2d = rs.getBoolean("vip2");
                session.vip3d = rs.getBoolean("vip3");
                session.vip4d = rs.getBoolean("vip4");
                session.vip5d = rs.getBoolean("vip5");
                session.vip6d = rs.getBoolean("vip6");
                session.tongnap = rs.getInt("tongnap");
                session.vnd = rs.getInt("vnd");
                session.mocnap = rs.getInt("mocnap");
                session.gioithieu = rs.getInt("gioithieu");
                session.goldBar = rs.getInt("account.thoi_vang");
                session.bdPlayer = rs.getDouble("account.bd_player");
                long lastTimeLogin = rs.getTimestamp("last_time_login").getTime();
                int secondsPass1 = (int) ((System.currentTimeMillis() - lastTimeLogin) / 1000);
                long lastTimeLogout = rs.getTimestamp("last_time_logout").getTime();
                int secondsPass = (int) ((System.currentTimeMillis() - lastTimeLogout) / 1000);
                if (rs.getBoolean("ban")) {
                    Service.getInstance().sendThongBaoOK(session, "Tài khoản đã bị khóa!");
                } //                else if (!session.isAdmin) {
                //                    Service.gI().sendThongBaoOK(session, "Chi danh cho admin");
                //                }
                else if (baotri && session.isAdmin) {
                    Service.getInstance().sendThongBaoOK(session, "Máy chủ đang bảo trì, vui lòng quay lại sau!");
                } else if (secondsPass1 < Manager.SECOND_WAIT_LOGIN) {
                    if (secondsPass < secondsPass1) {
                        Service.getInstance().sendThongBaoOK(session, "Vui lòng chờ " + (Manager.SECOND_WAIT_LOGIN - secondsPass) + "s");
                        return null;
                    }
                    Service.getInstance().sendThongBaoOK(session, "Vui lòng chờ " + (Manager.SECOND_WAIT_LOGIN - secondsPass1) + "s");
                    return null;
                } else if (rs.getTimestamp("last_time_login").getTime() > session.lastTimeLogout) {
                    Player plInGame = Client.gI().getPlayerByUser(session.userId);
                    if (plInGame != null) {
                        Client.gI().kickSession(plInGame.getSession());
                        Service.getInstance().sendThongBaoOK(session, "Có Người Đăng Nhập Tài Khoản?");
                    } else {
                    }
                    //Service.getInstance().sendThongBaoOK(session, "Tài khoản đang được đăng nhập tại máy chủ khác");
                } else {
                    if (secondsPass < Manager.SECOND_WAIT_LOGIN) {
                        Service.getInstance().sendThongBaoOK(session, "Vui lòng chờ " + (Manager.SECOND_WAIT_LOGIN - secondsPass) + "s");
                    } else {//set time logout trước rồi đọc data player
                        rs = GirlkunDB.executeQuery("select * from player where account_id = ? limit 1", session.userId);
                        if (!rs.first()) {
                            Service.gI().switchToCreateChar(session);
                            DataGame.sendDataItemBG(session);
                            DataGame.sendVersionGame(session);
                            DataGame.sendTileSetInfo(session);
                            Service.gI().sendMessage(session, -93, "1630679752231_-93_r");
                            DataGame.updateData(session);
                        } else {
                            Player plInGame = Client.gI().getPlayerByUser(session.userId);
                            if (plInGame != null) {
                                Client.gI().kickSession(plInGame.getSession());
                            }

                            // Sử dụng PlayerDataLoader thay thế toàn bộ duplicate code
                            player = PlayerDataLoader.loadPlayer(rs, PlayerDataLoader.LoadType.FULL_LOGIN);

                            // Thêm các logic đặc biệt cho login
                            long now = System.currentTimeMillis();
                            long thoiGianOffline = now - session.lastTimeOff;
                            player.timeoff = thoiGianOffline /= 60000;
                            player.totalPlayerViolate = 0;

                            GirlkunDB.executeUpdate("update account set last_time_login = '" + new Timestamp(System.currentTimeMillis()) + "', ip_address = '" + session.ipAddress + "' where id = " + session.userId);
                        }
                    }
                }
                al.reset();
            } else {
                Service.gI().sendThongBaoOK(session, "Thông tin tài khoản hoặc mật khẩu không chính xác");
                al.wrong();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error(session.uu);
            player.dispose();
            player = null;
            Logger.logException(GodGK.class, e);
        } finally {
            if (rs != null) {
                rs.dispose();
            }
        }
        return player;
    }

    public static void SetPlayer(Player pl) {
//        if(pl == null)
//        {
//            return;
//        }
//        if(!pl.getSession().isAdmin)
//        {  if(pl.nPoint.limitPower > 11)
//        {
//            pl.nPoint.limitPower = 11;
//        }
//        if(pl.nPoint.power > 130000000000L)
//        {
//           pl.nPoint.power = 130000000000L;
//        }
//        if(pl.nPoint.dameg > 27500)
//        {
//           pl.nPoint.power = 27500;
//        }
//        if(pl.nPoint.hpg > 630000)
//        {
//           pl.nPoint.hpg = 630000;
//        }
//        if(pl.nPoint.mpg > 630000)
//        {
//           pl.nPoint.mpg = 630000;
//        }}
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
                // Sử dụng PlayerDataLoader thay thế toàn bộ duplicate code
                player = PlayerDataLoader.loadPlayer(rs, PlayerDataLoader.LoadType.FULL_BY_ID);
            }
        } catch (Exception e) {

            player.dispose();
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
            e.printStackTrace();
            player.dispose();
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
