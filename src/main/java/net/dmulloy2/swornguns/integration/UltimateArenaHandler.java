/**
 * (c) 2014 dmulloy2
 */
package net.dmulloy2.swornguns.integration;

import lombok.Getter;
import net.dmulloy2.integration.IntegrationHandler;
import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.ultimatearena.UltimateArena;
import net.dmulloy2.ultimatearena.arenas.Arena;
import net.dmulloy2.ultimatearena.types.FieldType;

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
				enabled = true;
			}
		}
		catch (Throwable ex)
		{
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
				return arena != null && arena.getType() != FieldType.HUNGER;
			}
		} catch (Throwable ex) { }
		return false;
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