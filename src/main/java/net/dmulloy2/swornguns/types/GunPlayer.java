package net.dmulloy2.swornguns.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import lombok.Data;
import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornrpg.types.PlayerData;
import net.dmulloy2.types.MyMaterial;
import net.dmulloy2.types.Reloadable;
import net.dmulloy2.util.FormatUtil;
import net.dmulloy2.util.InventoryUtil;
import net.dmulloy2.util.Util;

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

@Data
public class GunPlayer implements Reloadable
{
	private ItemStack lastHeldItem;
	private Gun currentlyFiring;
	private List<Gun> guns;

	private boolean enabled;
	private boolean aimedIn;
	private int ticks;

	private final String name;
	private final UUID uniqueId;
	private final Player controller;

	private final SwornGuns plugin;
	public GunPlayer(SwornGuns plugin, Player player)
	{
		this.plugin = plugin;
		this.controller = player;
		this.name = player.getName();
		this.uniqueId = player.getUniqueId();
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

		Gun gun = getGun(inHand);
		if (gun == null)
			return;

		if (! canFireGun(gun))
		{
			if (! canFireGun(gun, false) && gun.isWarnIfNoPermission())
				controller.sendMessage(plugin.getPrefix() + FormatUtil.format("&cYou do not have permission to fire this gun!"));
			return;
		}

		if (clickType.equals("right"))
		{
			if (gun.isCanAimRight())
			{
				checkAim();
				return;
			}

			if (gun.isCanFireRight())
			{
				gun.setHeldDownTicks(gun.getHeldDownTicks() + 1);
				gun.setLastFired(0);
				if (currentlyFiring == null)
					fireGun(gun);
				return;
			}
		}
		else if (clickType.equals("left"))
		{
			if (gun.isCanAimLeft())
			{
				checkAim();
				return;
			}

			if (gun.isCanFireLeft())
			{
				gun.setHeldDownTicks(gun.getHeldDownTicks() + 1);
				gun.setLastFired(0);
				if (currentlyFiring == null)
					fireGun(gun);
				return;
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

		if (controller == null || ! controller.isOnline())
		{
			plugin.getPlayers().remove(uniqueId);
			unload();
			return;
		}

		ItemStack hand = controller.getItemInHand();
		this.lastHeldItem = hand;

		if (ticks % 10 == 0 && hand != null)
		{
			if (getGun(hand) == null)
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

			if (hand != null && gun.getMaterial().matches(hand) && isAimedIn() && ! gun.isCanAimLeft() && ! gun.isCanAimRight())
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

	public final Player getPlayer()
	{
		return controller;
	}

	public final ItemStack getLastItemHeld()
	{
		return lastHeldItem;
	}

	public final Gun getGun(ItemStack item)
	{
		return getGun(new MyMaterial(item.getType(), item.getDurability()));
	}

	private final Gun getGun(MyMaterial material)
	{
		for (Gun gun : guns)
		{
			if (gun.getMaterial().equals(material))
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
			controller.removePotionEffect(PotionEffectType.SLOW);
			this.aimedIn = false;
		}
		else
		{
			controller.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 4));
			this.aimedIn = true;
		}
	}

	// ---- Ammo

	public final int getAmmoNeeded(Gun gun)
	{
		if (gun.isUnlimitedAmmo())
			return 0;

		if (plugin.getUltimateArenaHandler().isEnabled())
		{
			if (plugin.getUltimateArenaHandler().isAmmoUnlimited(controller))
				return 0;
		}

		if (unlimitedAmmoEnabled())
			return 0;

		return gun.getAmmoAmtNeeded();
	}

	public final boolean checkAmmo(Gun gun, int amount)
	{
		MyMaterial ammo = gun.getAmmo();
		return InventoryUtil.amount(controller.getInventory(), ammo.getMaterial(), ammo.getData()) >= amount;
	}

	public final void removeAmmo(Gun gun, int amount)
	{
		if (amount <= 0)
			return;

		MyMaterial ammo = gun.getAmmo();
		InventoryUtil.remove(controller.getInventory(), ammo.getMaterial(), ammo.getData(), amount);
	}

	// ---- Naming

	public final void renameGuns()
	{
		PlayerInventory inv = controller.getInventory();
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
			MyMaterial ammo = gun.getAmmo();
			int maxClip = gun.getMaxClipSize();
			int amount = (int) Math.floor(InventoryUtil.amount(controller.getInventory(), ammo.getMaterial(), ammo.getData())
					/ gun.getAmmoAmtNeeded());
			int ammoLeft = amount - maxClip + gun.getRoundsFired();
			if (ammoLeft < 0)
				ammoLeft = 0;
			int leftInClip = amount - ammoLeft;

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

	public final void unload()
	{
		this.guns = null;
		this.lastHeldItem = null;
	}

	public final boolean canFireGun(Gun gun)
	{
		return canFireGun(gun, true);
	}

	public final boolean canFireGun(Gun gun, boolean world)
	{
		if (world && plugin.getDisabledWorlds().contains(controller.getWorld().getName()))
			return false;

		if (gun.isNeedsPermission())
			return plugin.getPermissionHandler().hasPermission(controller, "swornguns.fire." + gun.getFileName())
					|| plugin.getPermissionHandler().hasPermission(controller, "swornguns.fire.*");
		return true;
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

	private PlayerData data;

	public final boolean unlimitedAmmoEnabled()
	{
		try
		{
			if (! plugin.getSwornRPGHandler().isEnabled())
				return false;

			if (data == null)
				data = plugin.getSwornRPGHandler().getPlayerData(controller);

			return data != null && data.isUnlimitedAmmoEnabled();
		} catch (Throwable ex) { }
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
		if (obj instanceof GunPlayer)
		{
			GunPlayer that = (GunPlayer) obj;
			return this.uniqueId.equals(that.uniqueId);
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		int hash = 97;
		hash *= 1 + uniqueId.hashCode();
		return hash;
	}
}