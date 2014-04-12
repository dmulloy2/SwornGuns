package net.dmulloy2.swornguns.types;

import java.util.Map;
import java.util.Map.Entry;

import lombok.AllArgsConstructor;
import lombok.Getter;

import org.bukkit.enchantments.Enchantment;

/**
 * @author dmulloy2
 */

@Getter
@AllArgsConstructor
public enum EnchantmentType
{
	ARROW_DAMAGE("power"),
	ARROW_FIRE("fire"),
	ARROW_INFINITE("inf"),
	ARROW_KNOCKBACK("arrowkb"),
	DAMAGE_ALL("sharp"),
	DAMAGE_ARTHROPODS("bane"),
	DAMAGE_UNDEAD("smite"),
	DIG_SPEED("eff"),
	DURABILITY("dura"),
	FIRE_ASPECT("fireaspect"),
	KNOCKBACK("knockback"),
	LOOT_BONUS_BLOCKS("fortune"),
	LOOT_BONUS_MOBS("looting"),
	OXYGEN("breathing"),
	PROTECTION_ENVIRONMENTAL("prot"),
	PROTECTION_EXPLOSIONS("blast"),
	PROTECTION_FALL("feather"),
	PROTECTION_FIRE("fireprot"),
	PROTECTION_PROJECTILE("proj"),
	SILK_TOUCH("silk"),
	THORNS("thorns"),
	WATER_WORKER("aqua");

	private final String name;

	public static String toName(Enchantment enchant)
	{
		for (EnchantmentType e : EnchantmentType.values())
		{
			if (e.toString().equals(enchant.getName()))
				return e.name;
		}

		return "";
	}

	public static Enchantment toEnchantment(String enchant)
	{
		for (EnchantmentType e : EnchantmentType.values())
		{
			if (e.name.equalsIgnoreCase(enchant))
				return Enchantment.getByName(e.toString());
		}

		return null;
	}

	public static String toString(Map<Enchantment, Integer> enchantments)
	{
		StringBuilder result = new StringBuilder();
		for (Entry<Enchantment, Integer> entry : enchantments.entrySet())
		{
			result.append(EnchantmentType.toName(entry.getKey()) + ":" + entry.getValue() + ", ");
		}

		if (result.lastIndexOf(",") >= 0)
		{
			result.deleteCharAt(result.lastIndexOf(","));
			result.deleteCharAt(result.lastIndexOf(" "));
		}

		return result.toString();
	}
}