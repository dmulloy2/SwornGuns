package net.dmulloy2.swornguns.util;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Helper for Inventories
 * 
 * @author dmulloy2
 */

public class InventoryHelper
{
	public static boolean isEmpty(PlayerInventory inventory)
	{
		if (inventory != null)
		{
			for (ItemStack stack : inventory.getContents())
			{
				if (stack != null && stack.getType() != Material.AIR)
				{
					return false;
				}
			}

			if (inventory.getHelmet() != null)
				return false;

			if (inventory.getChestplate() != null)
				return false;

			if (inventory.getLeggings() != null)
				return false;

			if (inventory.getBoots() != null)
				return false;
		}

		return true;
	}

	public static int firstPartial(final Inventory inventory, final ItemStack item, final int maxAmount)
	{
		if (item == null)
		{
			return -1;
		}
		final ItemStack[] stacks = inventory.getContents();
		for (int i = 0; i < stacks.length; i++)
		{
			final ItemStack cItem = stacks[i];
			if (cItem != null && cItem.getAmount() < maxAmount && cItem.isSimilar(item))
			{
				return i;
			}
		}
		return -1;
	}

	public static boolean addAllItems(final Inventory inventory, final ItemStack... items)
	{
		final Inventory fakeInventory = Bukkit.getServer().createInventory(null, inventory.getType());
		fakeInventory.setContents(inventory.getContents());
		if (addItems(fakeInventory, items).isEmpty())
		{
			addItems(inventory, items);
			return true;
		}
		return false;
	}

	public static Map<Integer, ItemStack> addItem(final Player player, final ItemStack item)
	{
		if (hasRoom(item, player))
			return addItems(player.getInventory(), item);

		return null;
	}

	public static Map<Integer, ItemStack> addItems(final Inventory inventory, final ItemStack... items)
	{
		return addOversizedItems(inventory, 0, items);
	}

	public static Map<Integer, ItemStack> addOversizedItems(final Inventory inventory, final int oversizedStacks, final ItemStack... items)
	{
		final Map<Integer, ItemStack> leftover = new HashMap<Integer, ItemStack>();

		ItemStack[] combined = new ItemStack[items.length];
		for (int i = 0; i < items.length; i++)
		{
			if (items[i] == null || items[i].getAmount() < 1)
			{
				continue;
			}
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
			final ItemStack item = combined[i];
			if (item == null)
			{
				continue;
			}

			while (true)
			{
				// Do we already have a stack of it?
				final int maxAmount = oversizedStacks > item.getType().getMaxStackSize() ? oversizedStacks : item.getType().getMaxStackSize();
				final int firstPartial = firstPartial(inventory, item, maxAmount);

				// Drat! no partial stack
				if (firstPartial == -1)
				{
					// Find a free spot!
					final int firstFree = inventory.firstEmpty();

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
							final ItemStack stack = item.clone();
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
					final ItemStack partialItem = inventory.getItem(firstPartial);

					final int amount = item.getAmount();
					final int partialAmount = partialItem.getAmount();

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

	public static boolean hasRoom(ItemStack item, Player player)
	{
		final int maxStackSize = (item.getMaxStackSize() == -1) ? player.getInventory().getMaxStackSize() : item.getMaxStackSize();
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