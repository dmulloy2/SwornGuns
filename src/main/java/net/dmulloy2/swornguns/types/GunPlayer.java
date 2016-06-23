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
package net.dmulloy2.swornguns.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import lombok.Data;
import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.types.MyMaterial;
import net.dmulloy2.types.Reloadable;
import net.dmulloy2.util.CompatUtil;
import net.dmulloy2.util.FormatUtil;
import net.dmulloy2.util.InventoryUtil;
import net.dmulloy2.util.Util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.google.common.base.Objects;

/**
 * @author dmulloy2
 */

@Data
public class GunPlayer implements Reloadable
{
	private ItemStack lastHeldItem;
	private Gun currentlyFiring;
	private List<Gun> guns;

	private boolean errorReported;
	private boolean enabled;
	private boolean aimedIn;
	private int ticks;

	private String name;
	private UUID uniqueId;
	private Player player;
	private SwornGuns plugin;

	public GunPlayer(SwornGuns plugin, Player player)
	{
		this.plugin = plugin;
		this.player = player;
		this.name = player.getName();
		this.uniqueId = player.getUniqueId();
		this.enabled = true;
		this.calculateGuns();
	}

	// ---- Firing and Tick

	public final void handleClick(Action action)
	{
		if (! enabled || action == Action.PHYSICAL)
			return;

		Player player = getPlayer();
		if (player == null || ! player.isOnline())
		{
			plugin.getPlayers().remove(name);
			unload();
			return;
		}

		ItemStack inHand = CompatUtil.getItemInMainHand(player);
		if (inHand == null || inHand.getType() == Material.AIR)
			return;

		Gun gun = getGun(inHand);
		if (gun == null)
			return;

		if (! canFireGun(gun))
		{
			if (! canFireGun(gun, false) && gun.isWarnIfNoPermission())
				player.sendMessage(plugin.getPrefix() + FormatUtil.format("&cYou do not have permission to fire this gun!"));
			return;
		}

		if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)
		{
			if (gun.isCanFireRight() || gun.isCanAimRight())
			{
				if (! gun.isCanAimRight())
				{
					gun.setHeldDownTicks(gun.getHeldDownTicks() + 1);
					gun.setLastFired(0);
					if (currentlyFiring == null)
					{
						fireGun(gun);
					}
				}
				else
				{
					checkAim();
				}
			}
		}
		else
		{
			if (gun.isCanFireLeft() || gun.isCanAimLeft())
			{
				if (! gun.isCanAimLeft())
				{
					gun.setHeldDownTicks(gun.getHeldDownTicks() + 1);
					gun.setLastFired(0);
					if (currentlyFiring == null)
					{
						fireGun(gun);
					}
				}
				else
				{
					checkAim();
				}
			}
		}
	}

	private final void fireGun(Gun gun)
	{
		if (gun.getTimer() <= 0)
		{
			this.currentlyFiring = gun;
			gun.setFiring(true);
		}
	}

	public final void tick()
	{
		this.ticks++;

		Player player = getPlayer();
		if (player == null || ! player.isOnline())
		{
			plugin.getPlayers().remove(name);
			unload();
			return;
		}

		ItemStack hand = CompatUtil.getItemInMainHand(player);
		this.lastHeldItem = hand;

		if (ticks % 10 == 0 && hand != null)
		{
			if (getGun(hand) == null)
			{
				player.removePotionEffect(PotionEffectType.SLOW);
				this.aimedIn = false;
			}
		}

		Iterator<Gun> iter = guns.iterator();
		while (iter.hasNext())
		{
			Gun gun = iter.next();

			// Don't tick null guns
			if (gun == null)
			{
				iter.remove();
				return;
			}

			gun.tick();

			if (player.isDead())
			{
				gun.finishReloading();
			}

			if (hand != null && gun.getMaterial().matches(hand) && isAimedIn() && ! gun.isCanAimLeft() && ! gun.isCanAimRight())
			{
				player.removePotionEffect(PotionEffectType.SLOW);
				this.aimedIn = false;
			}

			if (currentlyFiring != null && gun.getTimer() <= 0 && currentlyFiring.equals(gun))
				this.currentlyFiring = null;
		}

		renameGuns();
	}

	// ---- Getters

	public final Player getPlayer()
	{
		if (player == null && name != null)
			return player = Util.matchPlayer(name);

		return player;
	}

	public final Gun getGun(ItemStack item)
	{
		for (Gun gun : guns)
		{
			if (gun.getMaterial().matches(item))
				return gun;
		}

		return null;
	}

	// ---- Aim

	public final boolean isAimedIn()
	{
		return aimedIn;
	}

	private final void checkAim()
	{
		if (aimedIn)
		{
			getPlayer().removePotionEffect(PotionEffectType.SLOW);
			this.aimedIn = false;
		}
		else
		{
			getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 12000, 4));
			this.aimedIn = true;
		}
	}

	// ---- Ammo

	public final int getAmmoNeeded(Gun gun)
	{
		if (gun.isUnlimitedAmmo())
			return 0;

		if (plugin.isUltimateArenaEnabled())
		{
			if (plugin.getUltimateArenaHandler().isAmmoUnlimited(getPlayer()))
				return 0;
		}

		if (unlimitedAmmoEnabled())
			return 0;

		return gun.getAmmoAmtNeeded();
	}

	public final boolean checkAmmo(Gun gun, int amount)
	{
		MyMaterial ammo = gun.getAmmo();
		return InventoryUtil.amount(getPlayer().getInventory(), ammo.getMaterial(), ammo.getData()) >= amount;
	}

	public final void removeAmmo(Gun gun, int amount)
	{
		if (amount <= 0)
			return;

		MyMaterial ammo = gun.getAmmo();
		InventoryUtil.remove(getPlayer().getInventory(), ammo.getMaterial(), ammo.getData(), amount);
	}

	// ---- Naming

	public final void renameGuns()
	{
		Player player = getPlayer();
		PlayerInventory inv = player.getInventory();
		for (ItemStack item : inv.getContents())
		{
			if (item != null && item.getType() != Material.AIR)
			{
				Gun gun = getGun(item);
				if (gun != null)
				{
					if (canFireGun(gun))
					{
						ItemMeta meta = item.getItemMeta();

						String name = getGunName(player, gun);
						if (! name.isEmpty())
							meta.setDisplayName(name);

						List<String> lore = gun.getLore();
						if (lore != null && ! lore.isEmpty())
							meta.setLore(lore);

						item.setItemMeta(meta);
					}
					else
					{
						ItemMeta meta = item.getItemMeta();
						if (meta.hasDisplayName() && meta.getDisplayName().contains("\u00AB"))
						{
							meta.setDisplayName(null);
							meta.setLore(null);
							item.setItemMeta(meta);
						}
					}
				}
			}
		}
	}

	private final String getGunName(Player player, Gun gun)
	{
		StringBuilder add = new StringBuilder();
		if (gun.isHasClip())
		{
			MyMaterial ammo = gun.getAmmo();
			int maxClip = gun.getMaxClipSize();
			int ammoAmtNeeded = Math.max(1, gun.getAmmoAmtNeeded());
			int amount = (int) Math.floor(InventoryUtil.amount(player.getInventory(), ammo.getMaterial(), ammo.getData())
					/ ammoAmtNeeded);

			int leftInClip, ammoLeft;
			if (gun.getReloadType() == ReloadType.CLIP)
			{
				leftInClip = Math.max(0, gun.getClipRemaining());
				ammoLeft = (Math.max(1, gun.getClipSize()) * amount) - leftInClip;
			}
			else
			{
				ammoLeft = Math.max(0, amount - maxClip + gun.getRoundsFired());
				leftInClip = amount - ammoLeft;
			}

			add.append(ChatColor.YELLOW + "    \u00AB" + leftInClip + " \uFFE8 " + ammoLeft + "\u00BB");

			StringBuilder reload = new StringBuilder();
			if (gun.isReloading())
			{
				int scale = 4;
				int reloadTime = Math.max(1, gun.getReloadTime());
				int bars = Math.round(scale - ((gun.getGunReloadTimer() * scale) / reloadTime));
				for (int i = 0; i < bars; i++)
				{
					reload.append("\u25AA");
				}

				int left = scale - bars;
				for (int ii = 0; ii < left; ii++)
				{
					reload.append("\u25AB");
				}

				add.append(ChatColor.RED + "    " + reload.reverse().toString() + "RELOADING" + reload.toString());
			}
		}

		return gun.getName() + add.toString();
	}

	// ---- Util

	public final void unload()
	{
		lastHeldItem = null;
		currentlyFiring = null;

		if (guns != null)
			guns.clear();
		guns = null;

		uniqueId = null;
		player = null;
		plugin = null;
	}

	public final boolean canFireGun(Gun gun)
	{
		return canFireGun(gun, true);
	}

	public final boolean canFireGun(Gun gun, boolean world)
	{
		Player player = getPlayer();
		if (world && plugin.getDisabledWorlds().contains(player.getWorld().getName()))
			return false;

		return ! gun.isNeedsPermission() || player.isOp() || player.hasPermission("swornguns.fire." + gun.getFileName())
				|| player.hasPermission("swornguns.fire.*");
	}

	public final void calculateGuns()
	{
		Map<MyMaterial, List<Gun>> byMaterial = new HashMap<>();

		for (Gun gun : plugin.getLoadedGuns().values())
		{
			if (canFireGun(gun, false))
			{
				Gun copy = gun.clone();
				copy.setOwner(this);

				if (! byMaterial.containsKey(copy.getMaterial()))
					byMaterial.put(copy.getMaterial(), new ArrayList<Gun>());

				byMaterial.get(copy.getMaterial()).add(copy);
			}
		}

		List<Gun> sortedGuns = new ArrayList<Gun>();

		for (Entry<MyMaterial, List<Gun>> entry : byMaterial.entrySet())
		{
			Map<Gun, Integer> priorityMap = new HashMap<Gun, Integer>();
			for (Gun gun : entry.getValue())
			{
				priorityMap.put(gun, gun.getPriority());
			}

			List<Entry<Gun, Integer>> sortedEntries = new ArrayList<Entry<Gun, Integer>>(priorityMap.entrySet());
			Collections.sort(sortedEntries, new Comparator<Entry<Gun, Integer>>()
			{
				@Override
				public int compare(Entry<Gun, Integer> entry1, Entry<Gun, Integer> entry2)
				{
					return -entry1.getValue().compareTo(entry2.getValue());
				}
			});

			Gun gun = sortedEntries.get(0).getKey();
			sortedGuns.add(gun);
		}

		this.guns = sortedGuns;
	}

	// ---- Integration

	public final boolean unlimitedAmmoEnabled()
	{
		if (plugin.isSwornRPGEnabled())
			return plugin.getSwornRPGHandler().isUnlimitedAmmoEnabled(player);

		return false;
	}

	// ---- Generic Methods

	@Override
	public void reload()
	{
		// Clear the list
		guns.clear();

		// Recalculate
		calculateGuns();
	}

	@Override
	public String toString()
	{
		return name;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null) return false;
		if (obj == this) return true;

		if (obj instanceof GunPlayer)
		{
			GunPlayer that = (GunPlayer) obj;
			return this.player.equals(that.player);
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return Objects.hashCode(player);
	}
}
