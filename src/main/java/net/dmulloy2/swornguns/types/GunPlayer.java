package net.dmulloy2.swornguns.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import lombok.Getter;
import lombok.Setter;
import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.util.FormatUtil;
import net.dmulloy2.swornguns.util.InventoryHelper;
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
		else if (gun.isWarnIfNoPermission())
		{
			controller.sendMessage(FormatUtil.format("&cYou do not have permission to fire this gun!"));
		}
	}

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

	public final void tick()
	{
		ticks++;

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

	public final void renameGuns()
	{
		PlayerInventory inv = controller.getInventory();
		for (ItemStack item : inv.getContents())
		{
			if (item != null && item.getType() != Material.AIR)
			{
				String name = getGunName(item);
				if (! name.isEmpty())
				{
					ItemMeta meta = item.getItemMeta();
					if (meta.hasDisplayName() && meta.getDisplayName().equals(name))
						continue;

					meta.setDisplayName(name);
					List<String> lore = getGunLore(item);
					if (lore != null && ! lore.isEmpty())
						meta.setLore(lore);
					
					item.setItemMeta(meta);
				}
			}
		}
	}

	private final List<String> getGunLore(ItemStack item)
	{
		Gun gun = getGun(item.getType());
		if (gun != null)
		{
			if (plugin.getPermissionHandler().canFireGun(controller, gun))
			{
				return gun.getLore();
			}
		}

		return null;
	}

	private final String getGunName(ItemStack item)
	{
		Gun gun = getGun(item.getType());
		if (gun != null)
		{
			if (plugin.getPermissionHandler().canFireGun(controller, gun))
			{
				return getGunName(gun);
			}
		}

		return "";
	}

	private final String getGunName(Gun gun)
	{
		StringBuilder add = new StringBuilder();
		if (gun.isHasClip())
		{
			int maxClip = gun.getMaxClipSize();
			int ammo = (int) Math.floor(InventoryHelper.amtItem(controller.getInventory(), gun.getAmmoType(), gun.getAmmoByte())
					/ gun.getAmmoAmtNeeded());
			int ammoLeft = ammo - maxClip + gun.getRoundsFired();
			if (ammoLeft < 0) ammoLeft = 0;
			int leftInClip = ammo - ammoLeft;

			add.append(ChatColor.YELLOW + "    \u00AB" + leftInClip + " \uFFE8 " + ammoLeft + "\u00BB");

			StringBuilder reload = new StringBuilder();
			if (gun.isReloading())
			{
				int scale = 4;
				int bars = Math.round(scale - ((gun.getGunReloadTimer()) * scale) / gun.getReloadTime());
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

	public final Player getPlayer()
	{
		return controller;
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

	public final void reloadAllGuns()
	{
		for (Gun gun : guns)
		{
			if (gun != null)
			{
				gun.reloadGun();
				gun.finishReloading();
			}
		}
	}

	public final boolean checkAmmo(Gun gun, int amount)
	{
		return InventoryHelper.amtItem(controller.getInventory(), gun.getAmmoType(), gun.getAmmoByte()) >= amount;
	}

	public final void removeAmmo(Gun gun, int amount)
	{
		if (amount == 0)
			return;

		InventoryHelper.removeItem(controller.getInventory(), gun.getAmmoType(), gun.getAmmoByte(), amount);
	}

	public final ItemStack getLastItemHeld()
	{
		return lastHeldItem;
	}

	public final Gun getGun(Material material)
	{
		for (Gun check : guns)
		{
			if (check.getGunMaterial() == material)
				return check;
		}

		return null;
	}

	public final void calculateGuns()
	{
		List<Gun> loadedGuns = new ArrayList<Gun>();

		for (Gun gun : plugin.getLoadedGuns())
		{
			if (plugin.getPermissionHandler().canFireGun(controller, gun))
			{
				Gun g = gun.copy();
				g.setOwner(this);
				loadedGuns.add(g);
			}
		}

		HashMap<Material, List<Gun>> map1 = new HashMap<Material, List<Gun>>();

		for (Gun gun : loadedGuns)
		{
			if (! map1.containsKey(gun.getGunMaterial()))
			{
				map1.put(gun.getGunMaterial(), new ArrayList<Gun>());
			}

			map1.get(gun.getGunMaterial()).add(gun);
		}

		List<Gun> sortedGuns = new ArrayList<Gun>();

		for (Entry<Material, List<Gun>> entry : map1.entrySet())
		{
			HashMap<Gun, Integer> priorityMap = new HashMap<Gun, Integer>();
			for (Gun gun : entry.getValue())
			{
				priorityMap.put(gun, gun.getPriority());
			}

			List<Entry<Gun, Integer>> sortedEntries = new ArrayList<Entry<Gun, Integer>>(priorityMap.entrySet());
			Collections.sort(sortedEntries, new Comparator<Entry<Gun, Integer>>()
			{
				@Override
				public int compare(final Entry<Gun, Integer> entry1, final Entry<Gun, Integer> entry2)
				{
					return -entry1.getValue().compareTo(entry2.getValue());
				}
			});

			Gun gun = sortedEntries.get(0).getKey();
			sortedGuns.add(gun);
		}

		this.guns = sortedGuns;
	}

	public final int getAmmoNeeded(Gun gun)
	{
		if (gun.isUnlimitedAmmo())
			return 0;

		if (isPlayerInArena() || unlimitedAmmoEnabled())
			return 0;

		return gun.getAmmoAmtNeeded();
	}

	private PlayerData data;

	public final boolean unlimitedAmmoEnabled()
	{
		if (! plugin.isUseSwornRPG())
			return false;

		if (data == null)
		{
			PlayerDataCache cache = plugin.getSwornRPG().getPlayerDataCache();
			data = cache.getData(controller);
		}

		return data.isUnlimitedAmmoEnabled();
	}

	public final boolean isPlayerInArena()
	{
		if (! plugin.isUseUltimateArena())
			return false;

		if (plugin.getUltimateArena().isInArena(controller))
		{
			Arena ar = plugin.getUltimateArena().getArenaPlayer(controller).getArena();
			return ar.getType() != FieldType.HUNGER;
		}

		return false;
	}

	@Override
	public String toString()
	{
		return controller.getName();
	}

	@Override
	public void reload()
	{
		guns.clear();
		calculateGuns();
	}
}