﻿/*
Zakum Altar - Summons Zakum.
*/

function act() {
    rm.changeMusic("Bgm06/FinalFight");
	rm.spawnZakum();
    rm.mapMessage("殘暴炎魔被火焰之眼的力量召喚出來了。");
	if (!rm.getPlayer().isGM()) {
		rm.getMap().startSpeedRun();
	}
}
