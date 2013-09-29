package net.dmulloy2.swornguns.util;

import java.text.MessageFormat;

import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

/**
 * Util used for general formatting
 * 
 * @author dmulloy2
 */

public class FormatUtil
{
	/**
	 * Formats a given string and its object
	 * 
	 * @param format
	 *            - Base string
	 * @param objects
	 *            - Objects to format in
	 * @return Formatted string
	 */
	public static String format(String format, Object... objects)
	{
		String ret = MessageFormat.format(format, objects);
		// ret = WordUtils.capitalize(ret, new char[]{'.'});
		return ChatColor.translateAlternateColorCodes('&', ret);
	}

	/**
	 * Formats a given string and its objects for logging
	 * 
	 * @param string
	 *            - Base string
	 * @param objects
	 *            - Objects to format in
	 * @return Formatted string for logging
	 */
	public static String formatLog(String string, Object... objects)
	{
		return MessageFormat.format(string, objects);
	}

	/**
	 * Returns the "Friendly" name of a material
	 * 
	 * @param mat
	 *            - Material to get the "friendly" name for
	 * @return The "friendly" name for the given material
	 */
	public static String getFriendlyName(Material mat)
	{
		return getFriendlyName(mat.toString());
	}

	/**
	 * Returns the "Friendly" name of an entity
	 * 
	 * @param mat
	 *            - Entity to get the "friendly" name for
	 * @return The "friendly" name for the given entity
	 */
	public static String getFriendlyName(EntityType entityType)
	{
		return getFriendlyName(entityType.toString());
	}

	/**
	 * Returns the "Friendly" version of a given string
	 * 
	 * @param mat
	 *            - String to get the "friendly" version for
	 * @return The "friendly" version of the given string
	 */
	public static String getFriendlyName(String string)
	{
		String ret = string.toLowerCase();
		ret = ret.replaceAll("_", " ");
		return (WordUtils.capitalize(ret));
	}

	/**
	 * Returns the proper article of a given string
	 * 
	 * @param string
	 *            - String to get the article for
	 * @return The article that should go with the string
	 */
	public static String getArticle(String string)
	{
		string = string.toLowerCase();
		if (string.startsWith("a") || string.startsWith("e") || string.startsWith("i") || string.startsWith("o") || string.startsWith("u"))
		{
			return "an";
		}

		return "a";
	}
}