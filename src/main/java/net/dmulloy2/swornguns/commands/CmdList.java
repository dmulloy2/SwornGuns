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

import java.util.ArrayList;
import java.util.List;

import net.dmulloy2.swornapi.commands.PaginatedCommand;
import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.types.Gun;
import net.dmulloy2.swornapi.util.FormatUtil;
import net.dmulloy2.swornapi.util.MaterialUtil;

/**
 * @author dmulloy2
 */

public class CmdList extends PaginatedCommand
{
	private final SwornGuns plugin;
	public CmdList(SwornGuns plugin)
	{
		super(plugin);
		this.plugin = plugin;
		this.name = "list";
		this.addOptionalArg("page");
		this.description = "Displays all available guns";
		this.linesPerPage = 6;
		this.usesPrefix = true;
	}

	@Override
	public int getListSize()
	{
		return plugin.getLoadedGuns().size();
	}

	@Override
	public String getHeader(int index)
	{
		return FormatUtil.format("&3 ---- &eGuns &3-- &e{0}&3/&e{1} &3----", index, getPageCount());
	}

	@Override
	public List<String> getLines(int startIndex, int endIndex)
	{
		List<String> lines = new ArrayList<>();
		List<Gun> guns = new ArrayList<>(plugin.getLoadedGuns().values());
		for (int i = startIndex; i < endIndex && i < getListSize(); i++)
		{
			Gun gun = guns.get(i);
			lines.add(FormatUtil.format(" &b- &e{0}  &bType: &e{1}  &bAmmo: &e{2} x {3}",
				gun.getName(), MaterialUtil.getName(gun.getMaterial()), MaterialUtil.getName(gun.getAmmo()),
				gun.getAmmoAmtNeeded()));
		}

		return lines;
	}

	@Override
	public String getLine(int index)
	{
		return null;
	}
}
