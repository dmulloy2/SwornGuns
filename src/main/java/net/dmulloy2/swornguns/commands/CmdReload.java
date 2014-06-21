package net.dmulloy2.swornguns.commands;

import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.types.Permission;
import net.dmulloy2.types.Reloadable;

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
	}

	@Override
	public void perform()
	{
		reload();
	}

	@Override
	public void reload()
	{
		sendpMessage("&eReloading SwornGuns...");

		long start = System.currentTimeMillis();

		plugin.reload();

		sendpMessage("&eReload Complete! Took &b{0} &ems!", System.currentTimeMillis() - start);
	}
}