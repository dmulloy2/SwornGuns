package net.dmulloy2.swornguns.handlers;

import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.types.Gun;
import net.dmulloy2.swornguns.types.Permission;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author dmulloy2
 */

public class PermissionHandler
{
	private final SwornGuns plugin;
	public PermissionHandler(SwornGuns plugin)
	{
		this.plugin = plugin;
	}

	public final boolean hasPermission(CommandSender sender, Permission permission)
	{
		return (permission == null) ? true : hasPermission(sender, getPermissionString(permission));
	}

	public final boolean hasPermission(CommandSender sender, String permission)
	{
		if (sender instanceof Player)
		{
			Player p = (Player) sender;
			return (p.hasPermission(permission) || p.isOp());
		}

		return true;
	}
	
	public final boolean canFireGun(Player player, Gun gun)
	{
		return (gun.isNeedsPermission() ? hasPermission(player, gun.getNode()) : true);
	}

	public final String getPermissionString(Permission permission)
	{
		return plugin.getName() + "." + permission.getNode().toLowerCase();
	}
}