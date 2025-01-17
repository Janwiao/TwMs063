/*
 This file is part of the ZeroFusion MapleStory Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>
 ZeroFusion organized by "RMZero213" <RMZero213@hotmail.com>

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
package server.events;

import client.MapleCharacter;
import handling.channel.ChannelServer;
import handling.world.World;
import server.MapleInventoryManipulator;
import server.RandomRewards;
import server.Randomizer;
import server.Timer.*;
import server.maps.MapleMap;
import server.maps.SavedLocationType;
import tools.MaplePacketCreator;

public abstract class MapleEvent {

    protected int[] mapid;
    protected int channel, world;
    protected boolean isRunning = false;

    public MapleEvent(final int world, final int channel, final int[] mapid) {
        this.world = world;
        this.channel = channel;
        this.mapid = mapid;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public MapleMap getMap(final int i) {
        return getChannelServer().getMapFactory().getMap(mapid[i]);
    }

    public ChannelServer getChannelServer() {
        return ChannelServer.getInstance(world, channel);
    }

    public void broadcast(final byte[] packet) {
        for (int i = 0; i < mapid.length; i++) {
            getMap(i).broadcastMessage(packet);
        }
    }

    public void givePrize(final MapleCharacter chr) {
        final int reward = RandomRewards.getInstance().getEventReward();
        switch (reward) {
            case 0:
                chr.gainMeso(666, true, false, false);
                chr.dropMessage(5, "你獲得 666 楓幣");
                break;
            case 1:
                chr.gainMeso(6666, true, false, false);
                chr.dropMessage(5, "你獲得 6666 楓幣");
                break;
            case 2:
                chr.gainMeso(66666, true, false, false);
                chr.dropMessage(5, "你獲得 66666 楓幣");
                break;
            case 3:
                chr.addFame(3);
                chr.dropMessage(5, "你獲得 3 名聲");
                break;
            default:
                int max_quantity = 1;
                switch (reward) {
                    case 5062000:
                        max_quantity = 3;
                        break;
                    case 5220000:
                        max_quantity = 25;
                        break;
                    case 4031307:
                    case 5050000:
                        max_quantity = 5;
                        break;
                    case 2022121:
                        max_quantity = 10;
                        break;
                }
                final int quantity = (max_quantity > 1 ? Randomizer.nextInt(max_quantity) : 0) + 1;
                if (MapleInventoryManipulator.checkSpace(chr.getClient(), reward, quantity, "")) {
                    MapleInventoryManipulator.addById(chr.getClient(), reward, (short) quantity);
                } else {
                    // givePrize(chr); //do again until they get
                    chr.gainMeso(100000, true, false, false);
                    chr.dropMessage(5, "參加獎 100000 楓幣");
                }
                //5062000 = 1-3
                //5220000 = 1-25
                //5050000 = 1-5
                //2022121 = 1-10
                //4031307 = 1-5
                break;
        }
    }

    public void finished(MapleCharacter chr) { //most dont do shit here
    }

    public void onMapLoad(MapleCharacter chr) { //most dont do shit here
    }

    public void startEvent() {
    }

    public void warpBack(MapleCharacter chr) {
        int map = chr.getSavedLocation(SavedLocationType.EVENT);
        if (map <= -1) {
            map = 104000000;
        }
        final MapleMap mapp = chr.getClient().getChannelServer().getMapFactory().getMap(map);
        chr.changeMap(mapp, mapp.getPortal(0));
    }

    public void reset() {
        isRunning = true;
    }

    public void unreset() {
        isRunning = false;
    }

    public static final void setEvent(final ChannelServer cserv, final boolean auto) {
        if (auto) {
            for (MapleEventType t : MapleEventType.values()) {
                final MapleEvent e = cserv.getEvent(t);
                if (e != null && e.isRunning) {
                    for (int i : e.mapid) {
                        if (cserv.getEvent() == i) {
                            e.broadcast(MaplePacketCreator.serverNotice("距離活動開始只剩一分鐘!"));
                            e.broadcast(MaplePacketCreator.getClock(60));
                            autoEventTimer.getInstance().schedule(new Runnable() {


                                @Override
                                public void run() {
                                    e.startEvent();
                                }
                            }, 60000);
                            break;
                        }
                    }
                }
            }
        }
        cserv.setEvent(-1);
    }

    public static final void mapLoad(final MapleCharacter chr, final int channel, final int world) {
        if (chr == null) {
            return;
        } //o_o
        for (MapleEventType t : MapleEventType.values()) {
            if (ChannelServer.getInstance(world, channel) != null) {
                final MapleEvent e = ChannelServer.getInstance(world, channel).getEvent(t);
                if (e != null && e.isRunning) {
                    if (chr.getMapId() == 109050000) { //finished map
                        e.finished(chr);
                    }
                    for (int i : e.mapid) {
                        if (chr.getMapId() == i) {
                            e.onMapLoad(chr);
                        }
                    }
                }
            }
        }
    }

    public static final void onStartEvent(final MapleCharacter chr) {
        for (MapleEventType t : MapleEventType.values()) {
            final MapleEvent e = chr.getClient().getChannelServer().getEvent(t);
            if (e != null && e.isRunning) {
                for (int i : e.mapid) {
                    if (chr.getMapId() == i) {
                        e.startEvent();
                        chr.getMap().broadcastMessage(MaplePacketCreator.getErrorNotice(String.valueOf(t) + " 活動開始。"));
                    }
                }
            }
        }
    }

    public static final String scheduleEvent(final MapleEventType event, final ChannelServer cserv) {
        if (cserv.getEvent() != -1 || cserv.getEvent(event) == null) {
            return "該活動已經被禁止安排了.";
        }
        for (int i : cserv.getEvent(event).mapid) {
            if (cserv.getMapFactory().getMap(i).getCharactersSize() > 0) {
                return "該活動已經在執行中.";
            }
        }
        cserv.setEvent(cserv.getEvent(event).mapid[0]);
        cserv.getEvent(event).reset();
        World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice("活動「" + String.valueOf(event) + "」即將在「頻道" + cserv.getChannel() + "」舉行,請至維多、天城、玩具城找NPC參加活動。 "));
        return "";
    }
}
