/**
 * SwornGuns - Guns in Minecraft
 * Copyright (C) dmulloy2 <http://dmulloy2.net>
 * Copyright (C) Contributors
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
package net.dmulloy2.swornguns.listeners;

import lombok.AllArgsConstructor;

import net.dmulloy2.swornguns.Config;
import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.types.Gun;
import net.dmulloy2.swornguns.types.GunPlayer;

import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * @author dmulloy2
 */

@AllArgsConstructor
public class PlayerListener implements Listener
{
	private final SwornGuns plugin;

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		plugin.getGunPlayer(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event)
	{
		plugin.onQuit(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerDropItem(PlayerDropItemEvent event)
	{
		Item dropped = event.getItemDrop();
		GunPlayer gp = plugin.getGunPlayer(event.getPlayer());
		Material lastHold = gp.getLastHeldType();
		if (lastHold == null)
		{
			return;
		}

		Gun gun = gp.getGun(dropped.getItemStack());
		if (gun == null)
		{
			return;
		}

		if (lastHold != dropped.getItemStack().getType())
		{
			return;
		}

		if (!gun.isHasClip() || !gun.isChanged() || !gun.isReloadGunOnDrop())
		{
			return;
		}

		gun.reloadGun();
		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerInteract(PlayerInteractEvent event)
	{
		GunPlayer gp = plugin.getGunPlayer(event.getPlayer());
		gp.handleClick(event.getAction());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerChangedWorld(PlayerChangedWorldEvent event)
	{
		// Updates a player's guns when they change worlds, useful for per-world permissions
		if (Config.updateGunsOnWorldChange)
		{
			final Player player = event.getPlayer();

			// This basically ensures that permissions have a chance to
			// load and accounts for async world changed event
			new BukkitRunnable()
			{
				@Override
				public void run()
				{
					GunPlayer gp = plugin.getGunPlayer(player);
					gp.calculateGuns();
				}
			}.runTaskLater(plugin, 20L);
		}
	}
}
