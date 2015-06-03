package net.dmulloy2.swornguns.listeners;

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
		Projectile entity = event.getEntity();
		Bullet bullet = plugin.getBullet(entity);
		if (bullet != null)
		{
			// Attempt to determine which entity we actually hit
			// The damage events don't always fire properly

			Entity hit = null;
			double hitDistance = -1.0D;

			Location loc = entity.getLocation();
			Location nLoc = new Location(null, 0, 0, 0);

			List<Entity> nearbyEntities = entity.getNearbyEntities(2.0D, 2.0D, 2.0D);
			for (Entity nearby : nearbyEntities)
			{
				if (nearby instanceof Damageable)
				{
					nearby.getLocation(nLoc);

					// Exact match
					if (Util.checkLocation(loc, nLoc))
					{
						hit = nearby;
						break;
					}

					// Find closest entity
					double distance = nLoc.distance(loc);
					if (hit == null || distance < hitDistance)
					{
						hit = nearby;
						hitDistance = distance;
					}
				}
			}

			bullet.onHit(hit);
			bullet.setDestroyNextTick(true);

			// ---- Effects
			Block block = loc.getBlock();
			Material mat = block.getType();

			// Try to get a non-air block
			double i = 0.2D;
			while (mat == Material.AIR && i < 4.0D)
			{
				block = block.getLocation().add(entity.getVelocity().normalize().multiply(i)).getBlock();
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
		// Ensure all modifiers are real numbers
		for (DamageModifier type : DamageModifier.values())
		{
			if (event.isApplicable(type) && Double.isNaN(event.getDamage(type)))
				event.setDamage(type, 0.0D);
		}

		if (event.getEntity() instanceof Damageable)
		{
			Damageable hurt = (Damageable) event.getEntity();

			// Fix NaN health
			double health = hurt.getHealth();
			if (Double.isNaN(health))
			{
				hurt.setHealth(0.0D);
				hurt.resetMaxHealth();
			}

			if (event.getDamager() instanceof Projectile)
			{
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

					Gun shotFrom = bullet.getShotFrom();
					double damage = shotFrom.getGunDamage();

					// Headshot
					double mult = Math.max(shotFrom.getHits(), 1);

					if (shotFrom.isCanHeadshot() && hurt instanceof LivingEntity)
					{
						LivingEntity lentity = (LivingEntity) hurt;
						if (isNear(proj.getLocation(), lentity.getEyeLocation(), 0.26D))
							mult = mult * 2.0D;
					}

					shotFrom.setHits(0);
					shotFrom.setLastHit(-1);

					damage = Math.min(20, damage * mult);
					hurt.damage(damage, shotFrom.getOwner().getPlayer());

					// Armor penetration
					double armorPenetration = shotFrom.getArmorPenetration();
					if (armorPenetration > 0.0D && hurt.getHealth() > 0.0D)
					{
						health = hurt.getHealth();
						double newHealth = health - armorPenetration;
						newHealth = Math.max(0, Math.min(newHealth, hurt.getMaxHealth()));
						hurt.setHealth(newHealth);
					}

					shotFrom.doKnockback(hurt, bullet.getVelocity());
					event.setCancelled(true);
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