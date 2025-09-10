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
package net.dmulloy2.swornguns.commands;

import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.types.GunPlayer;

/**
 * @author dmulloy2
 */

public class CmdToggle extends SwornGunsCommand
{
	public CmdToggle(SwornGuns plugin)
	{
		super(plugin);
		this.name = "toggle";
		this.description = "Toggle gun firing";
		this.mustBePlayer = true;
	}

	@Override
	public void perform()
	{
		GunPlayer gp = plugin.getGunPlayer(player);
		if (gp == null)
		{
			err("Could not get your GunPlayer instance. This is usually caused by custom plugin managers.");
			return;
		}

		gp.enabled(! gp.enabled());
		sendpMessage("&eYou have turned gun firing {0}&e!", gp.enabled() ? "&aon" : "&coff");
	}
}
