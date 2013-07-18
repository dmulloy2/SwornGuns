package net.dmulloy2.swornguns.util;

import net.dmulloy2.swornguns.gun.Gun;

import org.bukkit.entity.Player;

public class PermissionInterface {
	public static boolean checkPermission(Player player, String command) {
		return (player.isOp() || player.hasPermission(command));
	}
	
	public static boolean canFireGun(Player player, Gun gun) {
		if (! gun.needsPermission) {
			return true;
		}
		
		return checkPermission(player, gun.node);
	}
}