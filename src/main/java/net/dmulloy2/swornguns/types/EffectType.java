/**
 * SwornGuns - Guns in Minecraft
 * Copyright (C) 2012 - 2015 MineSworn
 * Copyright (C) 2013 - 2015 dmulloy2
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.dmulloy2.swornguns.types;

import lombok.Data;
import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.util.Util;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.google.common.base.Objects;

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
		return "EffectType[type=" + type + ", radius=" + radius + ", duration=" + duration + "]";
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null) return false;
		if (obj == this) return true;

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
		return Objects.hashCode(type, radius, duration);
	}
}
