/**
 * (c) 2014 dmulloy2
 */
package net.dmulloy2.swornguns.integration;

import java.util.logging.Level;

import net.dmulloy2.integration.DependencyProvider;
import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornrpg.SwornRPG;
import net.dmulloy2.swornrpg.types.PlayerData;
import net.dmulloy2.util.Util;

import org.bukkit.entity.Player;

/**
 * @author dmulloy2
 */

public class SwornRPGHandler extends DependencyProvider<SwornRPG>
{
	public SwornRPGHandler(SwornGuns plugin)
	{
		super(plugin, "SwornRPG");
	}

	public final boolean isUnlimitedAmmoEnabled(Player player)
	{
		if (! isEnabled())
			return false;

		try
		{
			PlayerData data = getDependency().getPlayerDataCache().getData(player);
			return data.isUnlimitedAmmoEnabled();
		}
		catch (Throwable ex)
		{
			handler.getLogHandler().debug(Level.WARNING, Util.getUsefulStack(ex, "isUnlimitedAmmoEnabled()"));
		}

		return false;
	}
}