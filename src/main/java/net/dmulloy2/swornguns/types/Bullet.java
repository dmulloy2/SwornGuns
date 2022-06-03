/**
 * SwornGuns - Guns in Minecraft
 * Copyright (C) dmulloy2 <http://dmulloy2.net>
 * Copyright (C) Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.dmulloy2.swornguns.types;

import java.util.*;

import com.google.common.base.Function;
import com.google.common.base.Functions;

import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.util.Util;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import lombok.Data;

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

		if (shotFrom.isThrowable())
		{
			ItemStack thrown = shotFrom.getMaterial().newItemStack(1);

			this.projectile = shooter.getPlayer().getWorld().dropItem(shooter.getPlayer().getEyeLocation(), thrown);
			this.id = projectile.getEntityId();

			((Item) projectile).setPickupDelay(9999999);
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
				case "fishhook":
					mclass = FishHook.class;
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
			this.id = projectile.getEntityId();

			((Projectile) projectile).setShooter(shooter.getPlayer());
		}
		this.startLocation = projectile.getLocation();

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
		// Make sure the bullet is still valid
		if (dead || destroyNextTick || projectile == null || shooter == null)
		{
			remove();
			return;
		}

		// Remove the bullet if it's below bedrock
		if (projectile.getLocation().getY() <= 0.0D)
		{
			remove();
			return;
		}

		// Make sure the shooter is still valid
		Player player = shooter.getPlayer();
		if (player == null || ! player.isOnline() || player.getHealth() <= 0.0D)
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
				eff.start(lastLocation);

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
			if (lastLocation.getWorld().equals(startLocation.getWorld()))
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
		}
	}

	public final void remove()
	{
		// Unregister
		plugin.removeBullet(this);

		if (destroyed)
			return;

		this.dead = true;

		// Final hit
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

	private void explosion(Location location)
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
			} catch (Throwable ignored) { }
		}
	}

	private FireworkEffect getFireworkEffect()
	{
		// Colors
		List<Color> colors = new ArrayList<>();
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

		return FireworkEffect.builder()
				.flicker(true)
				.withColor(colors)
				.withFade(colors)
				.with(type)
				.trail(true)
				.build();
	}

	@SuppressWarnings("Guava")
	private static final Function<? super Double, Double> ZERO = Functions.constant(-0.0)::apply;

	@SuppressWarnings("deprecation") // Old Event
	private EntityDamageByEntityEvent getDamageEvent(Entity damager, Entity entity, DamageCause cause, double damage)
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

	private void explosionDamage()
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

	private void fireSpread()
	{
		if (shotFrom.getFireRadius() > 0.0D)
		{
			lastLocation.getWorld().playSound(lastLocation, Sound.BLOCK_GLASS_BREAK, 20.0F, 20.0F);
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

	private void flash()
	{
		if (shotFrom.getFlashRadius() > 0.0D)
		{
			lastLocation.getWorld().playSound(lastLocation, Sound.ENTITY_GENERIC_SPLASH, 20.0F, 20.0F);
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
		if (destroyed)
			return "Destroyed Bullet";

		return "Bullet[shooter=" + shooter + ", shotFrom=" + shotFrom.getFileName() + ", id=" + id + "]";
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null) return false;
		if (obj == this) return true;

		if (destroyed)
			return false;

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
		return Objects.hash(shooter, shotFrom, id);
	}
}
