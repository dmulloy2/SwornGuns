package net.dmulloy2.swornguns.util;

import org.bukkit.entity.Player;

public class PermissionInterface {
	public static boolean checkPermission(Player player, String command) {
		return (player.isOp() || player.hasPermission(command));
	}
}