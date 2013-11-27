package net.dmulloy2.swornguns.listeners;

import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.types.Gun;
import net.dmulloy2.swornguns.types.GunPlayer;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * @author dmulloy2
 */

public class PlayerListener implements Listener
{
	private final SwornGuns plugin;
	public PlayerListener(SwornGuns plugin)
	{
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		plugin.onJoin(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerKick(PlayerKickEvent event)
	{
		if (! event.isCancelled())
		{
			onDisconnect(event.getPlayer());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event)
	{
		onDisconnect(event.getPlayer());
	}

	public void onDisconnect(Player player)
	{
		plugin.onQuit(player);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerDropItem(PlayerDropItemEvent event)
	{
		Item dropped = event.getItemDrop();
		Player dropper = event.getPlayer();
		GunPlayer gp = plugin.getGunPlayer(dropper);
		if (gp != null)
		{
			ItemStack lastHold = gp.getLastItemHeld();
			if (lastHold != null)
			{
				Gun gun = gp.getGun(dropped.getItemStack().getType());
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
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerInteract(PlayerInteractEvent event)
	{
		if (event.hasItem())
		{
			ItemStack item = event.getItem();
			if (item != null)
			{
				String clickType = "";
				Action action = event.getAction();

				if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK || action == Action.PHYSICAL)
					clickType = "left";
				else
					clickType = "right";

				Player player = event.getPlayer();
				if (! plugin.getConfig().getStringList("disabledWorlds").contains(player.getWorld().getName()))
				{
					GunPlayer gp = plugin.getGunPlayer(player);
					if (gp != null)
					{
						gp.handleClick(clickType);
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerChangedWorld(PlayerChangedWorldEvent event)
	{
		// Updates a player's guns when they change worlds,
		// Useful for per-world permissions and stuff
		if (plugin.getConfig().getBoolean("updateGunsOnWorldChange", false))
		{
			final GunPlayer gp = plugin.getGunPlayer(event.getPlayer());
	
			// This basically ensures that permissions have a chance to
			// load and accounts for async world changed event
			new BukkitRunnable()
			{
				@Override
				public void run()
				{
					gp.calculateGuns();
				}
			}.runTaskLater(plugin, 20L);
		}
	}
}