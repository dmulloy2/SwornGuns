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

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.*;

import com.google.common.base.Function;
import com.google.common.base.Functions;

import org.bukkit.*;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import net.dmulloy2.swornapi.util.Util;
import net.dmulloy2.swornguns.SwornGuns;

/**
 * @author dmulloy2
 */

@Getter
@ToString(of={"shooter", "shotFrom"})
@Accessors(fluent = true)
public class Bullet extends BukkitRunnable
{
	private int ticks;
	private int releaseTime;

	private boolean dead;
	private boolean active;
	private boolean released;
	private boolean destroyed;
	private @Setter boolean destroyNextTick;

	private Entity projectile;
	private Vector velocity;

	private Location lastLocation;
	private Location startLocation;

	private GunData data;
	private GunPlayer shooter;
	private Gun shotFrom;
	private UUID id;

	private final SwornGuns plugin;

	public Bullet(SwornGuns plugin, GunPlayer shooter, Gun shotFrom, Vector velocity)
	{
		this.plugin = plugin;
		this.shotFrom = shotFrom;
		this.data = shotFrom.data();
		this.shooter = shooter;
		this.velocity = velocity;
		this.active = true;

		if (data.isThrowable())
		{
			ItemStack thrown = new ItemStack(data.material());

			this.projectile = shooter.player().getWorld().dropItem(shooter.player().getEyeLocation(), thrown, item -> {
				item.setCanMobPickup(false);
				item.setCanPlayerPickup(false);
			});
		}
		else
		{
			Class<? extends Projectile> projectileClass = data.projectileClass();
			this.projectile = shooter.player().launchProjectile(projectileClass, velocity);
		}
		this.id = projectile.getUniqueId();

		this.startLocation = projectile.getLocation();

		if (data.releaseTime() == -1)
		{
			this.releaseTime = 80 + (data.isThrowable() ? 0 : 1) * 400;
		}
		else
		{
			this.releaseTime = data.releaseTime();
		}
	}

	@Override
	public void run()
	{
		boolean keepAlive = false;

		try
		{
			keepAlive = tick();
		}
		catch (Exception ex)
		{
			plugin.getLogHandler().warn(ex, "Ticking bullet " + this);
		}

		if (!keepAlive)
		{
			remove();
		}
	}

	private boolean tick()
	{
		// Make sure the bullet is still valid
		if (dead || destroyNextTick || projectile == null || shooter == null)
		{
			return false;
		}

		// Remove the bullet if it's below bedrock
		if (projectile.getLocation().getY() <= 0.0D)
		{
			return false;
		}

		// Make sure the shooter is still valid
		Player player = shooter.player();
		if (player == null || ! player.isOnline() || player.getHealth() <= 0.0D)
		{
			return false;
		}

		this.ticks++;
		this.lastLocation = projectile.getLocation();

		if (ticks > releaseTime)
		{
			EffectData eff = data.releaseEffect();
			if (eff != null)
			{
				new EffectTask(eff, lastLocation).runTaskTimer(plugin, 1L, 1L);
			}

			return false;
		}

		if (data.hasSmokeTrail())
		{
			lastLocation.getWorld().playEffect(lastLocation, Effect.SMOKE, 0);
		}

		if (data.isThrowable() && ticks == 90)
		{
			return false;
		}

		if (active)
		{
			if (lastLocation.getWorld().equals(startLocation.getWorld()))
			{
				double dis = lastLocation.distance(startLocation);
				if (dis > data.maxDistance())
				{
					this.active = false;
					if (! data.isThrowable() && ! data.canGoPastMaxDistance())
					{
						velocity.multiply(0.25D);
					}
				}
			}

			projectile.setVelocity(velocity);
		}

		return ticks <= 200;
	}

	public final void remove()
	{
		if (destroyed)
			return;

		plugin.removeBullet(this);
		cancel();

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
		if (projectile == null)
		{
			return;
		}

		this.lastLocation = projectile.getLocation();

		int rad = (int) data.explodeRadius();
		int rad2 = rad;
		if (data.fireRadius() > rad)
		{
			rad = (int) data.fireRadius();
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

	private void explosion(Location location)
	{
		if (data.explosionType().equals("TNT"))
		{
			double x = location.getX();
			double y = location.getY();
			double z = location.getZ();

			location.getWorld().createExplosion(x, y, z, (float) data.explodeRadius(), false, false);
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

	private boolean canDamage(Entity damager, Entity entity, DamageCause cause, double damage)
	{
		Map<DamageModifier, Double> modifiers = new HashMap<>();
		modifiers.put(DamageModifier.BASE, damage);

		Map<DamageModifier, Function<? super Double, Double>> functions = new HashMap<>();
		functions.put(DamageModifier.BASE, ZERO);

		DamageSource source = DamageSource
			.builder(DamageType.PLAYER_ATTACK)
			.withCausingEntity(damager)
			.withDirectEntity(projectile)
			.build();

		EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(damager, entity, cause, source, modifiers, functions, false);
		plugin.getServer().getPluginManager().callEvent(event);
		return !event.isCancelled();
	}

	private void explosionDamage()
	{
		if (data.explodeRadius() <= 0.0D)
		{
			return;
		}

		// Create the explosion
		lastLocation.getWorld().createExplosion(lastLocation, 0.0F);

		if (data.isThrowable())
			projectile.teleport(projectile.getLocation().add(0.0D, 1.0D, 0.0D));

		// Calculate damage
		double damage = data.explosionDamage();
		if (damage <= 0.0D)
			damage = data.gunDamage();

		if (damage <= 0.0D)
		{
			return;
		}

		double rad = data.explodeRadius();
		List<Entity> entities = projectile.getNearbyEntities(rad, rad, rad);
		for (Entity entity : entities)
		{
			if (!entity.isValid() || !(entity instanceof LivingEntity lentity))
			{
				continue;
			}

			if (lentity.getHealth() <= 0.0D || !lentity.hasLineOfSight(projectile))
			{
				continue;
			}

			if (!canDamage(shooter.player(), lentity, DamageCause.ENTITY_EXPLOSION, damage))
			{
				continue;
			}

			lentity.damage(damage, shooter.player());
		}
	}

	private void fireSpread()
	{
		if (data.fireRadius() <= 0.0D)
		{
			return;
		}

		lastLocation.getWorld().playSound(lastLocation, Sound.BLOCK_GLASS_BREAK, 20.0F, 20.0F);
		double rad = data.fireRadius();
		List<Entity> entities = projectile.getNearbyEntities(rad, rad, rad);
		for (Entity entity : entities)
		{
			if (!entity.isValid() || !(entity instanceof LivingEntity lentity))
			{
				continue;
			}

			if (lentity.getHealth() <= 0.0D || !lentity.hasLineOfSight(projectile))
			{
				continue;
			}

			if (!canDamage(shooter.player(), lentity, DamageCause.FIRE_TICK, 1.0D))
			{
				continue;
			}

			lentity.setFireTicks(7 * 20);
			lentity.damage(1.0D, shooter.player());
		}
	}

	private void flash()
	{
		if (data.flashRadius() <= 0.0D)
		{
			return;
		}

		lastLocation.getWorld().playSound(lastLocation, Sound.ENTITY_GENERIC_SPLASH, 20.0F, 20.0F);
		double rad = data.flashRadius();
		List<Entity> entities = projectile.getNearbyEntities(rad, rad, rad);
		for (Entity entity : entities)
		{
			if (!entity.isValid() || !(entity instanceof LivingEntity lentity))
			{
				continue;
			}

			if (lentity.getHealth() <= 0.0D || !lentity.hasLineOfSight(projectile))
			{
				continue;
			}

			if (!canDamage(shooter.player(), lentity, DamageCause.CUSTOM, 0.0D))
			{
				continue;
			}

			lentity.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 7 * 20, 1));
		}
	}

	public boolean isHeadshot(LivingEntity entity)
	{
		Location location = projectile.getLocation();
		return Math.abs(location.getY() - entity.getEyeLocation().getY()) <= 0.26D;
	}

	public final void destroy()
	{
		this.destroyed = true;
		this.projectile = null;
		this.velocity = null;
		this.data = null;
		this.shooter = null;
	}
}
