package net.dmulloy2.swornguns.listeners;

import java.util.ArrayList;
import java.util.List;

import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.api.events.SwornGunsBulletCollideEvent;
import net.dmulloy2.swornguns.api.events.SwornGunsDamageEntityEvent;
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
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.Faction;

/**
 * @author dmulloy2
 */

public class RealismListener implements Listener
{
	private final List<Material> shatterBlocks;
	
	private final SwornGuns plugin;
	public RealismListener(SwornGuns plugin)
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
	public void onBulletCollideWithBlock(SwornGunsBulletCollideEvent event)
	{
		if (event.isCancelled())
			return;

		Block block = event.getBlockHit();
		if (checkFactions(block.getLocation(), true))
			return;
		
		Material mat = block.getState().getType();
		
		BlockBreakEvent blockBreak = new BlockBreakEvent(block, event.getShooterAsPlayer());
		plugin.getServer().getPluginManager().callEvent(blockBreak);
		if (! blockBreak.isCancelled())
		{
			if (plugin.getConfig().getBoolean("block-crack"))
			{
				if (mat == Material.STONE)
				{
					block.setType(Material.COBBLESTONE);
					return;
				}

				if (mat == Material.SMOOTH_BRICK)
				{
//					((SmoothBrick) block.getState().getData()).setTexture(Texture.CRACKED);
//					return;
				}
			}
			
			if (shatterBlocks.contains(mat))
			{
				block.breakNaturally();
			}
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onGunDamageEntity(SwornGunsDamageEntityEvent event)
	{
		if (event.isCancelled())
			return;
		
		Entity entity = event.getEntityDamaged();
		if (entity == null)
			return;
		
		EntityDamageByEntityEvent damageEvent = new EntityDamageByEntityEvent(event.getShooter().getPlayer(), entity, DamageCause.ENTITY_ATTACK, (double) event.getDamage());
		plugin.getServer().getPluginManager().callEvent(damageEvent);
		if (!damageEvent.isCancelled())
		{
			World world = entity.getWorld();
			
			if (plugin.getConfig().getBoolean("blood-effect.enabled"))
			{
				int blockId = plugin.getConfig().getInt("blood-effect.block-id");
				world.playEffect(entity.getLocation(), Effect.STEP_SOUND, blockId);
				world.playEffect(entity.getLocation().add(0, 1, 0), Effect.STEP_SOUND, blockId);
			}
	
			if (plugin.getConfig().getBoolean("smoke-effect"))
			{
				world.playEffect(event.getShooter().getPlayer().getLocation(), Effect.SMOKE, 5);
			}

			if (plugin.getConfig().getBoolean("bullet-sound.enabled"))
			{
				Sound sound = Sound.valueOf(plugin.getConfig().getString("bullet-sound.sound").toUpperCase());
				world.playSound(entity.getLocation(), sound, 10, 1);
			}
		}
	}

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
				int blockId = plugin.getConfig().getInt("blood-effect.block-id");
				world.playEffect(entity.getLocation(), Effect.STEP_SOUND, blockId);
				world.playEffect(entity.getLocation().add(0, 1, 0), Effect.STEP_SOUND, blockId);
			}
		}
	}
	
	public boolean checkFactions(Location loc, boolean safeZoneCheck)
	{
		PluginManager pm = plugin.getServer().getPluginManager();
		if (pm.isPluginEnabled("Factions"))
		{
			Plugin pl = pm.getPlugin("Factions");
			String version = pl.getDescription().getVersion();
			if (version.startsWith("1.6."))
			{
				Faction otherFaction = Board.getFactionAt(new FLocation(loc));
				if (safeZoneCheck == true)
				{
					if (otherFaction.isWarZone() || otherFaction.isSafeZone())
						return true;
				}
				else
				{
					if (otherFaction.isWarZone())
						return true;
				}
			}
		}
		if (pm.isPluginEnabled("SwornNations"))
		{
			Faction otherFaction = Board.getFactionAt(new FLocation(loc));
			if (safeZoneCheck == true)
			{
				if (otherFaction.isWarZone() || otherFaction.isSafeZone())
					return true;
			}
			else
			{
				if (otherFaction.isWarZone())
					return true;
			}
		}
		return false;
	}
}