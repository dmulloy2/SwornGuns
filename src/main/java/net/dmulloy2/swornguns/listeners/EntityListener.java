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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

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
	private final Map<UUID, Integer> restoreNoDamageTicks = new ConcurrentHashMap<>();

	public EntityListener(SwornGuns plugin)
	{
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onProjectileHit(ProjectileHitEvent event)
	{
		Projectile proj = event.getEntity();
		Bullet bullet = plugin.getBullet(proj);
		if (bullet == null)
		{
			return;
		}

		GunPlayer shooter = bullet.shooter();
		Player player = shooter.player();

		Location loc = proj.getLocation();
		Damageable hitEntity = resolveHitEntity(event, bullet, player);
		if (Config.debugProjectileHits)
		{
			String hitType = hitEntity != null ? hitEntity.getType().name() : "none";
			plugin.getLogger().info("[SwornGuns] Projectile hit resolved to entity=" + hitType + " at " + formatLocation(loc));
		}

		bullet.onHit();
		bullet.destroyNextTick(true);

		if (hitEntity != null)
		{
			return;
		}

		// ---- Effects
		Block block = event.getHitBlock();
		if (block == null)
		{
			block = loc.getBlock();
		}

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
			BlockBreakEvent blockBreak = new BlockBreakEvent(block, bullet.shooter().player());
			plugin.getServer().getPluginManager().callEvent(blockBreak);
			if (! blockBreak.isCancelled())
				block.setType(Material.COBBLESTONE);
		}

		// Block shattering
		if (Config.blockShatterEnabled && Config.blockShatterBlocks.contains(mat.asBlockType()))
		{
			BlockBreakEvent blockBreak = new BlockBreakEvent(block, bullet.shooter().player());
			plugin.getServer().getPluginManager().callEvent(blockBreak);
			if (! blockBreak.isCancelled())
				block.breakNaturally();
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event)
	{
		if (event.getCause() == DamageCause.PROJECTILE)
		{
			handleProjectileDamage(event);
		}
		else if (event.getCause() == DamageCause.ENTITY_ATTACK)
		{
			handleMeleeDamage(event);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEntityDamageByEntityMonitor(EntityDamageByEntityEvent event)
	{
		if (event.getCause() != DamageCause.PROJECTILE)
		{
			return;
		}

		handleProjectileDamageMonitor(event);
	}

	private void handleMeleeDamage(EntityDamageByEntityEvent event)
	{
		if (!(event.getDamager() instanceof Player player))
		{
			return;
		}

		GunPlayer gp = plugin.getGunPlayer(player);
		if (gp == null)
		{
			return;
		}

		ItemStack item = player.getInventory().getItemInMainHand();
		if (item.getType() == Material.AIR)
		{
			return;
		}

		gp.handleClick(item, true);
	}

	private void handleProjectileDamage(EntityDamageByEntityEvent event)
	{
		if (!(event.getEntity() instanceof Damageable hurt))
		{
			return;
		}

		if (!(event.getDamager() instanceof Projectile proj))
		{
			return;
		}

		// Make sure the projectile is one of our bullets
		Bullet bullet = plugin.getBullet(proj);
		if (bullet == null)
		{
			return;
		}

		boolean isHeadshot = hurt instanceof LivingEntity lentity && bullet.isHeadshot(lentity);

		Gun gun = bullet.shotFrom();
		double damage = gun.calculateDamage(isHeadshot);
		event.setDamage(damage);

		if (Config.projectileOnlyNoHitDelay && hurt instanceof LivingEntity living)
		{
			applyProjectileNoHitDelay(living);
		}

		if (! Config.hitmarkerOnlyOnRealDamage && event.getFinalDamage() > 0.0D)
		{
			playProjectileHitEffects(bullet, hurt);
		}
	}

	private void applyProjectileNoHitDelay(LivingEntity living)
	{
		int hitDelayTicks = Math.max(0, Config.projectileNoHitDelayTicks);
		UUID uuid = living.getUniqueId();

		// Apply a projectile-only hit delay override before damage is finalized.
		// This prevents vanilla invulnerability from throttling automatic guns/shotgun pellets.
		restoreNoDamageTicks.computeIfAbsent(uuid, key -> {
			int originalMax = living.getMaximumNoDamageTicks();
			Bukkit.getScheduler().runTask(plugin, () -> restoreNoDamageTicks(living.getUniqueId()));
			return originalMax;
		});

		living.setMaximumNoDamageTicks(hitDelayTicks);
		living.setNoDamageTicks(hitDelayTicks);
		living.setLastDamage(0.0D);
	}

	private void restoreNoDamageTicks(UUID entityId)
	{
		Integer originalMax = restoreNoDamageTicks.remove(entityId);
		if (originalMax == null)
		{
			return;
		}

		Entity entity = Bukkit.getEntity(entityId);
		if (entity instanceof LivingEntity living && living.isValid())
		{
			living.setMaximumNoDamageTicks(originalMax);
		}
	}

	private void handleProjectileDamageMonitor(EntityDamageByEntityEvent event)
	{
		if (!(event.getEntity() instanceof Damageable hurt))
		{
			return;
		}

		if (!(event.getDamager() instanceof Projectile proj))
		{
			return;
		}

		Bullet bullet = plugin.getBullet(proj);
		if (bullet == null || event.getFinalDamage() <= 0.0D)
		{
			return;
		}

		if (Config.hitmarkerOnlyOnRealDamage)
		{
			playProjectileHitEffects(bullet, hurt);
		}

		Gun gun = bullet.shotFrom();
		gun.armorPenetration(hurt, event.getFinalDamage());
		gun.doKnockback(hurt, bullet.velocity().clone());
	}

	private void playProjectileHitEffects(Bullet bullet, Damageable hurt)
	{
		World world = hurt.getWorld();

		if (Config.bloodEffectEnabled && Config.bloodEffectGunsOnly && Config.bloodEffectType != null)
		{
			world.playEffect(hurt.getLocation(), Effect.STEP_SOUND, Config.bloodEffectType);
			world.playEffect(hurt.getLocation().add(0, 1, 0), Effect.STEP_SOUND, Config.bloodEffectType);
		}

		if (Config.smokeEffect)
		{
			world.playEffect(bullet.shooter().player().getLocation(), Effect.SMOKE, 5);
		}

		if (Config.bulletSoundEnabled && Config.bulletSound != null)
		{
			world.playSound(hurt.getLocation(), Config.bulletSound, 10, 1);
		}
	}

	private Damageable resolveHitEntity(ProjectileHitEvent event, Bullet bullet, Player shooter)
	{
		if (event.getHitEntity() instanceof Damageable hit && isValidProjectileTarget(hit, shooter, event.getEntity()))
		{
			return hit;
		}

		Location start = bullet.previousLocation();
		if (start == null)
		{
			start = bullet.lastLocation();
		}

		Location end = event.getEntity().getLocation();
		if (start == null || start.getWorld() == null || ! start.getWorld().equals(end.getWorld()))
		{
			return null;
		}

		Vector path = end.toVector().subtract(start.toVector());
		double distance = path.length();
		if (distance <= 0.0D)
		{
			return null;
		}

		RayTraceResult result = start.getWorld().rayTrace(
			start,
			path.normalize(),
			distance,
			FluidCollisionMode.NEVER,
			true,
			0.2D,
			entity -> isValidProjectileTarget(entity, shooter, event.getEntity())
		);

		if (result != null && result.getHitEntity() instanceof Damageable hit)
		{
			return hit;
		}

		return null;
	}

	private boolean isValidProjectileTarget(Entity entity, Player shooter, Projectile projectile)
	{
		return entity instanceof Damageable
			&& entity.isValid()
			&& ! entity.equals(shooter)
			&& ! entity.equals(projectile);
	}

	private String formatLocation(Location location)
	{
		return String.format("%s %.2f %.2f %.2f",
			location.getWorld() != null ? location.getWorld().getName() : "unknown",
			location.getX(),
			location.getY(),
			location.getZ());
	}

	// Realism start - blood effects
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent event)
	{
		if (event.getDamage() <= 0.0D)
		{
			return;
		}

		if (! Config.bloodEffectEnabled || Config.bloodEffectGunsOnly || Config.bloodEffectType == null)
		{
			return;
		}

		Entity entity = event.getEntity();
		if (!(event.getEntity() instanceof LivingEntity))
		{
			return;
		}

		if (entity instanceof Player player && player.getGameMode() == GameMode.CREATIVE)
		{
			return;
		}

		if (event.getCause() == DamageCause.DROWNING || event.getCause() == DamageCause.LAVA)
		{
			return;
		}

		World world = entity.getWorld();
		BlockData data = Config.bloodEffectType.createBlockData();
		world.playEffect(entity.getLocation(), Effect.STEP_SOUND, data);
		world.playEffect(entity.getLocation().add(0, 1, 0), Effect.STEP_SOUND, data);
	}
	// Realism end
}
