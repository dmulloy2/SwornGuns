/**
 * SwornGuns - Guns in Minecraft
 * Copyright (C) dmulloy2 <http://dmulloy2.net>
 * Copyright (C) Contributors
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
import net.dmulloy2.util.NumberUtil;
import net.dmulloy2.util.Util;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.google.common.base.Objects;

import java.util.UUID;

/**
 * @author dmulloy2
 */

@Data
public class EffectData
{
	private transient UUID id = UUID.randomUUID();

	private int maxDuration;
	private int duration;

	private Effect type;
	private double radius;
	private Location location;

	private byte specialDat = -1;

	public EffectData(Effect type, int duration, double radius, byte specialDat)
	{
		this.duration = duration;
		this.maxDuration = duration;
		this.type = type;
		this.radius = radius;
		this.specialDat = specialDat;
	}

	public void start(Location location)
	{
		this.location = location;
		this.duration = maxDuration;
	}

	@Override
	public EffectData clone()
	{
		return new EffectData(type, maxDuration, radius, specialDat);
	}

	public boolean tick()
	{
		this.duration -= 1;

		if (duration < 0)
		{
			return false;
		}

		double yRad = radius;
		if (type.equals(Effect.MOBSPAWNER_FLAMES))
		{
			yRad = 0.75D;

			World world = location.getWorld();
			if (world != null)
			{
				for (Player player : world.getPlayers())
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

		return true;
	}

	@Override
	public String toString()
	{
		return "EffectData[type=" + type + ", radius=" + radius + ", duration=" + duration + "]";
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == this) return true;

		if (obj instanceof EffectData that)
		{
			return this.type.equals(that.type) && this.radius == that.radius && this.duration == that.duration;
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return Objects.hashCode(type, radius, duration);
	}

	public Object serialize()
	{
		StringBuilder result = new StringBuilder();

		result.append(type.name());
		result.append(",");
		result.append(duration);
		result.append(",");
		result.append(radius);

		if (specialDat != -1)
		{
			result.append(",");
			result.append(specialDat);
		}

		return result.toString();
	}

	public static EffectData deserialize(Object data)
	{
		String[] parts = data.toString().split(",");

		double radius = NumberUtil.toDouble(parts[2]);
		int duration = NumberUtil.toInt(parts[1]);
		Effect eff = Effect.valueOf(parts[0].toUpperCase());
		byte specialDat = -1;

		if (parts.length == 4)
		{
			specialDat = Byte.parseByte(parts[3]);
		}

		return new EffectData(eff, duration, radius, specialDat);
	}
}
