package net.dmulloy2.swornguns.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;

import lombok.NonNull;
import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.types.StringJoiner;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;

/**
 * Base Util class
 * 
 * @author dmulloy2
 */

public class Util
{
	private Util() { }

	/**
	 * Gets the Player from a given name or {@link UUID}
	 * 
	 * @param identifier
	 *        - Player name or UUID
	 * @return Player from the given name or UUID, null if none exists
	 * @see {@link Bukkit#getPlayer(UUID)}
	 * @see {@link Bukkit#matchPlayer(String)}
	 */
	public static Player matchPlayer(@NonNull String identifier)
	{
		// Get by UUID first
		if (identifier.length() == 36)
			return Bukkit.getPlayer(UUID.fromString(identifier));

		// Then attempt to match
		List<Player> players = Bukkit.matchPlayer(identifier);
		if (! players.isEmpty())
			return players.get(0);

		return null;
	}

	/**
	 * Gets the OfflinePlayer from a given name or {@link UUID}
	 * <p>
	 * Use of this method is discouraged as it is potentially blocking
	 * 
	 * @param identifier
	 *        - Player name or UUID
	 * @return OfflinePlayer from the given name or UUID, null if none exists
	 * @see {@link Util#matchPlayer(String)}
	 * @see {@link Bukkit#getOfflinePlayer(UUID)}
	 * @see {@link Bukkit#getOfflinePlayer(String)}
	 */
	@SuppressWarnings("deprecation") // Bukkit#getOfflinePlayer(String)
	public static OfflinePlayer matchOfflinePlayer(@NonNull String identifier)
	{
		// Check online players first
		Player player = matchPlayer(identifier);
		if (player != null)
			return player;

		// Then check UUID
		if (identifier.length() == 36)
			return Bukkit.getOfflinePlayer(UUID.fromString(identifier));

		OfflinePlayer op = Bukkit.getOfflinePlayer(identifier);
		if (op.hasPlayedBefore())
			return op;

		return null;
	}

	/**
	 * Whether or not a player is banned
	 * 
	 * @param identifier
	 *        - Player name or UUID
	 * @return Whether or not the player is banned
	 */
	public static boolean isBanned(String identifier)
	{
		for (OfflinePlayer banned : Bukkit.getBannedPlayers())
		{
			if (banned.getUniqueId().toString().equals(identifier) || banned.getName().equalsIgnoreCase(identifier))
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
	 * Plays an effect to all online players
	 * 
	 * @param effect
	 *        - Effect type to play
	 * @param loc
	 *        - Location where the effect should be played
	 * @param data
	 *        - Data
	 * @see {@link Player#playEffect(Location, Effect, Object)}
	 */
	public static <T> void playEffect(Effect effect, Location loc, T data)
	{
		for (Player player : Bukkit.getOnlinePlayers())
		{
			player.playEffect(loc, effect, data);
		}
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
		return loc.getBlockX() == loc2.getBlockX() 
				&& loc.getBlockY() == loc2.getBlockY() 
				&& loc.getBlockZ() == loc2.getBlockZ()
				&& loc.getWorld().equals(loc2.getWorld());
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
		StringJoiner joiner = new StringJoiner("\n");
		joiner.append("Encountered an exception while " + circumstance + ": " + e.getClass().getName() + ": " + e.getMessage());
		joiner.append("Affected classes:");

		for (StackTraceElement ste : e.getStackTrace())
		{
			if (ste.getClassName().contains(SwornGuns.class.getPackage().getName()))
				joiner.append("\t" + ste.toString());
		}

		while (e.getCause() != null)
		{
			e = e.getCause();
			joiner.append("Caused by: " + e.getClass().getName() + ": " + e.getMessage());
			joiner.append("Affected classes:");
			for (StackTraceElement ste : e.getStackTrace())
			{
				if (ste.getClassName().contains(SwornGuns.class.getPackage().getName()))
					joiner.append("\t" + ste.toString());
			}
		}

		return joiner.toString();
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
	public static <T> List<T> newList(Collection<T> list)
	{
		return new ArrayList<T>(list);
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
	 * Filters duplicate entries from a {@link Map} according to the original
	 * map.

	 * @param map
	 *        - {@link Map} to filter
	 * @param original
	 *        - Original map
	 * @return Filtered map
	 */
	public static <K, V> Map<K, V> filterDuplicateEntries(Map<K,V> map, Map<K, V> original)
	{
		for (Entry<K, V> entry : new HashMap<K, V>(map).entrySet())
		{
			K key = entry.getKey();
			if (original.containsKey(key))
			{
				V val = entry.getValue();
				V def = original.get(key);
				if (val.equals(def))
				{
					map.remove(key);
				}
			}
		}

		return map;
	}

	/**
	 * Checks if a field is declared in a given {@link Class}
	 * 
	 * @param clazz
	 *        - Class object
	 * @param name
	 *        - Name of variable
	 * @return Whether or not the field is declared
	 */
	public static boolean isDeclaredField(Class<?> clazz, String name)
	{
		try
		{
			clazz.getDeclaredField(name);
			return true;
		} catch (Throwable ex) { }
		return false;
	}

	/**
	 * Parses a given {@link Object} (preferably a {@link String}) and returns a
	 * boolean value.
	 * 
	 * @param object
	 *        - Object to parse
	 * @return Boolean value from the given object. Defaults to
	 *         <code>false</code>
	 */
	public static boolean toBoolean(Object object)
	{
		if (object instanceof Boolean)
		{
			return ((Boolean) object).booleanValue();
		}

		if (object instanceof String)
		{
			String str = (String) object;
			return str.startsWith("y") || str.startsWith("t") || str.startsWith("on") || str.startsWith("+") || str.startsWith("1");
		}

		try
		{
			return Boolean.parseBoolean(object.toString());
		} catch (Exception e) { }
		return false;
	}

	/**
	 * Sets a {@link Block}'s {@link MaterialData}. Exists because Bukkit's
	 * BlockState API sucks.
	 * <p>
	 * This method is deprecated and is not guaranteed to work.
	 * 
	 * @param block
	 *        - Block to set data of
	 * @param data
	 *        - Data to set
	 * @deprecated {@link Block#setData(byte)} is deprecated
	 */
	public static void setData(Block block, MaterialData data)
	{
		block.setData(data.getData());
		block.getState().update(true);
	}
}