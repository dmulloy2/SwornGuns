/**
 * (c) 2014 dmulloy2
 */
package net.dmulloy2.swornguns.integration;

import java.util.logging.Level;

import net.dmulloy2.integration.DependencyProvider;
import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.ultimatearena.UltimateArena;
import net.dmulloy2.ultimatearena.arenas.Arena;
import net.dmulloy2.ultimatearena.types.ArenaPlayer;
import net.dmulloy2.util.Util;

import org.bukkit.entity.Player;

/**
 * @author dmulloy2
 */

public class UltimateArenaHandler extends DependencyProvider<UltimateArena>
{
	public UltimateArenaHandler(SwornGuns plugin)
	{
		super(plugin, "UltimateArena");
	}

	public final boolean isAmmoUnlimited(Player player)
	{
		if (! isEnabled())
			return false;

		try
		{
			ArenaPlayer ap = getDependency().getArenaPlayer(player);
			if (ap != null)
			{
				Arena arena = ap.getArena();
				return arena != null && arena.getConfig().isUnlimitedAmmo();
			}

			return false;
		}
		catch (Throwable ex)
		{
			handler.getLogHandler().debug(Level.WARNING, Util.getUsefulStack(ex, "isAmmoUnlimited(" + player.getName() + ")"));
			return false;
		}
	}

	public final boolean isInArena(Player player)
	{
		if (! isEnabled())
			return false;

		try
		{
			ArenaPlayer ap = getDependency().getArenaPlayer(player);
			return ap != null && ap.getArena() != null;
		}
		catch (Throwable ex)
		{
			handler.getLogHandler().debug(Level.WARNING, Util.getUsefulStack(ex, "isInArena(" + player.getName() + ")"));
			return false;
		}
	}
}