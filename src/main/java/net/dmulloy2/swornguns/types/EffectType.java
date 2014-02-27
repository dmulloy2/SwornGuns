package net.dmulloy2.swornguns.types;

import lombok.Getter;
import lombok.Setter;
import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.util.Util;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * @author dmulloy2
 */

@Getter @Setter
public class EffectType
{
	private int maxDuration;
	private int duration;

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

			for (Player player : plugin.getServer().getOnlinePlayers())
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
}