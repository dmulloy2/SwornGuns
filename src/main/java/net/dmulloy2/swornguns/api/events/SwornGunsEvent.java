package net.dmulloy2.swornguns.api.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * @author dmulloy2
 */

public class SwornGunsEvent extends Event implements Cancellable
{
	private static final HandlerList handlers = new HandlerList();
	private boolean cancelled = false;

	@Override
	public HandlerList getHandlers()
	{
		return handlers;
	}

	public static HandlerList getHandlerList()
	{
		return handlers;
	}

	@Override
	public boolean isCancelled()
	{
		return this.cancelled;
	}

	@Override
	public void setCancelled(boolean arg0)
	{
		this.cancelled = arg0;
	}
}