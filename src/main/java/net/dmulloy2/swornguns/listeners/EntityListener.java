package net.dmulloy2.swornguns.listeners;

import java.util.ArrayList;
import java.util.List;

import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.types.Bullet;
import net.dmulloy2.swornguns.types.Gun;
import net.dmulloy2.types.Reloadable;
import net.dmulloy2.util.MaterialUtil;

import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
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
		Projectile check = event.getEntity();
		Bullet bullet = plugin.getBullet(check);
		if (bullet != null)
		{
			bullet.onHit();
			bullet.setDestroyNextTick(true);
			Projectile p = event.getEntity();
			Block b = p.getLocation().getBlock();
			Material mat = b.getType();

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
				p.getLocation().getWorld().playEffect(b.getLocation(), Effect.STEP_SOUND, mat);
			}

			// Realism start - block cracking
			boolean applicable = false;

			if (! plugin.getFactionsHandler().checkFactions(b.getLocation(), true))
			{
				if (blockCrack)
				{
					if (mat == Material.STONE)
						applicable = true;
				}

				if (blockShatter)
				{
					if (shatterBlocks.contains(mat))
						applicable = true;
				}
			}

			// UltimateArena - check for inside arena
			if (plugin.getUltimateArenaHandler().isEnabled())
			{
				if (plugin.getUltimateArenaHandler().isInArena(bullet.getShooter().getPlayer()))
					applicable = false;
			}

			if (applicable)
			{
				BlockBreakEvent blockBreak = new BlockBreakEvent(b, bullet.getShooter().getPlayer());
				plugin.getServer().getPluginManager().callEvent(blockBreak);
				if (! blockBreak.isCancelled())
				{
					if (blockCrack)
					{
						if (mat == Material.STONE)
						{
							b.setType(Material.COBBLESTONE);
						}
					}

					if (blockShatter)
					{
						if (shatterBlocks.contains(mat))
						{
							b.breakNaturally();
						}
					}
				}
			}
			// Realism end
			check.remove();
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event)
	{
		if (event.getEntity() instanceof LivingEntity)
		{
			LivingEntity hurt = (LivingEntity) event.getEntity();
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
					double mult = 1.0D;
					if (isNear(proj.getLocation(), hurt.getEyeLocation(), 0.26D) && bullet.getShotFrom().isCanHeadshot())
						mult = 2.0D;

					// Prevent multiple deaths
					if (hurt.getHealth() <= 0.0D)
					{
						event.setCancelled(true);
						return;
					}

					// TODO: Take into account new DamageModifier API
					event.setDamage(damage * mult);

					// Armor penetration
					double armorPenetration = shotFrom.getArmorPenetration();
					if (armorPenetration > 0.0D && (hurt.getHealth() - event.getDamage()) > 0.0D)
					{
						double health = hurt.getHealth();
						double newHealth = health - armorPenetration;

						if (newHealth < 0)
							newHealth = 0;
						if (newHealth > 20)
							newHealth = 20;

						hurt.setHealth(newHealth);
					}

					bullet.getShotFrom().doKnockback(hurt, bullet.getVelocity());
					bullet.remove();
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

		this.shatterBlocks = new ArrayList<>();
		for (String configMaterial : plugin.getConfig().getStringList("block-shatter.blocks"))
		{
			Material material = MaterialUtil.getMaterial(configMaterial);
			if (material != null)
				shatterBlocks.add(material);
		}
	}
}