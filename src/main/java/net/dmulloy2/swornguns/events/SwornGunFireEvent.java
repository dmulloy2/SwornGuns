package net.dmulloy2.swornguns.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import net.dmulloy2.swornguns.types.Gun;

public class SwornGunFireEvent extends Event implements Cancellable
{
	private static final HandlerList handlers = new HandlerList();

	private boolean cancelled = false;

	private final Gun gun;
	private int ammoNeeded;

	public SwornGunFireEvent(Gun gun, int ammoNeeded)
	{
		this.gun = gun;
		this.ammoNeeded = ammoNeeded;
	}

	public Gun getGun()
	{
		return gun;
	}

	public Player getPlayer()
	{
		return gun.getOwner().getPlayer();
	}

	public int getAmmoNeeded()
	{
		return ammoNeeded;
	}

	public void setAmmoNeeded(int ammoNeeded)
	{
		this.ammoNeeded = ammoNeeded;
	}

	@Override
	public boolean isCancelled()
	{
		return cancelled;
	}

	@Override
	public void setCancelled(boolean b)
	{
		this.cancelled = b;
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
