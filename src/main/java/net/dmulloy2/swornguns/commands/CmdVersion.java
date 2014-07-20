/**
 * (c) 2014 dmulloy2
 */
package net.dmulloy2.swornguns.commands;

import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.types.StringJoiner;

/**
 * @author dmulloy2
 */

public class CmdVersion extends SwornGunsCommand
{
	public CmdVersion(SwornGuns plugin)
	{
		super(plugin);
		this.name = "version";
		this.aliases.add("v");
		this.description = "Displays version info";
	}

	@Override
	public void perform()
	{
		sendMessage("&3====[ &eSwornGuns &3]====");
		sendMessage("&bVersion&e: {0}", plugin.getDescription().getVersion());
		sendMessage("&bAuthors&e: {0}", new StringJoiner("&b, &e").appendAll(plugin.getDescription().getAuthors()));
		sendMessage("&bIssues&e: https://github.com/dmulloy2/SwornGuns/issues");
	}
}