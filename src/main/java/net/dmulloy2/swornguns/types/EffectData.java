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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.UUID;

import org.bukkit.Effect;

import net.dmulloy2.swornapi.util.NumberUtil;

/**
 * @author dmulloy2
 */

@Getter
@Accessors(fluent=true)
@EqualsAndHashCode(of={"type", "radius", "maxDuration", "specialDat"})
@ToString(of={"type", "radius", "maxDuration", "specialDat"})
public class EffectData
{
	private transient UUID id = UUID.randomUUID();

	private int maxDuration;
	private int duration;

	private Effect type;
	private double radius;

	private byte specialDat = -1;

	public EffectData(Effect type, int duration, double radius, byte specialDat)
	{
		this.duration = duration;
		this.maxDuration = duration;
		this.type = type;
		this.radius = radius;
		this.specialDat = specialDat;
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
