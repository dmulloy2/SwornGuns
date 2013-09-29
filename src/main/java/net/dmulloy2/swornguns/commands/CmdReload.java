package net.dmulloy2.swornguns.commands;

import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.types.Permission;

import org.bukkit.plugin.PluginManager;

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

		this.mustBePlayer = false;
	}

	@Override
	public void perform()
	{
		sendpMessage("&aReloading SwornGuns...");

		long start = System.currentTimeMillis();

		PluginManager pm = plugin.getServer().getPluginManager();
		pm.disablePlugin(plugin);
		pm.enablePlugin(plugin);

		long finish = System.currentTimeMillis();

		sendpMessage("&aReload Complete! Took {0} ms!", finish - start);
	}
}