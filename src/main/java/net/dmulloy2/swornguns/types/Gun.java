package net.dmulloy2.swornguns.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import lombok.Getter;
import lombok.Setter;
import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.util.FormatUtil;
import net.dmulloy2.swornguns.util.MaterialUtil;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * @author dmulloy2
 */

@Getter
@Setter
public class Gun
{
	private boolean canHeadshot;
	private boolean isThrowable;
	private boolean hasSmokeTrail;
	private boolean localGunSound;
	private boolean canAimLeft;
	private boolean canAimRight;
	private boolean canGoPastMaxDistance;
	private boolean needsPermission;
	private boolean canClickRight;
	private boolean canClickLeft;
	private boolean hasClip = true;
	private boolean ignoreItemData;
	private boolean reloadGunOnDrop = true;
	private boolean firing;
	private boolean reloading;
	private boolean changed;

	private byte gunByte;
	private byte ammoByte;

	private Material gunType;
	private Material ammoType;

	private int ammoAmtNeeded;
	private int gunDamage;
	private int explosionDamage = -1;
	private int roundsPerBurst;
	private int reloadTime;
	private int maxDistance;
	private int bulletsPerClick;
	private int bulletsShot;
	private int bulletDelay = 2;
	private int armorPenetration;
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

	private String node;
	private String reloadType = "NORMAL";

	private String gunName;
	private String fileName;
	private String projType = "";

	private SwornGuns plugin;

	private GunPlayer owner;

	private EffectType releaseEffect;

	private List<String> gunSound = new ArrayList<String>();

	public Gun(String name, SwornGuns plugin)
	{
		this.gunName = name;
		this.fileName = name;
		this.plugin = plugin;
	}

	/**
	 * Handles the actual shooting of the gun
	 */
	public final void shoot()
	{
		if (owner != null && owner.getPlayer().isOnline() && owner.getPlayer().getHealth() > 0.0D && ! reloading)
		{
			int ammoAmtNeeded = owner.getAmmoAmountNeeded(this);
			if ((owner.checkAmmo(this, ammoAmtNeeded) && ammoAmtNeeded > 0) || ammoAmtNeeded == 0)
			{
				owner.removeAmmo(this, ammoAmtNeeded);

				if (roundsFired >= maxClipSize && hasClip)
				{
					reloadGun();
					return;
				}

				doRecoil(owner.getPlayer());

				this.changed = true;
				this.roundsFired++;

				for (int i = 0; i < gunSound.size(); i++)
				{
					Sound sound = getSound(gunSound.get(i));
					if (sound != null)
					{
						if (localGunSound)
							owner.getPlayer().playSound(owner.getPlayer().getLocation(), sound, (float) gunVolume, 2.0F);
						else
							owner.getPlayer().getWorld().playSound(owner.getPlayer().getLocation(), sound, (float) gunVolume, 2.0F);
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

					Location ploc = owner.getPlayer().getLocation();

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

					Bullet bullet = new Bullet(plugin, owner, vec, this);
					plugin.addBullet(bullet);
				}

				if (roundsFired >= maxClipSize && hasClip)
					reloadGun();
			}
			else
			{
				owner.getPlayer().playSound(owner.getPlayer().getLocation(), Sound.ITEM_BREAK, 20.0F, 20.0F);
				owner.getPlayer().sendMessage(FormatUtil.format("&6This gun needs &c{0}", FormatUtil.getFriendlyName(ammoType)));

				finishShooting();
			}
		}
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
			{
				finishReloading();
			}

			this.reloading = false;
		}

		gunSounds();

		if (reloading)
		{
			// Update reload status
			owner.renameGuns();
		}

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
	public Gun copy()
	{
		Gun g = new Gun(gunName, plugin);

		g.gunName = this.gunName;
		g.gunType = this.gunType;
		g.gunByte = this.gunByte;
		g.ammoByte = this.ammoByte;
		g.ammoAmtNeeded = this.ammoAmtNeeded;
		g.ammoType = this.ammoType;
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
		g.canClickLeft = this.canClickLeft;
		g.canClickRight = this.canClickRight;
		g.hasSmokeTrail = this.hasSmokeTrail;
		g.armorPenetration = this.armorPenetration;
		g.isThrowable = this.isThrowable;
		g.ignoreItemData = this.ignoreItemData;
		g.projType = this.projType;
		g.needsPermission = this.needsPermission;
		g.node = this.node;
		g.gunSound = this.gunSound;
		g.bulletDelayTime = this.bulletDelayTime;
		g.hasClip = this.hasClip;
		g.maxClipSize = this.maxClipSize;
		g.reloadGunOnDrop = this.reloadGunOnDrop;
		g.localGunSound = this.localGunSound;
		g.fileName = this.fileName;
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
	}

	/**
	 * Plays various gun sounds
	 */
	private void gunSounds()
	{
		if (reloading)
		{
			int amtReload = reloadTime - gunReloadTimer;
			if (getReloadType().equalsIgnoreCase("bolt"))
			{
				if (amtReload == 6)
					owner.getPlayer().playSound(owner.getPlayer().getLocation(), Sound.DOOR_OPEN, 2.0F, 1.5F);
				if (amtReload == reloadTime - 4)
					owner.getPlayer().playSound(owner.getPlayer().getLocation(), Sound.DOOR_CLOSE, 1.0F, 1.5F);
			}
			else if (getReloadType().equalsIgnoreCase("pump") || getReloadType().equalsIgnoreCase("individual_bullet"))
			{
				int rep = (reloadTime - 10) / getMaxClipSize();
				if (amtReload >= 5 && amtReload <= reloadTime - 5 && amtReload % rep == 0)
				{
					owner.getPlayer().playSound(owner.getPlayer().getLocation(), Sound.NOTE_STICKS, 1.0F, 1.0F);
					owner.getPlayer().playSound(owner.getPlayer().getLocation(), Sound.NOTE_SNARE_DRUM, 1.0F, 2.0F);
				}

				if (amtReload == reloadTime - 3)
					owner.getPlayer().playSound(owner.getPlayer().getLocation(), Sound.PISTON_EXTEND, 1.0F, 2.0F);
				else if (amtReload == reloadTime - 1)
					owner.getPlayer().playSound(owner.getPlayer().getLocation(), Sound.PISTON_RETRACT, 1.0F, 2.0F);
			}
			else
			{
				if (amtReload == 6)
				{
					owner.getPlayer().playSound(owner.getPlayer().getLocation(), Sound.FIRE_IGNITE, 2.0F, 2.0F);
					owner.getPlayer().playSound(owner.getPlayer().getLocation(), Sound.DOOR_OPEN, 1.0F, 2.0F);
				}

				if (amtReload == reloadTime / 2)
					owner.getPlayer().playSound(owner.getPlayer().getLocation(), Sound.PISTON_RETRACT, 0.33F, 2.0F);

				if (amtReload == reloadTime - 4)
				{
					owner.getPlayer().playSound(owner.getPlayer().getLocation(), Sound.FIRE_IGNITE, 2.0F, 2.0F);
					owner.getPlayer().playSound(owner.getPlayer().getLocation(), Sound.DOOR_CLOSE, 1.0F, 2.0F);
				}
			}
		}
		else
		{
			if (getReloadType().equalsIgnoreCase("pump"))
			{
				if (timer == 8)
				{
					owner.getPlayer().playSound(owner.getPlayer().getLocation(), Sound.PISTON_EXTEND, 1.0F, 2.0F);
				}

				if (timer == 6)
				{
					owner.getPlayer().playSound(owner.getPlayer().getLocation(), Sound.PISTON_RETRACT, 1.0F, 2.0F);
				}
			}

			if (getReloadType().equalsIgnoreCase("bolt"))
			{
				if (timer == getBulletDelayTime() - 4)
					owner.getPlayer().playSound(owner.getPlayer().getLocation(), Sound.DOOR_OPEN, 2.0F, 1.25F);
				if (timer == 6)
					owner.getPlayer().playSound(owner.getPlayer().getLocation(), Sound.DOOR_CLOSE, 1.0F, 1.25F);
			}
		}
	}

	/**
	 * Handles recoil for a player
	 * 
	 * @param player - {@link Player} to handle recoil for
	 */
	private void doRecoil(Player player)
	{
		if (recoil != 0.0D)
		{
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
	}

	/**
	 * Does knockback for an entity
	 * 
	 * @param entity - {@link LivingEntity} to do knock back for
	 * @param speed - Knockback speed
	 */
	public void doKnockback(LivingEntity entity, Vector speed)
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

	/**
	 * @return Ammo material
	 */
	public Material getAmmoMaterial()
	{
		return ammoType;
	}

	/**
	 * @return Gun material
	 */
	public Material getGunMaterial()
	{
		return gunType;
	}

	/**
	 * Sets the name of the gun
	 * 
	 * @param val - Name of the gun
	 */
	public void setName(String val)
	{
		this.gunName = FormatUtil.format(val);
	}

	public String getValueFromString(String str)
	{
		if (str.contains(":"))
		{
			return str.substring(0, str.indexOf(":"));
		}

		return str;
	}

	public byte getByteDataFromString(String str)
	{
		if (str.contains(":"))
		{
			String news = str.substring(str.indexOf(":") + 1, str.length());
			return Byte.parseByte(news);
		}

		return -1;
	}

	/**
	 * Sets the gun's type
	 * 
	 * @param val - The gun's type
	 */
	public void setGunType(String val)
	{
		this.gunType = MaterialUtil.getMaterial(getValueFromString(val));

		this.gunByte = getByteDataFromString(val);

		if (gunByte < 0)
		{
			this.ignoreItemData = true;
			this.gunByte = 0;
		}
	}

	/**
	 * Sets the gun's ammo type
	 * 
	 * @param val - The gun's ammo type
	 */
	public void setAmmoType(String val)
	{
		this.ammoType = MaterialUtil.getMaterial(getValueFromString(val));
		this.ammoByte = getByteDataFromString(val);

		if (ammoByte < 0)
		{
			this.ammoByte = 0;
		}
	}

	/**
	 * Adds gun sounds from a composite string
	 * 
	 * @param val - Composite string of sounds
	 */
	public void addGunSounds(String val)
	{
		gunSound.addAll(Arrays.asList(val.split(",")));
	}

	public Sound getSound(String s)
	{
		try
		{
			s = s.replaceAll(" ", "_");
			s = s.toUpperCase();
			return Sound.valueOf(s);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	@Override
	public String toString()
	{
		return "Gun { name = " + gunName + ", type = " + gunType + ", priority = " + priority + " }";
	}
}