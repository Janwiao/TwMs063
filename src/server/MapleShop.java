package server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import client.inventory.IItem;
import client.SkillFactory;
import constants.GameConstants;
import client.inventory.MapleInventoryIdentifier;
import client.MapleClient;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import client.inventory.ModifyInventory;
import constants.ItemConstants;
import constants.PiPiConfig;
import database.DatabaseConnection;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;

public class MapleShop {

    private static final Set<Integer> rechargeableItems = new LinkedHashSet<>();
    private final int id;
    private final int npcId;
    private final List<MapleShopItem> items;

    static {
        rechargeableItems.add(2070000);
        rechargeableItems.add(2070001);
        rechargeableItems.add(2070002);
        rechargeableItems.add(2070003);
        rechargeableItems.add(2070004);
        rechargeableItems.add(2070005);
        rechargeableItems.add(2070006);
        rechargeableItems.add(2070007);
        rechargeableItems.add(2070008);
        rechargeableItems.add(2070009);
        rechargeableItems.add(2070010);
        rechargeableItems.add(2070011);
        rechargeableItems.add(2070012);
        rechargeableItems.add(2070013);
//        rechargeableItems.add(2070019); // Magic Throwing Star
//        rechargeableItems.add(2330000);
//        rechargeableItems.add(2330001);
//        rechargeableItems.add(2330002);
//        rechargeableItems.add(2330003);
//        rechargeableItems.add(2330004);
//        rechargeableItems.add(2330005);
//        rechargeableItems.add(2330007);
//        rechargeableItems.add(2331000); // Capsules
//        rechargeableItems.add(2332000); // Capsules
    }

    /**
     * Creates a new instance of MapleShop
     */
    private MapleShop(int id, int npcId) {
        this.id = id;
        this.npcId = npcId;
        items = new LinkedList<>();
    }

    public void addItem(MapleShopItem item) {
        items.add(item);
    }

    public void sendShop(MapleClient c) {
        if (c != null && c.getPlayer() != null) {
            c.getPlayer().setShop(this);
            c.sendPacket(MaplePacketCreator.getNPCShop(c, getNpcId(), items));
        }
    }

    public void buy(MapleClient c, int itemId, short quantity) {
        if (quantity <= 0) {
            AutobanManager.getInstance().addPoints(c, 1000, 0, "Buying " + quantity + " " + itemId);
            return;
        }

        if (!GameConstants.isMountItemAvailable(itemId, c.getPlayer().getJob())) {
            c.getPlayer().dropMessage(1, "你不可以買這道具。");
            c.sendPacket(MaplePacketCreator.enableActions());
            return;
        }

        MapleShopItem item = findById(itemId);

        if (item != null && item.getPrice() > 0 && item.getReqItem() == 0) {
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            final int buyPrice = GameConstants.isRechargable(itemId) ? item.getPrice() : (item.getPrice() * quantity);
            final int sellPrice = (int) (GameConstants.isRechargable(itemId) ? ii.getPrice(item.getItemId()) : (ii.getPrice(item.getItemId()) * quantity));

            if (buyPrice >= 0 && c.getPlayer().getMeso() >= buyPrice) {
                if (MapleInventoryManipulator.checkSpace(c, itemId, quantity, "")) {
                    if (c.getPlayer().getMapId() == 809030000) {
                        c.getPlayer().gainBeans(-buyPrice);
                    } else {
                        if (sellPrice > buyPrice) {
                            c.getPlayer().dropMessage("發生未知的錯誤");
                            System.out.println("商店漏洞 : 編號[" + getId() + "] 道具[" + itemId + "] 數量[" + quantity + "] 買入價格[" + buyPrice + "] 賣出價格[" + sellPrice + "]");
                            return;
                        } else {
                            c.getPlayer().gainMeso(-buyPrice, false);
                        }
                    }
                    if (GameConstants.isPet(itemId)) {
                        MapleInventoryManipulator.addById(c, itemId, quantity, "", MaplePet.createPet(itemId, MapleInventoryIdentifier.getInstance()), -1);
                    } else {
                        if (GameConstants.isRechargable(itemId)) {
                            quantity = ii.getSlotMax(c, item.getItemId());
                        }
                        MapleInventoryManipulator.addById(c, itemId, quantity);
                    }
                } else {
                    c.getPlayer().dropMessage(1, "你的道具欄滿了。");
                }
                c.sendPacket(MaplePacketCreator.confirmShopTransaction((byte) 0));
            }
        } else if (item != null && item.getReqItem() > 0 && quantity == 1 && c.getPlayer().haveItem(item.getReqItem(), item.getReqItemQ(), false, true)) {
            if (MapleInventoryManipulator.checkSpace(c, itemId, quantity, "")) {
                MapleInventoryManipulator.removeById(c, GameConstants.getInventoryType(item.getReqItem()), item.getReqItem(), item.getReqItemQ(), false, false);
                if (GameConstants.isPet(itemId)) {
                    MapleInventoryManipulator.addById(c, itemId, quantity, "", MaplePet.createPet(itemId, MapleInventoryIdentifier.getInstance()), -1);
                } else {
                    MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

                    if (GameConstants.isRechargable(itemId)) {
                        quantity = ii.getSlotMax(c, item.getItemId());
                    }
                    MapleInventoryManipulator.addById(c, itemId, quantity);
                }
            } else {
                c.getPlayer().dropMessage(1, "你的道具欄滿了。");
            }
            c.sendPacket(MaplePacketCreator.confirmShopTransaction((byte) 0));
        }
    }

    public void sell(MapleClient c, MapleInventoryType type, byte slot, short quantity) {
        if (quantity == 0xFFFF || quantity == 0) {
            quantity = 1;
        }
        IItem item = c.getPlayer().getInventory(type).getItem(slot);
        if (item == null) {
            return;
        }

        if (GameConstants.isThrowingStar(item.getItemId()) || GameConstants.isBullet(item.getItemId())) {
            quantity = item.getQuantity();
        }
        if (quantity < 0) {
            c.getPlayer().ban(c.getPlayer().getName() + "修改封包", true, true, false);
            FileoutputUtil.logToFile("logs/Hack/商店.txt", "\r\n" + FileoutputUtil.NowTime() + " 玩家：" + c.getPlayer().getName() + " 遭到封鎖，原因： 賣出" + item.getItemId() + "的數量小於0(為" + quantity + ")");
            return;
        }
        short iQuant = item.getQuantity();
        if (iQuant == 0xFFFF) {
            iQuant = 1;
        }
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (ii.cantSell(item.getItemId())) {
            return;
        }
        if (quantity <= iQuant && (iQuant > 0 || GameConstants.isRechargable(item.getItemId()))) {
            MapleInventoryManipulator.removeFromSlot(c, type, slot, quantity, false);
            double price;
            if (GameConstants.isThrowingStar(item.getItemId()) || GameConstants.isBullet(item.getItemId())) {
                price = ii.getWholePrice(item.getItemId()) / (double) ii.getSlotMax(c, item.getItemId());
            } else {
                price = ii.getPrice(item.getItemId());
            }
            int recvMesos = (int) Math.max(Math.ceil(price * quantity), 0);
            if (price != -1.0 && recvMesos > 0) {
                if (recvMesos > PiPiConfig.商店一次拍賣獲得最大楓幣) {
                    recvMesos = 1;
                }
                c.getPlayer().gainMeso(recvMesos, false);
            }
            c.sendPacket(MaplePacketCreator.confirmShopTransaction((byte) 0x8));
        }
    }

    public void recharge(final MapleClient c, final byte slot) {
        final IItem item = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);

        if (item == null || (!GameConstants.isThrowingStar(item.getItemId()) && !GameConstants.isBullet(item.getItemId()))) {
            return;
        }
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        short slotMax = ii.getSlotMax(c, item.getItemId());
        final int skill = GameConstants.getMasterySkill(c.getPlayer().getJob());

        if (skill != 0) {
            slotMax += c.getPlayer().getSkillLevel(SkillFactory.getSkill(skill)) * 10;
        }
        if (item.getQuantity() < slotMax) {
            final int price = (int) Math.round(ii.getPrice(item.getItemId()) * (slotMax - item.getQuantity()));
            if (c.getPlayer().getMeso() >= price) {
                item.setQuantity(slotMax);
                c.sendPacket(MaplePacketCreator.modifyInventory(false, new ModifyInventory(ModifyInventory.Types.UPDATE, item)));
                //c.sendPacket(MaplePacketCreator.updateInventorySlot(MapleInventoryType.USE, (Item) item, false));
                c.getPlayer().gainMeso(-price, false, true, false);
                c.sendPacket(MaplePacketCreator.confirmShopTransaction((byte) 0x8));
            }
        }
    }

    protected MapleShopItem findById(int itemId) {
        for (MapleShopItem item : items) {
            if (item.getItemId() == itemId) {
                return item;
            }
        }
        return null;
    }

    public static MapleShop createFromDB(int id, boolean isShopId) {
        MapleShop ret = null;
        int shopId;

        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(isShopId ? "SELECT * FROM shops WHERE shopid = ?" : "SELECT * FROM shops WHERE npcid = ?");

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                shopId = rs.getInt("shopid");
                ret = new MapleShop(shopId, rs.getInt("npcid"));
                rs.close();
                ps.close();
            } else {
                rs.close();
                ps.close();
                return null;
            }
            ps = con.prepareStatement("SELECT * FROM shopitems WHERE shopid = ? ORDER BY position ASC");
            ps.setInt(1, shopId);
            rs = ps.executeQuery();
            List<Integer> recharges = new ArrayList<>(rechargeableItems);
            while (rs.next()) {
                if (GameConstants.isThrowingStar(rs.getInt("itemid")) || GameConstants.isBullet(rs.getInt("itemid"))) {
                    MapleShopItem starItem = new MapleShopItem((short) 1, rs.getInt("itemid"), rs.getInt("price"), rs.getInt("reqitem"), rs.getInt("reqitemq"));
                    ret.addItem(starItem);
                    if (rechargeableItems.contains(starItem.getItemId())) {
                        recharges.remove(Integer.valueOf(starItem.getItemId()));
                    }
                } else {
                    ret.addItem(new MapleShopItem((short) 1000, rs.getInt("itemid"), rs.getInt("price"), rs.getInt("reqitem"), rs.getInt("reqitemq")));
                }
            }
            for (Integer recharge : recharges) {
                ret.addItem(new MapleShopItem((short) 1000, recharge, 0, 0, 0));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Could not load shop" + e);
        }
        return ret;
    }

    public int getNpcId() {
        return npcId;
    }

    public int getId() {
        return id;
    }
}
