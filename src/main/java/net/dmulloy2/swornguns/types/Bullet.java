package net.dmulloy2.swornguns.types;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.util.Util;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Fish;
import org.bukkit.entity.Item;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.ThrownExpBottle;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * @author dmulloy2
 */

@Getter
@Setter
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
			Class<? extends Projectile> mclass = Snowball.class;

			String check = gun.getProjType().toLowerCase().replaceAll("_", "").replaceAll(" ", "");
			switch (check)
			{
				// All valid Entities that extend Projectile
				case "arrow":
					mclass = Arrow.class;
				case "egg":
					mclass = Egg.class;
				case "enderpearl":
					mclass = EnderPearl.class;
				case "fireball":
					mclass = Fireball.class;
				case "fish":
					mclass = Fish.class;
				case "largefireball":
					mclass = LargeFireball.class;
				case "smallfireball":
					mclass = SmallFireball.class;
				case "thrownexpbottle":
					mclass = ThrownExpBottle.class;
				case "thrownpotion":
					mclass = ThrownPotion.class;
				case "witherskull":
					mclass = WitherSkull.class;
			}

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

	public final void tick()
	{
		if (! dead && shooter.getPlayer().getHealth() > 0.0D)
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

	public final void remove()
	{
		// (Final) hit
		onHit();

		// Mark as dead
		this.dead = true;

		// Destroy
		projectile.remove();
		destroy();

		// Unregister
		plugin.removeBullet(this);
	}

	public final void onHit()
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

	private final void explode()
	{
		if (shotFrom.getExplodeRadius() > 0.0D)
		{
			// Create the explosion
			lastLocation.getWorld().createExplosion(lastLocation, 0.0F);

			if (shotFrom.isThrowable())
				projectile.teleport(projectile.getLocation().add(0.0D, 1.0D, 0.0D));

			// Calculate damage
			double damage = shotFrom.getExplosionDamage();
			if (damage <= 0.0D)
				damage = shotFrom.getGunDamage();

			if (damage > 0.0D)
			{
				int rad = (int) shotFrom.getExplodeRadius();
				List<Entity> entities = projectile.getNearbyEntities(rad, rad, rad);
				for (Entity entity : entities)
				{
					if (entity.isValid() && entity instanceof LivingEntity)
					{
						LivingEntity lentity = (LivingEntity) entity;
						if (lentity.getHealth() > 0.0D)
						{
							// Call event
							EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(shooter.getPlayer(), lentity,
									DamageCause.ENTITY_EXPLOSION, damage);
							plugin.getServer().getPluginManager().callEvent(event);
							if (! event.isCancelled())
							{
								if (lentity.hasLineOfSight(projectile))
								{
//									lentity.setLastDamage(0.0D);
									lentity.damage(damage, shooter.getPlayer());
//									lentity.setLastDamage(0.0D);
								}
							}
						}
					}
				}
			}
		}
	}

	private final void fireSpread()
	{
		if (shotFrom.getFireRadius() > 0.0D)
		{
			lastLocation.getWorld().playSound(lastLocation, Sound.GLASS, 20.0F, 20.0F);
			int rad = (int) shotFrom.getFireRadius();
			List<Entity> entities = projectile.getNearbyEntities(rad, rad, rad);
			for (Entity entity : entities)
			{
				if (entity.isValid() && entity instanceof LivingEntity)
				{
					LivingEntity lentity = (LivingEntity) entity;
					if (lentity.getHealth() > 0.0D)
					{
						EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(shooter.getPlayer(), lentity, DamageCause.FIRE_TICK,
								1.0D);
						plugin.getServer().getPluginManager().callEvent(event);
						if (!event.isCancelled())
						{
							lentity.setFireTicks(140);
//							lentity.setLastDamage(0.0D);
							lentity.damage(1.0D, shooter.getPlayer());
						}
					}
				}
			}
		}
	}

	private final void flash()
	{
		if (shotFrom.getFlashRadius() > 0.0D)
		{
			lastLocation.getWorld().playSound(lastLocation, Sound.SPLASH, 20.0F, 20.0F);
			int c = (int) shotFrom.getFlashRadius();
			List<Entity> entities = projectile.getNearbyEntities(c, c, c);
			for (Entity entity : entities)
			{
				if (entity.isValid() && entity instanceof LivingEntity)
				{
					LivingEntity lentity = (LivingEntity) entity;
					if (lentity.getHealth() > 0.0D)
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

	public final void destroy()
	{
		this.projectile = null;
		this.velocity = null;
		this.shotFrom = null;
		this.shooter = null;
	}
}