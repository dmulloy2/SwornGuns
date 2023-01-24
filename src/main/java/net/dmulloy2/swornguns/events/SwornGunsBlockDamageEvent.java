package net.dmulloy2.swornguns.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called before SwornGuns damages a block from a bullet
 * @author dmulloy2
 */
public class SwornGunsBlockDamageEvent extends Event implements Cancellable
{
	private static final HandlerList handlers = new HandlerList();

	private boolean cancelled = false;
	private final Player player;
	private final Location location;

	public SwornGunsBlockDamageEvent(Player player, Location location)
	{
		this.player = player;
		this.location = location;
	}

	public Location getLocation()
	{
		return location;
	}

	public Player getPlayer()
	{
		return player;
	}

	@Override
	public boolean isCancelled()
	{
		return cancelled;
	}

	@Override
	public void setCancelled(boolean b)
	{
		this.cancelled = cancelled;
	}

	@Override
	public HandlerList getHandlers()
	{
		return handlers;
	}

	public static HandlerList getHandlerList()
	{
		return handlers;
	}
}
