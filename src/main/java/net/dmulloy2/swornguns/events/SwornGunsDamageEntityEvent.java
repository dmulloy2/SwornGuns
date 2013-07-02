package net.dmulloy2.swornguns.events;

import net.dmulloy2.swornguns.gun.Gun;
import net.dmulloy2.swornguns.gun.GunPlayer;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class SwornGunsDamageEntityEvent extends SwornGunsEvent {
	private Gun gun;
	private GunPlayer shooter;
	private Entity shot;
	private boolean isHeadshot;
	private int damage;
	private EntityDamageByEntityEvent event;

	public SwornGunsDamageEntityEvent(EntityDamageByEntityEvent event, GunPlayer shooter, Gun gun, Entity shot, boolean headshot) {
		this.event = event;
		this.gun = gun;
		this.shooter = shooter;
		this.shot = shot;
		this.isHeadshot = headshot;
		this.damage = gun.getGunDamage();
  }

	public EntityDamageByEntityEvent getEntityDamageEntityEvent() {
		return this.event;
	}

	public boolean isHeadshot() {
		return this.isHeadshot;
	}

	public void setHeadshot(boolean b) {
		this.isHeadshot = b;
	}

	public GunPlayer getShooter() {
		return this.shooter;
	}

	public Entity getEntityDamaged() {
		return this.shot;
	}

	public Player getShooterAsPlayer() {
		return this.shooter.getPlayer();
	}

	public Gun getGun() {
		return this.gun;
	}

	public int getDamage() {
		return this.damage;
	}
}