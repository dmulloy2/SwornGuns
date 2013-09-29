package net.dmulloy2.swornguns.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

/**
 * Util that deals with Items
 * 
 * @author dmulloy2
 */

public class ItemUtil
{
	/**
	 * Reads an ItemStack from configuration
	 * 
	 * @param string
	 *            - String to read
	 * @return ItemStack from given string
	 */
	public static ItemStack readItem(String string)
	{
		Material mat = null;

		int amt = 0;
		short dat = 0;

		Map<Enchantment, Integer> enchantments = new HashMap<Enchantment, Integer>();

		string = string.replaceAll(" ", "");
		if (string.contains(","))
		{
			String s = string.substring(0, string.indexOf(","));
			if (s.contains(":"))
			{
				mat = MaterialUtil.getMaterial(s.substring(0, s.indexOf(":")));
				
				dat = Short.parseShort(s.substring(s.indexOf(":") + 1, s.indexOf(",")));
			}
			else
			{
				mat = MaterialUtil.getMaterial(s);
			}

			amt = Integer.parseInt(string.substring(string.indexOf(",") + 1));
		}

		ItemStack ret = new ItemStack(mat, amt, dat);

		if (! enchantments.isEmpty())
		{
			for (Entry<Enchantment, Integer> entry : enchantments.entrySet())
			{
				ret.addUnsafeEnchantment(entry.getKey(), entry.getValue());
			}
		}

		return ret;
	}
}