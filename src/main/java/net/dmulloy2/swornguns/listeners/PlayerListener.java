package net.dmulloy2.swornguns.listeners;

import lombok.AllArgsConstructor;
import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.types.Gun;
import net.dmulloy2.swornguns.types.GunPlayer;

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
		ItemStack lastHold = gp.getLastHeldItem();
		if (lastHold != null)
		{
			Gun gun = gp.getGun(dropped.getItemStack());
			if (gun != null)
			{
				if (lastHold.getType() == dropped.getItemStack().getType())
				{
					if (gun.isHasClip() && gun.isChanged() && gun.isReloadGunOnDrop())
					{
						gun.reloadGun();
						event.setCancelled(true);
					}
				}
			}
		}
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
		if (plugin.getConfig().getBoolean("updateGunsOnWorldChange", false))
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