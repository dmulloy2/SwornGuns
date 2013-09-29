package net.dmulloy2.swornguns.api.events;

import net.dmulloy2.swornguns.types.Gun;
import net.dmulloy2.swornguns.types.GunPlayer;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * @author dmulloy2
 */

public class SwornGunsBulletCollideEvent extends SwornGunsEvent
{
	private final Gun gun;
	private final GunPlayer shooter;
	private final Block blockHit;

	public SwornGunsBulletCollideEvent(final GunPlayer shooter, final Gun gun, final Block block)
	{
		this.gun = gun;
		this.shooter = shooter;
		this.blockHit = block;
	}

	public Gun getGun()
	{
		return this.gun;
	}

	public GunPlayer getShooter()
	{
		return this.shooter;
	}

	public Player getShooterAsPlayer()
	{
		return this.shooter.getPlayer();
	}

	public Block getBlockHit()
	{
		return this.blockHit;
	}
}