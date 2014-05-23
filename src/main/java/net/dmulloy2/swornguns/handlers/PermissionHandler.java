package net.dmulloy2.swornguns.handlers;

import lombok.AllArgsConstructor;
import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.types.Gun;
import net.dmulloy2.swornguns.types.Permission;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author dmulloy2
 */

@AllArgsConstructor
public class PermissionHandler
{
	private final SwornGuns plugin;

	public final boolean hasPermission(CommandSender sender, Permission permission)
	{
		return permission == null || hasPermission(sender, getPermissionString(permission));
	}

	public final boolean hasPermission(CommandSender sender, String permission)
	{
		return sender.hasPermission(permission) || sender.isOp();
	}

	public final boolean canFireGun(Player player, Gun gun)
	{
		if (gun.isNeedsPermission())
		{
			return hasPermission(player, "swornguns.fire." + gun.getFileName()) || player.hasPermission("swornguns.fire.*");
		}

		return true;
	}

	public final String getPermissionString(Permission permission)
	{
		return plugin.getName() + "." + permission.getNode().toLowerCase();
	}
}