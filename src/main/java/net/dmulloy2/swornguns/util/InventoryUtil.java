package net.dmulloy2.swornguns.util;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Util dealing with Inventories
 * 
 * @author dmulloy2
 */

public class InventoryUtil
{
	private InventoryUtil() { }

	/**
	 * Returns whether or not a given inventory is empty
	 * 
	 * @param inventory
	 *        - {@link Inventory} to check
	 */
	public static boolean isEmpty(Inventory inventory)
	{
		for (ItemStack stack : inventory.getContents())
		{
			if (stack != null && stack.getType() != Material.AIR)
				return false;
		}

		if (inventory instanceof PlayerInventory)
		{
			PlayerInventory pInventory = (PlayerInventory) inventory;
			if (pInventory.getHelmet() != null)
				return false;

			if (pInventory.getChestplate() != null)
				return false;

			if (pInventory.getLeggings() != null)
				return false;

			if (pInventory.getBoots() != null)
				return false;
		}

		return true;
	}

	/**
	 * Whether or not a Player's inventory has room for a given item
	 * 
	 * @param item
	 *        - {@link ItemStack} to attempt to add
	 * @param player
	 *        - Player whose inventory is being checked
	 */
	public static boolean hasRoom(ItemStack item, Player player)
	{
		int maxStackSize = (item.getMaxStackSize() == -1) ? player.getInventory().getMaxStackSize() : item.getMaxStackSize();
		int amount = item.getAmount();

		for (ItemStack stack : player.getInventory().getContents())
		{
			if (stack == null || stack.getType().equals(Material.AIR))
				amount -= maxStackSize;
			else if (stack.getType() == item.getType()
					&& stack.getDurability() == item.getDurability()
					&& (stack.getEnchantments().size() == 0 ? item.getEnchantments().size() == 0 : stack.getEnchantments().equals(
							item.getEnchantments())))
				amount -= maxStackSize - stack.getAmount();

			if (amount <= 0)
				return true;
		}

		return false;
	}

	/**
	 * Gives a player an item
	 * 
	 * @param player
	 *        - {@link Player} to give them item to
	 * @param item
	 *        - {@link ItemStack} to give the player
	 * @return Leftovers, if any
	 */
	public static Map<Integer, ItemStack> giveItem(Player player, ItemStack item)
	{
		if (hasRoom(item, player))
			return addItems(player.getInventory(), item);

		return null;
	}

	/**
	 * Gives a player items
	 * 
	 * @param player
	 *        - {@link Player} to give them item to
	 * @param items
	 *        - Items to give the player
	 * @return Leftovers, if any
	 */
	public static Map<Integer, ItemStack> giveItems(Player player, ItemStack... items)
	{
		return addItems(player.getInventory(), items);
	}

	// ---- Internal Methods ---- //

	private static Map<Integer, ItemStack> addItems(Inventory inventory, ItemStack... items)
	{
		return addOversizedItems(inventory, 0, items);
	}

	private static Map<Integer, ItemStack> addOversizedItems(Inventory inventory, int oversizedStacks, ItemStack... items)
	{
		Map<Integer, ItemStack> leftover = new HashMap<Integer, ItemStack>();

		ItemStack[] combined = new ItemStack[items.length];
		for (int i = 0; i < items.length; i++)
		{
			if (items[i] == null || items[i].getAmount() < 1)
				continue;

			for (int j = 0; j < combined.length; j++)
			{
				if (combined[j] == null)
				{
					combined[j] = items[i].clone();
					combined[j].setData(items[i].getData());
					break;
				}

				if (combined[j].isSimilar(items[i]))
				{
					combined[j].setAmount(combined[j].getAmount() + items[i].getAmount());
					break;
				}
			}
		}

		for (int i = 0; i < combined.length; i++)
		{
			ItemStack item = combined[i];
			if (item == null)
				continue;

			while (true)
			{
				// Do we already have a stack of it?
				int maxAmount = oversizedStacks > item.getType().getMaxStackSize() ? oversizedStacks : item.getType().getMaxStackSize();
				int firstPartial = firstPartial(inventory, item, maxAmount);

				// Drat! no partial stack
				if (firstPartial == -1)
				{
					// Find a free spot!
					int firstFree = inventory.firstEmpty();

					if (firstFree == -1)
					{
						// No space at all!
						leftover.put(i, item);
						break;
					}
					else
					{
						// More than a single stack!
						if (item.getAmount() > maxAmount)
						{
							ItemStack stack = item.clone();
							stack.setData(item.getData());
							stack.setAmount(maxAmount);
							inventory.setItem(firstFree, stack);
							item.setAmount(item.getAmount() - maxAmount);
						}
						else
						{
							// Just store it
							inventory.setItem(firstFree, item);
							break;
						}
					}
				}
				else
				{
					// So, apparently it might only partially fit, well lets do
					// just that
					ItemStack partialItem = inventory.getItem(firstPartial);

					int amount = item.getAmount();
					int partialAmount = partialItem.getAmount();

					// Check if it fully fits
					if (amount + partialAmount <= maxAmount)
					{
						partialItem.setAmount(amount + partialAmount);
						break;
					}

					// It fits partially
					partialItem.setAmount(maxAmount);
					item.setAmount(amount + partialAmount - maxAmount);
				}
			}
		}

		return leftover;
	}

	private static int firstPartial(Inventory inventory, ItemStack item, int maxAmount)
	{
		if (item == null)
			return -1;

		ItemStack[] stacks = inventory.getContents();
		for (int i = 0; i < stacks.length; i++)
		{
			ItemStack cItem = stacks[i];
			if (cItem != null && cItem.getAmount() < maxAmount && cItem.isSimilar(item))
				return i;
		}

		return -1;
	}

	// ---- SwornGuns Methods

	public static int amtItem(Inventory inventory, Material type, short dat)
	{
		int ret = 0;
		if (inventory != null)
		{
			ItemStack[] items = inventory.getContents();
			for (int slot = 0; slot < items.length; slot++)
			{
				if (items[slot] != null)
				{
					Material mat = items[slot].getType();
					short duration = items[slot].getDurability();
					int amt = items[slot].getAmount();
					if ((mat == type) && ((dat == duration) || (dat == -1)))
					{
						ret += amt;
					}
				}
			}
		}

		return ret;
	}

	public static void removeItem(Inventory inventory, Material type, short dat, int amt)
	{
		int start = amt;
		if (inventory != null)
		{
			ItemStack[] items = inventory.getContents();
			for (int slot = 0; slot < items.length; slot++)
			{
				if (items[slot] != null)
				{
					Material mat = items[slot].getType();
					short duration = items[slot].getDurability();
					int itmAmt = items[slot].getAmount();
					if ((mat == type) && ((dat == duration) || (dat == -1)))
					{
						if (amt > 0)
						{
							if (itmAmt >= amt)
							{
								itmAmt -= amt;
								amt = 0;
							}
							else
							{
								amt = start - itmAmt;
								itmAmt = 0;
							}
							if (itmAmt > 0)
							{
								inventory.getItem(slot).setAmount(itmAmt);
							}
							else
							{
								inventory.setItem(slot, null);
							}
						}
						if (amt <= 0)
							return;
					}
				}
			}
		}
	}
}