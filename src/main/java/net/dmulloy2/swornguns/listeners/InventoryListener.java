package net.dmulloy2.swornguns.listeners;

import lombok.AllArgsConstructor;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.types.Gun;
import net.dmulloy2.swornguns.types.GunPlayer;

@AllArgsConstructor
public class InventoryListener implements Listener
{
	private final SwornGuns plugin;

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event)
	{
		if (!(event.getPlayer() instanceof Player player))
		{
			return;
		}

		GunPlayer gp = plugin.getGunPlayer(player);
		if (gp == null)
		{
			return;
		}

		// mark all guns dirty so they will be renamed
		for (Gun gun : gp.guns().values())
		{
			gun.dirty(true);
		}
	}
}
