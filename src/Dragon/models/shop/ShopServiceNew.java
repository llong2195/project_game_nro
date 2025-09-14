package Dragon.models.shop;

import Dragon.data.ItemData;
import Dragon.models.item.Item;
import Dragon.models.player.Inventory;
import Dragon.models.player.Player;
import com.girlkun.network.io.Message;
import Dragon.server.Manager;
import Dragon.services.InventoryServiceNew;
import Dragon.services.ItemService;
import Dragon.services.Service;
import Dragon.services.func.TransactionService;
import Dragon.utils.Logger;
import Dragon.utils.Util;

import java.util.List;

public class ShopServiceNew {

    private static final byte COST_GOLD = 0;
    private static final byte COST_GEM = 1;
    private static final byte COST_ITEM_SPEC = 2;
    private static final byte COST_RUBY = 3;
    private static final byte COST_COUPON = 4;

    private static final byte NORMAL_SHOP = 0;
    private static final byte SPEC_SHOP = 3;
    private static final byte BOX = 4;

    private static ShopServiceNew I;

    public static ShopServiceNew gI() {
        if (ShopServiceNew.I == null) {
            ShopServiceNew.I = new ShopServiceNew();
        }
        return ShopServiceNew.I;
    }

    public void opendShop(Player player, String tagName, boolean allGender) {
        if (tagName.equals("ITEMS_LUCKY_ROUND")) {
            openShopType4(player, tagName, player.inventory.itemsBoxCrackBall);
            return;
        } else if (tagName.equals("RUONG_PHU")) {
            openShopType5(player, tagName, player.inventory.itemsBoxCrackBall);
            return;
        } else if (tagName.equals("ITEMS_REWARD")) {
            player.getSession().initItemsReward();
            return;
        }
        try {
            Shop shop = this.getShop(tagName);
            shop = this.resolveShop(player, shop, allGender);
            switch (shop.typeShop) {
                case NORMAL_SHOP:
                    openShopType0(player, shop);
                    break;
                case SPEC_SHOP:
                    openShopType3(player, shop);
                    break;
            }
        } catch (Exception ex) {

            Service.gI().sendThongBao(player, ex.getMessage());
        }
    }

    private Shop getShop(String tagName) throws Exception {
        for (Shop s : Manager.SHOPS) {
            if (s.tagName != null && s.tagName.equals(tagName)) {
                return s;
            }
        }
        throw new Exception("Shop " + tagName + " không tồn tại!");
    }

//    private void _________________Xử_lý_cửa_hàng_trước_khi_gửi_______________() {
//        //**********************************************************************
//    }

    private Shop resolveShop(Player player, Shop shop, boolean allGender) {
        if (shop.tagName != null && (shop.tagName.equals("BUA_1H")
                || shop.tagName.equals("BUA_8H") || shop.tagName.equals("BUA_1M"))) {
            return this.resolveShopBua(player, new Shop(shop));
        }
        return allGender ? new Shop(shop) : new Shop(shop, player.gender);
    }

    private Shop resolveShopBua(Player player, Shop s) {
        for (TabShop tabShop : s.tabShops) {
            for (ItemShop item : tabShop.itemShops) {
                long min = 0;
                switch (item.temp.id) {
                    case 213:
                        long timeTriTue = player.charms.tdTriTue;
                        long current = System.currentTimeMillis();
                        min = (timeTriTue - current) / 60000;

                        break;
                    case 214:
                        min = (player.charms.tdManhMe - System.currentTimeMillis()) / 60000;
                        break;
                    case 215:
                        min = (player.charms.tdDaTrau - System.currentTimeMillis()) / 60000;
                        break;
                    case 216:
                        min = (player.charms.tdOaiHung - System.currentTimeMillis()) / 60000;
                        break;
                    case 217:
                        min = (player.charms.tdBatTu - System.currentTimeMillis()) / 60000;
                        break;
                    case 218:
                        min = (player.charms.tdDeoDai - System.currentTimeMillis()) / 60000;
                        break;
                    case 219:
                        min = (player.charms.tdThuHut - System.currentTimeMillis()) / 60000;
                        break;
                    case 522:
                        min = (player.charms.tdDeTu - System.currentTimeMillis()) / 60000;
                        break;
                    case 671:
                        min = (player.charms.tdTriTue3 - System.currentTimeMillis()) / 60000;
                        break;
                    case 672:
                        min = (player.charms.tdTriTue4 - System.currentTimeMillis()) / 60000;
                        break;
                }
                if (min > 0) {
                    item.options.clear();
                    if (min >= 1440) {
                        item.options.add(new Item.ItemOption(63, (int) min / 1440));
                    } else if (min >= 60) {
                        item.options.add(new Item.ItemOption(64, (int) min / 60));
                    } else {
                        item.options.add(new Item.ItemOption(65, (int) min));
                    }
                }
            }
        }
        return s;
    }

//    private void _________________Gửi_cửa_hàng_cho_ngư�?i_chơi________________() {
//        //**********************************************************************
//    }

    private void openShopType0(Player player, Shop shop) {
        player.iDMark.setShopOpen(shop);
        player.iDMark.setTagNameShop(shop.tagName);
        if (shop != null) {
            Message msg;
            try {
                msg = new Message(-44);
                msg.writer().writeByte(NORMAL_SHOP);
                msg.writer().writeByte(shop.tabShops.size());
                for (TabShop tab : shop.tabShops) {
                    msg.writer().writeUTF(tab.name);
                    msg.writer().writeByte(tab.itemShops.size());
                    for (ItemShop itemShop : tab.itemShops) {
                        msg.writer().writeShort(itemShop.temp.id);
                        if (itemShop.typeSell == COST_GOLD) {
                            msg.writer().writeInt(itemShop.cost);
                            msg.writer().writeInt(0);
                        } else if (itemShop.typeSell == COST_GEM) {
                            msg.writer().writeInt(0);
                            msg.writer().writeInt(itemShop.cost);
                        } else if (itemShop.typeSell == COST_RUBY) {
                            msg.writer().writeInt(0);
                            msg.writer().writeInt(itemShop.cost);
                        } else if (itemShop.typeSell == COST_COUPON) {
                            msg.writer().writeInt(0);
                            msg.writer().writeInt(itemShop.cost);
                        }
                        msg.writer().writeByte(itemShop.options.size());
                        for (Item.ItemOption option : itemShop.options) {
                            msg.writer().writeByte(option.optionTemplate.id);
                            msg.writer().writeShort(option.param);
                        }
                        msg.writer().writeByte(itemShop.isNew ? 1 : 0);
                        if (itemShop.temp.type == 5) {
                            msg.writer().writeByte(1);
                            msg.writer().writeShort(itemShop.temp.head);
                            msg.writer().writeShort(itemShop.temp.body);
                            msg.writer().writeShort(itemShop.temp.leg);
                            msg.writer().writeShort(-1);
                        } else {
                            msg.writer().writeByte(0);
                        }
                    }
                }
                player.sendMessage(msg);
                msg.cleanup();
            } catch (Exception e) {
                Logger.logException(ShopServiceNew.class, e);
            }
        }
    }

    private void openShopType3(Player player, Shop shop) {
        player.iDMark.setShopOpen(shop);
        player.iDMark.setTagNameShop(shop.tagName);
        if (shop != null) {
            Message msg;
            try {
                msg = new Message(-44);
                msg.writer().writeByte(SPEC_SHOP);
                msg.writer().writeByte(shop.tabShops.size());
                for (TabShop tab : shop.tabShops) {
                    msg.writer().writeUTF(tab.name);
                    msg.writer().writeByte(tab.itemShops.size());
                    for (ItemShop itemShop : tab.itemShops) {
                        msg.writer().writeShort(itemShop.temp.id);
                        msg.writer().writeShort(itemShop.iconSpec);
                        msg.writer().writeInt(itemShop.cost);
                        msg.writer().writeByte(itemShop.options.size());
                        for (Item.ItemOption option : itemShop.options) {
                            msg.writer().writeByte(option.optionTemplate.id);
                            msg.writer().writeShort(option.param);
                        }
                        msg.writer().writeByte(itemShop.isNew ? 1 : 0);
                        if (itemShop.temp.type == 5) {
                            msg.writer().writeByte(1);
                            msg.writer().writeShort(itemShop.temp.head);
                            msg.writer().writeShort(itemShop.temp.body);
                            msg.writer().writeShort(itemShop.temp.leg);
                            msg.writer().writeShort(-1);
                        } else {
                            msg.writer().writeByte(0);
                        }
                    }
                }
                player.sendMessage(msg);
                msg.cleanup();
            } catch (Exception e) {
                Logger.logException(ShopServiceNew.class, e);
            }
        }
    }

    private void openShopType4(Player player, String tagName, List<Item> items) {
        if (items == null) {
            return;
        }
        player.iDMark.setTagNameShop(tagName);
        Message msg;
        try {
            msg = new Message(-44);
            msg.writer().writeByte(4);
            msg.writer().writeByte(1);
            msg.writer().writeUTF("Rương");
            msg.writer().writeByte(items.size());
            for (Item item : items) {
                msg.writer().writeShort(item.template.id);
                msg.writer().writeUTF("\n|7|NROTUONGLAI.COM:");
                msg.writer().writeByte(item.itemOptions.size() + 1);
                for (Item.ItemOption io : item.itemOptions) {
                    msg.writer().writeByte(io.optionTemplate.id);
                    msg.writer().writeShort(io.param);
                }
                //số lượng
                msg.writer().writeByte(31);
                msg.writer().writeShort(item.quantity);
                //
                msg.writer().writeByte(1);
                if (item.template.type == 5) {
                    msg.writer().writeByte(1);
                    msg.writer().writeShort(item.template.head);
                    msg.writer().writeShort(item.template.body);
                    msg.writer().writeShort(item.template.leg);
                    msg.writer().writeShort(-1);
                } else {
                    msg.writer().writeByte(0);
                }
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {

        }
    }

    private void openShopType5(Player player, String tagName, List<Item> items) {
        if (items == null) {
            return;
        }
        player.iDMark.setTagNameShop(tagName);
        Message msg;
        try {
            msg = new Message(-44);
            msg.writer().writeByte(4);
            msg.writer().writeByte(1);
            msg.writer().writeUTF("Phần\nthưởng");
            msg.writer().writeByte(items.size());
            for (Item item : items) {
                msg.writer().writeShort(item.template.id);
                msg.writer().writeUTF("\n|7|Nro Kuroko:");
                msg.writer().writeByte(item.itemOptions.size() + 1);
                for (Item.ItemOption io : item.itemOptions) {
                    msg.writer().writeByte(io.optionTemplate.id);
                    msg.writer().writeShort(io.param);
                }
                //số lượng
                msg.writer().writeByte(31);
                msg.writer().writeShort(item.quantity);
                //
                msg.writer().writeByte(1);
                if (item.template.type == 5) {
                    msg.writer().writeByte(1);
                    msg.writer().writeShort(item.template.head);
                    msg.writer().writeShort(item.template.body);
                    msg.writer().writeShort(item.template.leg);
                    msg.writer().writeShort(-1);
                } else {
                    msg.writer().writeByte(0);
                }
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {

        }
    }
    public void takeItem(Player player, byte type, int tempId) {
        String tagName = player.iDMark.getTagNameShop();
        if (tagName == null || tagName.length() <= 0) {
            return;
        }
        if (tagName.equals("ITEMS_LUCKY_ROUND")) {
            getItemSideBoxLuckyRound(player, player.inventory.itemsBoxCrackBall, type, tempId);
            return;
        } else if (tagName.equals("RUONG_PHU")) {
            getItemSideBoxGapThu(player, player.inventory.itemsBoxCrackBall, type, tempId);
            return;
        } else if (tagName.equals("ITEMS_REWARD")) {
            return;
        }

        if (player.iDMark.getShopOpen() == null) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        if (tagName.equals("BUA_1H") || tagName.equals("BUA_8H") || tagName.equals("BUA_1M")) {
            buyItemBua(player, tempId);
        } else {
            buyItem(player, tempId);
        }
        Service.gI().sendMoney(player);
    }

    private boolean subMoneyByItemShop(Player player, ItemShop is) {
        int gold = 0;
        int gem = 0;
        int ruby = 0;
        int coupon = 0;
        switch (is.typeSell) {
            case COST_GOLD:
                gold = is.cost;
                break;
            case COST_GEM:
                gem = is.cost;
                break;
            case COST_RUBY:
                ruby = is.cost;
                break;
            case COST_COUPON:
                coupon = is.cost;
                break;

        }
        if (player.inventory.gold < gold) {
            Service.gI().sendThongBao(player, "Bạn Không Có �?ủ Vàng");
            return false;
        } else if (player.inventory.gem < gem) {
            Service.gI().sendThongBao(player, "Bạn Không Có �?ủ Ng�?c");
            return false;
        } else if (player.inventory.ruby < ruby) {
            Service.gI().sendThongBao(player, "Bạn Không Có �?ủ hồng Ng�?c");
            return false;
        } else if (player.inventory.coupon < coupon) {
            Service.gI().sendThongBao(player, "Bạn Không Có �?ủ �?iểm");
            return false;
        }
        player.inventory.gold -= is.temp.gold;
        player.inventory.gem -= is.temp.gem;
        player.inventory.ruby -= ruby;
        player.inventory.coupon -= coupon;
        return true;
    }

    private void buyItemBua(Player player, int itemTempId) {
        Shop shop = player.iDMark.getShopOpen();
        ItemShop is = shop.getItemShop(itemTempId);
        if (is == null) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        if (!subMoneyByItemShop(player, is)) {
            return;
        }
        InventoryServiceNew.gI().addItemBag(player, ItemService.gI().createItemFromItemShop(is));
        InventoryServiceNew.gI().sendItemBags(player);
        opendShop(player, shop.tagName, true);
    }


    public void buyItem(Player player, int itemTempId) {
        Shop shop = player.iDMark.getShopOpen();
        ItemShop is = shop.getItemShop(itemTempId);
        if (is == null) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        if (InventoryServiceNew.gI().getCountEmptyBag(player) == 0) {
            Service.gI().sendThongBao(player, "Hành trang đã đầy");
            return;
        }

        // Kiểm tra nếu là vật phẩm hủy diệt và không đủ thức ăn
        if (isHuyDietItem(is) && !hasEnoughThucAn(player)) {
            Service.gI().sendThongBao(player, "Không đủ thức ăn để mua đồ!");
            return;
        }

        if (shop.typeShop == ShopServiceNew.NORMAL_SHOP) {
            if (!subMoneyByItemShop(player, is)) {
                return;
            }
        } else if (shop.typeShop == ShopServiceNew.SPEC_SHOP) {
            if (!this.subIemByItemShop(player, is)) {
                return;
            }
        }

        Item item = ItemService.gI().createItemFromItemShop(is);
        InventoryServiceNew.gI().addItemBag(player, item);
        InventoryServiceNew.gI().sendItemBags(player);
        Service.gI().sendThongBao(player, "Mua thành công " + is.temp.name);
    }

// Kiểm tra nếu là vật phẩm hủy diệt
    private boolean isHuyDietItem(ItemShop itemShop) {
        int itemId = itemShop.temp.id;
        return itemId >= 650 && itemId <= 662;
    }

// Kiểm tra nếu có đủ thức ăn
    private boolean hasEnoughThucAn(Player player) {
        return player.inventory.itemsBag.stream()
                .filter(item -> item.isNotNullItem() && item.isThucAn() && item.quantity >= 99)
                .findFirst().isPresent();
    }

//    private void _________________Bán_vật_phẩm______________________________() {
//        //**********************************************************************
//    }

    private boolean subIemByItemShop(Player pl, ItemShop itemShop) {
        boolean isBuy = false;
        short itSpec = ItemService.gI().getItemIdByIcon((short) itemShop.iconSpec);
        int buySpec = itemShop.cost;
        Item itS = ItemService.gI().createNewItem(itSpec);
        switch (itS.template.id) {
            case 861:
            case 188:
            case 189:
            case 190:
                if (pl.inventory.ruby >= buySpec) {
                    pl.inventory.ruby -= buySpec;
                    isBuy = true;
                } else {
                    Service.gI().sendThongBao(pl, "Bạn Không �?ủ Vàng �?ể Mua Vật Phẩm");
                    isBuy = false;
                }
                break;
            case 76:
                if (pl.inventory.ruby >= buySpec) {
                    pl.inventory.ruby -= buySpec;
                    isBuy = true;
                } else {
                    Service.gI().sendThongBao(pl, "Bạn Không �?ủ Hồng Ng�?c �?ể Mua Vật Phẩm");
                    isBuy = false;
                }
                break;
            case 457:
                if (itemShop.tabShop.shop.tagName.equals("BILL")) {

                    for (Item i : pl.inventory.itemsBag) {
                        if (i.template != null) {
                            if (ItemData.list_thuc_an.contains((int) i.template.id)) {
                                if (InventoryServiceNew.gI().findItemBag(pl, i.template.id).quantity < 99) {
                                    Service.getInstance().sendThongBao(pl, "Không đủ số lượng thức ăn");
                                    return false;
                                } else {
                                    InventoryServiceNew.gI().subQuantityItemsBag(pl, InventoryServiceNew.gI().findItemBag(pl, i.template.id), 99);
                                    InventoryServiceNew.gI().subQuantityItemsBag(pl, InventoryServiceNew.gI().findItemBag(pl, itSpec), buySpec);
                                    return true;
                                }
                            }
                        }

                    }
                    Service.getInstance().sendThongBao(pl, "Không tìm thấy thức ăn");
                    return false;
                }
            default:
                if (InventoryServiceNew.gI().findItemBag(pl, itSpec) == null || !InventoryServiceNew.gI().findItemBag(pl, itSpec).isNotNullItem()) {
                    Service.gI().sendThongBao(pl, "Không tìm thấy " + itS.template.name);
                    isBuy = false;
                } else if (InventoryServiceNew.gI().findItemBag(pl, itSpec).quantity < buySpec) {
                    Service.gI().sendThongBao(pl, "Bạn Không Có �?ủ " + buySpec + " " + itS.template.name);
                    isBuy = false;
                } else {
                    InventoryServiceNew.gI().subQuantityItemsBag(pl, InventoryServiceNew.gI().findItemBag(pl, itSpec), buySpec);
                    isBuy = true;
                }
                break;
        }
        return isBuy;
    }

    public void showConfirmSellItem(Player pl, int where, int index) {

        TransactionService.gI().cancelTrade(pl);
        if (index < 0) {
            return;
        }
        Item item = null;
        if (where == 0) {
            item = pl.inventory.itemsBody.get(index);
        } else {
            if (pl.getSession().version < 6) {
                index -= (pl.inventory.itemsBody.size() - 7);
            }
            if (index >= 0) {
                item = pl.inventory.itemsBag.get(index);
            } else {
                item = pl.inventory.itemsBag.get(0);
            }
        }
        if (item != null && item.isNotNullItem()) {
            int quantity = item.quantity;
            int cost = item.template.gold;
            if (item.template.id == 457) {
                quantity = 1;
            } else {
                cost /= 4;
            }
            if (cost == 0) {
                cost = 1;
            }
            cost *= quantity;

            String text = "Bạn Có Muốn Bán\nx" + quantity
                    + " " + item.template.name + "\nVới Giá Là: " + Util.numberToMoney(cost) + " Vàng?";
            Message msg = new Message(7);
            try {
                msg.writer().writeByte(where);
                msg.writer().writeShort(index);
                msg.writer().writeUTF(text);
                pl.sendMessage(msg);
                msg.cleanup();
            } catch (Exception e) {

            }
        }
    }

    public void sellItem(Player pl, int where, int index) {
        Item item = null;
        if (index < 0) {
            return;
        }
        if (where == 0) {
            item = pl.inventory.itemsBody.get(index);
        } else {
            item = pl.inventory.itemsBag.get(index);
        }
        if (item != null && item.template.id != 921 && item.template.id != 454 && item.template.id != 194) { // Thêm đi�?u kiện kiểm tra id của vật phẩm khác với 921
            int quantity = item.quantity;
            int cost = item.template.gold;
            if (item.template.id == 457) {
                quantity = 1;
            } else {
                cost /= 4;
            }
            if (cost == 0) {
                cost = 1;
            }
            cost *= quantity;

            if (pl.inventory.gold + cost > Inventory.LIMIT_GOLD) {
                Service.gI().sendThongBao(pl, "Vàng Sau Khi Bán Vượt Quá Giới Hạn");
                return;
            }
            pl.inventory.gold += cost;
            Service.gI().sendMoney(pl);
            Service.gI().sendThongBao(pl, "�?ã Bán " + item.template.name
                    + " Thu �?ược " + Util.numberToMoney(cost) + " Vàng");
            if (where == 0) {
                InventoryServiceNew.gI().subQuantityItemsBody(pl, item, quantity);
                InventoryServiceNew.gI().sendItemBody(pl);
                Service.gI().Send_Caitrang(pl);
            } else {
                InventoryServiceNew.gI().subQuantityItemsBag(pl, item, quantity);
                InventoryServiceNew.gI().sendItemBags(pl);
            }
        } else {
            Service.gI().sendThongBao(pl, "Không Thể Bán " + item.template.name + " Này �?ược");
        }
    }
//
//    private void _________________Nhận_vật_phẩm_từ_rương_đặc_biệt___________() {
//        //**********************************************************************
//    }

    private void getItemSideBoxLuckyRound(Player player, List<Item> items, byte type, int index) {
        if (items == null) {
            return;
        }
        Item item = items.get(index);
        switch (type) {
            case 0: //nhận
                if (item.isNotNullItem()) {
                    if (InventoryServiceNew.gI().getCountEmptyBag(player) != 0) {
                        InventoryServiceNew.gI().addItemBag(player, item);
                        Service.gI().sendThongBao(player,
                                "Bạn nhận được " + (item.template.id == 189
                                        ? Util.numberToMoney(item.quantity) + " vàng" : item.template.name));
                        InventoryServiceNew.gI().sendItemBags(player);
                        items.remove(index);
                    } else {
                        Service.gI().sendThongBao(player, "Hành trang đã đầy");
                    }
                } else {
                    Service.gI().sendThongBao(player, "Không thể thực hiện");
                }
                break;
            case 1: //xóa
                items.remove(index);
                Service.gI().sendThongBao(player, "Xóa vật phẩm thành công");
                break;
            case 2: //nhận hết
                for (int i = items.size() - 1; i >= 0; i--) {
                    item = items.get(i);
                    if (InventoryServiceNew.gI().addItemBag(player, item)) {
                        Service.gI().sendThongBao(player,
                                "Bạn nhận được " + (item.template.id == 189
                                        ? Util.numberToMoney(item.quantity) + " vàng" : item.template.name));
                        items.remove(i);
                    }
                }
                InventoryServiceNew.gI().sendItemBags(player);
                break;
        }
        openShopType4(player, player.iDMark.getTagNameShop(), items);
    }

    private void getItemSideBoxGapThu(Player player, List<Item> items, byte type, int index) {
        if (items == null) {
            return;
        }
        Item item = items.get(index);
        switch (type) {
            case 0: //nhận
                if (item.isNotNullItem()) {
                    if (InventoryServiceNew.gI().getCountEmptyBag(player) != 0) {
                        InventoryServiceNew.gI().addItemBag(player, item);
                        Service.gI().sendThongBao(player,
                                "Bạn nhận được " + (item.template.id == 189
                                        ? Util.numberToMoney(item.quantity) + " vàng" : item.template.name));
                        InventoryServiceNew.gI().sendItemBags(player);
                        items.remove(index);
                    } else {
                        Service.gI().sendThongBao(player, "Hành trang đã đầy");
                    }
                } else {
                    Service.gI().sendThongBao(player, "Không thể thực hiện");
                }
                break;
            case 1: //xóa
                items.remove(index);
                Service.gI().sendThongBao(player, "Xóa vật phẩm thành công");
                break;
            case 2: //nhận hết
                for (int i = items.size() - 1; i >= 0; i--) {
                    item = items.get(i);
                    if (InventoryServiceNew.gI().addItemBag(player, item)) {
                        Service.gI().sendThongBao(player,
                                "Bạn nhận được " + (item.template.id == 189
                                        ? Util.numberToMoney(item.quantity) + " vàng" : item.template.name));
                        items.remove(i);
                    }
                }
                InventoryServiceNew.gI().sendItemBags(player);
                break;
        }
        openShopType5(player, player.iDMark.getTagNameShop(), items);
    }
}

