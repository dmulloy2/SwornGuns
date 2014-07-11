/**
 * (c) 2014 dmulloy2
 */
package net.dmulloy2.swornguns.types;

import lombok.Data;
import net.dmulloy2.util.FormatUtil;
import net.dmulloy2.util.MaterialUtil;
import net.dmulloy2.util.NumberUtil;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

/**
 * Represents a Material and data combination
 *
 * @author dmulloy2
 */

@Data
public class MyMaterial
{
	private final boolean ignoreData;
	private final Material material;
	private final short data;

	public MyMaterial(Material material, short data, boolean ignoreData)
	{
		this.ignoreData = ignoreData;
		this.material = material;
		this.data = data;
	}

	public MyMaterial(Material material, short data)
	{
		this(material, data, false);
	}

	@SuppressWarnings("deprecation") // MaterialData#getData()
	public MyMaterial(Material material, MaterialData data)
	{
		this(material, data.getData(), false);
	}

	public MyMaterial(Material material)
	{
		this(material, (short) 0, true);
	}

	/**
	 * @deprecated ID's are deprecated
	 */
	@Deprecated
	public MyMaterial(int id, short data)
	{
		this(MaterialUtil.getMaterial(id), data);
	}

	/**
	 * @deprecated ID's are deprecated
	 */
	@Deprecated
	public MyMaterial(int id)
	{
		this(id, (short) 0);
	}

	// --- ItemStacks

	/**
	 * Whether or not a given {@link ItemStack} matches this MyMaterial.
	 *
	 * @param item ItemStack to check
	 * @return Whether or not they match
	 */
	public final boolean matches(ItemStack item)
	{
		return item.getType() == material && ignoreData ? true : item.getDurability() == data;
	}

	/**
	 * Creates a new {@link ItemStack} based around this MyMaterial.
	 *
	 * @param amount Amount, defaults to 1
	 * @return The new {@link ItemStack}
	 */
	public final ItemStack newItemStack(int amount)
	{
		if (amount <= 0)
			amount = 1;

		return new ItemStack(material, amount, ignoreData ? 0 : data);
	}

	// ---- Getters

	/**
	 * @return The friendly name of the underlying Material
	 */
	public final String getName()
	{
		return FormatUtil.getFriendlyName(material);
	}

	/**
	 * @deprecated Replaced with {@link MyMaterial#getMaterial()}
	 */
	@Deprecated
	public final Material getType()
	{
		return material;
	}

	// ---- Generic Methods

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString()
	{
		if (ignoreData)
			return material.toString();

		return "MyMaterial { material = " + material + ", data = " + data + " }";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof MyMaterial)
		{
			MyMaterial that = (MyMaterial) obj;
			return this.material == that.material && (ignoreData ? true : this.data == that.data);
		}

		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode()
	{
		int hash = 101;
		hash *= 1 + material.hashCode();
		hash *= 1 + (ignoreData ? data : 0);
		return hash;
	}

	/**
	 * Attempts to get a MyMaterial from a given string.
	 * <p>
	 * Format: <code>Material:Data</code>
	 *
	 * @param string String to get the MyMaterial from
	 * @return Resulting MyMaterial, or null if parsing failed
	 */
	public static MyMaterial fromString(String string)
	{
		try
		{
			if (string.contains(":"))
			{
				String[] split = string.split(":");
				Material material = MaterialUtil.getMaterial(split[0]);
				short data = NumberUtil.toShort(split[1]);
				boolean ignoreData = data == -1;
				if (data <= 0)
					data = 0;

				return new MyMaterial(material, data, ignoreData);
			}

			Material material = MaterialUtil.getMaterial(string);
			return new MyMaterial(material);
		} catch (Throwable ex) { }
		return null;
	}
}