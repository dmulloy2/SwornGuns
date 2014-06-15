/**
 * (c) 2014 dmulloy2
 */
package net.dmulloy2.swornguns.commands;

import net.dmulloy2.swornguns.SwornGuns;

/**
 * @author dmulloy2
 */

public abstract class PaginatedCommand extends net.dmulloy2.commands.PaginatedCommand
{
	protected final SwornGuns plugin;

	public PaginatedCommand(SwornGuns plugin)
	{
		super(plugin);
		this.plugin = plugin;
	}
}