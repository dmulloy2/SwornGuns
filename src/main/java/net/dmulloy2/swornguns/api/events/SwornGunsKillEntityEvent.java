package net.dmulloy2.swornguns.api.events;

import net.dmulloy2.swornguns.types.Gun;
import net.dmulloy2.swornguns.types.GunPlayer;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * @author dmulloy2
 */

public class SwornGunsKillEntityEvent extends SwornGunsEvent
{
	private Gun gun;
	private GunPlayer shooter;
	private Entity shot;

	public SwornGunsKillEntityEvent(GunPlayer shooter, Gun gun, Entity killed)
	{
		this.gun = gun;
		this.shooter = shooter;
		this.shot = killed;
	}

	public GunPlayer getKiller()
	{
		return this.shooter;
	}

	public Player getKillerAsPlayer()
	{
		return this.shooter.getPlayer();
	}

	public Entity getKilled()
	{
		return this.shot;
	}

	public Gun getGun()
	{
		return this.gun;
	}
}