package net.dmulloy2.swornguns.commands;

import net.dmulloy2.commands.Command;
import net.dmulloy2.swornguns.SwornGuns;

/**
 * @author dmulloy2
 */

public abstract class SwornGunsCommand extends Command
{
	protected final SwornGuns plugin;
	public SwornGunsCommand(SwornGuns plugin)
	{
		super(plugin);
		this.plugin = plugin;
		this.usesPrefix = true;
	}
}