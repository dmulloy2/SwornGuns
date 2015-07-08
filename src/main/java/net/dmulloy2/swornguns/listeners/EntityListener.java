/**
 * SwornGuns - Guns in Minecraft
 * Copyright (C) 2012 - 2015 MineSworn
 * Copyright (C) 2013 - 2015 dmulloy2
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.dmulloy2.swornguns.listeners;

import java.util.EnumMap;
import java.util.List;

import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.types.Bullet;
import net.dmulloy2.swornguns.types.Gun;
import net.dmulloy2.types.Reloadable;
import net.dmulloy2.util.MaterialUtil;
import net.dmulloy2.util.Util;

import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.event.entity.ProjectileHitEvent;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableMap;

/**
 * @author dmulloy2
 */

public class EntityListener implements Listener, Reloadable
{
	private boolean blockCrack;
	private boolean blockShatter;
	private boolean bloodEffectEnabled;
	private boolean bloodEffectGunsOnly;
	private boolean smokeEffect;
	private boolean bulletSoundEnabled;

	private Sound bulletSound;
	private Material bloodEffectType;
	private List<Material> shatterBlocks;

	private final SwornGuns plugin;
	public EntityListener(SwornGuns plugin)
	{
		this.plugin = plugin;
		this.reload(); // Load configuration
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onProjectileHit(ProjectileHitEvent event)
	{
		Projectile proj = event.getEntity();
		Bullet bullet = plugin.getBullet(proj);
		if (bullet != null)
		{
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
					if (nearby.equals(bullet.getShooter().getPlayer()))
						continue; // Don't shoot ourselves

					nearby.getLocation(nLoc);

					// Exact match
					if (Util.coordsEqual(loc, nLoc))
					{
						hurt = (Damageable) nearby;
						break;
					}

					// Find closest entity
					double distance = nLoc.distance(loc);
					if (hurt == null || distance < hurtDistance)
					{
						hurt = (Damageable) nearby;
						hurtDistance = distance;
					}
				}
			}

			if (hurt != null)
			{
				// Call the damage event, which will be handled below
				// Note: This is the same code from the EntityDamageByEntityEvent constructor, just non-deprecated
				EntityDamageByEntityEvent damageEvent = new EntityDamageByEntityEvent(proj, hurt, DamageCause.PROJECTILE,
						new EnumMap<DamageModifier, Double>(ImmutableMap.of(DamageModifier.BASE, 0.0D)),
						new EnumMap<DamageModifier, Function<? super Double, Double>>(ImmutableMap.of(DamageModifier.BASE,
								Functions.constant(-0.0))));

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

			// Make sure they aren't in an arena
			if (plugin.isUltimateArenaEnabled())
			{
				if (plugin.getUltimateArenaHandler().isInArena(bullet.getShooter().getPlayer()))
					return;
			}

			// Check with Factions too
			if (plugin.isSwornNationsEnabled())
			{
				if (! plugin.getSwornNationsHandler().checkFactions(block.getLocation(), true))
					return;
			}

			// Block cracking
			if (blockCrack && mat == Material.STONE)
			{
				BlockBreakEvent blockBreak = new BlockBreakEvent(block, bullet.getShooter().getPlayer());
				plugin.getServer().getPluginManager().callEvent(blockBreak);
				if (! blockBreak.isCancelled())
					block.setType(Material.COBBLESTONE);
			}

			// Block shattering
			if (blockShatter && shatterBlocks.contains(mat))
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
			if (event.getEntity() instanceof Damageable)
			{
				Damageable hurt = (Damageable) event.getEntity();
				if (event.getDamager() instanceof Projectile)
				{
					// Make sure the projectile is one of our bullets
					Projectile proj = (Projectile) event.getDamager();
					Bullet bullet = plugin.getBullet(proj);
					if (bullet != null)
					{
						// Realism start - gun effects
						World world = hurt.getWorld();

						if (bloodEffectEnabled && bloodEffectGunsOnly)
						{
							if (bloodEffectType != null)
							{
								world.playEffect(hurt.getLocation(), Effect.STEP_SOUND, bloodEffectType);
								world.playEffect(hurt.getLocation().add(0, 1, 0), Effect.STEP_SOUND, bloodEffectType);
							}
						}

						if (smokeEffect)
						{
							world.playEffect(bullet.getShooter().getPlayer().getLocation(), Effect.SMOKE, 5);
						}

						if (bulletSoundEnabled)
						{
							if (bulletSound != null)
								world.playSound(hurt.getLocation(), bulletSound, 10, 1);
						}
						// Realism end

						Location loc = proj.getLocation();

						// Headshots
						double mult = 1.0D;
						if (hurt instanceof LivingEntity)
						{
							LivingEntity lentity = (LivingEntity) hurt;
							if (isNear(loc, lentity.getEyeLocation(), 0.26D))
								mult = 1.5D;
						}

						Gun shotFrom = bullet.getShotFrom();

						// Deal the damage
						double damage = shotFrom.getGunDamage() * mult;
						event.setDamage(DamageModifier.BASE, damage);

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

		if (! bloodEffectEnabled || bloodEffectGunsOnly || bloodEffectType == null)
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
				world.playEffect(entity.getLocation(), Effect.STEP_SOUND, bloodEffectType);
				world.playEffect(entity.getLocation().add(0, 1, 0), Effect.STEP_SOUND, bloodEffectType);
			}
		}
	}
	// Realism end

	@Override
	public void reload()
	{
		this.blockCrack = plugin.getConfig().getBoolean("block-crack");
		this.blockShatter = plugin.getConfig().getBoolean("block-shatter.enabled");
		this.bloodEffectEnabled = plugin.getConfig().getBoolean("blood-effect.enabled");
		this.bloodEffectGunsOnly = plugin.getConfig().getBoolean("blood-effect.guns-only");
		this.smokeEffect = plugin.getConfig().getBoolean("smoke-effect");
		this.bulletSoundEnabled = plugin.getConfig().getBoolean("bullet-sound.enabled");
		this.bulletSound = SwornGuns.getSound(plugin.getConfig().getString("bullet-sound.sound"));
		this.bloodEffectType = MaterialUtil.getMaterial(plugin.getConfig().getString("blood-effect.block-id"));
		this.shatterBlocks = MaterialUtil.fromStrings(plugin.getConfig().getStringList("block-shatter.blocks"));
	}
}