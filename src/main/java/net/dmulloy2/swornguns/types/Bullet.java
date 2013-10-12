package net.dmulloy2.swornguns.types;

import java.util.List;

import lombok.Data;
import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.util.Util;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * @author dmulloy2
 */

@Data
public class Bullet
{
	private int ticks;
	private int releaseTime;

	private boolean dead;
	private boolean active;
	private boolean destroyNextTick;
	private boolean released;

	private Entity projectile;
	private Vector velocity;

	private Location lastLocation;
	private Location startLocation;

	private GunPlayer shooter;
	private Gun shotFrom;

	private final SwornGuns plugin;

	public Bullet(SwornGuns plugin, GunPlayer owner, Vector vec, Gun gun)
	{
		this.plugin = plugin;
		this.shotFrom = gun;
		this.shooter = owner;
		this.velocity = vec;
		this.active = true;

		if (gun.isThrowable())
		{
			ItemStack thrown = new ItemStack(gun.getGunMaterial(), 1, gun.getGunByte());

			this.projectile = owner.getPlayer().getWorld().dropItem(owner.getPlayer().getEyeLocation(), thrown);

			((Item) projectile).setPickupDelay(9999999);

			this.startLocation = projectile.getLocation();
		}
		else
		{
			Class<? extends Projectile> mclass = null;

			String check = gun.getProjType().toLowerCase().replaceAll("_", "").replaceAll(" ", "");
			if (check.equalsIgnoreCase("egg"))
				mclass = Egg.class;
			else if (check.equalsIgnoreCase("arrow"))
				mclass = Arrow.class;
			else
				mclass = Snowball.class;

			this.projectile = owner.getPlayer().launchProjectile(mclass);

			((Projectile) projectile).setShooter(owner.getPlayer());

			this.startLocation = projectile.getLocation();
		}

		if (shotFrom.getReleaseTime() == -1)
		{
			this.releaseTime = (80 + (! gun.isThrowable() ? 1 : 0) * 400);
		}
		else
		{
			this.releaseTime = shotFrom.getReleaseTime();
		}
	}

	public void tick()
	{
		if (! dead)
		{
			this.ticks++;
			if (projectile != null)
			{
				this.lastLocation = projectile.getLocation();

				if (ticks > releaseTime)
				{
					EffectType eff = shotFrom.getReleaseEffect();
					if (eff != null)
					{
						eff.start(lastLocation);
					}

					this.dead = true;
					return;
				}

				if (shotFrom.isHasSmokeTrail())
				{
					lastLocation.getWorld().playEffect(lastLocation, Effect.SMOKE, 0);
				}

				if (shotFrom.isThrowable() && ticks == 90)
				{
					remove();
					return;
				}

				if (active)
				{
					if (lastLocation.getWorld().getUID() == startLocation.getWorld().getUID())
					{
						double dis = lastLocation.distance(startLocation);
						if (dis > shotFrom.getMaxDistance())
						{
							this.active = false;
							if (! shotFrom.isThrowable() && ! shotFrom.isCanGoPastMaxDistance())
							{
								velocity.multiply(0.25D);
							}
						}
					}

					projectile.setVelocity(velocity);
				}
			}
			else
			{
				this.dead = true;
			}

			if (ticks > 200)
			{
				this.dead = true;
			}
		}
		else
		{
			remove();
		}

		if (destroyNextTick)
		{
			this.dead = true;
		}
	}

	public void remove()
	{
		this.dead = true;

		plugin.removeBullet(this);

		projectile.remove();

		onHit();
		destroy();
	}

	public void onHit()
	{
		if (released)
			return;

		this.released = true;
		if (projectile != null)
		{
			this.lastLocation = projectile.getLocation();

			if (shotFrom != null)
			{
				int rad = (int) shotFrom.getExplodeRadius();
				int rad2 = rad;
				if (shotFrom.getFireRadius() > rad)
				{
					rad = (int) shotFrom.getFireRadius();
					rad2 = 2;

					for (int i = -rad; i <= rad; i++)
					{
						for (int ii = -rad2 / 2; ii <= rad2 / 2; ii++)
						{
							for (int iii = -rad; iii <= rad; iii++)
							{
								Location nloc = lastLocation.clone().add(i, ii, iii);
								if (nloc.distance(lastLocation) <= rad && Util.random(5) == 1)
								{
									nloc.getWorld().playEffect(nloc, Effect.MOBSPAWNER_FLAMES, 2);
								}
							}
						}
					}
				}
				else if (rad > 0)
				{
					for (int i = -rad; i <= rad; i++)
					{
						for (int ii = -rad2 / 2; ii <= rad2 / 2; ii++)
						{
							for (int iii = -rad; iii <= rad; iii++)
							{
								Location nloc = lastLocation.clone().add(i, ii, iii);
								if (nloc.distance(lastLocation) <= rad && Util.random(10) == 1)
								{
									new Explosion(nloc).explode();
								}
							}
						}
					}

					new Explosion(lastLocation).explode();
				}

				explode();
				fireSpread();
				flash();
			}
		}
	}

	public void explode()
	{
		if (shotFrom.getExplodeRadius() > 0.0D)
		{
			lastLocation.getWorld().createExplosion(lastLocation, 0.0F);

			if (shotFrom.isThrowable())
				projectile.teleport(projectile.getLocation().add(0.0D, 1.0D, 0.0D));

			int c = (int) shotFrom.getExplodeRadius();
			List<Entity> entities = projectile.getNearbyEntities(c, c, c);
			for (Entity entity : entities)
			{
				if (entity instanceof LivingEntity)
				{
					LivingEntity lentity = (LivingEntity) entity;
					if (! lentity.isDead() || lentity.getHealth() > 0.0D)
					{
						EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(shooter.getPlayer(), lentity, DamageCause.CUSTOM,
								0.0D);
						plugin.getServer().getPluginManager().callEvent(event);
						if (! event.isCancelled())
						{
							if (lentity.hasLineOfSight(projectile))
							{
								int dmg = shotFrom.getExplosionDamage();
								if (dmg == -1)
									dmg = shotFrom.getGunDamage();
	
								lentity.setLastDamage(0.0D);
								lentity.damage(dmg, shooter.getPlayer());
								lentity.setLastDamage(0.0D);
							}
						}
					}
				}
			}
		}
	}

	public void fireSpread()
	{
		if (shotFrom.getFireRadius() > 0.0D)
		{
			lastLocation.getWorld().playSound(lastLocation, Sound.GLASS, 20.0F, 20.0F);
			int c = (int) shotFrom.getFireRadius();
			List<Entity> entities = projectile.getNearbyEntities(c, c, c);
			for (Entity entity : entities)
			{
				if (entity instanceof LivingEntity)
				{
					LivingEntity lentity = (LivingEntity) entity;
					if (! lentity.isDead() || lentity.getHealth() > 0.0D)
					{
						EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(shooter.getPlayer(), lentity, DamageCause.CUSTOM,
								0.0D);
						plugin.getServer().getPluginManager().callEvent(event);
						if (! event.isCancelled())
						{
							lentity.setFireTicks(140);
							lentity.setLastDamage(0);
							lentity.damage(1, shooter.getPlayer());
						}
					}
				}
			}
		}
	}

	public void flash()
	{
		if (shotFrom.getFlashRadius() > 0.0D)
		{
			lastLocation.getWorld().playSound(lastLocation, Sound.SPLASH, 20.0F, 20.0F);
			int c = (int) shotFrom.getFlashRadius();
			List<Entity> entities = projectile.getNearbyEntities(c, c, c);
			for (Entity entity : entities)
			{
				if (entity instanceof LivingEntity)
				{
					LivingEntity lentity = (LivingEntity) entity;
					if (! lentity.isDead() || lentity.getHealth() > 0.0D)
					{
						EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(shooter.getPlayer(), lentity, DamageCause.CUSTOM,
								0.0D);
						plugin.getServer().getPluginManager().callEvent(event);
						if (! event.isCancelled())
						{
							if (lentity.hasLineOfSight(projectile))
							{
								lentity.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 140, 1));
							}
						}
					}
				}
			}
		}
	}

	public void destroy()
	{
		this.projectile = null;
		this.velocity = null;
		this.shotFrom = null;
		this.shooter = null;
	}
}