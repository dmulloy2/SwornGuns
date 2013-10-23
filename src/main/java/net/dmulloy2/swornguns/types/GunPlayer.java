package net.dmulloy2.swornguns.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import lombok.Data;
import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.util.InventoryHelper;
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

@Data
public class GunPlayer
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

		calculateGuns();
	}

	public final void handleClick(String clickType)
	{
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
		this.ticks++;

		if (controller != null)
		{
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

			for (Gun g : guns)
			{
				if (g != null)
				{
					g.tick();

					if (controller.isDead())
					{
						g.finishReloading();
					}

					if (hand != null && g.getGunMaterial() == hand.getType() && isAimedIn() && ! g.isCanAimLeft() && ! g.isCanAimRight())
					{
						controller.removePotionEffect(PotionEffectType.SLOW);
						this.aimedIn = false;
					}

					if (currentlyFiring != null && g.getTimer() <= 0 && currentlyFiring.equals(g))
						this.currentlyFiring = null;
				}
			}
		}

		renameGuns();
	}

	private final void renameGuns()
	{
		PlayerInventory inv = controller.getInventory();
		for (ItemStack item : inv.getContents())
		{
			if (item != null && item.getType() != Material.AIR)
			{
				String name = getGunName(item);
				if (name != null && ! name.isEmpty())
				{
					ItemMeta meta = item.getItemMeta();
					meta.setDisplayName(name);
					item.setItemMeta(meta);
				}
			}
		}
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

//	private final List<Gun> getGunsByType(ItemStack item)
//	{
//		List<Gun> ret = new ArrayList<Gun>();
//		for (Gun gun : guns)
//		{
//			if (gun.getGunMaterial() == item.getType())
//			{
//				ret.add(gun);
//			}
//		}
//
//		return ret;
//	}

//	protected String getGunName(ItemStack item)
//	{
//		String ret = "";
//		List<Gun> tempgun = getGunsByType(item);
//		int amtGun = tempgun.size();
//		if (amtGun > 0)
//		{
//			for (Gun current : tempgun)
//			{
//				if (plugin.getPermissionHandler().canFireGun(controller, current))
//				{
//					if (current.getGunMaterial() != null && current.getGunMaterial() == item.getType())
//					{
//						byte gunDat = current.getGunByte();
//
//						@SuppressWarnings("deprecation") // TODO
//						byte itmDat = item.getData().getData();
//
//						if ((gunDat == itmDat) || (current.isIgnoreItemData()))
//						{
//							return getGunName(current);
//						}
//					}
//				}
//			}
//		}
//		return ret;
//	}

	private final String getGunName(Gun current)
	{
		String add = "";
		String refresh = "";
		if (current.isHasClip())
		{
			int leftInClip = 0;
			int ammoLeft = 0;
			int maxInClip = current.getMaxClipSize();

			int currentAmmo = (int) Math.floor(InventoryHelper.amtItem(controller.getInventory(), current.getAmmoType(),
					current.getAmmoByte()) / current.getAmmoAmtNeeded());
			ammoLeft = currentAmmo - maxInClip + current.getRoundsFired();
			if (ammoLeft < 0)
				ammoLeft = 0;
			leftInClip = currentAmmo - ammoLeft;
			add = ChatColor.YELLOW + "    \u00AB" + Integer.toString(leftInClip) + " \uFFE8 " + Integer.toString(ammoLeft) + "\u00BB";
			if (current.isReloading())
			{
				int reloadSize = 4;
				double reloadFrac = (current.getReloadTime() - current.getGunReloadTimer()) / current.getReloadTime();
				int amt = (int) Math.round(reloadFrac * reloadSize);
				for (int ii = 0; ii < amt; ii++)
				{
					refresh = refresh + "\u25AA";
				}
				for (int ii = 0; ii < reloadSize - amt; ii++)
				{
					refresh = refresh + "\u25AB";
				}

				add = ChatColor.RED + "    " + new StringBuffer(refresh).reverse() + "RELOADING" + refresh;
			}
		}

		return current.getName() + add;
	}

//	protected ItemStack setName(ItemStack item, String name)
//	{
//		ItemMeta im = item.getItemMeta();
//		im.setDisplayName(name);
//		item.setItemMeta(im);
//
//		return item;
//	}

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
		{
			return;
		}

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
		guns.clear();

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

	public final int getAmmoAmountNeeded(Gun gun)
	{
		int ret = gun.getAmmoAmtNeeded();
		if (isPlayerInArena() || unlimitedAmmoEnabled())
			ret = 0;
		
		return ret;
	}

	private PlayerData data;

	public final boolean unlimitedAmmoEnabled()
	{
		if (data == null)
		{
			if (plugin.getSwornRPG() != null)
			{
				PlayerDataCache cache = plugin.getSwornRPG().getPlayerDataCache();
				data = cache.getData(controller);
			}
		}

		return data != null && data.isUnlimitedAmmoEnabled();
	}
	
	public final boolean isPlayerInArena()
	{
		if (plugin.getUltimateArena() != null)
		{
			if (plugin.getUltimateArena().isInArena(controller))
			{
				Arena ar = plugin.getUltimateArena().getArenaPlayer(controller).getArena();
				return ar.getType() != FieldType.HUNGER;
			}
		}

		return false;
	}
	
	@Override
	public String toString()
	{
		return "GunPlayer { name = " + controller.getName() + " }";
	}
}