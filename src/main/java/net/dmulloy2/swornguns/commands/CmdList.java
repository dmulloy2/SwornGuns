package net.dmulloy2.swornguns.commands;

import java.util.ArrayList;
import java.util.List;

import net.dmulloy2.commands.PaginatedCommand;
import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.types.Gun;
import net.dmulloy2.util.FormatUtil;

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
		return FormatUtil.format("&3====[ &eGuns &3(&e{0}&3/&e{1}&3)", index, getPageCount());
	}

	@Override
	public List<String> getLines(int startIndex, int endIndex)
	{
		List<String> lines = new ArrayList<String>();
		List<Gun> guns = new ArrayList<Gun>(plugin.getLoadedGuns().values());
		for (int i = startIndex; i < endIndex && i < getListSize(); i++)
		{
			Gun gun = guns.get(i);
			lines.add(FormatUtil.format(" &b- &e{0}  &bType: &e{1}  &bAmmo: &e{2} x {3}", gun.getName(), gun.getMaterial().getName(),
					gun.getAmmo().getName(), gun.getAmmoAmtNeeded()));
		}

		return lines;
	}

	@Override
	public String getLine(int index)
	{
		return null;
	}
}