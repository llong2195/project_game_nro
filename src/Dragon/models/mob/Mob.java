package Dragon.models.mob;

import Dragon.De2.Thu_TrieuHoi;
import Dragon.consts.ConstMap;
import Dragon.consts.ConstMob;
import Dragon.consts.ConstTask;
import Dragon.models.item.Item;
import Dragon.models.map.ItemMap;

import java.util.List;

import Dragon.models.map.Zone;
import Dragon.models.map.BDKB.BanDoKhoBau;
import Dragon.consts.ConstPlayer;
import Dragon.models.item.Item.ItemOption;
import Dragon.models.player.Location;
import Dragon.models.player.Pet;
import Dragon.models.player.Player;
import Dragon.models.reward.ItemMobReward;
import Dragon.models.reward.MobReward;
import Dragon.models.skill.PlayerSkill;
import Dragon.models.skill.Skill;
import com.girlkun.network.io.Message;
import Dragon.server.Maintenance;
import Dragon.server.Manager;
import Dragon.server.ServerManager;
import Dragon.services.*;
import Dragon.services.func.SummonDragon;
import Dragon.utils.Logger;
import Dragon.utils.Util;
import java.io.IOException;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

public class Mob {

    public int id;
    public Zone zone;
    public int tempId;
    public String name;
    public byte level;

    public MobPoint point;
    public MobEffectSkill effectSkill;
    public Location location;

    public byte pDame;
    public int pTiemNang;
    private long maxTiemNang;

    public long lastTimeDie;
    public int lvMob = 0;
    public int status = 5;

    public boolean isMobMe;

    public Mob(Mob mob) {
        this.point = new MobPoint(this);
        this.effectSkill = new MobEffectSkill(this);
        this.location = new Location();
        this.id = mob.id;
        this.tempId = mob.tempId;
        this.level = mob.level;
        this.point.setHpFull(mob.point.getHpFull());
        this.point.sethp(this.point.getHpFull());
        this.location.x = mob.location.x;
        this.location.y = mob.location.y;
        this.pDame = mob.pDame;
        this.pTiemNang = mob.pTiemNang;
        this.setTiemNang();
    }

    public Mob() {
        this.point = new MobPoint(this);
        this.effectSkill = new MobEffectSkill(this);
        this.location = new Location();
    }

    public void setTiemNang() {
        this.maxTiemNang = (long) this.point.getHpFull() * (this.pTiemNang + Util.nextInt(-2, 2)) / 100;
    }

    public static void initMobBanDoKhoBau(Mob mob, byte level) {
        mob.point.dame = level * 3250 * mob.level * 4;
        mob.point.maxHp = level * 12472 * mob.level * 2 + level * 7263 * mob.tempId;
    }

    public static void initMopbKhiGas(Mob mob, int level) {
        mob.point.maxHp = 20000000 * level;
        mob.point.dame = 10000 * level;
    }

    public static void hoiSinhMob(Mob mob) {
        mob.point.hp = mob.point.maxHp;
        mob.setTiemNang();
        Message msg;
        try {
            msg = new Message(-13);
            msg.writer().writeByte(mob.id);
            msg.writer().writeByte(mob.tempId);
            msg.writer().writeByte(0);
            msg.writer().writeDouble(Dragon.utils.Util.limitDouble(mob.point.hp));
            Service.getInstance().sendMessAllPlayerInMap(mob.zone, msg);
            msg.cleanup();
        } catch (Exception e) {

        }
    }

    private long lastTimeAttackPlayer;

    public boolean isDie() {
        return this.point.gethp() <= 0;
    }

    public boolean isSieuQuai() {
        return this.lvMob > 0;
    }

    public synchronized void injured(Player plAtt, double damage, boolean dieWhenHpFull) {
        if (!this.isDie()) {
            if (damage >= this.point.hp) {
                damage = this.point.hp;
            }
            if (!dieWhenHpFull) {
                if (this.point.hp == this.point.maxHp && damage >= this.point.hp) {
                    damage = this.point.hp - 1;
                }
                if (this.tempId == 0 && damage > 10) {
                    damage = 10;
                }
            }
            this.point.hp -= damage;

            if (this.isDie()) {
                if (plAtt != null) {
                    this.lvMob = 0;
                    this.status = 0;
                    this.sendMobDieAffterAttacked(plAtt, damage);
                    TaskService.gI().checkDoneTaskKillMob(plAtt, this);
                    TaskService.gI().checkDoneSideTaskKillMob(plAtt, this);

                    for (Item item : plAtt.inventory.itemsBody) {
                        if (item != null) {
                            for (ItemOption io : item.itemOptions) {
                                if (io.optionTemplate.id == 224) {
                                    io.param += 1;
                                    break;
                                }
                            }
                        }
                    }
                    InventoryServiceNew.gI().sendItemBody(plAtt);
                    Service.gI().point(plAtt);
                }
                this.lastTimeDie = System.currentTimeMillis();

                if (this.id == 13) {
                    this.zone.isbulon13Alive = false;
                }
                if (this.id == 14) {
                    this.zone.isbulon14Alive = false;
                }

                // ========================DUNGEON=======================
                if (plAtt != null) {
                    Dragon.thuongnhanthanbi.Dungeon_Manager dungeonManager = Dragon.thuongnhanthanbi.Dungeon_Manager
                            .gI();
                    Dragon.thuongnhanthanbi.DungeonInstance instance = dungeonManager.getPlayerInstance(plAtt);
                    if (instance != null && instance.isActive() && instance.isMobFromThisInstance(this.id)) {
                        instance.onMobKilled();
                    }
                }
                // ========================END DUNGEON=======================
            } else {
                this.sendMobStillAliveAffterAttacked(damage, plAtt != null ? plAtt.nPoint.isCrit : false);
            }
            if (plAtt != null) {
                Service.gI().addSMTN(plAtt, (byte) 2, getTiemNangForPlayer(plAtt, damage), true);
            }
        }
    }

    private long calculateTiemNang(Player pl, long dame) {
        if (dame > this.point.maxHp) {
            dame = (long) this.point.maxHp;
        }

        int levelPlayer = Service.gI().getCurrLevel(pl);
        int n = levelPlayer - this.level;
        long pDameHit = (long) (dame * 100 / point.getHpFull());
        long tiemNang = pDameHit * maxTiemNang / 100;
        if (tiemNang <= 0) {
            tiemNang = 1;
        }

        tiemNang = pl.nPoint.calSucManhTiemNang(tiemNang);
        if (pl.zone.map.mapId == 30) {
            tiemNang *= 15;
        }
        if (pl.zone.map.mapId == 36) {
            tiemNang *= 15;
        }

        return tiemNang;
    }

    private double calculateTiemNang(Player pl, double dame) {
        if (dame > this.point.maxHp) {
            dame = this.point.maxHp;
        }

        int levelPlayer = Service.gI().getCurrLevel(pl);
        int n = levelPlayer - this.level;
        double pDameHit = dame * 100 / point.getHpFull();
        double tiemNang = pDameHit * maxTiemNang / 100;
        if (tiemNang <= 0) {
            tiemNang = 1;
        }

        tiemNang = pl.nPoint.calSucManhTiemNang(tiemNang);
        if (pl.zone.map.mapId == 30) {
            tiemNang *= 15;
        }
        if (pl.zone.map.mapId == 36) {
            tiemNang *= 15;
        }
        return tiemNang;
    }

    public long getTiemNangForPlayer(Player pl, long dame) {
        return calculateTiemNang(pl, dame);
    }

    public double getTiemNangForPlayer(Player pl, double dame) {
        return calculateTiemNang(pl, dame);
    }

    public boolean FindChar(int CharID) {
        List<Player> players = this.zone.getPlayers();
        for (int i = 0; i < players.size(); i++) {
            Player pl = players.get(i);
            if (pl != null && pl.id == CharID) {
                return true;
            }
        }
        return false;
    }

    public void update() {
        if (this.isDie() && !Maintenance.isRuning) {
            switch (zone.map.type) {
                case ConstMap.MAP_DOANH_TRAI:// Phước Map Doanh Trại
                    if (this.tempId == 22 && this.zone.map.mapId == 999 && FindChar(-2_147_479_965)) {
                        if (Util.canDoWithTime(lastTimeDie, 10000)) {
                            if (this.id == 13) {
                                this.zone.isbulon13Alive = true;
                            }
                            if (this.id == 14) {
                                this.zone.isbulon14Alive = true;
                            }
                            this.hoiSinh();
                            this.sendMobHoiSinh();
                        }

                    }
                    break;
                case ConstMap.MAP_BAN_DO_KHO_BAU:
                    if (this.tempId == 72 || this.tempId == 71) {// ro bot bao ve
                        if (System.currentTimeMillis() - this.lastTimeDie > 3000) {
                            try {
                                Message t = new Message(102);
                                t.writer().writeByte((this.tempId == 71 ? 7 : 6));
                                Service.getInstance().sendMessAllPlayerInMap(this.zone, t);
                                t.cleanup();
                            } catch (IOException e) {

                            }
                        }
                    }
                    break;
                case ConstMap.MAP_KHI_GAS:
                    break;
                default:
                    if (Util.canDoWithTime(lastTimeDie, 5000)) {
                        this.randomSieuQuai();
                        this.hoiSinh();
                        this.sendMobHoiSinh();
                    }
            }
        }
        effectSkill.update();
        attackPlayer();
    }

    private void attackPlayer() {
        if (!isDie() && !effectSkill.isHaveEffectSkill() && !(tempId == 0)) {

            if ((this.tempId == 72 || this.tempId == 71) && Util.canDoWithTime(lastTimeAttackPlayer, 300)) {
                List<Player> pl = getListPlayerCanAttack();
                if (!pl.isEmpty()) {
                    this.sendMobBossBdkbAttack(pl, this.point.getDameAttack());
                } else {
                    if (this.tempId == 71) {
                        Player plA = getPlayerCanAttack();
                        if (plA != null) {
                            try {
                                Message t = new Message(102);
                                t.writer().writeByte(5);
                                t.writer().writeByte(plA.location.x);
                                this.location.x = plA.location.x;
                                Service.getInstance().sendMessAllPlayerInMap(this.zone, t);
                                t.cleanup();
                            } catch (IOException e) {

                            }
                        }

                    }
                }
                this.lastTimeAttackPlayer = System.currentTimeMillis();
            } else if (Util.canDoWithTime(lastTimeAttackPlayer, 2000)) {
                Player pl = getPlayerCanAttack();
                if (pl != null) {
                    this.mobAttackPlayer(pl);
                }
                this.lastTimeAttackPlayer = System.currentTimeMillis();
            }

        }
    }

    private void sendMobBossBdkbAttack(List<Player> players, long dame) {
        if (this.tempId == 72) {
            try {
                Message t = new Message(102);
                int action = Util.nextInt(0, 2);
                t.writer().writeByte(action);
                if (action != 1) {
                    this.location.x = players.get(Util.nextInt(0, players.size() - 1)).location.x;
                }
                t.writer().writeByte(players.size());
                for (Byte i = 0; i < players.size(); i++) {
                    t.writer().writeInt((int) players.get(i).id);
                    t.writer().writeInt((int) players.get(i).injured(null, (int) dame, false, true));
                }
                Service.getInstance().sendMessAllPlayerInMap(this.zone, t);
                t.cleanup();
            } catch (IOException e) {

            }
        } else if (this.tempId == 71) {
            try {
                Message t = new Message(102);
                t.writer().writeByte(Util.getOne(3, 4));
                t.writer().writeByte(players.size());
                for (Byte i = 0; i < players.size(); i++) {
                    t.writer().writeInt((int) players.get(i).id);
                    t.writer().writeInt((int) players.get(i).injured(null, (int) dame, false, true));
                }
                Service.getInstance().sendMessAllPlayerInMap(this.zone, t);
                t.cleanup();
            } catch (IOException e) {

            }
        }
    }

    private void sendMobBossBdkbAttack(List<Player> players, double dame) {
        if (this.tempId == 72) {
            try {
                Message t = new Message(102);
                int action = Util.nextInt(0, 2);
                t.writer().writeByte(action);
                if (action != 1) {
                    this.location.x = players.get(Util.nextInt(0, players.size() - 1)).location.x;
                }
                t.writer().writeByte(players.size());
                for (Byte i = 0; i < players.size(); i++) {
                    t.writer().writeInt((int) players.get(i).id);
                    t.writer().writeInt((int) players.get(i).injured(null, (int) dame, false, true));
                }
                Service.getInstance().sendMessAllPlayerInMap(this.zone, t);
                t.cleanup();
            } catch (IOException e) {

            }
        } else if (this.tempId == 71) {
            try {
                Message t = new Message(102);
                t.writer().writeByte(Util.getOne(3, 4));
                t.writer().writeByte(players.size());
                for (Byte i = 0; i < players.size(); i++) {
                    t.writer().writeInt((int) players.get(i).id);
                    t.writer().writeInt((int) players.get(i).injured(null, (int) dame, false, true));
                }
                Service.getInstance().sendMessAllPlayerInMap(this.zone, t);
                t.cleanup();
            } catch (IOException e) {

            }
        }
    }

    private List<Player> getListPlayerCanAttack() {
        List<Player> plAttack = new ArrayList<>();
        int distance = (this.tempId == 71 ? 250 : 600);
        try {
            List<Player> players = this.zone.getNotBosses();
            for (Player pl : players) {
                if (!pl.isDie() && !pl.isBoss && !pl.effectSkin.isVoHinh) {
                    int dis = Util.getDistance(pl, this);
                    if (dis <= distance) {
                        plAttack.add(pl);
                    }
                }
            }
        } catch (Exception e) {

        }

        return plAttack;
    }

    private Player getPlayerCanAttack() {
        int distance = 100;
        Player plAttack = null;
        try {
            List<Player> players = this.zone.getNotBosses();
            for (Player pl : players) {
                if (pl != null && pl.effectSkill != null) {
                    if (!pl.isDie() && !pl.isBoss && !pl.effectSkin.isVoHinh && !pl.isNewPet) {
                        int dis = Util.getDistance(pl, this);
                        if (dis <= distance) {
                            plAttack = pl;
                            distance = dis;
                        }
                    }
                }
            }
        } catch (Exception e) {

        }
        return plAttack;
    }

    // **************************************************************************
    private void mobAttackPlayer(Player player) {
        double dameMob = this.point.getDameAttack();

        // Giảm sát thương nếu có hiệu ứng đặc biệt
        if (player.charms.tdDaTrau > System.currentTimeMillis()) {
            dameMob /= 2;
        }

        // Tăng sát thương nếu là Siêu Quái
        if (this.isSieuQuai()) {
            dameMob *= 2;
        }

        // Gây sát thương và gửi thông báo
        double dame = player.injured(null, dameMob, false, true);
        this.sendMobAttackMe(player, dame);
        this.sendMobAttackPlayer(player);
    }

    private void sendMobAttackMe(Player player, double dame) {
        if (!player.isPet && !player.isNewPet) {
            Message msg;
            try {
                msg = new Message(-11);
                msg.writer().writeByte(this.id);
                msg.writer().writeDouble(Dragon.utils.Util.limitDouble(dame)); // dame
                player.sendMessage(msg);
                msg.cleanup();
            } catch (Exception e) {

            }
        }
    }

    private void sendMobAttackPlayer(Player player) {
        Message msg;
        try {
            msg = new Message(-10);
            msg.writer().writeByte(this.id);
            msg.writer().writeInt((int) player.id);
            msg.writeDouble(Dragon.utils.Util.limitDouble(player.nPoint.hp));
            Service.gI().sendMessAnotherNotMeInMap(player, msg);
            msg.cleanup();
        } catch (Exception e) {

        }
    }

    public void randomSieuQuai() {
        if (this.tempId != 0 && Util.nextInt(0, 100) < 3) {
            this.lvMob = 1;
        }
    }

    public void hoiSinh() {
        this.status = 5;
        this.point.hp = this.point.maxHp;
        this.setTiemNang();
    }

    public void sendMobHoiSinh() {
        Message msg;
        try {
            msg = new Message(-13);
            msg.writer().writeByte(this.id);
            msg.writer().writeByte(this.tempId);
            msg.writer().writeByte(lvMob);
            msg.writer().writeDouble(Dragon.utils.Util.limitDouble(this.point.hp));
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
            msg.cleanup();
        } catch (Exception e) {

        }
    }

    private boolean isMobBay() {
        return this.tempId == ConstMob.PHI_LONG || this.tempId == ConstMob.PHI_LONG_ME
                || this.tempId == ConstMob.QUY_BAY || this.tempId == ConstMob.QUY_BAY_ME
                || this.tempId == ConstMob.QUY_DAU_TO || this.tempId == ConstMob.THAN_LAN_BAY
                || this.tempId == ConstMob.THAN_LAN_ME || this.tempId == ConstMob.KHONG_TAC
                || this.tempId == ConstMob.TAMBOURINE || this.tempId == ConstMob.ALIEN
                || this.tempId == ConstMob.QUY_DIA_NGUC || this.tempId == ConstMob.THAN_LAN_XANH
                || this.tempId == ConstMob.DOI_DA_XANH || this.tempId == ConstMob.QUY_CHIM
                || this.tempId == ConstMob.TABURINE_DO || this.tempId == ConstMob.DA_XANH
                || this.tempId == ConstMob.ARBEE;
    }

    private boolean isXenCon() {
        return this.tempId >= ConstMob.XEN_CON_CAP_1 && this.tempId <= ConstMob.XEN_CON_CAP_8;
    }

    // **************************************************************************
    private void sendMobDieAffterAttacked(Player plKill, double dameHit) {
        Message msg;
        try {
            msg = new Message(-12);
            msg.writer().writeByte(this.id);
            msg.writer().writeDouble(Dragon.utils.Util.limitDouble(dameHit));
            msg.writer().writeBoolean(plKill.nPoint.isCrit); // crit
            List<ItemMap> items = mobReward(plKill, this.dropItemTask(plKill), msg);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
            msg.cleanup();
            hutItem(plKill, items);
        } catch (Exception e) {

        }
    }

    public void sendMobDieAfterMobMeAttacked(Player plKill, int dameHit) {
        this.status = 0;
        Message msg;
        try {
            if (this.id == 13) {
                this.zone.isbulon13Alive = false;
            }
            if (this.id == 14) {
                this.zone.isbulon14Alive = false;
            }
            msg = new Message(-12);
            msg.writer().writeByte(this.id);
            msg.writer().writeInt(dameHit);
            msg.writer().writeBoolean(false); // crit

            List<ItemMap> items = mobReward(plKill, this.dropItemTask(plKill), msg);
            Service.getInstance().sendMessAllPlayerInMap(this.zone, msg);
            msg.cleanup();
            hutItem(plKill, items);
        } catch (IOException e) {
            Logger.logException(Mob.class, e);
        }
        this.lastTimeDie = System.currentTimeMillis();
    }

    private void hutItem(Player player, List<ItemMap> items) {
        if (!player.isPet && !player.isNewPet) {
            if (player.charms.tdThuHut > System.currentTimeMillis()) {
                for (ItemMap item : items) {
                    if (item.itemTemplate.id != 1963) {
                        ItemMapService.gI().pickItem(player, item.itemMapId, true);
                    }
                }
            }
        } else if (player.isTrieuhoipet) {
            if (((Thu_TrieuHoi) player).masterr.charms.tdThuHut > System.currentTimeMillis()) {
                for (ItemMap item : items) {
                    ItemMapService.gI().pickItem(((Thu_TrieuHoi) player).masterr, item.itemMapId, true);
                }
            }
        } else {
            if (((Pet) player).master.charms.tdThuHut > System.currentTimeMillis()) {
                for (ItemMap item : items) {
                    if (item.itemTemplate.id != 590) {
                        ItemMapService.gI().pickItem(((Pet) player).master, item.itemMapId, true);
                    }
                }
            }
        }
    }

    private List<ItemMap> mobReward(Player player, ItemMap itemTask, Message msg) {
        List<ItemMap> itemReward = new ArrayList<>();
        try {
            if (player == null) {
                return new ArrayList<>();
            }
            if (zone.map.mapId == 9999 && Util.isTrue(1, 100)) {
                Item mts = ItemService.gI().createNewItem((short) (1066 + Util.nextInt(5)), 1);
                InventoryServiceNew.gI().addItemBag(player, mts);
                InventoryServiceNew.gI().sendItemBags(player);
                Service.gI().sendThongBao(player, "Bạn vừa nhận được x1" + mts.template.name);
            }
            if (zone.map.mapId == 9999) {
                if (Util.isTrue(50, 100)) {
                    Item mts = ItemService.gI().createNewItem((short) Util.nextInt(1545, 1559));
                    InventoryServiceNew.gI().addItemBag(player, mts);
                    InventoryServiceNew.gI().sendItemBags(player);
                    Service.gI().sendThongBao(player, "Bạn vừa nhận được x1" + mts.template.name);
                }
            }
            if (Util.isTrue(50, 100)) {
                if (player.setClothes.godClothes && MapService.gI().isMapCold(player.zone.map)) {
                    ArrietyDrop.DropItemReWard(player,
                            ArrietyDrop.list_thuc_an[Util.nextInt(0, (ArrietyDrop.list_thuc_an.length - 1))], 1,
                            this.location.x, this.location.y);
                }
            }
            itemReward = this.getItemMobReward(player, this.location.x + Util.nextInt(-10, 10),
                    this.zone.map.yPhysicInTop(this.location.x, this.location.y));
            if (itemTask != null) {
                itemReward.add(itemTask);
            }
            if (Util.isTrue(1, 900000)) {
                if (MapService.gI().isMapCold(player.zone.map)) {
                    byte randomDo = (byte) new Random().nextInt(Manager.itemIds_TL.length - 1);
                    ItemMap itemTL = Util.ratiItem(zone, Manager.itemIds_TL[randomDo], 1, this.location.x,
                            this.location.y, player.id);
                    Service.gI().dropItemMap(this.zone, itemTL);
                    if (player.charms.tdThuHut > System.currentTimeMillis()) {
                        ItemMapService.gI().pickItem(player, itemTL.itemMapId, true);
                    }
                }
            }
            msg.writer().writeByte(itemReward.size());
            for (ItemMap itemMap : itemReward) {

                msg.writer().writeShort(itemMap.itemMapId);
                msg.writer().writeShort(itemMap.itemTemplate.id);
                msg.writer().writeShort(itemMap.x);
                msg.writer().writeShort(itemMap.y);
                msg.writer().writeInt((int) itemMap.playerId);
            }
        } catch (Exception e) {

        }
        return itemReward;
    }

    private boolean MapStart(int mapid) {
        return mapid == 0;
    }

    public List<ItemMap> getItemMobReward(Player player, int x, int yEnd) {
        List<ItemMap> list = new ArrayList<>();
        MobReward mobReward = Manager.MOB_REWARDS.get(this.tempId);
        if (mobReward == null) {
            return list;
        }
        final Calendar rightNow = Calendar.getInstance();
        int hour = rightNow.get(11);
        if (MapStart(player.zone.map.mapId)) {
            return new ArrayList<>();
        }
        List<ItemMobReward> items = mobReward.getItemReward();
        List<ItemMobReward> golds = mobReward.getGoldReward();
        if (!items.isEmpty()) {
            ItemMobReward item = items.get(Util.nextInt(0, items.size() - 1));
            ItemMap itemMap = item.getItemMap(zone, player, x, yEnd);
            if (itemMap != null) {
                list.add(itemMap);
            }
        }

        if (player.isPl()) {
            int mapId = player.zone.map.mapId;
            if (!player.isBoss && (mapId == 3)) {
                if (Util.isTrue(5, 10000)) {
                    int[][] itemKH = { { 0, 6, 21, 27, 12 }, { 1, 7, 22, 28, 12 }, { 2, 8, 23, 29, 12 } }; // td,
                    int skhId = ItemService.gI().randomSKHId(player.gender);
                    ItemMap it = ItemService.gI().itemMapSKH(zone, itemKH[player.gender][Util.nextInt(0, 4)], 1,
                            this.location.x, this.location.y, player.id, skhId);
                    list.add(it);
                }
            }
        }

        if (!golds.isEmpty()) {
            ItemMobReward gold = golds.get(Util.nextInt(0, golds.size() - 1));
            ItemMap itemMap = gold.getItemMap(zone, player, x, yEnd);
            if (itemMap != null) {
                list.add(itemMap);
            }
        }

        if (this.tempId > 0 && this.zone.map.mapId >= 0 && this.zone.map.mapId <= 3) {
            if (Util.isTrue(5, 100)) { // up bí kíp phước
                list.add(new ItemMap(zone, 590, 1, x, player.location.y, player.id));
            }
        }
        if (this.tempId > 0 && this.zone.map.mapId >= 3 && this.zone.map.mapId <= 99) {
            if (Util.isTrue(5, 100)) { // up nr phước
                list.add(new ItemMap(zone, 20, 1, x, player.location.y, player.id));
            }
        }

        // Danh sách thông tin các sao pha lê: {itemId, optionId, optionLevel}
        int[][] saoPhaLeInfo = {
                { 441, 95, 5 }, // Sao Pha Lê Đỏ
                { 442, 96, 5 }, // Sao Pha Lê Xanh Dương
                { 443, 97, 5 }, // Sao Pha Lê Hồng
                { 444, 98, 5 }, // Sao Pha Lê Tím
                { 445, 99, 5 }, // Sao Pha Lê Cam
                { 446, 10, 5 }, // Sao Pha Lê Vàng
                { 447, 101, 5 } // Sao Pha Lê Xanh Lá Cây
        };

        if (this.zone.map.mapId >= 3 && this.zone.map.mapId <= 99) {
            if (Util.isTrue(5, 100)) { // Xác suất chung 10%
                // Random chọn 1 loại sao pha lên
                int randomIndex = Util.nextInt(0, saoPhaLeInfo.length - 1); // Random từ 0 đến 6
                int[] selectedSao = saoPhaLeInfo[randomIndex];

                // Tạo sao pha lê
                Item saoPhaLe = ItemService.gI().createNewItem((short) selectedSao[0]);
                saoPhaLe.itemOptions.add(new Item.ItemOption(selectedSao[1], selectedSao[2])); // Thêm option

                // Thêm vào túi và gửi thông báo
                InventoryServiceNew.gI().addItemBag(player, saoPhaLe);
                InventoryServiceNew.gI().sendItemBags(player);
                Service.gI().sendThongBao(player, "Bạn Vừa Nhận Được " + saoPhaLe.template.name);
            }
        }

        if (Util.isTrue(30, 100)) { // Tỷ lệ 30%
            int quantity = Util.nextInt(1, 5); // Random số lượng từ 1-5
            ItemMap item1804 = new ItemMap(zone, 1804, quantity, x, yEnd, player.id);
            list.add(item1804);
        }
        if (this.zone != null && this.zone.map != null) {
            int[][] itemIds = new int[][] {
                    // Trái đất
                    { 0, 6, 21, 27, 12 }, // level 1
                    { 33, 35, 24, 30, 57 }, // level 2
                    { 3, 9, 37, 39, 58 }, // level 3
                    { 34, 36, 38, 40, 59 }, // level 4
                    { 136, 140, 144, 148, 184 }, // level 5
                    { 137, 141, 145, 149, 185 }, // level 6
                    { 138, 142, 146, 150, 186 }, // level 7
                    { 139, 143, 147, 151, 187 }, // level 8
                    { 230, 242, 254, 266, 278 }, // level 9
                    { 231, 243, 255, 267, 279 }, // level 10
                    { 232, 244, 256, 268, 280 }, // level 11
                    { 233, 245, 257, 269, 281 }, // level 12
                    // Namec
                    { 1, 7, 22, 28, 12 }, // level 1
                    { 41, 43, 46, 47, 57 }, // level 2
                    { 4, 10, 25, 31, 58 }, // level 3
                    { 42, 44, 45, 48, 59 }, // level 4
                    { 152, 156, 160, 164, 184 }, // level 5
                    { 153, 157, 161, 165, 185 }, // level 6
                    { 154, 158, 162, 166, 186 }, // level 7
                    { 155, 159, 163, 167, 187 }, // level 8
                    { 234, 246, 258, 270, 278 }, // level 9
                    { 235, 247, 259, 271, 279 }, // level 10
                    { 236, 248, 260, 272, 280 }, // level 11
                    { 237, 249, 261, 273, 281 }, // level 12
                    // Xayda
                    { 2, 8, 23, 29, 12 }, // level 1
                    { 49, 51, 53, 55, 57 }, // level 2
                    { 5, 11, 26, 32, 58 }, // level 3
                    { 50, 52, 54, 56, 59 }, // level 4
                    { 168, 172, 176, 180, 184 }, // level 5
                    { 169, 173, 177, 181, 185 }, // level 6
                    { 170, 174, 178, 182, 186 }, // level 7
                    { 171, 175, 179, 183, 187 }, // level 8
                    { 238, 250, 262, 274, 278 }, // level 9
                    { 239, 251, 263, 275, 279 }, // level 10
                    { 240, 252, 264, 276, 280 }, // level 11
                    { 241, 253, 265, 277, 281 },
                    // thần linh và hủy diệt
                    { 555, 556, 557, 558, 559, 560, 561, 562, 563, 564, 565, 566, 567 },
                    { 650, 651, 652, 653, 654, 655, 656, 657, 658, 659, 660, 661, 662 }// level 12

            };
            if (Util.isTrue(2, 100)) {
                if (itemIds != null) {
                    // random level
                    int levelIndex = Util.nextInt(0, itemIds.length - 1);
                    int[] possibleItems = itemIds[levelIndex];

                    // random item trong level đó
                    int itemId = possibleItems[Util.nextInt(0, possibleItems.length - 1)];

                    // tạo item
                    Item item = ItemService.gI().createNewItem((short) itemId);

                    // ★ Thêm Option ngẫu nhiên
                    if (Util.isTrue(20, 100)) {
                        item.itemOptions.add(new Item.ItemOption(0,
                                Util.isTrue(5, 100) ? Util.nextInt(10, 5000) : Util.nextInt(5, 2500)));
                        item.itemOptions.add(new Item.ItemOption(6,
                                Util.isTrue(5, 100) ? Util.nextInt(10, 100000) : Util.nextInt(5, 90000)));
                        item.itemOptions.add(new Item.ItemOption(7,
                                Util.isTrue(5, 100) ? Util.nextInt(10, 100000) : Util.nextInt(5, 90000)));
                    } else if (Util.isTrue(15, 100)) {
                        item.itemOptions.add(new Item.ItemOption(0,
                                Util.isTrue(5, 100) ? Util.nextInt(10, 15000) : Util.nextInt(5, 5000)));
                        item.itemOptions.add(new Item.ItemOption(6,
                                Util.isTrue(5, 100) ? Util.nextInt(10, 350000) : Util.nextInt(5, 250000)));
                        item.itemOptions.add(new Item.ItemOption(7,
                                Util.isTrue(5, 100) ? Util.nextInt(10, 350000) : Util.nextInt(5, 250000)));
                        item.itemOptions.add(new Item.ItemOption(47,
                                Util.isTrue(5, 100) ? Util.nextInt(10, 2000) : Util.nextInt(5, 1000)));
                    } else if (Util.isTrue(10, 100)) {
                        item.itemOptions.add(new Item.ItemOption(0,
                                Util.isTrue(5, 100) ? Util.nextInt(10, 20000) : Util.nextInt(5, 10000)));
                        item.itemOptions.add(new Item.ItemOption(6,
                                Util.isTrue(5, 100) ? Util.nextInt(10, 550000) : Util.nextInt(5, 450000)));
                        item.itemOptions.add(new Item.ItemOption(7,
                                Util.isTrue(5, 100) ? Util.nextInt(10, 550000) : Util.nextInt(5, 450000)));
                        item.itemOptions.add(new Item.ItemOption(95,
                                Util.isTrue(5, 100) ? Util.nextInt(10, 50) : Util.nextInt(5, 40)));
                        item.itemOptions.add(new Item.ItemOption(96,
                                Util.isTrue(5, 100) ? Util.nextInt(10, 50) : Util.nextInt(5, 40)));

                    } else if (Util.isTrue(5, 100)) {
                        item.itemOptions.add(new Item.ItemOption(0,
                                Util.isTrue(5, 100) ? Util.nextInt(10, 30000) : Util.nextInt(5, 15000)));
                        item.itemOptions.add(new Item.ItemOption(6,
                                Util.isTrue(5, 100) ? Util.nextInt(10, 1000000) : Util.nextInt(5, 900000)));
                        item.itemOptions.add(new Item.ItemOption(7,
                                Util.isTrue(5, 100) ? Util.nextInt(10, 1000000) : Util.nextInt(5, 900000)));
                        item.itemOptions.add(new Item.ItemOption(108,
                                Util.isTrue(5, 100) ? Util.nextInt(10, 50) : Util.nextInt(5, 40)));
                    } else if (Util.isTrue(2, 100)) {
                        item.itemOptions.add(new Item.ItemOption(50,
                                Util.isTrue(5, 100) ? Util.nextInt(10, 50) : Util.nextInt(5, 25)));
                        item.itemOptions.add(new Item.ItemOption(77,
                                Util.isTrue(5, 100) ? Util.nextInt(10, 50) : Util.nextInt(5, 25)));
                        item.itemOptions.add(new Item.ItemOption(103,
                                Util.isTrue(5, 100) ? Util.nextInt(10, 50) : Util.nextInt(5, 25)));
                        item.itemOptions.add(new Item.ItemOption(48,
                                Util.isTrue(5, 100) ? Util.nextInt(10, 1000000) : Util.nextInt(5, 900000)));
                    } else {
                        item.itemOptions.add(new Item.ItemOption(50,
                                Util.isTrue(5, 100) ? Util.nextInt(10, 50) : Util.nextInt(5, 25)));
                        item.itemOptions.add(new Item.ItemOption(77,
                                Util.isTrue(5, 100) ? Util.nextInt(10, 50) : Util.nextInt(5, 25)));
                        item.itemOptions.add(new Item.ItemOption(103,
                                Util.isTrue(5, 100) ? Util.nextInt(10, 50) : Util.nextInt(5, 25)));
                        item.itemOptions.add(new Item.ItemOption(48,
                                Util.isTrue(5, 100) ? Util.nextInt(10, 1000000) : Util.nextInt(5, 900000)));
                    }

                    // rớt ra map
                    ItemMap itemMap = new ItemMap(zone, item.template.id, 1, x, yEnd, player.id);
                    itemMap.options = item.itemOptions; // gán option cho itemMap
                    list.add(itemMap);
                }
            }
        }

        return list;
    }

    private ItemMap dropItemTask(Player player) {
        ItemMap itemMap = null;
        switch (this.tempId) {
            case ConstMob.KHUNG_LONG:
            case ConstMob.LON_LOI:
            case ConstMob.QUY_DAT:
                if (TaskService.gI().getIdTask(player) == ConstTask.TASK_2_0) {
                    itemMap = new ItemMap(this.zone, 73, 1, this.location.x, this.location.y, player.id);
                }
                break;
        }
        if (itemMap != null) {
            return itemMap;
        }
        return null;
    }

    private void sendMobStillAliveAffterAttacked(long dameHit, boolean crit) {
        Message msg;
        try {
            msg = new Message(-9);
            msg.writer().writeByte(this.id);
            msg.writer().writeDouble(Dragon.utils.Util.limitDouble(this.point.gethp()));
            msg.writer().writeDouble(Dragon.utils.Util.limitDouble(dameHit));
            msg.writer().writeBoolean(crit); // chí mạng
            msg.writer().writeInt(-1);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
            msg.cleanup();
        } catch (Exception e) {

        }
    }

    private void sendMobStillAliveAffterAttacked(double dameHit, boolean crit) {
        Message msg;
        try {
            msg = new Message(-9);
            msg.writer().writeByte(this.id);
            msg.writer().writeDouble(Dragon.utils.Util.limitDouble(this.point.gethp()));
            msg.writer().writeDouble(Dragon.utils.Util.limitDouble(dameHit));
            msg.writer().writeBoolean(crit); // chí mạng
            msg.writer().writeInt(-1);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
            msg.cleanup();
        } catch (Exception e) {

        }
    }
}
