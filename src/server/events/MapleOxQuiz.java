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
import client.MapleStat;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import server.Timer.*;
import server.events.MapleOxQuizFactory.MapleOxQuizEntry;
import server.maps.MapleMap;
import tools.MaplePacketCreator;
import tools.Pair;

public class MapleOxQuiz extends MapleEvent {

    private ScheduledFuture<?> oxSchedule, oxSchedule2;
    private int timesAsked = 0;

    public MapleOxQuiz(final int world, final int channel, final int[] mapid) {
        super(world, channel, mapid);
    }

    private void resetSchedule() {
        if (oxSchedule != null) {
            oxSchedule.cancel(false);
            oxSchedule = null;
        }
        if (oxSchedule2 != null) {
            oxSchedule2.cancel(false);
            oxSchedule2 = null;
        }
    }

    @Override
    public void onMapLoad(MapleCharacter chr) {
        if (chr.getMapId() == mapid[0] && !chr.isGM()) {
            chr.canTalk_event(false);
        }
    }

    @Override
    public void reset() {
        super.reset();
        getMap(0).getPortal("join00").setPortalState(false);
        resetSchedule();
        timesAsked = 0;
    }

    @Override
    public void unreset() {
        super.unreset();
        getMap(0).getPortal("join00").setPortalState(true);
        resetSchedule();
    }
    //apparently npc says 10 questions

    @Override
    public void startEvent() {
        sendQuestion();
    }

    public void sendQuestion() {
        sendQuestion(getMap(0));
    }

    public void sendQuestion(final MapleMap toSend) {
        if (oxSchedule2 != null) {
            oxSchedule2.cancel(false);
        }
        oxSchedule2 = autoEventTimer.getInstance().schedule(new Runnable() {


            @Override
            public void run() {
                int number = 0;
                final int enditem = 9 - timesAsked;
                for (MapleCharacter mc : toSend.getCharactersThreadsafe()) {
                    if (mc.isGM() || !mc.isAlive()) {
                        number++;
                    }
                }
                if (toSend.getCharactersSize() - number <= 1 || timesAsked == 10) {
                    toSend.broadcastMessage(MaplePacketCreator.getItemNotice("本次活動已經結束。"));
                    unreset();
                    for (MapleCharacter chr : toSend.getCharactersThreadsafe()) {
                        if (chr != null && !chr.isGM() && chr.isAlive()) {
                            chr.canTalk_event(true);
                            //chr.finishAchievement(19);
                            givePrize(chr);
                            warpBack(chr);
                        }
                    }
                    //prizes here
                    return;
                }
                final Entry<Pair<Integer, Integer>, MapleOxQuizEntry> question = MapleOxQuizFactory.getInstance().grabRandomQuestion();
                toSend.broadcastMessage(MaplePacketCreator.showOXQuiz(question.getKey().left, question.getKey().right, true));
                toSend.broadcastMessage(MaplePacketCreator.getClock(12)); //quickly change to 12
                if (oxSchedule != null) {
                    oxSchedule.cancel(false);
                }
                oxSchedule = EventTimer.getInstance().schedule(new Runnable() {

                    @Override
                    public void run() {
                        toSend.broadcastMessage(MaplePacketCreator.showOXQuiz(question.getKey().left, question.getKey().right, false));
                        timesAsked++;
                        for (MapleCharacter chr : toSend.getCharactersThreadsafe()) {
                            if (chr != null && !chr.isGM() && chr.isAlive()) { // make sure they aren't null... maybe something can happen in 12 seconds.
                                if (!isCorrectAnswer(chr, question.getValue().getAnswer())) {
                                    chr.getStat().setHp((short) 0);
                                    chr.updateSingleStat(MapleStat.HP, 0);
                                } else {
                                    chr.dropMessage(6, "目前已經第:" + timesAsked + "題/距離活動結束還有:" + enditem + "題");
                                    chr.gainExp(3000, true, true, false);
                                }
                            }
                        }
                        sendQuestion();
                    }
                }, 12000); // Time to answer = 30 seconds ( Ox Quiz packet shows a 30 second timer.
            }
        }, 10000);
    }

    private boolean isCorrectAnswer(MapleCharacter chr, int answer) {
        double x = chr.getPosition().getX();
        double y = chr.getPosition().getY();
        if ((x > -234 && y > -26 && answer == 0) || (x < -234 && y > -26 && answer == 1)) {
            chr.dropMessage(6, "恭喜回答正確！"); //i think this is its own packet
            return true;
        }
        chr.dropMessage(6, "很抱歉答錯了！");
        return false;
    }
}
