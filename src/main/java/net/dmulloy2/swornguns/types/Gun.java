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
import java.util.logging.Level;

import lombok.Data;
import net.dmulloy2.swornapi.io.FileSerialization;
import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornapi.util.FormatUtil;
import net.dmulloy2.swornapi.util.MaterialUtil;
import net.dmulloy2.swornapi.util.NumberUtil;
import net.dmulloy2.swornapi.util.Util;
import net.dmulloy2.swornguns.events.SwornGunFireEvent;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * @author dmulloy2
 */

@Data
public class Gun implements Cloneable, ConfigurationSerializable
{
	private boolean canHeadshot;
	private boolean isThrowable;
	private boolean hasSmokeTrail;
	private boolean localGunSound;
	private boolean canAimLeft;
	private boolean canAimRight;
	private boolean canGoPastMaxDistance;
	private boolean needsPermission;
	private boolean canFireRight;
	private boolean canFireLeft;
	private boolean hasClip = true;
	private boolean reloadGunOnDrop = true;
	private boolean firing;
	private boolean reloading;
	private boolean changed;
	private boolean unlimitedAmmo;
	private boolean warnIfNoPermission;

	private Material material;
	private Material ammo;

	private int ammoAmtNeeded;
	private int roundsPerBurst;
	private int reloadTime;
	private int maxDistance;
	private int bulletsPerClick;
	private int bulletsShot;
	private int bulletDelay = 2;
	private int releaseTime = -1;
	private int maxClipSize = 30;
	private int bulletDelayTime = 10;
	private int roundsFired;
	private int gunReloadTimer;
	private int timer;
	private int lastFired;
	private int ticks;
	private int heldDownTicks;
	private int priority;
	private int clipRemaining = -1;
	private int clipSize;
	private int initialClip = -1;

	private double gunDamage;
	private double armorPenetration;
	private double explosionDamage = -1.0D;
	private double bulletSpeed;
	private double accuracy;
	private double accuracy_aimed = -1.0D;
	private double accuracy_crouched = -1.0D;
	private double explodeRadius;
	private double fireRadius;
	private double flashRadius;
	private double knockback;
	private double recoil;

	private double gunVolume = 1.0D;
	private double gunPitch = 2.0D;

	private String projType = "";
	private String explosionType = "FIREWORK";
	private ReloadType reloadType = ReloadType.NORMAL;

	private transient String gunName;
	private String displayName;
	private String outOfAmmoMessage = "";

	private EffectData releaseEffect;

	private transient List<Sound> gunSound = new ArrayList<>();
	private transient List<String> lore = new ArrayList<>();

	private transient GunPlayer owner;
	private transient final SwornGuns plugin;

	public Gun(String name, SwornGuns plugin)
	{
		this.gunName = name;
		this.plugin = plugin;
	}

	@SuppressWarnings("unchecked")
	public void loadFromConfig(Map<String, Object> config)
	{
		// deserialize all the primitive values
		FileSerialization.deserialize(this, config);

		List<String> gunSoundNames = (List<String>) config.get("gunSound");
		if (gunSoundNames == null)
		{
			return;
		}

		gunSound.clear();
		for (String name : gunSoundNames)
		{
			try
			{
				gunSound.add(Sound.valueOf(name.toUpperCase()));
			}
			catch (IllegalArgumentException ex)
			{
				plugin.getLogHandler().log(Level.WARNING, "Invalid sound \"{0}\" configured for gun {1}",
					name, gunName);
			}
		}
	}

	/**
	 * Handles the actual shooting of the gun
	 */
	public final void shoot()
	{
		if (reloading || owner == null)
			return;

		Player player = owner.getPlayer();
		if (!player.isOnline() || player.getHealth() <= 0.0D)
		{
			return;
		}

		if (reloadType == ReloadType.CLIP && clipRemaining == -1)
		{
			if (initialClip < 0 || initialClip > clipSize)
				clipRemaining = clipSize;
			else
				clipRemaining = initialClip;
		}

		SwornGunFireEvent event = new SwornGunFireEvent(this, getAmmoAmtNeeded());
		plugin.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled())
			return;

		int ammoAmtNeeded = event.getAmmoNeeded();
		if (ammoAmtNeeded != 0 && !owner.checkAmmo(this, ammoAmtNeeded) && clipRemaining <= 0)
		{
			player.playSound(owner.getPlayer().getLocation(), Sound.ENTITY_ITEM_BREAK, 20.0F, 20.0F);

			if (outOfAmmoMessage.isEmpty())
				player.sendMessage(plugin.getPrefix()
					+ FormatUtil.format("&eThis gun needs &b{0}&e!", MaterialUtil.getName(ammo)));
			else
				player.sendMessage(plugin.getPrefix()
					+ FormatUtil.format(outOfAmmoMessage, MaterialUtil.getName(ammo)));

			finishShooting();
			return;
		}

		if (reloadType == ReloadType.CLIP)
		{
			clipRemaining--;
			if (clipRemaining <= 0 && owner.checkAmmo(this, 1))
			{
				owner.removeAmmo(this, 1);
				reloadGun();
				return;
			}
		}
		else
		{
			owner.removeAmmo(this, ammoAmtNeeded);

			if (roundsFired >= maxClipSize && hasClip)
			{
				reloadGun();
				return;
			}
		}

		doRecoil(player);

		this.changed = true;
		this.roundsFired++;

		for (Sound sound : gunSound.toArray(new Sound[0]))
		{
			if (sound != null)
			{
				if (localGunSound)
					player.playSound(player.getLocation(), sound, (float) gunVolume, (float) gunPitch);
				else
					player.getWorld().playSound(player.getLocation(), sound, (float) gunVolume, (float) gunPitch);
			}
		}

		double accuracy = this.accuracy;
		if (owner.getPlayer().isSneaking() && accuracy_crouched > -1.0D)
		{
			accuracy = accuracy_crouched;
		}

		if (owner.isAimedIn() && accuracy_aimed > -1.0D)
		{
			accuracy = accuracy_aimed;
		}

		for (int i = 0; i < bulletsPerClick; i++)
		{
			int acc = (int) (accuracy * 1000.0D);

			if (acc <= 0)
				acc = 1;

			Location ploc = player.getLocation();

			Random rand = new Random();

			double dir = -ploc.getYaw() - 90.0F;
			double pitch = -ploc.getPitch();
			double xwep = (rand.nextInt(acc) - rand.nextInt(acc) + 0.5D) / 1000.0D;
			double ywep = (rand.nextInt(acc) - rand.nextInt(acc) + 0.5D) / 1000.0D;
			double zwep = (rand.nextInt(acc) - rand.nextInt(acc) + 0.5D) / 1000.0D;
			double xd = Math.cos(Math.toRadians(dir)) * Math.cos(Math.toRadians(pitch)) + xwep;
			double yd = Math.sin(Math.toRadians(pitch)) + ywep;
			double zd = -Math.sin(Math.toRadians(dir)) * Math.cos(Math.toRadians(pitch)) + zwep;

			Vector vec = new Vector(xd, yd, zd);
			vec.multiply(bulletSpeed);

			Bullet bullet = new Bullet(plugin, owner, this, vec);
			plugin.addBullet(bullet);
		}

		if (roundsFired >= maxClipSize && hasClip)
			reloadGun();
	}

	/**
	 * Makes this gun generic again
	 */
	public final void clear()
	{
		this.owner = null;
	}

	/**
	 * Handles bullet movement, cooldowns, etc
	 */
	public final void tick()
	{
		this.ticks++;
		this.lastFired++;
		this.timer--;
		this.gunReloadTimer--;

		if (gunReloadTimer < 0)
		{
			if (reloading)
				finishReloading();

			this.reloading = false;
		}

		gunSounds();

		if (lastFired > 6)
		{
			this.heldDownTicks = 0;
		}

		if ((heldDownTicks >= 2 && timer <= 0) || firing && ! reloading)
		{
			if (roundsPerBurst > 1)
			{
				if (ticks % bulletDelay == 0)
				{
					this.bulletsShot++;

					if (bulletsShot <= roundsPerBurst)
						shoot();
					else
						finishShooting();
				}
			}
			else
			{
				shoot();
				finishShooting();
			}
		}

		if (reloading)
			this.firing = false;
	}

	/**
	 * Returns an exact replica of this gun
	 *
	 * @return An exact replica of this gun
	 */
	private Gun copy()
	{
		Gun g = new Gun(gunName, plugin);

		g.gunName = this.gunName;
		g.material = this.material;
		g.ammo = this.ammo;
		g.ammoAmtNeeded = this.ammoAmtNeeded;
		g.roundsPerBurst = this.roundsPerBurst;
		g.bulletsPerClick = this.bulletsPerClick;
		g.bulletSpeed = this.bulletSpeed;
		g.accuracy = this.accuracy;
		g.accuracy_aimed = this.accuracy_aimed;
		g.accuracy_crouched = this.accuracy_crouched;
		g.maxDistance = this.maxDistance;
		g.gunVolume = this.gunVolume;
		g.gunDamage = this.gunDamage;
		g.explodeRadius = this.explodeRadius;
		g.fireRadius = this.fireRadius;
		g.flashRadius = this.flashRadius;
		g.canHeadshot = this.canHeadshot;
		g.reloadTime = this.reloadTime;
		g.canAimLeft = this.canAimLeft;
		g.canAimRight = this.canAimRight;
		g.canFireLeft = this.canFireLeft;
		g.canFireRight = this.canFireRight;
		g.hasSmokeTrail = this.hasSmokeTrail;
		g.armorPenetration = this.armorPenetration;
		g.isThrowable = this.isThrowable;
		g.projType = this.projType;
		g.needsPermission = this.needsPermission;
		g.gunSound = this.gunSound;
		g.bulletDelayTime = this.bulletDelayTime;
		g.hasClip = this.hasClip;
		g.maxClipSize = this.maxClipSize;
		g.reloadGunOnDrop = this.reloadGunOnDrop;
		g.localGunSound = this.localGunSound;
		g.explosionDamage = this.explosionDamage;
		g.recoil = this.recoil;
		g.knockback = this.knockback;
		g.reloadType = this.reloadType;
		g.releaseTime = this.releaseTime;
		g.canGoPastMaxDistance = this.canGoPastMaxDistance;
		g.priority = this.priority;

		if (releaseEffect != null)
		{
			g.releaseEffect = this.releaseEffect.clone();
		}

		return g;
	}

	/**
	 * Reloads the gun
	 */
	public void reloadGun()
	{
		this.reloading = true;
		this.gunReloadTimer = reloadTime;

		if (reloadType == ReloadType.CLIP)
			this.clipRemaining = clipSize;
	}

	/**
	 * Plays various gun sounds
	 */
	private void gunSounds()
	{
		try
		{
			if (reloading)
			{
				int amtReload = reloadTime - gunReloadTimer;
				if (reloadType == ReloadType.BOLT)
				{
					if (amtReload == 6)
						owner.getPlayer().playSound(owner.getPlayer().getLocation(), Sound.BLOCK_WOODEN_DOOR_OPEN, 2.0F, 1.5F);
					if (amtReload == reloadTime - 4)
						owner.getPlayer().playSound(owner.getPlayer().getLocation(), Sound.BLOCK_WOODEN_DOOR_CLOSE, 1.0F, 1.5F);
				}
				else if (reloadType == ReloadType.PUMP || reloadType == ReloadType.INDIVIDUAL_BULLET)
				{
					int rep = (reloadTime - 10) / getMaxClipSize();
					if (amtReload >= 5 && amtReload <= reloadTime - 5 && amtReload % rep == 0)
					{
						owner.getPlayer().playSound(owner.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F);
						owner.getPlayer().playSound(owner.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_SNARE, 1.0F, 2.0F);
					}

					if (amtReload == reloadTime - 3)
						owner.getPlayer().playSound(owner.getPlayer().getLocation(), Sound.BLOCK_PISTON_EXTEND, 1.0F, 2.0F);
					else if (amtReload == reloadTime - 1)
						owner.getPlayer().playSound(owner.getPlayer().getLocation(), Sound.BLOCK_PISTON_CONTRACT, 1.0F, 2.0F);
				}
				else
				{
					if (amtReload == 6)
					{
						owner.getPlayer().playSound(owner.getPlayer().getLocation(), Sound.BLOCK_FIRE_AMBIENT, 2.0F, 2.0F);
						owner.getPlayer().playSound(owner.getPlayer().getLocation(), Sound.BLOCK_WOODEN_DOOR_OPEN, 1.0F, 2.0F);
					}

					if (amtReload == reloadTime / 2)
						owner.getPlayer().playSound(owner.getPlayer().getLocation(), Sound.BLOCK_PISTON_CONTRACT, 0.33F, 2.0F);

					if (amtReload == reloadTime - 4)
					{
						owner.getPlayer().playSound(owner.getPlayer().getLocation(), Sound.BLOCK_FIRE_AMBIENT, 2.0F, 2.0F);
						owner.getPlayer().playSound(owner.getPlayer().getLocation(), Sound.BLOCK_WOODEN_DOOR_CLOSE, 1.0F, 2.0F);
					}
				}
			}
			else
			{
				if (reloadType == ReloadType.PUMP)
				{
					if (timer == 8)
					{
						owner.getPlayer().playSound(owner.getPlayer().getLocation(), Sound.BLOCK_PISTON_EXTEND, 1.0F, 2.0F);
					}

					if (timer == 6)
					{
						owner.getPlayer().playSound(owner.getPlayer().getLocation(), Sound.BLOCK_PISTON_CONTRACT, 1.0F, 2.0F);
					}
				}

				if (reloadType == ReloadType.BOLT)
				{
					if (timer == getBulletDelayTime() - 4)
						owner.getPlayer().playSound(owner.getPlayer().getLocation(), Sound.BLOCK_WOODEN_DOOR_OPEN, 2.0F, 1.25F);
					if (timer == 6)
						owner.getPlayer().playSound(owner.getPlayer().getLocation(), Sound.BLOCK_WOODEN_DOOR_CLOSE, 1.0F, 1.25F);
				}
			}
		}
		catch (Throwable ex)
		{
			plugin.getLogHandler().debug(Level.WARNING, Util.getUsefulStack(ex, "playing gun sounds"));
		}
	}

	/**
	 * Handles recoil for a player
	 *
	 * @param player {@link Player} to handle recoil for
	 */
	private void doRecoil(Player player)
	{
		if (recoil == 0.0D)
		{
			return;
		}

		Location ploc = player.getLocation();
		double dir = -ploc.getYaw() - 90.0F;
		double pitch = -ploc.getPitch() - 180.0F;
		double xd = Math.cos(Math.toRadians(dir)) * Math.cos(Math.toRadians(pitch));
		double yd = Math.sin(Math.toRadians(pitch));
		double zd = -Math.sin(Math.toRadians(dir)) * Math.cos(Math.toRadians(pitch));

		Vector vec = new Vector(xd, yd, zd);
		vec.multiply(recoil / 2.0D).setY(0);

		player.setVelocity(player.getVelocity().add(vec));
	}

	/**
	 * Does knockback for an entity
	 *
	 * @param entity {@link Entity} to do knock back for
	 * @param speed Knockback speed
	 */
	public void doKnockback(Entity entity, Vector speed)
	{
		if (knockback > 0.0D)
		{
			speed.normalize().setY(0.6D).multiply(knockback / 4.0D);
			entity.setVelocity(speed);
		}
	}

	/**
	 * Finishes reloading
	 */
	public void finishReloading()
	{
		this.bulletsShot = 0;
		this.roundsFired = 0;
		this.changed = false;
		this.gunReloadTimer = 0;
	}

	/**
	 * Finishes shooting
	 */
	private void finishShooting()
	{
		this.bulletsShot = 0;
		this.timer = bulletDelayTime;
		this.firing = false;
	}

	/**
	 * @return Name of the gun
	 */
	public String getName()
	{
		return gunName;
	}

	public String getValueFromString(String str)
	{
		if (str.contains(":"))
		{
			return str.substring(0, str.indexOf(":"));
		}

		return str;
	}

	/**
	 * Sets the gun's type
	 *
	 * @param val The gun's type
	 */
	public void setGunType(String val)
	{
		Material material = Material.matchMaterial(val);
		if (material == null)
		{
			throw new IllegalArgumentException("Invalid gun material: " + val);
		}

		this.material = material;
	}

	/**
	 * Sets the gun's ammo type
	 *
	 * @param val The gun's ammo type
	 */
	public void setAmmoType(String val)
	{
		Material material = Material.matchMaterial(val);
		if (material == null)
		{
			throw new IllegalArgumentException("Invalid ammo material: " + val);
		}

		this.ammo = material;
	}

	/**
	 * Sets this gun's lore
	 *
	 * @param val Composite lore string
	 */
	public void setLore(String val)
	{
		lore.clear();

		for (String s : val.split(";"))
		{
			lore.add(FormatUtil.format(s));
		}
	}

	/**
	 * Adds gun sounds from a composite string
	 *
	 * @param val Composite string of sounds
	 */
	public void addGunSounds(String val)
	{
		for (String name : val.split(","))
		{
			Sound sound = SwornGuns.getSound(name);
			if (sound != null)
				gunSound.add(sound);
		}
	}

	@Override
	public String toString()
	{
		return "Gun[name=" + gunName + ", material=" + material + ", priority=" + priority + "]";
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null) return false;
		if (obj == this) return true;

		if (obj instanceof Gun that)
		{
			return this.gunName.equals(that.gunName) && this.material.equals(that.material) && this.priority == that.priority;
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(gunName, material, priority);
	}

	@Override
	public Gun clone()
	{
		try
		{
			Gun clone = (Gun) super.clone();
			clone.clear();
			return clone;
		} catch (Throwable ignored) { }
		return copy();
	}

	@Override
	public Map<String, Object> serialize()
	{
		return FileSerialization.serialize(this);
	}
}
