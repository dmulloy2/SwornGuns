/**
 * SwornGuns - Guns in Minecraft
 * Copyright (C) 2012 - 2015 MineSworn
 * Copyright (C) 2013 - 2015 dmulloy2
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
