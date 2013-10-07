package net.dmulloy2.swornguns.util;

import net.dmulloy2.swornguns.types.Material;

/**
 * Util dealing with the loss of item id's
 * 
 * @author dmulloy2
 */

public class MaterialUtil
{
	/**
	 * Returns the {@link org.bukkit.Material} from a given string
	 * 
	 * @param string
	 *            - String to get the Material from
	 * @return The {@link org.bukkit.Material} from a given string
	 */
	public static org.bukkit.Material getMaterial(String string)
	{
		if (isInteger(string))
		{
			return getMaterial(Integer.parseInt(string));
		}
		else
		{
			return org.bukkit.Material.matchMaterial(string);
		}
	}

	/**
	 * Returns the {@link org.bukkit.Material} from a given integer
	 * 
	 * @param id
	 *            - Integer to get the Material from
	 * @return The {@link org.bukkit.Material} from a given integer
	 */
	public static org.bukkit.Material getMaterial(int id)
	{
		Material mat = Material.getMaterial(id);
		if (mat != null)
		{
			return mat.getMaterial();
		}

		return null;
	}
	
	/**
	 * Returns whether or not a String can be parsed as an Integer
	 * 
	 * @param string
	 *            - String to check
	 * @return Whether or not a String can be parsed as an Integer
	 */
	public static boolean isInteger(String string)
	{
		int ret = -1;
		try
		{
			ret = Integer.parseInt(string);
		}
		catch (Exception e)
		{
			//
		}

		return ret != -1;
	}

	/**
	 * Gets the type id for a Bukkit Material
	 * 
	 * @param mat 
	 *            - Bukkit material
	 * @return Item ID (if applicable)
	 */
	public static int getItemId(org.bukkit.Material mat)
	{
		return Material.getTypeId(mat);
	}

	/**
	 * Gets the friendly name of a material
	 * 
	 * @param s 
	 *            - Material name
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
	 *            - Item ID
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