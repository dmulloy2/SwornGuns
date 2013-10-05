package net.dmulloy2.swornguns.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import lombok.Data;
import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.util.FormatUtil;
import net.dmulloy2.swornguns.util.MaterialUtil;
import net.dmulloy2.swornguns.util.Util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * @author dmulloy2
 */

@Data
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
	private String outOfAmmoMessage = "";
	private String permissionMessage = "";

	private SwornGuns plugin;

	private GunPlayer owner;

	private EffectType releaseEffect;

	private List<String> gunSound = new ArrayList<String>();

	public Gun(final String name, final SwornGuns plugin)
	{
		this.gunName = name;
		this.fileName = name;
		this.outOfAmmoMessage = "Out of ammo!";
		this.plugin = plugin;
	}

	public void shoot()
	{
		if (owner != null && owner.getPlayer().isOnline() && ! owner.getPlayer().isDead() && ! reloading)
		{
			int ammoAmtNeeded = owner.getAmmoAmountNeeded(this);
			if ((owner.checkAmmo(this, ammoAmtNeeded) && ammoAmtNeeded > 0) || ammoAmtNeeded == 0)
			{
				owner.removeAmmo(this, ammoAmtNeeded);

				if (needsReload())
				{
					reloadGun();
					return;
				}

				doRecoil(owner.getPlayer());

				this.changed = true;
				this.roundsFired++;

				for (int i = 0; i < gunSound.size(); i++)
				{
					Sound sound = Util.getSound(gunSound.get(i));
					if (sound != null)
					{
						if (localGunSound)
							owner.getPlayer().playSound(owner.getPlayer().getLocation(), sound, (float) gunVolume, 2.0F);
						else
							owner.getPlayer().getWorld().playSound(owner.getPlayer().getLocation(), sound, (float) gunVolume, 2.0F);
					}
				}

				double accuracy = this.accuracy;
				if ((owner.getPlayer().isSneaking()) && (accuracy_crouched > -1.0D))
				{
					accuracy = accuracy_crouched;
				}
				if ((owner.isAimedIn()) && (accuracy_aimed > -1.0D))
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

				attemptReload();
			}
			else
			{
				owner.getPlayer().playSound(owner.getPlayer().getLocation(), Sound.ITEM_BREAK, 20.0F, 20.0F);
				owner.getPlayer().sendMessage(outOfAmmoMessage);

				finishShooting();
			}
		}
	}

	public void clear()
	{
//		try
//		{
//			finalize();
//		}
//		catch (Throwable e)
//		{
//			//
//		}
	}

	public boolean needsReload()
	{
		return ((roundsFired >= maxClipSize) && (hasClip));
	}

	public void attemptReload()
	{
		if (needsReload())
			reloadGun();
	}

	public void tick()
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

		if (lastFired > 6)
		{
			this.heldDownTicks = 0;
		}

		if ((heldDownTicks >= 2 && timer <= 0) || firing && !reloading)
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
		g.setIgnoreItemData(this.ignoreItemData);
		g.outOfAmmoMessage = this.outOfAmmoMessage;
		g.setProjType(this.projType);
		g.setNeedsPermission(this.needsPermission);
		g.node = this.node;
		g.gunSound = this.gunSound;
		g.setBulletDelayTime(this.bulletDelayTime);
		g.setHasClip(this.hasClip);
		g.setMaxClipSize(this.maxClipSize);
		g.setReloadGunOnDrop(this.reloadGunOnDrop);
		g.localGunSound = this.localGunSound;
		g.fileName = this.fileName;
		g.explosionDamage = this.explosionDamage;
		g.recoil = this.recoil;
		g.knockback = this.knockback;
		g.setReloadType(this.reloadType);
		g.releaseTime = this.releaseTime;
		g.canGoPastMaxDistance = this.canGoPastMaxDistance;
		g.permissionMessage = this.permissionMessage;
		g.priority = this.priority;

		if (getReleaseEffect() != null)
		{
			g.setReleaseEffect(this.getReleaseEffect().clone());
		}

		return g;
	}

	public void reloadGun()
	{
		this.reloading = true;
		this.gunReloadTimer = reloadTime;
	}

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

	public void doKnockback(LivingEntity entity, Vector speed)
	{
		if (knockback > 0.0D)
		{
			speed.normalize().setY(0.6D).multiply(knockback / 4.0D);
			entity.setVelocity(speed);
		}
	}

	public void finishReloading()
	{
		this.bulletsShot = 0;
		this.roundsFired = 0;
		this.changed = false;
		this.gunReloadTimer = 0;
	}

	private void finishShooting()
	{
		this.bulletsShot = 0;
		this.timer = getBulletDelayTime();
		this.firing = false;
	}

	public String getName()
	{
		return gunName;
	}

	public Material getAmmoMaterial()
	{
		// return Material.getMaterial(ammoType);
		return ammoType;
	}

	public Material getGunMaterial()
	{
		// Material mat = Material.getMaterial(getGunType());
		// if (mat == null)
		// plugin.getLogHandler().log(Level.WARNING,
		// "Null material in gun \"{0}\". Type ID: {1}", gunName, getGunType());
		//
		return gunType;
	}

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

	public void setGunType(String val)
	{
		this.gunType = MaterialUtil.getMaterial(getValueFromString(val));

		this.gunByte = getByteDataFromString(val);

		if (gunByte == -1)
		{
			this.setIgnoreItemData(true);
			this.gunByte = 0;
		}
	}

	public void setAmmoType(String val)
	{
		this.ammoType = MaterialUtil.getMaterial(getValueFromString(val));
		this.ammoByte = getByteDataFromString(val);

		if (ammoByte == -1)
		{
			this.ammoByte = 0;
		}
	}

	public void addGunSounds(String val)
	{
		String[] sounds = val.split(",");
		for (int i = 0; i < sounds.length; i++)
			gunSound.add(sounds[i]);
	}
	
	@Override
	public String toString()
	{
		return "Gun { name = " + gunName + ", type = " + gunType + ", priority = " + priority + " }";
	}
}