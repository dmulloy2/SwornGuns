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

import lombok.Data;

import java.util.*;
import java.util.Map.Entry;

import com.google.common.base.Objects;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import net.dmulloy2.swornapi.types.Reloadable;
import net.dmulloy2.swornapi.util.FormatUtil;
import net.dmulloy2.swornapi.util.InventoryUtil;
import net.dmulloy2.swornguns.Config;
import net.dmulloy2.swornguns.SwornGuns;

/**
 * @author dmulloy2
 */

@Data
public class GunPlayer implements Reloadable
{
	private Material lastHeldType;
	private Gun currentlyFiring;
	private Map<Material, Gun> guns;

	private boolean errorReported;
	private boolean enabled;
	private boolean aimedIn;
	private int ticks;

	private final Player player;
	private final SwornGuns plugin;

	public GunPlayer(SwornGuns plugin, Player player)
	{
		this.plugin = plugin;
		this.player = player;
		this.enabled = true;
		this.calculateGuns();
	}

	// ---- Firing and Tick

	public void handleClick(Action action)
	{
		if (! enabled || action == Action.PHYSICAL)
			return;

		if (!player.isOnline())
		{
			plugin.getPlayers().remove(player.getUniqueId());
			unload();
			return;
		}

		ItemStack inHand = player.getInventory().getItemInMainHand();
		if (inHand.getType() == Material.AIR)
			return;

		Gun gun = getGun(inHand);
		if (gun == null)
			return;

		if (!canFireGun(gun))
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

	private void fireGun(Gun gun)
	{
		if (gun.getTimer() <= 0)
		{
			this.currentlyFiring = gun;
			gun.setFiring(true);
		}
	}

	public void tick()
	{
		this.ticks++;

		if (!player.isOnline())
		{
			plugin.getPlayers().remove(player.getUniqueId());
			unload();
			return;
		}

		ItemStack hand = player.getInventory().getItemInMainHand();
		Material handType = hand.getType();

		if (!handType.equals(lastHeldType) && handType != Material.AIR)
		{
			this.lastHeldType = handType;
		}

		if (ticks % 10 == 0)
		{
			if (getGun(hand) == null)
			{
				player.removePotionEffect(PotionEffectType.SLOWNESS);
				this.aimedIn = false;
			}
		}

		for (Gun gun : guns.values())
		{
			gun.tick();

			if (player.isDead())
			{
				gun.finishReloading();
			}

			if (gun.getMaterial() == hand.getType() && isAimedIn() && ! gun.isCanAimLeft() && ! gun.isCanAimRight())
			{
				player.removePotionEffect(PotionEffectType.SLOWNESS);
				this.aimedIn = false;
			}

			if (currentlyFiring != null && gun.getTimer() <= 0 && currentlyFiring.equals(gun))
				this.currentlyFiring = null;
		}

		renameGuns();
	}

	// ---- Getters

	public Player getPlayer()
	{
		return player;
	}

	public Gun getGun(ItemStack item)
	{
		return guns.get(item.getType());
	}

	// ---- Aim

	public boolean isAimedIn()
	{
		return aimedIn;
	}

	private void checkAim()
	{
		if (aimedIn)
		{
			getPlayer().removePotionEffect(PotionEffectType.SLOWNESS);
			this.aimedIn = false;
		}
		else
		{
			getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 12000, 4));
			this.aimedIn = true;
		}
	}

	// ---- Ammo

	public boolean checkAmmo(Gun gun, int amount)
	{
		return InventoryUtil.amount(player.getInventory(), gun.getAmmo(), (short) -1) >= amount;
	}

	public void removeAmmo(Gun gun, int amount)
	{
		if (amount <= 0)
			return;

		InventoryUtil.remove(getPlayer().getInventory(), gun.getAmmo(), (short) -1, amount);
	}

	// ---- Naming

	public void renameGuns()
	{
		PlayerInventory inv = player.getInventory();
		for (ItemStack item : inv.getContents())
		{
			if (item == null || item.getType() == Material.AIR)
			{
				continue;
			}

			Gun gun = getGun(item);
			if (gun == null)
			{
				continue;
			}

			ItemMeta meta = item.getItemMeta();
			if (meta == null)
			{
				continue;
			}

			if (canFireGun(gun))
			{
				String name = FormatUtil.format(getGunName(player, gun));
				if (! name.isEmpty())
					meta.setDisplayName(name);

				List<String> lore = gun.getLore();
				if (lore != null && ! lore.isEmpty())
					meta.setLore(lore.stream().map(FormatUtil::format).toList());

				item.setItemMeta(meta);
			}
			else if (meta.hasDisplayName() && meta.getDisplayName().contains("\u00AB"))
			{
				meta.setDisplayName(null);
				meta.setLore(null);
				item.setItemMeta(meta);
			}
		}
	}

	private String getGunName(Player player, Gun gun)
	{
		String gunName = gun.getDisplayName();
		if (gunName == null || gunName.isEmpty())
			gunName = gun.getGunName();

		if (!gun.isHasClip())
		{
			return gunName;
		}

		StringBuilder add = new StringBuilder();
		Material ammo = gun.getAmmo();
		int maxClip = gun.getMaxClipSize();
		int ammoAmtNeeded = Math.max(1, gun.getAmmoAmtNeeded());
		int amount = (int) Math.floor(InventoryUtil.amount(player.getInventory(), ammo, (short) -1)
				/ (double) ammoAmtNeeded);

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

		add.append(ChatColor.YELLOW)
				.append("    \u00AB")
				.append(leftInClip).append(" \uFFE8 ")
				.append(ammoLeft)
				.append("\u00BB");

		StringBuilder reload = new StringBuilder();
		if (gun.isReloading())
		{
			int scale = 4;
			int reloadTime = Math.max(1, gun.getReloadTime());
			int bars = (int) Math.round(scale - (((double)gun.getGunReloadTimer() * scale) / reloadTime));
			reload.append("\u25AA".repeat(Math.max(0, bars)));

			int left = scale - bars;
			reload.append("\u25AB".repeat(Math.max(0, left)));

			add.append(ChatColor.RED)
					.append("    ")
					.append(reload.reverse())
					.append("RELOADING")
					.append(reload);
		}

		return gunName + add;
	}

	// ---- Util

	public void unload()
	{
		lastHeldType = null;
		currentlyFiring = null;

		if (guns != null)
			guns.clear();
		guns = null;
	}

	public boolean canFireGun(Gun gun)
	{
		return canFireGun(gun, true);
	}

	public boolean canFireGun(Gun gun, boolean world)
	{
		Player player = getPlayer();
		if (world && Config.disabledWorlds.contains(player.getWorld().getName()))
			return false;

		return ! gun.isNeedsPermission() || player.isOp() || player.hasPermission("swornguns.fire." + gun.getGunName())
				|| player.hasPermission("swornguns.fire.*");
	}

	public void calculateGuns()
	{
		Map<Material, List<Gun>> byMaterial = new HashMap<>();

		for (Gun gun : plugin.getLoadedGuns().values())
		{
			if (canFireGun(gun, false))
			{
				Gun copy = gun.clone();
				copy.setOwner(this);

				if (! byMaterial.containsKey(copy.getMaterial()))
					byMaterial.put(copy.getMaterial(), new ArrayList<>());

				byMaterial.get(copy.getMaterial()).add(copy);
			}
		}

		Map<Material, Gun> sortedGuns = new LinkedHashMap<>();

		for (Entry<Material, List<Gun>> entry : byMaterial.entrySet())
		{
			Map<Gun, Integer> priorityMap = new HashMap<>();
			for (Gun gun : entry.getValue())
			{
				priorityMap.put(gun, gun.getPriority());
			}

			List<Entry<Gun, Integer>> sortedEntries = new ArrayList<>(priorityMap.entrySet());
			sortedEntries.sort((entry1, entry2) -> -entry1.getValue().compareTo(entry2.getValue()));

			Gun gun = sortedEntries.get(0).getKey();
			sortedGuns.put(gun.getMaterial(), gun);
		}

		this.guns = sortedGuns;
	}

	public String getName()
	{
		return player.getName();
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
		return player.getName();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == this) return true;

		if (obj instanceof GunPlayer that)
		{
			return this.player.equals(that.player);
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return Objects.hashCode(player.getUniqueId());
	}
}
