package Dragon.services;

import Dragon.consts.ConstMap;
import Dragon.models.boss.Boss;
import Dragon.models.map.Map;
import Dragon.models.map.WayPoint;
import Dragon.models.map.Zone;
import Dragon.models.map.blackball.BlackBallWar;
import Dragon.models.map.doanhtrai.DoanhTraiService;
import Dragon.models.map.gas.GasService;
import Dragon.models.mob.Mob;
import Dragon.models.player.Pet;
import Dragon.models.player.Player;
import Dragon.server.Manager;
import com.girlkun.network.io.Message;
import Dragon.services.func.ChangeMapService;
import Dragon.utils.Logger;
import Dragon.utils.Util;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import com.girlkun.database.GirlkunDB;
import Dragon.models.Template.MapTemplate;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import Dragon.server.Client;
import Dragon.services.Service;

public class MapService {

    private static MapService i;

    public static MapService gI() {
        if (i == null) {
            i = new MapService();
        }
        return i;
    }

    public WayPoint getWaypointPlayerIn(Player player) {
        for (WayPoint wp : player.zone.map.wayPoints) {
            if (player.location.x >= wp.minX && player.location.x <= wp.maxX && player.location.y >= wp.minY && player.location.y <= wp.maxY) {
                return wp;
            }
        }
        return null;
    }

    /**
     * @param tileTypeFocus tile type: top, bot, left, right...
     * @return [tileMapId][tileType]
     */
    public int[][] readTileIndexTileType(int tileTypeFocus) {
        int[][] tileIndexTileType = null;
        try {
            DataInputStream dis = new DataInputStream(new FileInputStream("data/girlkun/map/tile_set_info"));
            int numTileMap = dis.readByte();
            tileIndexTileType = new int[numTileMap][];
            for (int i = 0; i < numTileMap; i++) {
                int numTileOfMap = dis.readByte();
                for (int j = 0; j < numTileOfMap; j++) {
                    int tileType = dis.readInt();
                    int numIndex = dis.readByte();
                    if (tileType == tileTypeFocus) {
                        tileIndexTileType[i] = new int[numIndex];
                    }
                    for (int k = 0; k < numIndex; k++) {
                        int typeIndex = dis.readByte();
                        if (tileType == tileTypeFocus) {
                            tileIndexTileType[i][k] = typeIndex;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.logException(MapService.class, e);
        }
        return tileIndexTileType;
    }

    //tilemap for paint
    public int[][] readTileMap(int mapId) {
        int[][] tileMap = null;
        try {
            DataInputStream dis = new DataInputStream(new FileInputStream("data/girlkun/map/tile_map_data/" + mapId));
            dis.readByte();
            int w = dis.readByte();
            int h = dis.readByte();
            tileMap = new int[h][w];
            for (int i = 0; i < tileMap.length; i++) {
                for (int j = 0; j < tileMap[i].length; j++) {
                    tileMap[i][j] = dis.readByte();
                }
            }
            dis.close();
        } catch (Exception e) {

        }
        return tileMap;
    }

    public Zone getMapCanJoin(Player player, int mapId, int zoneId) {
        if (isMapOffline(mapId)) {
            return getMapById(mapId).zones.get(0);
        }
        if (this.isMapKhiGas(mapId)) {
            if (player.clan == null || player.clan.khiGas == null) {
                return null;
            }
            if (this.isMapKhiGas(player.zone.map.mapId)) {
                for (Mob mob : player.zone.mobs) {
                    if (!mob.isDie()) {
                        return null;
                    }
                }

            }
            return player.clan.khiGas.getMapById(mapId);
        }
        if (this.isMapDoanhTrai(mapId)) {
            if (player.clan == null || player.clan.doanhTrai == null) {
                return null;
            }
            if (this.isMapDoanhTrai(player.zone.map.mapId)) {
                for (Mob mob : player.zone.mobs) {
                    if (!mob.isDie()) {
                        return null;
                    }
                }
                for (Player boss : player.zone.getBosses()) {
                    if (!boss.isDie()) {
                        return null;
                    }
                }
            }
            /**
             * Qua map mới thì làm mới lại mob
             */
            if (this.isMapDoanhTrai(mapId)) {
                if (player.clan == null || player.clan.doanhTrai == null) {
                    return null;
                }
                if (this.isMapDoanhTrai(player.zone.map.mapId)) {
                    for (Mob mob : player.zone.mobs) {
                        if (!mob.isDie()) {
                            return null;
                        }
                    }
                    for (Player boss : player.zone.getBosses()) {
                        if (!boss.isDie()) {
                            return null;
                        }
                    }
                }
                return player.clan.doanhTrai.getMapById(mapId);
            }
        }
        if (this.isMapBanDoKhoBau(mapId)) {
            if (player.clan == null || player.clan.BanDoKhoBau == null) {
                return null;
            }
            if (this.isMapBanDoKhoBau(player.zone.map.mapId)) {
                for (Mob mob : player.zone.mobs) {
                    if (!mob.isDie()) {
                        return null;
                    }
                }
                for (Player boss : player.zone.getBosses()) {
                    if (!boss.isDie()) {
                        return null;
                    }
                }
            }
            /**
             * Qua map mới thì làm mới lại mob
             */
//            if (player.clan.BanDoKhoBau.getListMap().indexOf(mapId) > player.clan.BanDoKhoBau.getCurrentIndexMap()) {
//                player.clan.BanDoKhoBau.setCurrentIndexMap(player.clan.BanDoKhoBau.getListMap().indexOf(mapId));
//                player.clan.BanDoKhoBau.init();
//
//            }
            return player.clan.BanDoKhoBau.getMapById(mapId);
        }

        //**********************************************************************
        if (zoneId == -1) { //vào khu bất kỳ
            return getZone(mapId);
        } else {
            return getZoneByMapIDAndZoneID(mapId, zoneId);
        }
    }

    public Zone getZone(int mapId) {
        Map map = getMapById(mapId);
        int z = 0;
        try {
            if (map == null) {
                return null;
            }
            while (map.zones.get(z).getNumOfPlayers() >= map.zones.get(z).maxPlayer) {
                z++;
            }
        } catch (Exception e) {
            Logger.logException(MapService.class, e);
        }
        return map.zones.get(z);
    }

    private Zone getZoneByMapIDAndZoneID(int mapId, int zoneId) {
        Zone zoneJoin = null;
        try {
            Map map = getMapById(mapId);
            if (map != null) {
                zoneJoin = map.zones.get(zoneId);
            }
        } catch (Exception e) {
            Logger.logException(MapService.class, e);
        }
        return zoneJoin;
    }

    public Map getMapById(int mapId) {
        for (Map map : Manager.MAPS) {
            if (map.mapId == mapId) {
                return map;
            }
        }
        return null;
    }

    public Map getMapForCalich() {
        int mapId = Util.nextInt(27, 29);
        return MapService.gI().getMapById(mapId);
    }

    /**
     * Trả về 1 map random cho boss
     */
    public Zone getMapWithRandZone(int mapId) {
        Map map = MapService.gI().getMapById(mapId);
        Zone zone = null;
        try {
            if (map != null) {
                zone = map.zones.get(Util.nextInt(0, map.zones.size() - 1));
            }
        } catch (Exception e) {

        }
        return zone;
    }

    public String getPlanetName(byte planetId) {
        switch (planetId) {
            case 0:
                return "Trái đất";
            case 1:
                return "Namếc";
            case 2:
                return "Xayda";
            default:
                return "";
        }
    }

    public int getMapTrainOff(Player pl) {
        switch (pl.typetrain) {
            case 0:
                return 46;
            case 1:
            case 2:
                return 29;
            case 3:
            case 4:
                return 48;
            case 5:
                return 50;
            case 6:
            case 7:
                return 154;
            default:
                return 0;
        }
    }

    /**
     * lấy danh sách map cho capsule
     */
    public List<Zone> getMapCapsule(Player pl) {
        List<Zone> list = new ArrayList<>();
        if (pl.mapBeforeCapsule != null && pl.mapBeforeCapsule.map.mapId != 2) {
            addListMapCapsule(pl, list, pl.mapBeforeCapsule);
        }
        addListMapCapsule(pl, list, getMapCanJoin(pl, 2, 0));
        addListMapCapsule(pl, list, getMapCanJoin(pl, 4, 0));
        addListMapCapsule(pl, list, getMapCanJoin(pl, 5, 0));
        addListMapCapsule(pl, list, getMapCanJoin(pl, 6, 0));
        addListMapCapsule(pl, list, getMapCanJoin(pl, 7, 0));
        addListMapCapsule(pl, list, getMapCanJoin(pl, 8, 0));
        addListMapCapsule(pl, list, getMapCanJoin(pl, 9, 0));
        addListMapCapsule(pl, list, getMapCanJoin(pl, 13, 0));
        addListMapCapsule(pl, list, getMapCanJoin(pl, 14, 0));
        addListMapCapsule(pl, list, getMapCanJoin(pl, 13, 0));
        addListMapCapsule(pl, list, getMapCanJoin(pl, 28, 0));
        addListMapCapsule(pl, list, getMapCanJoin(pl, 11, 0));
        return list;
    }

    public List<Zone> getMapBlackBall() {
        List<Zone> list = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            list.add(getMapById(16 + i).zones.get(0));
        }
        return list;
    }

    public List<Zone> getMapMaBu() {
        List<Zone> list = new ArrayList<>();
        // Thêm map ID 0 và 5
        list.add(getMapById(0).zones.get(0));
        list.add(getMapById(5).zones.get(0));
        // Thêm các map từ 114 đến 120
        for (int i = 0; i < 7; i++) {
            list.add(getMapById(114 + i).zones.get(0));
        }
        return list;
    }

    private void addListMapCapsule(Player pl, List<Zone> list, Zone zone) {
        for (Zone z : list) {
            if (z != null && zone != null && z.map.mapId == zone.map.mapId) {
                return;
            }
        }
        if (zone != null && pl.zone.map.mapId != zone.map.mapId) {
            list.add(zone);
        }
    }

    public void sendPlayerMove(Player player) {
        Message msg;
        try {
            msg = new Message(-7);
            msg.writer().writeInt((int) player.id);
            msg.writer().writeShort(player.location.x);
            msg.writer().writeShort(player.location.y);
            Service.gI().sendMessAllPlayerInMap(player, msg);
            msg.cleanup();
        } catch (Exception e) {
            Logger.logException(MapService.class, e);
        }
    }

    public boolean isMapOffline(int mapId) {
        for (Map map : Manager.MAPS) {
            if (map.mapId == mapId) {
                return map.type == ConstMap.MAP_OFFLINE;
            }
        }
        return false;
    }

    public boolean isMapBlackBallWar(int mapId) {
        return mapId >= 16 && mapId <= 22;
    }

    public boolean isMapKhiGas(int mapId) {
        return mapId == 149 || mapId == 148 || mapId == 147 || mapId == 151 || mapId == 152;
    }

    // không cho người khác vào map phước
    public boolean isMapMaBu(int mapId) {
        return (mapId >= 114 && mapId <= 120) || mapId == 0;
    }

    public boolean isMapPVP(int mapId) {
        return mapId == 112;
    }
// phước map cold

    public boolean isMapCold(Map map) {
        int mapId = map.mapId;
        return mapId == 212;
    }
// Map Nhà Phước

    public boolean isMapNha(int mapId) {
        return mapId == 99;
    }

    public boolean isMapDoanhTrai(int mapId) {
        return mapId == 999;
    }

    public boolean isMapHuyDiet(int mapId) {
        return mapId >= 146 && mapId <= 148;
    }

    public boolean isMapBanDoKhoBau(int mapId) {
        return mapId >= 135 && mapId <= 138;
    }

    public boolean isMapNgucTu(int mapId) {
        return mapId == 155;
    }

    public boolean isMapCauCa(int mapId) {
        return mapId == 5 || mapId == 29 || mapId == 30;
    }

    public boolean isnguhs(int mapId) {
        return mapId >= 122 && mapId <= 124;
    }

    //phó bản phước
    public boolean isdiacung(int mapId) {
        return mapId == 36;
    }

//    147	Sa Mạc
//148	Lâu đài Lychee
//149	Thành phố Santa
//150	Lôi Đài
//151	Hành tinh bóng tối
//152	Vùng đất băng giá
    public boolean isMapTuongLai(int mapId) {
        return (mapId >= 92 && mapId <= 94)
                || (mapId >= 96 && mapId <= 100)
                || mapId == 102 || mapId == 103;
    }

    public void goToMap(Player player, Zone zoneJoin) {
        Zone oldZone = player.zone;
        if (oldZone != null) {
            ChangeMapService.gI().exitMap(player);
            if (player.mobMe != null) {
                player.mobMe.goToMap(zoneJoin);
            }
        }
        player.zone = zoneJoin;
        player.zone.addPlayer(player);
    }

    public boolean isMapSetKichHoat(int mapId) {
        return (mapId >= 1 && mapId <= 3)
                || (mapId == 8 || mapId == 9 || mapId == 11)
                || (mapId >= 15 && mapId <= 17);
    }

    public boolean isMapKhongCoSieuQuai(int mapId) {
        return !isMapSetKichHoat(mapId)
                && mapId != 4 && mapId != 27 && mapId != 28
                && mapId != 12 && mapId != 31 && mapId != 32
                && mapId != 18 && mapId != 35 && mapId != 36;
    }

    public boolean isMapTrainOff(Player pl, int mapId) {
        return mapId == 46 || mapId == 47 || mapId == 29 || mapId == 48 || mapId == 50 || mapId == 154;
    }

    /**
     * Refresh map cache by reloading map templates from database
     * Extracted from Manager.loadDatabase() for better separation of concerns
     */
    public void refreshMapCache() throws Exception {
        Connection con = GirlkunDB.getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            // Clear old cache first
            Manager.MAP_TEMPLATES = null;
            
            loadMapTemplates(con);
            Logger.log(Logger.GREEN, "[REFRESH] MAP TEMPLATES(" + Manager.MAP_TEMPLATES.length + ") cache refreshed successfully\n");
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
            if (con != null) con.close();
        }
    }

    /**
     * Load map templates from database
     * Extracted from Manager.loadDatabase() for better separation of concerns
     */
    public static void loadMapTemplates(Connection con) throws Exception {
        PreparedStatement ps = null;
        ResultSet rs = null;
        JSONArray dataArray = null;
        Object jv = JSONValue.parse("");
        
        try {
            // load map template
            ps = con.prepareStatement("select count(id) from map_template");
            rs = ps.executeQuery();
            if (rs.first()) {
                int countRow = rs.getShort(1);
                Manager.MAP_TEMPLATES = new MapTemplate[countRow];
                ps = con.prepareStatement("select * from map_template");
                rs = ps.executeQuery();
                short i = 0;
                while (rs.next()) {
                    MapTemplate mapTemplate = new MapTemplate();
                    int mapId = rs.getInt("id");
                    String mapName = rs.getString("name");
                    mapTemplate.id = mapId;
                    mapTemplate.name = mapName;
                    // load data
                    dataArray = (JSONArray) JSONValue.parse(rs.getString("data"));
                    mapTemplate.type = Byte.parseByte(String.valueOf(dataArray.get(0)));
                    mapTemplate.planetId = Byte.parseByte(String.valueOf(dataArray.get(1)));
                    mapTemplate.bgType = Byte.parseByte(String.valueOf(dataArray.get(2)));
                    mapTemplate.tileId = Byte.parseByte(String.valueOf(dataArray.get(3)));
                    mapTemplate.bgId = Byte.parseByte(String.valueOf(dataArray.get(4)));
                    dataArray.clear();
                    ///////////////////////////////////////////////////////////////////
                    mapTemplate.type = rs.getByte("type");
                    mapTemplate.planetId = rs.getByte("planet_id");
                    mapTemplate.bgType = rs.getByte("bg_type");
                    mapTemplate.tileId = rs.getByte("tile_id");
                    mapTemplate.bgId = rs.getByte("bg_id");
                    mapTemplate.zones = rs.getByte("zones");
                    mapTemplate.maxPlayerPerZone = rs.getByte("max_player");
                    // load waypoints
                    dataArray = (JSONArray) JSONValue.parse(rs.getString("waypoints")
                            .replaceAll("\\[\"\\[", "[[")
                            .replaceAll("\\]\"\\]", "]]")
                            .replaceAll("\",\"", ","));
                    for (int j = 0; j < dataArray.size(); j++) {
                        WayPoint wp = new WayPoint();
                        JSONArray dtwp = (JSONArray) JSONValue.parse(String.valueOf(dataArray.get(j)));
                        wp.name = String.valueOf(dtwp.get(0));
                        wp.minX = Short.parseShort(String.valueOf(dtwp.get(1)));
                        wp.minY = Short.parseShort(String.valueOf(dtwp.get(2)));
                        wp.maxX = Short.parseShort(String.valueOf(dtwp.get(3)));
                        wp.maxY = Short.parseShort(String.valueOf(dtwp.get(4)));
                        wp.isEnter = Byte.parseByte(String.valueOf(dtwp.get(5))) == 1;
                        wp.isOffline = Byte.parseByte(String.valueOf(dtwp.get(6))) == 1;
                        wp.goMap = Short.parseShort(String.valueOf(dtwp.get(7)));
                        wp.goX = Short.parseShort(String.valueOf(dtwp.get(8)));
                        wp.goY = Short.parseShort(String.valueOf(dtwp.get(9)));
                        mapTemplate.wayPoints.add(wp);
                        dtwp.clear();
                    }
                    dataArray.clear();
                    // load mobs
                    dataArray = (JSONArray) JSONValue.parse(rs.getString("mobs").replaceAll("\\\"", ""));
                    mapTemplate.mobTemp = new byte[dataArray.size()];
                    mapTemplate.mobLevel = new byte[dataArray.size()];
                    mapTemplate.mobHp = new double[dataArray.size()];
                    mapTemplate.mobX = new short[dataArray.size()];
                    mapTemplate.mobY = new short[dataArray.size()];
                    for (int j = 0; j < dataArray.size(); j++) {
                        JSONArray dtm = (JSONArray) JSONValue.parse(String.valueOf(dataArray.get(j)));
                        mapTemplate.mobTemp[j] = Byte.parseByte(String.valueOf(dtm.get(0)));
                        mapTemplate.mobLevel[j] = Byte.parseByte(String.valueOf(dtm.get(1)));
                        // phước, chỉnh máu quái lên chục K tỷ
                        mapTemplate.mobHp[j] = (long) Long.parseLong(String.valueOf(dtm.get(2)));
                        mapTemplate.mobX[j] = Short.parseShort(String.valueOf(dtm.get(3)));
                        mapTemplate.mobY[j] = Short.parseShort(String.valueOf(dtm.get(4)));
                        dtm.clear();
                    }
                    dataArray.clear();
                    // load npcs
                    dataArray = (JSONArray) JSONValue.parse(rs.getString("npcs").replaceAll("\\\"", ""));
                    mapTemplate.npcId = new byte[dataArray.size()];
                    mapTemplate.npcX = new short[dataArray.size()];
                    mapTemplate.npcY = new short[dataArray.size()];
                    for (int j = 0; j < dataArray.size(); j++) {
                        JSONArray dtn = (JSONArray) JSONValue.parse(String.valueOf(dataArray.get(j)));
                        mapTemplate.npcId[j] = Byte.parseByte(String.valueOf(dtn.get(0)));
                        mapTemplate.npcX[j] = Short.parseShort(String.valueOf(dtn.get(1)));
                        mapTemplate.npcY[j] = Short.parseShort(String.valueOf(dtn.get(2)));
                        dtn.clear();
                    }
                    dataArray.clear();
                    Manager.MAP_TEMPLATES[i++] = mapTemplate;
                }
                Logger.log(Logger.GREEN, "[DONE] MAPTEMPLATE(" + Manager.MAP_TEMPLATES.length + ")\n");
                Manager.RUBY_REWARDS.add(Util.sendDo(861, 0, new ArrayList<>()));
            }
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
    }
}
