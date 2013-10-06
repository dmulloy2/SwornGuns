package net.dmulloy2.swornguns.listeners;

import java.util.ArrayList;
import java.util.List;

import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.types.Bullet;
import net.dmulloy2.swornguns.util.MaterialUtil;

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
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.Faction;

/**
 * @author dmulloy2
 */

public class EntityListener implements Listener
{
	private final List<Material> shatterBlocks;
	
	private final SwornGuns plugin;
	public EntityListener(SwornGuns plugin)
	{
		this.plugin = plugin;
		
		this.shatterBlocks = new ArrayList<Material>();
		
		List<String> configMaterials = plugin.getConfig().getStringList("block-shatter.blocks");
		for (String configMaterial : configMaterials)
		{
			Material material = MaterialUtil.getMaterial(configMaterial);
			if (material != null)
			{
				shatterBlocks.add(material);
			}
		}
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
			
//			SwornGunsBulletCollideEvent evv = new SwornGunsBulletCollideEvent(bullet.getShooter(), bullet.getShotFrom(), b);
//			plugin.getServer().getPluginManager().callEvent(evv);

			// Realism start - block cracking
			BlockBreakEvent blockBreak = new BlockBreakEvent(b, bullet.getShooter().getPlayer());
			plugin.getServer().getPluginManager().callEvent(blockBreak);
			if (! blockBreak.isCancelled() && ! checkFactions(b.getLocation(), true))
			{
				if (plugin.getConfig().getBoolean("block-crack"))
				{
					if (mat == Material.STONE)
					{
						b.setType(Material.COBBLESTONE);
						return;
					}

					if (mat == Material.SMOOTH_BRICK)
					{
//						((SmoothBrick) block.getState().getData()).setTexture(Texture.CRACKED);
//						return;
					}
				}
				
				if (shatterBlocks.contains(mat))
				{
					b.breakNaturally();
				}
			}
			// Realism end
		}

		event.getEntity().remove();
	}

//	@EventHandler(priority = EventPriority.HIGHEST)
//	public void onEntityDeath(EntityDeathEvent event)
//	{
//		Entity dead = event.getEntity();
//		if (dead.getLastDamageCause() != null)
//		{
//			EntityDamageEvent e = dead.getLastDamageCause();
//			if (e instanceof EntityDamageByEntityEvent)
//			{
//				EntityDamageByEntityEvent ede = (EntityDamageByEntityEvent) e;
//				Entity damager = ede.getDamager();
//				if (damager instanceof Projectile)
//				{
//					Projectile proj = (Projectile) damager;
//					Bullet bullet = plugin.getBullet(proj);
//					if (bullet != null)
//					{
//						Gun used = bullet.getShotFrom();
//						GunPlayer shooter = bullet.getShooter();
//
//						SwornGunsKillEntityEvent sworngunskill = new SwornGunsKillEntityEvent(shooter, used, dead);
//						plugin.getServer().getPluginManager().callEvent(sworngunskill);
//					}
//				}
//			}
//		}
//	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event)
	{
		if (event.isCancelled())
			return;
		
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

					if (plugin.getConfig().getBoolean("blood-effect.enabled"))
					{
						Material mat = MaterialUtil.getMaterial(plugin.getConfig().getString("blood-effect.block-id"));
						world.playEffect(hurt.getLocation(), Effect.STEP_SOUND, mat);
						world.playEffect(hurt.getLocation().add(0, 1, 0), Effect.STEP_SOUND, mat);
					}

					if (plugin.getConfig().getBoolean("smoke-effect"))
					{
						world.playEffect(bullet.getShooter().getPlayer().getLocation(), Effect.SMOKE, 5);
					}

					if (plugin.getConfig().getBoolean("bullet-sound.enabled"))
					{
						Sound sound = Sound.valueOf(plugin.getConfig().getString("bullet-sound.sound").toUpperCase());
						world.playSound(hurt.getLocation(), sound, 10, 1);
					}
					// Realism end

					double damage = bullet.getShotFrom().getGunDamage();
					double mult = 1.0D;
					if (isNear(proj.getLocation(), hurt.getEyeLocation(), 0.26D) && bullet.getShotFrom().isCanHeadshot())
						mult = 2.0D;

					if (! hurt.isDead() && hurt.getHealth() > 0)
					{
						hurt.setLastDamage(0);
						event.setDamage(Math.ceil(damage * mult));
						int armorPenetration = bullet.getShotFrom().getArmorPenetration();
						if (armorPenetration > 0 && hurt.getHealth() - event.getDamage() > 0)
						{
							int health = (int) hurt.getHealth();
							int newHealth = health - armorPenetration;
							if (newHealth < 0)
							{
								newHealth = 0;
							}
							if (newHealth > 20)
							{
								newHealth = 20;
							}

							hurt.setHealth(newHealth);
						}

						bullet.getShotFrom().doKnockback(hurt, bullet.getVelocity());

						bullet.remove();
					}
					else
					{
						event.setCancelled(true);
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
		if (event.isCancelled() || event.getDamage() <= 0)
			return;

		Entity entity = event.getEntity();
		if (! (entity instanceof LivingEntity))
			return;
		
		if (entity instanceof Player)
		{
			if (((Player)entity).getGameMode() == GameMode.CREATIVE)
				return;
		}

		World world = entity.getWorld();
		
		if (plugin.getConfig().getBoolean("blood-effect.enabled"))
		{
			if (plugin.getConfig().getBoolean("blood-effect.guns-only") == false)
			{
				Material mat = MaterialUtil.getMaterial(plugin.getConfig().getString("blood-effect.block-id"));
				world.playEffect(entity.getLocation(), Effect.STEP_SOUND, mat);
				world.playEffect(entity.getLocation().add(0, 1, 0), Effect.STEP_SOUND, mat);
			}
		}
	}
	// Realism end

	// Realism start - factions check
	public boolean checkFactions(Location location, boolean safeZoneCheck)
	{
		PluginManager pm = plugin.getServer().getPluginManager();
		if (pm.isPluginEnabled("Factions"))
		{
			Plugin pl = pm.getPlugin("Factions");
			String version = pl.getDescription().getVersion();
			if (version.startsWith("1.6."))
			{
				Faction otherFaction = Board.getFactionAt(new FLocation(location));
				return safeZoneCheck ? (otherFaction.isWarZone() || otherFaction.isSafeZone()) : otherFaction.isWarZone();
			}
		}
		
		if (pm.isPluginEnabled("SwornNations"))
		{
			Faction otherFaction = Board.getFactionAt(new FLocation(location));
			return safeZoneCheck ? (otherFaction.isWarZone() || otherFaction.isSafeZone()) : otherFaction.isWarZone();
		}
		
		return false;
	}
	// Realism end
}