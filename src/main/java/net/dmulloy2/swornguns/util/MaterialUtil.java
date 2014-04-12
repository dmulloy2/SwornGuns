package net.dmulloy2.swornguns.util;

import net.dmulloy2.swornguns.types.Material;

import org.bukkit.Bukkit;

/**
 * Util dealing with the loss of item id's
 * 
 * @author dmulloy2
 */

public class MaterialUtil
{
	private MaterialUtil() { }

	/**
	 * Returns the {@link org.bukkit.Material} from a given string
	 * 
	 * @param string
	 *        - String to get the Material from
	 * @return The {@link org.bukkit.Material} from a given string
	 */
	public static org.bukkit.Material getMaterial(String string)
	{
		if (NumberUtil.isInt(string))
		{
			return getMaterial(Integer.parseInt(string));
		}
		else
		{
			return matchMaterial(string);
		}
	}

	@SuppressWarnings("deprecation")
	private static org.bukkit.Material matchMaterial(String string)
	{
		org.bukkit.Material material = null;

		try
		{
			material = org.bukkit.Material.matchMaterial(string);
		}
		catch (Throwable ex)
		{
		}

		if (material == null)
		{
			try
			{
				material = Bukkit.getUnsafe().getMaterialFromInternalName(string);
			}
			catch (Throwable ex)
			{
			}
		}

		return material;
	}

	/**
	 * Returns the {@link org.bukkit.Material} from a given integer
	 * 
	 * @param id
	 *        - Integer to get the Material from
	 * @return The {@link org.bukkit.Material} from a given integer
	 */
	public static org.bukkit.Material getMaterial(int id)
	{
		Material mat = Material.getMaterial(id);
		if (mat != null)
		{
			return mat.getBukkitMaterial();
		}

		return null;
	}

	/**
	 * Gets the type id for a Bukkit Material
	 * 
	 * @param mat
	 *        - Bukkit material
	 * @return Item ID (if applicable)
	 */
	public static int getItemId(org.bukkit.Material bukkitMaterial)
	{
		Material mat = Material.getByBukkitMaterial(bukkitMaterial);
		if (mat != null)
		{
			return mat.getId();
		}

		return 1; // Stone
	}

	/**
	 * Gets the friendly name of a material
	 * 
	 * @param s
	 *        - Material name
	 * @return Friendly name
	 */
	public static String getMaterialName(String s)
	{
		org.bukkit.Material mat = getMaterial(s);
		if (mat == null)
		{
			return "Null";
		}

		return FormatUtil.getFriendlyName(mat);
	}

	/**
	 * Gets the friendly name of a material
	 * 
	 * @param id
	 *        - Item ID
	 * @return Friendly name
	 */
	public static String getMaterialName(int id)
	{
		org.bukkit.Material mat = getMaterial(id);
		if (mat == null)
		{
			return "Null";
		}

		return FormatUtil.getFriendlyName(mat);
	}
}