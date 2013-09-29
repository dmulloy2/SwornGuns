package net.dmulloy2.swornguns.api;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;

import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.types.Bullet;
import net.dmulloy2.swornguns.types.EffectType;
import net.dmulloy2.swornguns.types.Gun;
import net.dmulloy2.swornguns.types.GunPlayer;

/**
 * API Implementation for {@link SwornGuns}
 * 
 * @author dmulloy2
 */

public abstract interface SwornGunsAPI
{
	public abstract Permission getPermission(Gun gun);
	
	public abstract GunPlayer getGunPlayer(Player player);
	
	public abstract Gun getGun(Material material);
	
	public abstract Gun getGun(String gunName);
	
	public abstract void removeBullet(Bullet bullet);
	
	public abstract void addBullet(Bullet bullet);
	
	public abstract Bullet getBullet(Entity proj);
	
	public abstract List<Gun> getGunsByType(ItemStack item);
	
	public abstract void removeEffect(EffectType effectType);
	
	public abstract void addEffect(EffectType effectType);
}