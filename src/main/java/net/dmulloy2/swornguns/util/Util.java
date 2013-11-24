package net.dmulloy2.swornguns.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.dmulloy2.swornguns.SwornGuns;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * Base Util class
 * 
 * @author dmulloy2
 */

public class Util
{
	/**
	 * Gets the OfflinePlayer from a given string
	 * 
	 * @param pl
	 *        - String to match with a player
	 * @return Player from the given string, null if none exists
	 */
	public static Player matchPlayer(String pl)
	{
		List<Player> players = Bukkit.matchPlayer(pl);

		if (players.size() >= 1)
			return players.get(0);

		return null;
	}

	/**
	 * Gets the OfflinePlayer from a given string
	 * 
	 * @param pl
	 *        - String to match with a player
	 * @return Player from the given string, null if none exists
	 */
	public static OfflinePlayer matchOfflinePlayer(String pl)
	{
		if (matchPlayer(pl) != null)
			return matchPlayer(pl);

		for (OfflinePlayer o : Bukkit.getOfflinePlayers())
		{
			if (o.getName().equalsIgnoreCase(pl))
				return o;
		}

		return null;
	}

	/**
	 * Returns whether or not a player is banned
	 * 
	 * @param p
	 *        - OfflinePlayer to check for banned status
	 * @return Whether or not the player is banned
	 */
	public static boolean isBanned(OfflinePlayer p)
	{
		return isBanned(p.getName());
	}

	/**
	 * Returns whether or not a player is banned
	 * 
	 * @param p
	 *        - Player name to check for banned status
	 * @return Whether or not the player is banned
	 */
	public static boolean isBanned(String p)
	{
		for (OfflinePlayer banned : Bukkit.getBannedPlayers())
		{
			if (p.equalsIgnoreCase(banned.getName()))
				return true;
		}

		return false;
	}

	/**
	 * Returns a random integer out of x
	 * 
	 * @param x
	 *        - Integer the random should be out of
	 * @return A random integer out of x
	 */
	public static int random(int x)
	{
		Random rand = new Random();
		return rand.nextInt(x);
	}

	/**
	 * Returns how far two locations are from each other
	 * 
	 * @param loc1
	 *        - First location to compare
	 * @param loc2
	 *        - Second location to compare
	 * @return Integer value of how far away they are
	 */
	public static int pointDistance(Location loc1, Location loc2)
	{
		int p1x = (int) loc1.getX();
		int p1y = (int) loc1.getY();
		int p1z = (int) loc1.getZ();

		int p2x = (int) loc2.getX();
		int p2y = (int) loc2.getY();
		int p2z = (int) loc2.getZ();

		return (int) magnitude(p1x, p1y, p1z, p2x, p2y, p2z);
	}

	public static double magnitude(int x1, int y1, int z1, int x2, int y2, int z2)
	{
		int xdist = x1 - x2;
		int ydist = y1 - y2;
		int zdist = z1 - z2;
		return Math.sqrt(xdist * xdist + ydist * ydist + zdist * zdist);
	}

	/**
	 * Returns whether or not two locations are identical
	 * 
	 * @param loc1
	 *        - First location
	 * @param loc2
	 *        - Second location
	 * @return Whether or not the two locations are identical
	 */
	public static boolean checkLocation(Location loc, Location loc2)
	{
		return (loc.getBlockX() == loc2.getBlockX() && loc.getBlockY() == loc2.getBlockY() && loc.getBlockZ() == loc2.getBlockZ() && loc
				.getWorld().getUID() == loc2.getWorld().getUID());
	}

	/**
	 * Turns a {@link Location} into a string for debug purpouses
	 * 
	 * @param loc
	 *        - {@link Location} to convert
	 * @return String for debug purpouses
	 */
	public static String locationToString(Location loc)
	{
		StringBuilder ret = new StringBuilder();
		ret.append("World: " + loc.getWorld().getName());
		ret.append(" X: " + loc.getBlockX());
		ret.append(" Y: " + loc.getBlockY());
		ret.append(" Z: " + loc.getBlockZ());
		return ret.toString();
	}

	/**
	 * Returns a useful Stack Trace for debugging purpouses
	 * 
	 * @param e
	 *        - Underlying {@link Throwable}
	 * @param circumstance
	 *        - Circumstance in which the Exception occured
	 */
	public static String getUsefulStack(Throwable e, String circumstance)
	{
		StringBuilder ret = new StringBuilder();
		ret.append("Encountered an exception while " + circumstance + ":" + '\n');
		ret.append(e.getClass().getName() + ": " + e.getMessage() + '\n');
		ret.append("Affected classes: " + '\n');

		for (StackTraceElement ste : e.getStackTrace())
		{
			if (ste.getClassName().contains(SwornGuns.class.getPackage().getName()))
				ret.append('\t' + ste.toString() + '\n');
		}

		if (ret.lastIndexOf("\n") >= 0)
		{
			ret.replace(ret.lastIndexOf("\n"), ret.length(), "");
		}

		return ret.toString();
	}

	/**
	 * Constructs a new list from an existing {@link List}
	 * <p>
	 * This fixes concurrency for some reason
	 * <p>
	 * Should not be used to edit the base List
	 * 
	 * @param list
	 *        - Base {@link List}
	 * @return a new list from the given list
	 */
	public static <T> List<T> newList(List<T> list)
	{
		List<T> ret = new ArrayList<T>();

		for (int i = 0; i < list.size(); i++)
		{
			ret.add(list.get(i));
		}

		return ret;
	}

	/**
	 * Constructs a new {@link List} paramaterized with <code>T</code>
	 * 
	 * @param objects
	 *        - Array of <code>T</code> to create the list with
	 * @return a new {@link List} from the given objects
	 */
	@SafeVarargs
	public static <T> List<T> toList(T... objects)
	{
		List<T> ret = new ArrayList<T>();

		for (T t : objects)
		{
			ret.add(t);
		}

		return ret;
	}

	/**
	 * Basically just a wrapper for {@link Integer#parseInt(String)}
	 * <p>
	 * Catches the {@link NumberFormatException} and returns -1
	 * 
	 * @param s
	 *        - String to attempt to parse into an Integer
	 */
	public static int parseInt(String s)
	{
		int ret = -1;

		try
		{
			ret = Integer.parseInt(s);
		}
		catch (Exception e)
		{
			// Return -1, move on
		}

		return ret;
	}

	/**
	 * Returns whether or not a String can be parsed as an Integer
	 * 
	 * @param string
	 *        - String to check
	 * @return Whether or not a String can be parsed as an Integer
	 */
	public static boolean isInteger(String s)
	{
		return parseInt(s) != -1;
	}
}