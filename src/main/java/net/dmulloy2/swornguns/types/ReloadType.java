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

/**
 * @author dmulloy2
 */

public enum ReloadType
{
	BOLT,
	CLIP,
	INDIVIDUAL_BULLET,
	NORMAL,
	PUMP,
	;

	public static ReloadType getByName(String name)
	{
		for (ReloadType type : values())
		{
			if (type.name().equalsIgnoreCase(name))
				return type;
		}

		return null;
	}

	public Object serialize()
	{
		return name().toLowerCase();
	}

	public static ReloadType deserialize(Object data)
	{
		return getByName(data.toString());
	}
}
