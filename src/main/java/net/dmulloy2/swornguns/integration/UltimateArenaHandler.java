/**
 * (c) 2014 dmulloy2
 */
package net.dmulloy2.swornguns.integration;

import java.util.logging.Level;

import lombok.Getter;
import net.dmulloy2.integration.IntegrationHandler;
import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.ultimatearena.UltimateArena;
import net.dmulloy2.ultimatearena.arenas.Arena;
import net.dmulloy2.util.Util;

import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

/**
 * @author dmulloy2
 */

public class UltimateArenaHandler extends IntegrationHandler
{
	private @Getter boolean enabled;
	private @Getter UltimateArena ultimateArena;

	private final SwornGuns plugin;
	public UltimateArenaHandler(SwornGuns plugin)
	{
		this.plugin = plugin;
		this.setup();
	}

	@Override
	public final void setup()
	{
		try
		{
			PluginManager pm = plugin.getServer().getPluginManager();
			if (pm.getPlugin("UltimateArena") != null)
			{
				ultimateArena = (UltimateArena) pm.getPlugin("UltimateArena");
				plugin.getLogHandler().log("UltimateArena integration successful!");
				enabled = true;
			}
		}
		catch (Throwable ex)
		{
			plugin.getLogHandler().debug(Level.WARNING, Util.getUsefulStack(ex, "setting up UA integration"));
			enabled = false;
		}
	}

	public final boolean isInGunArena(Player player)
	{
		try
		{
			if (enabled && ultimateArena != null)
			{
				Arena arena = ultimateArena.getArena(player);
				return arena != null && arena.getConfig().isUnlimitedAmmo();
			}

			return false;
		}
		catch (Throwable ex)
		{
			plugin.getLogHandler().debug(Level.WARNING, Util.getUsefulStack(ex, "isInGunArena(" + player.getName() + ")"));
			return false;
		}
	}

	public final boolean isInArena(Player player)
	{
		try
		{
			if (enabled && ultimateArena != null)
			{
				Arena arena = ultimateArena.getArena(player);
				return arena != null;
			}
		} catch (Throwable ex) { }
		return false;
	}
}