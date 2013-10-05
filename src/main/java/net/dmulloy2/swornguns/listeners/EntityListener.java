package net.dmulloy2.swornguns.listeners;

import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.api.events.SwornGunsBulletCollideEvent;
import net.dmulloy2.swornguns.api.events.SwornGunsDamageEntityEvent;
import net.dmulloy2.swornguns.api.events.SwornGunsKillEntityEvent;
import net.dmulloy2.swornguns.types.Bullet;
import net.dmulloy2.swornguns.types.Gun;
import net.dmulloy2.swornguns.types.GunPlayer;
import net.dmulloy2.swornguns.util.Util;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
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

/**
 * @author dmulloy2
 */

public class EntityListener implements Listener
{
	private final SwornGuns plugin;
	public EntityListener(SwornGuns plugin)
	{
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onProjectileHit(ProjectileHitEvent event)
	{
		Projectile check = event.getEntity();
		Bullet bullet = plugin.getBullet(check);
		if (bullet != null)
		{
			bullet.onHit();
			bullet.setDestroyNextTick(true);
			Projectile p = event.getEntity();
			Block b = p.getLocation().getBlock();
			Material mat = b.getType();
//			int id = b.getTypeId();

			for (double i = 0.2D; i < 4.0D; i += 0.2D)
			{
				if (mat == Material.AIR)
				{
					b = p.getLocation().add(p.getVelocity().normalize().multiply(i)).getBlock();
					mat = b.getType();
				}
			}

			if (mat != Material.AIR)
			{
//				p.getLocation().getWorld().playEffect(b.getLocation(), Effect.STEP_SOUND, mat.getId());
				p.getLocation().getWorld().playEffect(b.getLocation(), Effect.STEP_SOUND, mat);
			}

			SwornGunsBulletCollideEvent evv = new SwornGunsBulletCollideEvent(bullet.getShooter(), bullet.getShotFrom(), b);
			plugin.getServer().getPluginManager().callEvent(evv);
		}
		event.getEntity().remove();
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityDeath(EntityDeathEvent event)
	{
		Entity dead = event.getEntity();
		if (dead.getLastDamageCause() != null)
		{
			EntityDamageEvent e = dead.getLastDamageCause();
			if ((e instanceof EntityDamageByEntityEvent))
			{
				EntityDamageByEntityEvent ede = (EntityDamageByEntityEvent) e;
				Entity damager = ede.getDamager();
				if ((damager instanceof Projectile))
				{
					Projectile proj = (Projectile) damager;
					Bullet bullet = plugin.getBullet(proj);
					if (bullet != null)
					{
						Gun used = bullet.getShotFrom();
						GunPlayer shooter = bullet.getShooter();

						SwornGunsKillEntityEvent sworngunskill = new SwornGunsKillEntityEvent(shooter, used, dead);
						this.plugin.getServer().getPluginManager().callEvent(sworngunskill);
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event)
	{
		if (event.isCancelled())
		{
			return;
		}
		Entity damager = event.getDamager();
		if ((event.getEntity() instanceof LivingEntity))
		{
			LivingEntity hurt = (LivingEntity) event.getEntity();
			if ((damager instanceof Projectile))
			{
				Projectile proj = (Projectile) damager;
				Bullet bullet = plugin.getBullet(proj);
				if (bullet != null)
				{
					boolean headshot = false;
					if ((isNear(proj.getLocation(), hurt.getEyeLocation(), 0.26D)) && (bullet.getShotFrom().isCanHeadshot()))
					{
						headshot = true;
					}
					SwornGunsDamageEntityEvent sworngunsdmg = new SwornGunsDamageEntityEvent(event, bullet.getShooter(),
							bullet.getShotFrom(), event.getEntity(), headshot);
//					plugin.getServer().getPluginManager().callEvent(sworngunsdmg);
					if (! sworngunsdmg.isCancelled())
					{
						double damage = sworngunsdmg.getDamage();
						double mult = 1.0D;
						if (sworngunsdmg.isHeadshot())
						{
							Util.playEffect(Effect.ZOMBIE_DESTROY_DOOR, hurt.getLocation(), 3);
							mult = 2.0D;
						}
						hurt.setLastDamage(0);
						event.setDamage(Math.ceil(damage * mult));
						int armorPenetration = bullet.getShotFrom().getArmorPenetration();
						if (armorPenetration > 0)
						{
							int health = (int) hurt.getHealth();
							int newHealth = health - armorPenetration;
							if (newHealth < 0)
							{
								newHealth = 0;
							}
							if (newHealth > 20)
							{
								newHealth = 20;
							}
							hurt.setHealth(newHealth);
						}

						bullet.getShotFrom().doKnockback(hurt, bullet.getVelocity());

						bullet.remove();
					}
					else
					{
						event.setCancelled(true);
					}
				}
			}
		}
	}

	private boolean isNear(Location location, Location eyeLocation, double d)
	{
		return Math.abs(location.getY() - eyeLocation.getY()) <= d;
	}
}