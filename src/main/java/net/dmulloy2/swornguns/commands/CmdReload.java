package net.dmulloy2.swornguns.commands;

import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.types.Permission;

/**
 * @author dmulloy2
 */

public class CmdReload extends SwornGunsCommand
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
		long start = System.currentTimeMillis();
		sendpMessage("&eReloading SwornGuns...");

		plugin.reload();

		sendpMessage("&eReload Complete! Took &b{0} &ems!", System.currentTimeMillis() - start);
	}
}