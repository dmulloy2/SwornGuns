package net.dmulloy2.swornguns.io;

import java.io.File;
import java.util.Map.Entry;
import java.util.logging.Level;

import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.types.EffectType;
import net.dmulloy2.swornguns.types.Gun;

import org.bukkit.Effect;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * @author dmulloy2
 */

public class WeaponReader
{
	private final SwornGuns plugin;
	
	private boolean loaded;
	
	private File file;
	private Gun ret;

	public WeaponReader(SwornGuns plugin, File file)
	{
		this.plugin = plugin;
		this.file = file;
		
		this.ret = new Gun(file.getName(), plugin);
		this.ret.setFileName(file.getName().toLowerCase());
		this.ret.setNode("swornguns.fire." + file.getName().toLowerCase());
		
		load();
	}

	private void computeData(String var, String val)
	{
		try
		{
				if (var.equals("gunname"))
					ret.setName(val);
				if (var.equals("guntype"))
					ret.setGunType(val);
				if (var.equals("ammoamtneeded"))
					ret.setAmmoAmtNeeded(Integer.parseInt(val));
				if (var.equals("reloadtime"))
					ret.setReloadTime(Integer.parseInt(val));
				if (var.equals("gundamage"))
					ret.setGunDamage(Integer.parseInt(val));
				if (var.equals("armorpenetration"))
					ret.setArmorPenetration(Integer.parseInt(val));
				if (var.equals("ammotype"))
					ret.setAmmoType(val);
				if (var.equals("roundsperburst"))
					ret.setRoundsPerBurst(Integer.parseInt(val));
				if (var.equals("maxdistance"))
					ret.setMaxDistance(Integer.parseInt(val));
				if (var.equals("bulletsperclick"))
					ret.setBulletsPerClick(Integer.parseInt(val));
				if (var.equals("bulletspeed"))
					ret.setBulletSpeed(Double.parseDouble(val));
				if (var.equals("accuracy"))
					ret.setAccuracy(Double.parseDouble(val));
				if (var.equals("accuracy_aimed"))
					ret.setAccuracy_aimed(Double.parseDouble(val));
				if (var.equals("accuracy_crouched"))
					ret.setAccuracy_crouched(Double.parseDouble(val));
				if (var.equals("exploderadius"))
					ret.setExplodeRadius(Double.parseDouble(val));
				if (var.equals("gunvolume"))
					ret.setGunVolume(Double.parseDouble(val));
				if (var.equals("fireradius"))
					ret.setFireRadius(Double.parseDouble(val));
				if (var.equals("flashradius"))
					ret.setFlashRadius(Double.parseDouble(val));
				if (var.equals("canheadshot"))
					ret.setCanHeadshot(Boolean.parseBoolean(val));
				if (var.equals("canshootleft"))
					ret.setCanClickLeft(Boolean.parseBoolean(val));
				if (var.equals("canshootright"))
					ret.setCanClickRight(Boolean.parseBoolean(val));
				if (var.equals("canclickleft"))
					ret.setCanClickLeft(Boolean.parseBoolean(val));
				if (var.equals("canclickright"))
					ret.setCanClickRight(Boolean.parseBoolean(val));
				if (var.equals("knockback"))
					ret.setKnockback(Double.parseDouble(val));
				if (var.equals("recoil"))
					ret.setRecoil(Double.parseDouble(val));
				if (var.equals("canaim"))
					ret.setCanAimLeft(Boolean.parseBoolean(val));
				if (var.equals("canaimleft"))
					ret.setCanAimLeft(Boolean.parseBoolean(val));
				if (var.equals("canaimright"))
					ret.setCanAimRight(Boolean.parseBoolean(val));
				if (var.equals("outofammomessage"))
					ret.setOutOfAmmoMessage(val);
				if (var.equals("permissionmessage"))
					ret.setPermissionMessage(val);
				if (var.equals("bullettype"))
					ret.setProjType(val);
				if (var.equals("needspermission"))
					ret.setNeedsPermission(Boolean.parseBoolean(val));
				if (var.equals("hassmoketrail"))
					ret.setHasSmokeTrail(Boolean.parseBoolean(val));
				if (var.equals("gunsound"))
					ret.addGunSounds(val);
				if (var.equals("maxclipsize"))
					ret.setMaxClipSize(Integer.parseInt(val));
				if (var.equals("hasclip"))
					ret.setHasClip(Boolean.parseBoolean(val));
				if (var.equals("reloadgunondrop"))
					ret.setReloadGunOnDrop(Boolean.parseBoolean(val));
				if (var.equals("localgunsound"))
					ret.setLocalGunSound(Boolean.parseBoolean(val));
				if (var.equalsIgnoreCase("canGoPastMaxDistance"))
					ret.setCanGoPastMaxDistance(Boolean.parseBoolean(val));
				if (var.equalsIgnoreCase("ignoreitemdata"))
					ret.setIgnoreItemData(Boolean.parseBoolean(val));
				if (var.equals("bulletdelaytime"))
					ret.setBulletDelayTime(Integer.parseInt(val));
				if (var.equals("explosiondamage"))
					ret.setExplosionDamage(Integer.parseInt(val));
				if (var.equals("timeuntilrelease"))
					ret.setReleaseTime(Integer.parseInt(val));
				if (var.equals("reloadtype"))
					ret.setReloadType(val);
				if (var.equals("play_effect_on_release"))
				{
					String[] effDat = val.split(",");
					if (effDat.length == 3)
					{
						double radius = Double.parseDouble(effDat[0]);
						int duration = Integer.parseInt(effDat[1]);
						Effect eff = Effect.valueOf(effDat[2].toUpperCase());
						EffectType effect = new EffectType(plugin, duration, radius, eff);
						ret.setReleaseEffect(effect);
					}
					else if (effDat.length == 4)
					{
						double radius = Double.parseDouble(effDat[0]);
						int duration = Integer.parseInt(effDat[1]);
						Effect eff = Effect.valueOf(effDat[2].toUpperCase());
						byte specialDat = Byte.parseByte(effDat[3]);
						EffectType effect = new EffectType(plugin, duration, radius, eff);
						effect.setSpecialDat(specialDat);
						ret.setReleaseEffect(effect);
					}
				}
		}
		catch (Exception e)
		{
			plugin.getLogHandler().log(Level.SEVERE, "Could not load gun {0}: {1}", file.getName(), e.getMessage());
			
			this.loaded = false;
		}
	}

	public void load()
	{
		this.loaded = true;
		
		YamlConfiguration fc = YamlConfiguration.loadConfiguration(file);
		
		for (Entry<String, Object> entry : fc.getValues(true).entrySet())
		{
			computeData(entry.getKey(), "" + entry.getValue());
		}
	}
	
	public final boolean isLoaded()
	{
		return loaded;
	}
	
	public final Gun getGun()
	{
		return ret;
	}
}