package net.dmulloy2.swornguns.listeners;

import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.events.SwornGunsBulletCollideEvent;
import net.dmulloy2.swornguns.events.SwornGunsDamageEntityEvent;
import net.dmulloy2.swornguns.events.SwornGunsKillEntityEvent;
import net.dmulloy2.swornguns.gun.Bullet;
import net.dmulloy2.swornguns.gun.Gun;
import net.dmulloy2.swornguns.gun.GunPlayer;
import net.dmulloy2.swornguns.util.Util;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

public class EntityListener implements Listener {
	private final SwornGuns plugin;
	public EntityListener(final SwornGuns plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onProjectileHit(ProjectileHitEvent event) {
		Projectile check = event.getEntity();
		Bullet bullet = plugin.getBullet(check);
		if (bullet != null) {
			bullet.onHit();
			bullet.setNextTickDestroy();
			Projectile p = event.getEntity();
			Block b = p.getLocation().getBlock();
			int id = b.getTypeId();
			for (double i = 0.2D; i < 4.0D; i += 0.2D) {
				if (id == 0) {
					b = p.getLocation().add(p.getVelocity().normalize().multiply(i)).getBlock();
					id = b.getTypeId();
				}
			}
			if (id > 0) {
				p.getLocation().getWorld().playEffect(b.getLocation(), Effect.STEP_SOUND, id);
			}

			SwornGunsBulletCollideEvent evv = new SwornGunsBulletCollideEvent(bullet.getShooter(), bullet.getGun(), b);
			this.plugin.getServer().getPluginManager().callEvent(evv);
		}
		event.getEntity().remove();
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityDeath(EntityDeathEvent event) {
		Entity dead = event.getEntity();
		if (dead.getLastDamageCause() != null) {
			EntityDamageEvent e = dead.getLastDamageCause();
			if ((e instanceof EntityDamageByEntityEvent)) {
				EntityDamageByEntityEvent ede = (EntityDamageByEntityEvent)e;
				Entity damager = ede.getDamager();
				if ((damager instanceof Projectile)) {
					Projectile proj = (Projectile)damager;
					Bullet bullet = plugin.getBullet(proj);
					if (bullet != null) {
						Gun used = bullet.getGun();
						GunPlayer shooter = bullet.getShooter();

						SwornGunsKillEntityEvent pvpgunkill = new SwornGunsKillEntityEvent(shooter, used, dead);
						this.plugin.getServer().getPluginManager().callEvent(pvpgunkill);
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		if (event.isCancelled()) {
			return; 
		}
		Entity damager = event.getDamager();
		if ((event.getEntity() instanceof LivingEntity)) {
			LivingEntity hurt = (LivingEntity)event.getEntity();
			if ((damager instanceof Projectile)) {
				Projectile proj = (Projectile)damager;
				Bullet bullet = plugin.getBullet(proj);
				if (bullet != null) {
					boolean headshot = false;
					if ((isNear(proj.getLocation(), hurt.getEyeLocation(), 0.26D)) && (bullet.getGun().canHeadShot())) {
						headshot = true;
					}
					SwornGunsDamageEntityEvent pvpgundmg = new SwornGunsDamageEntityEvent(event, bullet.getShooter(), bullet.getGun(), event.getEntity(), headshot);
					this.plugin.getServer().getPluginManager().callEvent(pvpgundmg);
					if (!pvpgundmg.isCancelled()) {
						double damage = pvpgundmg.getDamage();
						double mult = 1.0D;
						if (pvpgundmg.isHeadshot()) {
							Util.playEffect(Effect.ZOMBIE_DESTROY_DOOR, hurt.getLocation(), 3);
							mult = 2.0D;
						}
						hurt.setLastDamage(0);
						event.setDamage((int)Math.ceil(damage * mult));
						int armorPenetration = bullet.getGun().getArmorPenetration();
						if (armorPenetration > 0) {
							int health = hurt.getHealth();
							int newHealth = health - armorPenetration;
							if (newHealth < 0) {
								newHealth = 0;
							}
							if (newHealth > 20) {
								newHealth = 20;
							}
							hurt.setHealth(newHealth);
						}
						
						bullet.getGun().doKnockback(hurt, bullet.getVelocity());

						bullet.remove();
					} else {
						event.setCancelled(true);
					}
				}
			}
		}
	}

	private boolean isNear(Location location, Location eyeLocation, double d) {
		return Math.abs(location.getY() - eyeLocation.getY()) <= d;
	}
}