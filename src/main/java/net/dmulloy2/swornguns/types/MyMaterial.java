/**
 * (c) 2014 dmulloy2
 */
package net.dmulloy2.swornguns.types;

import lombok.Data;
import net.dmulloy2.swornguns.util.FormatUtil;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
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

	public final String getName()
	{
		return FormatUtil.getFriendlyName(material);
	}

	// --- ItemStacks

	public final boolean matches(ItemStack item)
	{
		return item.getType() == material && ignoreData ? true : item.getDurability() == data;
	}

	public final ItemStack newItemStack(int amount)
	{
		return new ItemStack(material, amount, ignoreData ? 0 : data);
	}

	// ---- Generic Methods

	@Override
	public String toString()
	{
		StringJoiner joiner = new StringJoiner(", ");
		joiner.append("material = " + material);
		joiner.append(data > 0 ? "data = " + data : "");
		joiner.append(ignoreData ? "ignoreData = true" : "");
		return "MyMaterial { " + joiner.toString() + " }";
	}

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

	@Override
	public int hashCode()
	{
		int hash = 101;
		hash *= 1 + material.hashCode();
		hash *= 1 + (ignoreData ? data : 0);
		return hash;
	}
}