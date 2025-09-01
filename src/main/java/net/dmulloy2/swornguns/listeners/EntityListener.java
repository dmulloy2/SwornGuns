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
package net.dmulloy2.swornguns.listeners;

import java.util.List;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileHitEvent;

import net.dmulloy2.swornapi.util.Util;
import net.dmulloy2.swornguns.Config;
import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.events.SwornGunsBlockDamageEvent;
import net.dmulloy2.swornguns.types.Bullet;
import net.dmulloy2.swornguns.types.Gun;
import net.dmulloy2.swornguns.types.GunPlayer;

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
		Projectile proj = event.getEntity();
		Bullet bullet = plugin.getBullet(proj);
		if (bullet != null)
		{
			GunPlayer shooter = bullet.getShooter();
			Player player = shooter.getPlayer();

			// Attempt to determine which Entity (if any) we hit.
			// This is necessary because while the ProjectileHitEvent fires reliably,
			// the EntityDamageByEntityEvent only fires once per round / bullet.

			// This seems to work well enough, although I hope to find a better solution.

			Damageable hurt = null;
			double hurtDistance = -1.0D;

			Location loc = proj.getLocation();
			Location nLoc = new Location(null, 0, 0, 0);

			double radius = 2.0D;
			List<Entity> nearbyEntities = proj.getNearbyEntities(radius, radius, radius);
			for (Entity nearby : nearbyEntities)
			{
				if (nearby instanceof Damageable)
				{
					if (nearby.equals(player))
						continue; // Don't shoot ourselves

					nearby.getLocation(nLoc);

					// Exact match
					if (Util.coordsEqual(loc, nLoc))
					{
						hurt = (Damageable) nearby;
						break;
					}

					// Find the closest entity
					double distance = nLoc.distanceSquared(loc);
					if (hurt == null || distance < hurtDistance)
					{
						hurt = (Damageable) nearby;
						hurtDistance = distance;
					}
				}
			}

			if (hurt != null)
			{
				// TODO we don't check if this is cancelled
				// Call the damage event, which will be handled below
				EntityDamageByEntityEvent damageEvent = new EntityDamageByEntityEvent(proj, hurt, DamageCause.PROJECTILE, 0);
				plugin.getServer().getPluginManager().callEvent(damageEvent);
			}

			bullet.onHit();
			bullet.setDestroyNextTick(true);

			// ---- Effects
			Block block = loc.getBlock();
			Material mat = block.getType();

			// Try to get a non-AIR block
			// TODO Maybe use a BlockIterator?

			double i = 0.2D;
			while (mat == Material.AIR && i < 4.0D)
			{
				block = block.getLocation().add(proj.getVelocity().normalize().multiply(i)).getBlock();
				mat = block.getType();
				i += 0.2D;
			}

			if (mat != Material.AIR)
			{
				block.getLocation().getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, mat);
			}

			// allow UltimateArena and SwornNations to cancel if they'd like
			SwornGunsBlockDamageEvent sgDamageEvent = new SwornGunsBlockDamageEvent(player, block.getLocation());
			plugin.getServer().getPluginManager().callEvent(sgDamageEvent);
			if (sgDamageEvent.isCancelled())
			{
				return;
			}

			// Block cracking
			if (Config.blockCrack && mat == Material.STONE)
			{
				BlockBreakEvent blockBreak = new BlockBreakEvent(block, bullet.getShooter().getPlayer());
				plugin.getServer().getPluginManager().callEvent(blockBreak);
				if (! blockBreak.isCancelled())
					block.setType(Material.COBBLESTONE);
			}

			// Block shattering
			if (Config.blockShatterEnabled && Config.blockShatterBlocks.contains(mat))
			{
				BlockBreakEvent blockBreak = new BlockBreakEvent(block, bullet.getShooter().getPlayer());
				plugin.getServer().getPluginManager().callEvent(blockBreak);
				if (! blockBreak.isCancelled())
					block.breakNaturally();
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event)
	{
		if (event.getCause() == DamageCause.PROJECTILE)
		{
			if (event.getEntity() instanceof Damageable hurt)
			{
				if (event.getDamager() instanceof Projectile proj)
				{
					// Make sure the projectile is one of our bullets
					Bullet bullet = plugin.getBullet(proj);
					if (bullet != null)
					{
						// Realism start - gun effects
						World world = hurt.getWorld();

						if (Config.bloodEffectEnabled && Config.bloodEffectGunsOnly && Config.bloodEffectEnabled)
						{
							world.playEffect(hurt.getLocation(), Effect.STEP_SOUND, Config.bloodEffectType);
							world.playEffect(hurt.getLocation().add(0, 1, 0), Effect.STEP_SOUND, Config.bloodEffectType);
						}

						if (Config.smokeEffect)
						{
							world.playEffect(bullet.getShooter().getPlayer().getLocation(), Effect.SMOKE, 5);
						}

						if (Config.bulletSoundEnabled && Config.bulletSound != null)
						{
							world.playSound(hurt.getLocation(), Config.bulletSound, 10, 1);
						}
						// Realism end

						Location loc = proj.getLocation();

						// Headshots
						double mult = 1.0D;
						if (hurt instanceof LivingEntity lentity)
						{
							if (isNear(loc, lentity.getEyeLocation(), 0.26D))
								mult = 1.5D;
						}

						Gun shotFrom = bullet.getShotFrom();

						// Deal the damage
						double damage = shotFrom.getGunDamage() * mult;
						event.setDamage(damage);
						
						// Armor penetration
						double armorPenetration = shotFrom.getArmorPenetration();
						if (armorPenetration > 0.0D && hurt.getHealth() - event.getFinalDamage() > 0.0D)
						{
							double newHealth = Math.max(0, hurt.getHealth() - armorPenetration);
							hurt.setHealth(newHealth);
						}

						shotFrom.doKnockback(hurt, bullet.getVelocity());
					}
				}
			}
		}
	}

	private boolean isNear(Location location, Location eyeLocation, double d)
	{
		return Math.abs(location.getY() - eyeLocation.getY()) <= d;
	}

	// Realism start - blood effects
	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityDamage(EntityDamageEvent event)
	{
		if (event.isCancelled() || event.getDamage() <= 0.0D)
			return;

		if (! Config.bloodEffectEnabled || Config.bloodEffectGunsOnly || Config.bloodEffectType == null)
			return;

		Entity entity = event.getEntity();
		if (event.getEntity() instanceof LivingEntity)
		{
			if (entity instanceof Player)
			{
				if (((Player) entity).getGameMode() == GameMode.CREATIVE)
					return;
			}

			if (event.getCause() != DamageCause.DROWNING && event.getCause() != DamageCause.LAVA)
			{
				World world = entity.getWorld();
				world.playEffect(entity.getLocation(), Effect.STEP_SOUND, Config.bloodEffectType);
				world.playEffect(entity.getLocation().add(0, 1, 0), Effect.STEP_SOUND, Config.bloodEffectType);
			}
		}
	}
	// Realism end
}
