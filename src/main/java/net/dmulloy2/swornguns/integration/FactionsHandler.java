/**
 * (c) 2014 dmulloy2
 */
package net.dmulloy2.swornguns.integration;

import net.dmulloy2.integration.IntegrationHandler;
import net.dmulloy2.swornguns.SwornGuns;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.Faction;

/**
 * @author dmulloy2
 */

public class FactionsHandler extends IntegrationHandler
{
	private boolean factionsEnabled;
	private boolean swornNationsEnabled;

	private final SwornGuns plugin;
	public FactionsHandler(SwornGuns plugin)
	{
		this.plugin = plugin;
		this.setup();
	}

	@Override
	public void setup()
	{
		try
		{
			PluginManager pm = plugin.getServer().getPluginManager();
			if (pm.isPluginEnabled("Factions"))
			{
				Plugin pl = pm.getPlugin("Factions");
				String version = pl.getDescription().getVersion();
				factionsEnabled = version.startsWith("1.6");
			}

			if (pm.isPluginEnabled("SwornNations"))
			{
				factionsEnabled = true;
				swornNationsEnabled = true;
			}

			if (factionsEnabled)
			{
				plugin.getLogHandler().log("Factions integration successful!");
			}
		}
		catch (Throwable ex)
		{
			factionsEnabled = false;
			swornNationsEnabled = false;
		}
	}

	@Override
	public boolean isEnabled()
	{
		return factionsEnabled;
	}

	public final boolean checkFactions(Player player, boolean safeZoneCheck)
	{
		return checkFactions(player.getLocation(), safeZoneCheck);
	}

	public final boolean checkFactions(Location location, boolean safeZoneCheck)
	{
		return safeZoneCheck ? isSafeZone(location) || isWarZone(location) : isWarZone(location);
	}

	private final boolean isWarZone(Location location)
	{
		if (factionsEnabled)
		{
			Faction fac = Board.getFactionAt(new FLocation(location));
			if (swornNationsEnabled)
				fac = Board.getAbsoluteFactionAt(new FLocation(location));

			return fac.isWarZone();
		}

		return false;
	}

	private final boolean isSafeZone(Location location)
	{
		if (factionsEnabled)
		{
			Faction fac = Board.getFactionAt(new FLocation(location));
			if (swornNationsEnabled)
				fac = Board.getAbsoluteFactionAt(new FLocation(location));

			return fac.isSafeZone();
		}

		return false;
	}
}