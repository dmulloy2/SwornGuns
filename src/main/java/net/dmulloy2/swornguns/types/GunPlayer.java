package net.dmulloy2.swornguns.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lombok.Getter;
import lombok.Setter;
import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.util.InventoryUtil;
import net.dmulloy2.swornguns.util.Util;
import net.dmulloy2.swornrpg.io.PlayerDataCache;
import net.dmulloy2.swornrpg.types.PlayerData;
import net.dmulloy2.ultimatearena.arenas.Arena;
import net.dmulloy2.ultimatearena.types.FieldType;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * @author dmulloy2
 */

@Getter @Setter
public class GunPlayer implements Reloadable
{
	private ItemStack lastHeldItem;
	private Gun currentlyFiring;
	private Player controller;
	private List<Gun> guns;

	private boolean enabled;
	private boolean aimedIn;
	private int ticks;

	private final SwornGuns plugin;

	public GunPlayer(SwornGuns plugin, Player player)
	{
		this.plugin = plugin;
		this.controller = player;
		this.enabled = true;
		this.calculateGuns();
	}

	// ---- Firing and Tick

	public final void handleClick(String clickType)
	{
		if (! enabled)
			return;

		ItemStack inHand = controller.getItemInHand();
		if (inHand == null || inHand.getType() == Material.AIR)
			return;

		Gun gun = getGun(inHand.getType());
		if (gun == null)
			return;

		if (plugin.getPermissionHandler().canFireGun(controller, gun))
		{
			if (clickType.equalsIgnoreCase("right"))
			{
				if (gun.isCanClickRight() || gun.isCanAimRight())
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
			else if (clickType.equalsIgnoreCase("left"))
			{
				if (gun.isCanClickLeft() || gun.isCanAimLeft())
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
	}

	public final void tick()
	{
		this.ticks++;

		if (controller == null)
		{
			plugin.getPlayers().remove(this);
			return;
		}

		ItemStack hand = controller.getItemInHand();
		this.lastHeldItem = hand;

		if (ticks % 10 == 0 && hand != null)
		{
			if (plugin.getGun(hand.getType()) == null)
			{
				controller.removePotionEffect(PotionEffectType.SLOW);
				this.aimedIn = false;
			}
		}

		for (Gun gun : Util.newList(guns))
		{
			// Don't tick null guns
			if (gun == null)
			{
				guns.remove(gun);
				continue;
			}

			gun.tick();

			if (controller.isDead())
			{
				gun.finishReloading();
			}

			if (hand != null && gun.getGunMaterial() == hand.getType() && isAimedIn() && ! gun.isCanAimLeft() && ! gun.isCanAimRight())
			{
				controller.removePotionEffect(PotionEffectType.SLOW);
				this.aimedIn = false;
			}

			if (currentlyFiring != null && gun.getTimer() <= 0 && currentlyFiring.equals(gun))
				this.currentlyFiring = null;
		}

		renameGuns();
	}

	// ---- Getters

	public final Gun getGun(Material material)
	{
		for (Gun check : guns)
		{
			if (check.getGunMaterial() == material)
				return check;
		}

		return null;
	}

	public final ItemStack getLastItemHeld()
	{
		return lastHeldItem;
	}

	public final Player getPlayer()
	{
		return controller;
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
			controller.removePotionEffect(PotionEffectType.SLOW);
			this.aimedIn = false;
		}
		else
		{
			controller.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 12000, 4));
			this.aimedIn = true;
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

	// ---- Ammo

	public final int getAmmoNeeded(Gun gun)
	{
		if (gun.isUnlimitedAmmo())
			return 0;

		if (isPlayerInArena() || unlimitedAmmoEnabled())
			return 0;

		return gun.getAmmoAmtNeeded();
	}

	public final boolean checkAmmo(Gun gun, int amount)
	{
		return InventoryUtil.amtItem(controller.getInventory(), gun.getAmmoType(), gun.getAmmoByte()) >= amount;
	}

	public final void removeAmmo(Gun gun, int amount)
	{
		if (amount <= 0)
			return;

		InventoryUtil.removeItem(controller.getInventory(), gun.getAmmoType(), gun.getAmmoByte(), amount);
	}

	// ---- Naming

	public final void renameGuns()
	{
		PlayerInventory inv = controller.getInventory();
		for (ItemStack item : inv.getContents())
		{
			if (item != null && item.getType() != Material.AIR)
			{
				Gun gun = getGun(item.getType());
				if (gun != null)
				{
					if (plugin.getPermissionHandler().canFireGun(controller, gun))
					{
						ItemMeta meta = item.getItemMeta();

						String name = getGunName(gun);						
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

	private final String getGunName(Gun gun)
	{
		StringBuilder add = new StringBuilder();
		if (gun.isHasClip())
		{
			int maxClip = gun.getMaxClipSize();
			int ammo = (int) Math.floor(InventoryUtil.amtItem(controller.getInventory(), gun.getAmmoType(), gun.getAmmoByte())
					/ gun.getAmmoAmtNeeded());
			int ammoLeft = ammo - maxClip + gun.getRoundsFired();
			if (ammoLeft < 0)
				ammoLeft = 0;
			int leftInClip = ammo - ammoLeft;

			add.append(ChatColor.YELLOW + "    \u00AB" + leftInClip + " \uFFE8 " + ammoLeft + "\u00BB");

			StringBuilder reload = new StringBuilder();
			if (gun.isReloading())
			{
				int scale = 4;
				int bars = Math.round(scale - ((gun.getGunReloadTimer() * scale) / gun.getReloadTime()));
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

	public final void calculateGuns()
	{
		Map<Material, List<Gun>> byMaterial = new HashMap<Material, List<Gun>>();

		for (Gun gun : plugin.getLoadedGuns())
		{
			if (plugin.getPermissionHandler().canFireGun(controller, gun))
			{
				Gun copy = gun.copy();
				copy.setOwner(this);

				if (! byMaterial.containsKey(copy.getGunMaterial()))
					byMaterial.put(copy.getGunMaterial(), new ArrayList<Gun>());

				byMaterial.get(copy.getGunMaterial()).add(gun);
			}
		}

		List<Gun> sortedGuns = new ArrayList<Gun>();

		for (Entry<Material, List<Gun>> entry : byMaterial.entrySet())
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

	public final void unload()
	{
		this.controller = null;
		this.currentlyFiring = null;

		for (Gun gun : guns)
		{
			gun.clear();
		}
	}

	// ---- Integration

	private PlayerData data;

	public final boolean unlimitedAmmoEnabled()
	{
		try
		{
			if (! plugin.isUseSwornRPG())
				return false;
	
			if (data == null)
			{
				PlayerDataCache cache = plugin.getSwornRPG().getPlayerDataCache();
				data = cache.getData(controller);
			}
	
			return data != null && data.isUnlimitedAmmoEnabled();
		} catch (Throwable ex) { }
		return false;
	}

	public final boolean isPlayerInArena()
	{
		try
		{
			if (! plugin.isUseUltimateArena())
				return false;
	
			if (plugin.getUltimateArena().isInArena(controller))
			{
				Arena ar = plugin.getUltimateArena().getArenaPlayer(controller).getArena();
				return ar.getType() != FieldType.HUNGER;
			}
		} catch (Throwable ex) { }
		return false;
	}

	// ---- Generic Methods

	@Override
	public void reload()
	{
		// Clear the list
		guns = new ArrayList<Gun>();

		// Recalculate
		calculateGuns();
	}

	@Override
	public String toString()
	{
		return "GunPlayer { name = " + controller.getName() + " }";
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof GunPlayer)
		{
			GunPlayer that = (GunPlayer) obj;
			return this.controller.getName().equals(that.controller.getName());
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		int hash = 97;
		hash *= controller.hashCode();
		return hash;
	}
}