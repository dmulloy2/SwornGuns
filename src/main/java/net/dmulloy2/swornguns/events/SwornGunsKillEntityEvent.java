package net.dmulloy2.swornguns.events;

import net.dmulloy2.swornguns.gun.Gun;
import net.dmulloy2.swornguns.gun.GunPlayer;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class SwornGunsKillEntityEvent extends SwornGunsEvent {
	private Gun gun;
	private GunPlayer shooter;
	private Entity shot;

	public SwornGunsKillEntityEvent(GunPlayer shooter, Gun gun, Entity killed) {
		this.gun = gun;
		this.shooter = shooter;
		this.shot = killed;
	}

	public GunPlayer getKiller() {
		return this.shooter;
	}

	public Player getKillerAsPlayer() {
		return this.shooter.getPlayer();
	}

	public Entity getKilled() {
		return this.shot;
	}

	public Gun getGun() {
		return this.gun;
	}
}