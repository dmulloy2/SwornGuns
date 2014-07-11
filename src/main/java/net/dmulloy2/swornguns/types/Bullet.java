package net.dmulloy2.swornguns.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import lombok.Data;
import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.util.Util;

import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Fish;
import org.bukkit.entity.Item;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.ThrownExpBottle;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.google.common.base.Function;
import com.google.common.base.Functions;

/**
 * @author dmulloy2
 */

@Data
public class Bullet
{
	public static int nextID = 1;

	private int ticks;
	private int releaseTime;

	private boolean dead;
	private boolean active;
	private boolean released;
	private boolean destroyed;
	private boolean destroyNextTick;

	private Entity projectile;
	private Vector velocity;

	private Location lastLocation;
	private Location startLocation;

	private GunPlayer shooter;
	private Gun shotFrom;
	private int id;

	private final SwornGuns plugin;
	public Bullet(SwornGuns plugin, GunPlayer shooter, Gun shotFrom, Vector velocity)
	{
		this.plugin = plugin;
		this.shotFrom = shotFrom;
		this.shooter = shooter;
		this.velocity = velocity;
		this.active = true;
		this.id = nextID++;

		if (shotFrom.isThrowable())
		{
			ItemStack thrown = shotFrom.getMaterial().newItemStack(1);

			this.projectile = shooter.getPlayer().getWorld().dropItem(shooter.getPlayer().getEyeLocation(), thrown);
			((Item) projectile).setPickupDelay(9999999);
			this.startLocation = projectile.getLocation();
		}
		else
		{
			Class<? extends Projectile> mclass = Snowball.class;

			String check = shotFrom.getProjType().toLowerCase().replaceAll("_", "").replaceAll(" ", "");
			switch (check)
			{
				case "arrow":
					mclass = Arrow.class;
					break;
				case "egg":
					mclass = Egg.class;
					break;
				case "enderpearl":
					mclass = EnderPearl.class;
					break;
				case "fireball":
					mclass = Fireball.class;
					break;
				case "fish":
					mclass = Fish.class;
					break;
				case "largefireball":
					mclass = LargeFireball.class;
					break;
				case "smallfireball":
					mclass = SmallFireball.class;
					break;
				case "thrownexpbottle":
					mclass = ThrownExpBottle.class;
					break;
				case "thrownpotion":
					mclass = ThrownPotion.class;
					break;
				case "witherskull":
					mclass = WitherSkull.class;
					break;
				default:
					break;
			}

			this.projectile = shooter.getPlayer().launchProjectile(mclass);
			((Projectile) projectile).setShooter(shooter.getPlayer());
			this.startLocation = projectile.getLocation();
		}

		if (shotFrom.getReleaseTime() == -1)
		{
			this.releaseTime = 80 + (shotFrom.isThrowable() ? 0 : 1) * 400;
		}
		else
		{
			this.releaseTime = shotFrom.getReleaseTime();
		}
	}

	public final void tick()
	{
		if (dead || destroyNextTick)
		{
			remove();
			return;
		}

		// Projectile Check
		if (projectile == null)
		{
			remove();
			return;
		}

		// Shooter check
		if (shooter == null)
		{
			remove();
			return;
		}

		// Player check
		Player player = shooter.getPlayer();
		if (player == null || ! player.isOnline() || player.getHealth() <= 0.0D)
		{
			remove();
			return;
		}

		// Location check
		if (projectile.getLocation().getY() <= 3.0D)
		{
			remove();
			return;
		}

		this.ticks++;
		this.lastLocation = projectile.getLocation();

		if (ticks > releaseTime)
		{
			EffectType eff = shotFrom.getReleaseEffect();
			if (eff != null)
			{
				eff.start(lastLocation);
			}

			remove();
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

		if (ticks > 200)
		{
			remove();
			return;
		}
	}

	public final void remove()
	{
		// Unregister
		plugin.removeBullet(this);

		if (destroyed)
			return;

		this.dead = true;

		// (Final) hit
		onHit();

		// Destroy
		if (projectile != null)
			projectile.remove();

		destroy();
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
									explosion(nloc);
								}
							}
						}
					}

					explosion(lastLocation);
					explosionDamage();
				}

				fireSpread();
				flash();
			}
		}
	}

	private final void explosion(Location location)
	{
		if (shotFrom.getExplosionType().equals("TNT"))
		{
			double x = location.getX();
			double y = location.getY();
			double z = location.getZ();

			location.getWorld().createExplosion(x, y, z, (float) shotFrom.getExplodeRadius(), false, false);
		}
		else
		{
			World world = location.getWorld();
			Firework firework = world.spawn(location, Firework.class);

			FireworkMeta meta = firework.getFireworkMeta();
			meta.addEffect(getFireworkEffect());
			meta.setPower(1);

			firework.setFireworkMeta(meta);

			try
			{
				firework.detonate();
			} catch (Throwable ex) { }
		}
	}

	private final FireworkEffect getFireworkEffect()
	{
		// Colors
		List<Color> colors = new ArrayList<Color>();
		colors.add(Color.RED);
		colors.add(Color.RED);
		colors.add(Color.RED);
		colors.add(Color.ORANGE);
		colors.add(Color.ORANGE);
		colors.add(Color.ORANGE);
		colors.add(Color.BLACK);
		colors.add(Color.GRAY);

		// Type
		Random rand = new Random();
		FireworkEffect.Type type = FireworkEffect.Type.BALL_LARGE;
		if (rand.nextInt(2) == 0)
		{
			type = FireworkEffect.Type.BURST;
		}

		// Build the effect
		FireworkEffect effect = FireworkEffect.builder()
				.flicker(true)
				.withColor(colors)
				.withFade(colors)
				.with(type)
				.trail(true)
				.build();

		return effect;
	}

	private static final Function<? super Double, Double> ZERO = Functions.constant(-0.0);

	@SuppressWarnings("deprecation") // Old Event
	private final EntityDamageByEntityEvent getDamageEvent(Entity damager, Entity entity, DamageCause cause, double damage)
	{
		try
		{
			Map<DamageModifier, Double> modifiers = new HashMap<>();
			modifiers.put(DamageModifier.BASE, damage);

			Map<DamageModifier, Function<? super Double, Double>> functions = new HashMap<>();
			functions.put(DamageModifier.BASE, ZERO);

			return new EntityDamageByEntityEvent(damager, entity, cause, modifiers, functions);
		}
		catch (Throwable ex)
		{
			return new EntityDamageByEntityEvent(damager, entity, cause, damage);
		}
	}

	private final void explosionDamage()
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
				double rad = shotFrom.getExplodeRadius();
				List<Entity> entities = projectile.getNearbyEntities(rad, rad, rad);
				for (Entity entity : entities)
				{
					if (entity.isValid() && entity instanceof LivingEntity)
					{
						LivingEntity lentity = (LivingEntity) entity;
						if (lentity.getHealth() > 0.0D)
						{
							// Call event
							EntityDamageByEntityEvent event = getDamageEvent(shooter.getPlayer(), lentity, DamageCause.ENTITY_EXPLOSION,
									damage);
							plugin.getServer().getPluginManager().callEvent(event);
							if (! event.isCancelled())
							{
								if (lentity.hasLineOfSight(projectile))
								{
									lentity.damage(damage, shooter.getPlayer());
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
			double rad = shotFrom.getFireRadius();
			List<Entity> entities = projectile.getNearbyEntities(rad, rad, rad);
			for (Entity entity : entities)
			{
				if (entity.isValid() && entity instanceof LivingEntity)
				{
					LivingEntity lentity = (LivingEntity) entity;
					if (lentity.getHealth() > 0.0D)
					{
						EntityDamageByEntityEvent event = getDamageEvent(shooter.getPlayer(), lentity, DamageCause.FIRE_TICK, 1.0D);
						plugin.getServer().getPluginManager().callEvent(event);
						if (! event.isCancelled())
						{
							lentity.setFireTicks(7 * 20);
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
			double rad = shotFrom.getFlashRadius();
			List<Entity> entities = projectile.getNearbyEntities(rad, rad, rad);
			for (Entity entity : entities)
			{
				if (entity.isValid() && entity instanceof LivingEntity)
				{
					LivingEntity lentity = (LivingEntity) entity;
					if (lentity.getHealth() > 0.0D)
					{
						EntityDamageByEntityEvent event = getDamageEvent(shooter.getPlayer(), lentity, DamageCause.CUSTOM, 0.0D);
						plugin.getServer().getPluginManager().callEvent(event);
						if (! event.isCancelled())
						{
							if (lentity.hasLineOfSight(projectile))
							{
								lentity.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 7 * 20, 1));
							}
						}
					}
				}
			}
		}
	}

	public final void destroy()
	{
		this.destroyed = true;
		this.projectile = null;
		this.velocity = null;
		this.shotFrom = null;
		this.shooter = null;
	}

	@Override
	public String toString()
	{
		if (dead)
			return "Bullet { dead = true }";

		return "Bullet { shooter = " + shooter + ", shotFrom = " + shotFrom.getFileName() + ", id = " + id + " }";
	}

	@Override
	public boolean equals(Object obj)
	{
		if (dead)
			return this == obj;

		if (obj instanceof Bullet)
		{
			Bullet that = (Bullet) obj;
			return this.shooter.equals(that.shooter) && this.shotFrom.equals(that.shotFrom) && this.id == that.id;
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		int hash = 99;
		hash *= 1 + shooter.hashCode();
		hash *= 1 + shotFrom.hashCode();
		hash *= 1 + id;
		return hash;
	}
}