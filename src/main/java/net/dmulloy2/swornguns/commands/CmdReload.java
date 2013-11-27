package net.dmulloy2.swornguns.commands;

import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.types.Permission;
import net.dmulloy2.swornguns.types.Reloadable;

/**
 * @author dmulloy2
 */

public class CmdReload extends SwornGunsCommand implements Reloadable
{
	public CmdReload(SwornGuns plugin)
	{
		super(plugin);
		this.name = "reload";
		this.aliases.add("rl");
		this.description = "Reload SwornGuns";
		this.permission = Permission.RELOAD;

		this.mustBePlayer = false;
	}

	@Override
	public void perform()
	{
		reload();
	}

	@Override
	public void reload()
	{
		sendpMessage("&aReloading SwornGuns...");

		long start = System.currentTimeMillis();

		plugin.reload();

		sendpMessage("&aReload Complete! Took {0} ms!", System.currentTimeMillis() - start);
	}
}