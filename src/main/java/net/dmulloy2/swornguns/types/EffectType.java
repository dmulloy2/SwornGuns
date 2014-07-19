package net.dmulloy2.swornguns.types;

import lombok.Data;
import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.util.Util;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * @author dmulloy2
 */

@Data
public class EffectType
{
	private static int nextID = 1;

	private int maxDuration;
	private int duration;
	private int id;

	private Effect type;
	private double radius;
	private Location location;

	private byte specialDat = -1;

	private final SwornGuns plugin;
	public EffectType(SwornGuns plugin, int duration, double radius, Effect type)
	{
		this.plugin = plugin;
		this.duration = duration;
		this.maxDuration = duration;
		this.type = type;
		this.radius = radius;
		this.id = nextID++;
	}

	public void start(Location location)
	{
		this.location = location;
		this.duration = maxDuration;

		plugin.addEffect(this);
	}

	@Override
	public EffectType clone()
	{
		return new EffectType(plugin, maxDuration, radius, type).setSpecialDat(specialDat);
	}

	public void tick()
	{
		this.duration -= 1;

		if (duration < 0)
		{
			plugin.removeEffect(this);
			return;
		}

		double yRad = radius;
		if (type.equals(Effect.MOBSPAWNER_FLAMES))
		{
			yRad = 0.75D;

			for (Player player : Util.getOnlinePlayers())
			{
				if (player.getWorld().getUID() == location.getWorld().getUID())
				{
					if (location.distance(player.getLocation()) < radius)
					{
						player.setFireTicks(20);
					}
				}
			}
		}

		for (double i = -radius; i <= radius; i += 1.0D)
		{
			for (double ii = -radius; ii <= radius; ii += 1.0D)
			{
				for (double iii = 0.0D; iii <= yRad * 2.0D; iii += 1.0D)
				{
					int rand = Util.random(8);
					if (rand == 2)
					{
						Location newloc = location.clone().add(i, iii - 1.0D, ii);
						Location testLoc = location.clone().add(0.0D, yRad - 1.0D, 0.0D);
						if (newloc.distance(testLoc) <= radius)
						{
							byte dat = (byte) Util.random(8);

							if (specialDat > -1)
								dat = specialDat;

							newloc.getWorld().playEffect(newloc, type, dat);
						}
					}
				}
			}
		}
	}

	public EffectType setSpecialDat(byte specialDat)
	{
		this.specialDat = specialDat;
		return this;
	}

	@Override
	public String toString()
	{
		return "EffectType { type = " + type + ", radius = " + radius + ", duration = " + duration + " }";
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof EffectType)
		{
			EffectType that = (EffectType) obj;
			return this.type.equals(that.type) && this.radius == that.radius && this.duration == that.duration;
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		int hash = 100;
		hash *= 1 + type.hashCode();
		hash *= 1 + radius;
		hash *= 1 + duration;
		return hash;
	}
}