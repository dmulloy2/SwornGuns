package net.dmulloy2.swornguns.handlers;

import java.util.logging.Level;

import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.util.FormatUtil;

/**
 * @author dmulloy2
 */

public class LogHandler
{
	private SwornGuns plugin;
	public LogHandler(SwornGuns plugin)
	{
		this.plugin = plugin;
	}

	public final void log(Level level, String msg, Object... objects)
	{
		plugin.getLogger().log(level, FormatUtil.format(msg, objects));
	}

	public final void log(String msg, Object... objects)
	{
		log(Level.INFO, msg, objects);
	}
	
	public final void debug(String msg, Object...objects)
	{
		if (plugin.getConfig().getBoolean("debug", false))
		{
			log(msg, objects);
		}
	}
}