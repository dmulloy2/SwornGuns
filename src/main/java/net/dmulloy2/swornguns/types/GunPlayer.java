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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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

@Getter
@Accessors(fluent = true)
@EqualsAndHashCode(of={"player"})
@ToString(of={"player"})
public class GunPlayer implements Reloadable
{
	private Material lastHeldType;
	private Gun currentlyFiring;
	private Map<Material, Gun> guns;

	private @Setter boolean errorReported;
	private @Setter boolean enabled;
	private boolean aimedIn;
	private int ticks;

	private final @Getter Player player;
	private final SwornGuns plugin;

	public GunPlayer(SwornGuns plugin, Player player)
	{
		this.plugin = plugin;
		this.player = player;
		this.enabled = true;
		this.calculateGuns();
	}

	// ---- Firing and Tick

	public void handleClick(ItemStack inHand, boolean left)
	{
		if (! enabled || currentlyFiring != null)
		{
			return;
		}

		Gun gun = getGun(inHand);
		if (gun == null)
		{
			return;
		}

		if (!canFireGun(gun.data()))
		{
			if (! canFireGun(gun.data(), false) && gun.data().warnIfNoPermission())
				player.sendMessage(plugin.getPrefix() + FormatUtil.format("&cYou do not have permission to fire this gun!"));
			return;
		}

		if (left)
		{
			if (gun.data().canFireLeft() && gun.fire())
			{
				this.currentlyFiring = gun;
			}
			else if (gun.data().canAimLeft())
			{
				checkAim();
			}
		} else
		{
			if (gun.data().canFireRight() && gun.fire())
			{
				this.currentlyFiring = gun;
			}
			else if (gun.data().canAimRight())
			{
				checkAim();
			}
		}
	}

	public boolean tick()
	{
		this.ticks++;

		if (!player.isOnline())
		{
			unload();
			return false;
		}

		ItemStack hand = player.getInventory().getItemInMainHand();
		Material handType = hand.getType();

		if (!handType.equals(lastHeldType) && handType != Material.AIR)
		{
			this.lastHeldType = handType;
		}

		if (ticks % 10 == 0)
		{
			if (this.aimedIn && getGun(hand) == null)
			{
				player.removePotionEffect(PotionEffectType.SLOWNESS);
				this.aimedIn = false;
			}
		}

		ItemStack[] items = null;

		for (Gun gun : guns.values())
		{
			GunData data = gun.data();
			Material material = data.material();

			boolean changed = gun.tick();
			if (changed)
			{
				if (items == null)
				{
					items = player.getInventory().getContents();
				}

				for (ItemStack item : items)
				{
					if (item != null && item.getType() == material)
					{
						gun.applyCustomName(item);
					}
				}
			}

			if (player.isDead())
			{
				gun.finishReloading();
			}

			if (this.aimedIn && material == hand.getType() && ! data.canAimLeft() && ! data.canAimRight())
			{
				player.removePotionEffect(PotionEffectType.SLOWNESS);
				this.aimedIn = false;
			}

			if (gun.equals(currentlyFiring) && gun.timer() <= 0)
			{
				this.currentlyFiring = null;
			}
		}

		return true;
	}

	public Gun getGun(ItemStack item)
	{
		return guns.get(item.getType());
	}

	private void checkAim()
	{
		if (aimedIn)
		{
			player.removePotionEffect(PotionEffectType.SLOWNESS);
			this.aimedIn = false;
		}
		else
		{
			player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 12000, 4));
			this.aimedIn = true;
		}
	}

	// ---- Ammo

	public boolean checkAmmo(Gun gun, int amount)
	{
		return InventoryUtil.amount(player.getInventory(), gun.data().ammo()) >= amount;
	}

	public void removeAmmo(Gun gun, int amount)
	{
		if (amount <= 0)
			return;

		InventoryUtil.remove(player.getInventory(), gun.data().ammo(), amount);
	}

	public void unload()
	{
		lastHeldType = null;
		currentlyFiring = null;

		if (guns != null)
			guns.clear();
		guns = null;
	}

	public boolean canFireGun(GunData gun)
	{
		return canFireGun(gun, true);
	}

	public boolean canFireGun(GunData gun, boolean world)
	{
		if (world && Config.disabledWorlds.contains(player.getWorld().getName()))
			return false;

		return ! gun.needsPermission() || player.isOp() || player.hasPermission("swornguns.fire." + gun.gunName())
				|| player.hasPermission("swornguns.fire.*");
	}

	public void calculateGuns()
	{
		Map<Material, List<GunData>> byMaterial = new HashMap<>();

		for (GunData gun : plugin.getLoadedGuns().values())
		{
			if (canFireGun(gun, false))
			{
				if (! byMaterial.containsKey(gun.material()))
					byMaterial.put(gun.material(), new ArrayList<>());

				byMaterial.get(gun.material()).add(gun);
			}
		}

		Map<Material, Gun> sortedGuns = new HashMap<>();

		for (Entry<Material, List<GunData>> entry : byMaterial.entrySet())
		{
			List<GunData> guns = entry.getValue();
			if (guns.size() > 1)
			{
				guns.sort((g1, g2) -> -Integer.compare(g1.priority(), g2.priority()));
			}

			Gun gun = new Gun(guns.getFirst(), this, plugin);
			sortedGuns.put(entry.getKey(), gun);
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
}
