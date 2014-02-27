package net.dmulloy2.swornguns.commands;

import java.util.ArrayList;
import java.util.List;

import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.types.Gun;
import net.dmulloy2.swornguns.util.FormatUtil;

/**
 * @author dmulloy2
 */

public class CmdList extends PaginatedCommand
{
	public CmdList(SwornGuns plugin)
	{
		super(plugin);
		this.name = "list";
		this.optionalArgs.add("page");
		this.description = "Displays all available guns";
		this.linesPerPage = 6;
	}

	@Override
	public int getListSize()
	{
		return plugin.getLoadedGuns().size();
	}

	@Override
	public String getHeader(int index)
	{
		return FormatUtil.format("&3====[ &eAvailable Guns &3(&e{0}&3/&e{1}&3)", index, getListSize());
	}

	@Override
	public List<String> getLines(int startIndex, int endIndex)
	{
		List<String> lines = new ArrayList<String>();
		for (int i = startIndex; i < endIndex && i < getListSize(); i++)
		{
			Gun gun = plugin.getLoadedGuns().get(i);
			lines.add(FormatUtil.format(" &b- &e{0}  &bType: &e{1}  &bAmmo: &e{2} x {3}", gun.getName(),
					FormatUtil.getFriendlyName(gun.getGunMaterial()), FormatUtil.getFriendlyName(gun.getAmmoMaterial()),
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