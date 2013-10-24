package net.dmulloy2.swornguns.handlers;

import java.util.ArrayList;
import java.util.List;

import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.commands.CmdHelp;
import net.dmulloy2.swornguns.commands.SwornGunsCommand;
import net.dmulloy2.swornguns.util.FormatUtil;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * @author dmulloy2
 */

public class CommandHandler implements CommandExecutor
{
	private final SwornGuns plugin;
	private String commandPrefix;
	private List<SwornGunsCommand> registeredCommands;

	public CommandHandler(final SwornGuns plugin)
	{
		this.plugin = plugin;
		this.registeredCommands = new ArrayList<SwornGunsCommand>();
	}

	public void registerCommand(SwornGunsCommand command)
	{
		if (commandPrefix != null)
			registeredCommands.add(command);
	}

	public List<SwornGunsCommand> getRegisteredCommands()
	{
		return registeredCommands;
	}

	public String getCommandPrefix()
	{
		return commandPrefix;
	}

	public void setCommandPrefix(String commandPrefix)
	{
		this.commandPrefix = commandPrefix;
		plugin.getCommand(commandPrefix).setExecutor(this);
	}

	public boolean usesCommandPrefix()
	{
		return commandPrefix != null;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		List<String> argsList = new ArrayList<String>();

		if (args.length > 0)
		{
			String commandName = args[0];
			for (int i = 1; i < args.length; i++)
				argsList.add(args[i]);

			for (SwornGunsCommand command : registeredCommands)
			{
				if (commandName.equalsIgnoreCase(command.getName()) || command.getAliases().contains(commandName.toLowerCase()))
				{
					command.execute(sender, argsList.toArray(new String[0]));
					return true;
				}
			}

			sender.sendMessage(plugin.getPrefix() +
					FormatUtil.format("&cUnknown SwornGuns command \"{0}\". Try /swornguns help!"));
		}
		else
		{
			new CmdHelp(plugin).execute(sender, args);
		}

		return true;
	}
}