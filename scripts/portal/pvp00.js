﻿load('nashorn:mozilla_compat.js');
importPackage(org.server.maps);

function enter(pi) {
	var returnMap = pi.getSavedLocation("PVP");
	pi.clearSavedLocation("PVP");
	if (returnMap < 0) {
		returnMap = 102000000;
	}
	var target = pi.getMap(returnMap);
	var portal = target.getPortal("pvp00");
	if (portal == null) {
		portal = target.getPortal(0);
	}
	pi.getPlayer().changeMap(target, portal);
	return true;
}