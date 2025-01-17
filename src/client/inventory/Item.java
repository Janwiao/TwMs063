/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package client.inventory;

import constants.GameConstants;
import java.io.Serializable;
import server.MapleItemInformationProvider;

public class Item implements IItem, Serializable {

    private final int id;
    private short position;
    private short quantity;
    private byte flag;
    private long expiration = -1;
    private MaplePet pet = null;
    private int uniqueid = -1;
    private String owner = "";
    private String GameMaster_log = null;
    private String giftFrom = "";
    protected MapleRing ring = null;
    private long equipOnlyId = -1, inventoryitemid = -1;
    private int world;

    public Item(final int id, final short position, final short quantity, final byte flag, final int uniqueid) {
        super();
        this.id = id;
        this.position = position;
        this.quantity = quantity;
        this.flag = flag;
        this.uniqueid = uniqueid;
        this.equipOnlyId = -1;
    }

    public Item(final int id, final short position, final short quantity, final byte flag, long onlyid) {
        super();
        this.id = id;
        this.position = position;
        this.quantity = quantity;
        this.flag = flag;
        this.equipOnlyId = onlyid;
    }

    public Item(final int id, final short position, final short quantity, final byte flag) {
        super();
        this.id = id;
        this.position = position;
        this.quantity = quantity;
        this.flag = flag;
        this.equipOnlyId = -1;
    }

    public Item(int id, byte position, short quantity) {
        super();
        this.id = id;
        this.position = position;
        this.quantity = quantity;
    }

    @Override
    public IItem copy() {
        final Item ret = new Item(id, position, quantity, flag, uniqueid);
        ret.pet = pet;
        ret.owner = owner;
        ret.GameMaster_log = GameMaster_log;
        ret.expiration = expiration;
        ret.giftFrom = giftFrom;
        ret.quantity = quantity;
        ret.equipOnlyId = equipOnlyId;
        ret.world = world;
        return ret;
    }

    @Override
    public Item copyWithQuantity(final short qq) {
        final Item ret = new Item(id, position, qq, flag, uniqueid);
        ret.pet = pet;
        ret.owner = owner;
        ret.GameMaster_log = GameMaster_log;
        ret.expiration = expiration;
        ret.giftFrom = giftFrom;
        ret.equipOnlyId = equipOnlyId;
        ret.world = world;
        return ret;
    }

    @Override
    public final void setPosition(final short position) {
        this.position = position;

        if (pet != null) {
            pet.setInventoryPosition(position);
        }
    }

    @Override
    public void setQuantity(final short quantity) {
        this.quantity = quantity;
    }

    @Override
    public final String getItemName() {
        return MapleItemInformationProvider.getInstance().getName(getItemId());
    }

    @Override
    public final int getItemId() {
        return id;
    }

    @Override
    public final short getPosition() {
        return position;
    }

    @Override
    public final byte getFlag() {
        return flag;
    }

    @Override
    public final short getQuantity() {
        return quantity;
    }

    @Override
    public byte getType() {
        return 2; // An Item
    }

    @Override
    public final String getOwner() {
        return owner;
    }

    @Override
    public final void setOwner(final String owner) {
        this.owner = owner;
    }

    @Override
    public final void setFlag(final byte flag) {
        this.flag = flag;
    }

    @Override
    public final long getExpiration() {
        return expiration;
    }

    @Override
    public final void setExpiration(final long expire) {
        this.expiration = expire;
    }

    @Override
    public final String getGMLog() {
        return GameMaster_log;
    }

    @Override
    public void setGMLog(final String GameMaster_log) {
        this.GameMaster_log = GameMaster_log;
    }

    @Override
    public final int getUniqueId() {
        return uniqueid;
    }

    @Override
    public final void setUniqueId(final int id) {
        this.uniqueid = id;
    }

    @Override
    public final MaplePet getPet() {
        return pet;
    }

    public final void setPet(final MaplePet pet) {
        this.pet = pet;
    }

    @Override
    public void setGiftFrom(String gf) {
        this.giftFrom = gf;
    }

    @Override
    public String getGiftFrom() {
        return giftFrom;
    }

    @Override
    public int compareTo(IItem other) {
        if (Math.abs(position) < Math.abs(other.getPosition())) {
            return -1;
        } else if (Math.abs(position) == Math.abs(other.getPosition())) {
            return 0;
        } else {
            return 1;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IItem)) {
            return false;
        }
        final IItem ite = (IItem) obj;
        return uniqueid == ite.getUniqueId() && id == ite.getItemId() && quantity == ite.getQuantity() && Math.abs(position) == Math.abs(ite.getPosition());
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + this.id;
        hash = 97 * hash + this.position;
        hash = 97 * hash + this.quantity;
        hash = 97 * hash + this.uniqueid;
        return hash;
    }

    @Override
    public String toString() {
        return "物品: " + id + " 數量: " + quantity;
    }

    @Override
    public MapleRing getRing() {
        if (!GameConstants.isEffectRing(id) || getUniqueId() <= 0) {
            return null;
        }
        if (ring == null) {
            ring = MapleRing.loadFromDb(getUniqueId(), position < 0);
        }
        return ring;
    }

    public void setRing(MapleRing ring) {
        this.ring = ring;
    }

    @Override
    public boolean hasSetOnlyId() {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        switch (id) {
            case 2340000:
            case 2041200:
            case 2070019:
            case 2290010:
            case 2290011:
            case 2290085:
            case 2290056:
            case 2290057:
            case 2290004:
            case 2290005:
            case 2290096:
            case 2290125:
            case 4031997:
            case 4032385:
                return this.equipOnlyId <= 0;
        }
        // 限定裝備類
        if (this.id / 1000000 != 1) {
            return false;
        }

        return this.equipOnlyId <= 0;
    }

    @Override
    public long getEquipOnlyId() {
        return equipOnlyId;
    }

    @Override
    public void setEquipOnlyId(long OnlyId) {
        this.equipOnlyId = OnlyId;
    }

    @Override
    public int getWorld() {
        return world;
    }

    @Override
    public void setWorld(int id) {
        this.world = id;
    }

    @Override
    public long getInventoryitemId() {
        return inventoryitemid;
    }

    @Override
    public void setInventoryitemId(long inventoryitemid) {
        this.inventoryitemid = inventoryitemid;
    }
}
