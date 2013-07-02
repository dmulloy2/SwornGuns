package net.dmulloy2.swornguns.gun;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.events.SwornGunsFireEvent;
import net.dmulloy2.swornguns.util.Util;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class Gun {
	private boolean canHeadshot;
	private boolean isThrowable;
	private boolean hasSmokeTrail;
	private boolean localGunSound;
	private boolean canAimLeft;
	private boolean canAimRight;
	private boolean canGoPastMaxDistance;
	public boolean needsPermission;
	public boolean canClickRight;
	public boolean canClickLeft;
	public boolean hasClip = true;
	public boolean ignoreItemData = false;
	public boolean reloadGunOnDrop = true;
	public boolean firing = false;
	public boolean reloading;
	public boolean changed = false;
	
	private byte gunByte;
	private byte ammoByte;
	
	private int gunType;
	private int ammoType;
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
    public int maxClipSize = 30;
    public int bulletDelayTime = 10;
    public int roundsFired;
    public int gunReloadTimer;
    public int timer;
    public int lastFired;
    public int ticks;
    public int heldDownTicks;
  	
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
  	
  	public String node;
  	public String reloadType = "NORMAL";
  	
  	private String gunName;
  	private String fileName;
  	public String projType = "";
  	public String outOfAmmoMessage = "";
  	public String permissionMessage = "";
  	
  	private SwornGuns plugin;
  	
  	public GunPlayer owner;
  
  	public EffectType releaseEffect;
  
  	public List<String> gunSound = new ArrayList<String>();

  	public Gun(final String name, final SwornGuns plugin) {
  		this.gunName = name;
  		this.fileName = name;
  		this.outOfAmmoMessage = "Out of ammo!";
  		this.plugin = plugin;
  	}

  	public void shoot() {
  		if ((this.owner != null) && 
  				(this.owner.getPlayer().isOnline()) && 
  				(!this.reloading)) {
  			SwornGunsFireEvent event = new SwornGunsFireEvent(this.owner, this);
  			plugin.getServer().getPluginManager().callEvent(event);
  			if (!event.isCancelled())
  				if (((this.owner.checkAmmo(this, event.getAmountAmmoNeeded())) && (event.getAmountAmmoNeeded() > 0)) || (event.getAmountAmmoNeeded() == 0)) {
  					this.owner.removeAmmo(this, event.getAmountAmmoNeeded());
  					if ((this.roundsFired >= this.maxClipSize) && (this.hasClip)) {
  						reloadGun();
  						return;
  					}
  					doRecoil(this.owner.getPlayer());
  					this.changed = true;
  					this.roundsFired += 1;
  					for (int i = 0; i < this.gunSound.size(); i++) {
  						Sound sound = Util.getSound((String)this.gunSound.get(i));
  						if (sound != null) {
  							if (this.localGunSound)
  								this.owner.getPlayer().playSound(this.owner.getPlayer().getLocation(), sound, (float)this.gunVolume, 2.0F);
  							else {
  								this.owner.getPlayer().getWorld().playSound(this.owner.getPlayer().getLocation(), sound, (float)this.gunVolume, 2.0F);
  							}
  						}
  					}
  					for (int i = 0; i < this.bulletsPerClick; i++) {
  						int acc = (int)(event.getGunAccuracy() * 1000.0D);
  						
  						if (acc <= 0) {
  							acc = 1;
  						}
  						Location ploc = this.owner.getPlayer().getLocation();
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
  						vec.multiply(this.bulletSpeed);
  						Bullet bullet = new Bullet(plugin, this.owner, vec, this);
  						plugin.addBullet(bullet);
  					}
  					
  					if ((this.roundsFired >= this.maxClipSize) && (this.hasClip))
  						reloadGun();
  				} else {
  					this.owner.getPlayer().playSound(this.owner.getPlayer().getLocation(), Sound.ITEM_BREAK, 20.0F, 20.0F);
  					this.owner.getPlayer().sendMessage(this.outOfAmmoMessage);
  					finishShooting();
  				}
  		}
  	}

  	public void tick() {
  		this.ticks += 1;
  		this.lastFired += 1;
  		this.timer -= 1;
  		this.gunReloadTimer -= 1;

        if (this.gunReloadTimer < 0) {
        	if (this.reloading) {
        		finishReloading();
        	}
        	this.reloading = false;
        }

        gunSounds();

        if (this.lastFired > 6) {
        	this.heldDownTicks = 0;
        }
        if (((this.heldDownTicks >= 2) && (this.timer <= 0)) || ((this.firing) && (!this.reloading))) {
        	if (this.roundsPerBurst > 1) {
        		if (this.ticks % this.bulletDelay == 0) {
        			this.bulletsShot += 1;
        			if (this.bulletsShot <= this.roundsPerBurst)
        				shoot();
        			else
        				finishShooting();
        		}
        	} else {
        		shoot();
        		finishShooting();
        	}
        }

        if (this.reloading)
        	this.firing = false;
  	}

  	public Gun copy() {
  		Gun g = new Gun(this.gunName, this.plugin);
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
  		g.outOfAmmoMessage = this.outOfAmmoMessage;
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
  		g.permissionMessage = this.permissionMessage;
  		if (this.releaseEffect != null) {
  			g.releaseEffect = this.releaseEffect.clone();
  		}
  		return g;
  	}

  	public void reloadGun() {
  		this.reloading = true;
  		this.gunReloadTimer = this.reloadTime;
  	}

  	private void gunSounds() {
  		if (this.reloading) {
  			int amtReload = this.reloadTime - this.gunReloadTimer;
  			if (this.reloadType.equalsIgnoreCase("bolt")) {
  				if (amtReload == 6)
  					this.owner.getPlayer().playSound(this.owner.getPlayer().getLocation(), Sound.DOOR_OPEN, 2.0F, 1.5F);
  				if (amtReload == this.reloadTime - 4)
  					this.owner.getPlayer().playSound(this.owner.getPlayer().getLocation(), Sound.DOOR_CLOSE, 1.0F, 1.5F);
  			} else if ((this.reloadType.equalsIgnoreCase("pump")) || (this.reloadType.equals("INDIVIDUAL_BULLET"))) {
  				int rep = (this.reloadTime - 10) / this.maxClipSize;
  				if ((amtReload >= 5) && (amtReload <= this.reloadTime - 5) && (amtReload % rep == 0)) {
  					this.owner.getPlayer().playSound(this.owner.getPlayer().getLocation(), Sound.NOTE_STICKS, 1.0F, 1.0F);
  					this.owner.getPlayer().playSound(this.owner.getPlayer().getLocation(), Sound.NOTE_SNARE_DRUM, 1.0F, 2.0F);
  				}

  				if (amtReload == this.reloadTime - 3)
  					this.owner.getPlayer().playSound(this.owner.getPlayer().getLocation(), Sound.PISTON_EXTEND, 1.0F, 2.0F);
  				if (amtReload == this.reloadTime - 1)
  					this.owner.getPlayer().playSound(this.owner.getPlayer().getLocation(), Sound.PISTON_RETRACT, 1.0F, 2.0F);
  			} else {
  				if (amtReload == 6) {
  					this.owner.getPlayer().playSound(this.owner.getPlayer().getLocation(), Sound.FIRE_IGNITE, 2.0F, 2.0F);
  					this.owner.getPlayer().playSound(this.owner.getPlayer().getLocation(), Sound.DOOR_OPEN, 1.0F, 2.0F);
  				}
  				if (amtReload == this.reloadTime / 2)
  					this.owner.getPlayer().playSound(this.owner.getPlayer().getLocation(), Sound.PISTON_RETRACT, 0.33F, 2.0F);
  				if (amtReload == this.reloadTime - 4) {
  					this.owner.getPlayer().playSound(this.owner.getPlayer().getLocation(), Sound.FIRE_IGNITE, 2.0F, 2.0F);
  					this.owner.getPlayer().playSound(this.owner.getPlayer().getLocation(), Sound.DOOR_CLOSE, 1.0F, 2.0F);
  				}
  			}
  		} else {
  			if (this.reloadType.equalsIgnoreCase("pump")) {
  				if (this.timer == 8) {
  					this.owner.getPlayer().playSound(this.owner.getPlayer().getLocation(), Sound.PISTON_EXTEND, 1.0F, 2.0F);
  				}
  				if (this.timer == 6) {
  					this.owner.getPlayer().playSound(this.owner.getPlayer().getLocation(), Sound.PISTON_RETRACT, 1.0F, 2.0F);
  				}
  			}
  			if (this.reloadType.equalsIgnoreCase("bolt")) {
  				if (this.timer == this.bulletDelayTime - 4)
  					this.owner.getPlayer().playSound(this.owner.getPlayer().getLocation(), Sound.DOOR_OPEN, 2.0F, 1.25F);
  				if (this.timer == 6)
  					this.owner.getPlayer().playSound(this.owner.getPlayer().getLocation(), Sound.DOOR_CLOSE, 1.0F, 1.25F);
  			}
  		}
  	}

  	private void doRecoil(Player player) {
  		if (this.recoil != 0.0D) {
  			Location ploc = player.getLocation();
  			double dir = -ploc.getYaw() - 90.0F;
  			double pitch = -ploc.getPitch() - 180.0F;
  			double xd = Math.cos(Math.toRadians(dir)) * Math.cos(Math.toRadians(pitch));
  			double yd = Math.sin(Math.toRadians(pitch));
  			double zd = -Math.sin(Math.toRadians(dir)) * Math.cos(Math.toRadians(pitch));
  			Vector vec = new Vector(xd, yd, zd);
  			vec.multiply(this.recoil / 2.0D).setY(0);
  			player.setVelocity(player.getVelocity().add(vec));
	  }
  }

  	public void doKnockback(LivingEntity entity, Vector speed) {
  		if (this.knockback > 0.0D) {
  			speed.normalize().setY(0.6D).multiply(this.knockback / 4.0D);
  			entity.setVelocity(speed);
  		}
  	}

  	public void finishReloading() {
  		this.bulletsShot = 0;
  		this.roundsFired = 0;
  		this.changed = false;
  		this.gunReloadTimer = 0;
  	}

  	private void finishShooting() {
  		this.bulletsShot = 0;
  		this.timer = this.bulletDelayTime;
  		this.firing = false;
  	}
  	
  	public String getName() {
  		return this.gunName;
  	}
  	
  	public Material getAmmoMaterial() {
  		int id = getAmmoType();
  		Material mat = Material.getMaterial(id);
  		if (mat != null) {
  			return mat;
  		}
  		return null;
  	}

  	public int getAmmoType() {
  		return this.ammoType;
  	}

  	public int getAmmoAmtNeeded() {
  		return this.ammoAmtNeeded;
  	}

  	public Material getGunMaterial() {
  		int id = getGunType();
  		Material mat = Material.getMaterial(id);
  		if (mat != null) {
  			return mat;
  		}
  		plugin.getLogger().warning("Null material in gun: " + this.gunName + " / Type ID: " + id);
  		return null;
  	}

  	public int getGunType() {
  		return this.gunType;
  	}

  	public double getExplodeRadius() {
  		return this.explodeRadius;
  	}

  	public double getFireRadius() {
  		return this.fireRadius;
  	}

  	public boolean isThrowable() {
  		return this.isThrowable;
  	}

  	public void setName(String val) {
  		val = ChatColor.translateAlternateColorCodes('&', val);
  		this.gunName = val;
  	}

  	public int getValueFromString(String str) {
  		if (str.contains(":")) {
  			String news = str.substring(0, str.indexOf(":"));
  			return Integer.parseInt(news);
  		}
  		return Integer.parseInt(str);
  	}

  	public byte getByteDataFromString(String str) {
  		if (str.contains(":")) {
  			String news = str.substring(str.indexOf(":") + 1, str.length());
  			return Byte.parseByte(news);
  		}
  		return -1;
  	}

  	public void setGunType(String val) {
  		this.gunType = getValueFromString(val);
  		this.gunByte = getByteDataFromString(val);
  		if (this.gunByte == -1) {
  			this.ignoreItemData = true;
  			this.gunByte = 0;
  		}
  	}

  	public void setAmmoType(String val) {
  		this.ammoType = getValueFromString(val);
  		this.ammoByte = getByteDataFromString(val);
  		if (this.ammoByte == -1) {
  			this.ammoByte = 0;
  		}
  	}

  	public void setAmmoAmountNeeded(int parseInt) {
  		this.ammoAmtNeeded = parseInt;
  	}

  	public void setRoundsPerBurst(int parseInt) {
  		this.roundsPerBurst = parseInt;
  	}

  	public void setBulletsPerClick(int parseInt) {
  		this.bulletsPerClick = parseInt;
  	}

  	public void setBulletSpeed(double parseDouble) {
  		this.bulletSpeed = parseDouble;
  	}

  	public void setAccuracy(double parseDouble) {
  		this.accuracy = parseDouble;
  	}

  	public void setAccuracyAimed(double parseDouble) {
  		this.accuracy_aimed = parseDouble;
  	}

  	public void setAccuracyCrouched(double parseDouble) {
  		this.accuracy_crouched = parseDouble;
  	}

  	public void setExplodeRadius(double parseDouble) {
  		this.explodeRadius = parseDouble;
  	}
  	
  	public void setFireRadius(double parseDouble) {
  		this.fireRadius = parseDouble;
  	}

  	public void setCanHeadshot(boolean parseBoolean) {
  		this.canHeadshot = parseBoolean;
  	}

  	public void setCanClickLeft(boolean parseBoolean) {
  		this.canClickLeft = parseBoolean;
  	}

  	public void setCanClickRight(boolean parseBoolean) {
  		this.canClickRight = parseBoolean;
  	}

  	public void clear() {
  		this.owner = null;
  	}

  	public void setReloadTime(int parseInt) {
  		this.reloadTime = parseInt;
  	}

  	public int getReloadTime() {
  		return this.reloadTime;
  	}

  	public int getGunDamage() {
  		return this.gunDamage;
  	}

  	public void setGunDamage(int parseInt) {
  		this.gunDamage = parseInt;
  	}

  	public double getMaxDistance() {
  		return this.maxDistance;
  	}

  	public void setMaxDistance(int i) {
  		this.maxDistance = i;
  	}

  	public boolean canAimLeft() {
  		return this.canAimLeft;
  	}

  	public boolean canAimRight() {
  		return this.canAimRight;
  	}

  	public void setCanAimLeft(boolean parseBoolean) {
  		this.canAimLeft = parseBoolean;
  	}

  	public void setCanAimRight(boolean parseBoolean) {
  		this.canAimRight = parseBoolean;
  	}

  	public void setOutOfAmmoMessage(String val) {
  		val = ChatColor.translateAlternateColorCodes('&', val);
  		this.outOfAmmoMessage = val;
  	}

  	public void setPermissionMessage(String val) {
  		val = ChatColor.translateAlternateColorCodes('&', val);
  		this.permissionMessage = val;
  	}

  	public void setFlashRadius(double parseDouble) {
  		this.flashRadius = parseDouble;
  	}

  	public double getFlashRadius() {
  		return this.flashRadius;
  	}

  	public void setIsThrowable(boolean b) {
  		this.isThrowable = b;
  	}
  	
  	public boolean canHeadShot() {
  		return this.canHeadshot;
  	}

  	public boolean hasSmokeTrail() {
  		return this.hasSmokeTrail;
  	}

  	public void setSmokeTrail(boolean b) {
  		this.hasSmokeTrail = b;
  	}

  	public boolean isLocalGunSound() {
  		return this.localGunSound;
  	}

  	public void setLocalGunSound(boolean b) {
  		this.localGunSound = b;
  	}

  	public void setArmorPenetration(int parseInt) {
 	   this.armorPenetration = parseInt;
  	}

  	public int getArmorPenetration() {
  		return this.armorPenetration;
  	}

  	public void setExplosionDamage(int i) {
  		this.explosionDamage = i;
  	}

  	public int getExplosionDamage() {
  		return this.explosionDamage;
  	}

  	public String getFilename() {
  		return this.fileName;
  	}

  	public void setFilename(String string) {
  		this.fileName = string;
  	}

  	public void setGunTypeByte(byte b) {
  		this.gunByte = b;
  	}

  	public byte getGunTypeByte() {
  		return this.gunByte;
  	}

  	public void setAmmoTypeByte(byte b) {
  		this.ammoByte = b;
  	}

  	public byte getAmmoTypeByte() {
  		return this.ammoByte;
  	}

  	public void setRecoil(double d) {
  		this.recoil = d;
  	}

  	public double getRecoil() {
  		return this.recoil;
  	}

  	public void setKnockback(double d) {
  		this.knockback = d;
  	}

  	public double getKnockback() {
	  return this.knockback;
  	}

  	public void addGunSounds(String val) {
  		String[] sounds = val.split(",");
  		for (int i = 0; i < sounds.length; i++)
  			this.gunSound.add(sounds[i]);
  	}

  	public int getReleaseTime() {
  		return this.releaseTime;
  	}

  	public void setReleaseTime(int v) {
	  this.releaseTime = v;
  	}

  	public void setCanGoPastMaxDistance(boolean parseBoolean) {
  		this.canGoPastMaxDistance = parseBoolean;
  	}

  	public boolean canGoPastMaxDistance() {
  		return this.canGoPastMaxDistance;
  	}

  	public void setGunVolume(double parseDouble) {
  		this.gunVolume = parseDouble;
  	}

  	public double getGunVolume() {
  		return this.gunVolume;
  	}

  	public double getAccuracy() {
  		return this.accuracy;
  	}

  	public double getAccuracy_aimed() {
  		return this.accuracy_aimed;
  	}
  	
  	public double getAccuracy_crouched() {
  		return this.accuracy_crouched;
  	}
}