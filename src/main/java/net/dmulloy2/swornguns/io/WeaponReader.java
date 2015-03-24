package net.dmulloy2.swornguns.io;

import java.io.File;
import java.util.List;
import java.util.logging.Level;

import lombok.Getter;
import net.dmulloy2.io.IOUtil;
import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.types.EffectType;
import net.dmulloy2.swornguns.types.Gun;
import net.dmulloy2.util.NumberUtil;
import net.dmulloy2.util.Util;

import org.bukkit.Effect;

/**
 * @author dmulloy2
 */

public class WeaponReader
{
	private @Getter boolean loaded;
	private @Getter Gun gun;

	private final File file;
	private final SwornGuns plugin;
	public WeaponReader(SwornGuns plugin, File file)
	{
		this.plugin = plugin;
		this.file = file;

		this.gun = new Gun(file.getName(), plugin);
		this.gun.setFileName(file.getName().toLowerCase());
		this.load();
	}

	public final void load()
	{
		try
		{
			List<String> lines = IOUtil.readLines(file);
			for (String line : lines)
				computeData(line);

			if (gun.getMaterial() == null)
			{
				plugin.getLogHandler().log("Failed to load gun " + file.getName() + ": null material!");
				return;
			}
		}
		catch (Throwable ex)
		{
			plugin.getLogHandler().log(Level.SEVERE, Util.getUsefulStack(ex, "loading gun: " + file.getName()));
			return;
		}

		// Loading was successful
		this.loaded = true;
	}

	private final void computeData(String str)
	{
		if (str.indexOf("=") > 0)
		{
			String var = str.substring(0, str.indexOf("=")).toLowerCase();
			String val = str.substring(str.indexOf("=") + 1);
			if (var.equals("gunname"))
				gun.setName(val);
			if (var.equals("guntype"))
				gun.setGunType(val);
			if (var.equals("ammoamtneeded"))
				gun.setAmmoAmtNeeded(NumberUtil.toInt(val));
			if (var.equals("reloadtime"))
				gun.setReloadTime(NumberUtil.toInt(val));
			if (var.equals("gundamage"))
				gun.setGunDamage(NumberUtil.toDouble(val));
			if (var.equals("armorpenetration"))
				gun.setArmorPenetration(NumberUtil.toDouble(val));
			if (var.equals("ammotype"))
				gun.setAmmoType(val);
			if (var.equals("roundsperburst"))
				gun.setRoundsPerBurst(NumberUtil.toInt(val));
			if (var.equals("maxdistance"))
				gun.setMaxDistance(NumberUtil.toInt(val));
			if (var.equals("bulletsperclick"))
				gun.setBulletsPerClick(NumberUtil.toInt(val));
			if (var.equals("bulletspeed"))
				gun.setBulletSpeed(NumberUtil.toDouble(val));
			if (var.equals("accuracy"))
				gun.setAccuracy(NumberUtil.toDouble(val));
			if (var.equals("accuracy_aimed"))
				gun.setAccuracy_aimed(NumberUtil.toDouble(val));
			if (var.equals("accuracy_crouched"))
				gun.setAccuracy_crouched(NumberUtil.toDouble(val));
			if (var.equals("exploderadius"))
				gun.setExplodeRadius(NumberUtil.toDouble(val));
			if (var.equals("gunvolume"))
				gun.setGunVolume(NumberUtil.toDouble(val));
			if (var.equals("gunpitch"))
				gun.setGunPitch(NumberUtil.toDouble(val));
			if (var.equals("fireradius"))
				gun.setFireRadius(NumberUtil.toDouble(val));
			if (var.equals("flashradius"))
				gun.setFlashRadius(NumberUtil.toDouble(val));
			if (var.equals("canheadshot"))
				gun.setCanHeadshot(Boolean.parseBoolean(val));

			// Firing
			if (var.equals("canshootleft") || var.equals("canclickleft") || var.equals("canfireleft"))
				gun.setCanFireLeft(Boolean.parseBoolean(val));
			if (var.equals("canshootright") || var.equals("canclickright") || var.equals("canfireright"))
				gun.setCanFireRight(Boolean.parseBoolean(val));

			// Aiming
			if (var.equals("canaimleft") || var.equals("canaim"))
				gun.setCanAimLeft(Boolean.parseBoolean(val));
			if (var.equals("canaimright"))
				gun.setCanAimRight(Boolean.parseBoolean(val));

			if (var.equals("knockback"))
				gun.setKnockback(NumberUtil.toDouble(val));
			if (var.equals("recoil"))
				gun.setRecoil(NumberUtil.toDouble(val));
			if (var.equals("bullettype"))
				gun.setProjType(val);
			if (var.equals("needspermission"))
				gun.setNeedsPermission(Boolean.parseBoolean(val));
			if (var.equals("hassmoketrail"))
				gun.setHasSmokeTrail(Boolean.parseBoolean(val));
			if (var.equals("gunsound"))
				gun.addGunSounds(val);
			if (var.equals("maxclipsize"))
				gun.setMaxClipSize(NumberUtil.toInt(val));
			if (var.equals("hasclip"))
				gun.setHasClip(Boolean.parseBoolean(val));
			if (var.equals("reloadgunondrop"))
				gun.setReloadGunOnDrop(Boolean.parseBoolean(val));
			if (var.equals("localgunsound"))
				gun.setLocalGunSound(Boolean.parseBoolean(val));
			if (var.equalsIgnoreCase("canGoPastMaxDistance"))
				gun.setCanGoPastMaxDistance(Boolean.parseBoolean(val));
			if (var.equals("bulletdelaytime"))
				gun.setBulletDelayTime(NumberUtil.toInt(val));
			if (var.equals("explosiondamage"))
				gun.setExplosionDamage(NumberUtil.toDouble(val));
			if (var.equals("timeuntilrelease"))
				gun.setReleaseTime(NumberUtil.toInt(val));
			if (var.equals("reloadtype"))
				gun.setReloadType(val);
			if (var.equals("priority"))
				gun.setPriority(NumberUtil.toInt(val));
			if (var.equals("lore"))
				gun.setLore(val);
			if (var.equals("warnifnopermission"))
				gun.setWarnIfNoPermission(Boolean.parseBoolean(val));
			if (var.equals("unlimitedammo"))
				gun.setUnlimitedAmmo(Boolean.parseBoolean(val));
			if (var.equals("explosiontype"))
				gun.setExplosionType(val.toUpperCase());
			if (var.equals("play_effect_on_release"))
			{
				String[] effDat = val.split(",");
				if (effDat.length == 3)
				{
					double radius = NumberUtil.toDouble(effDat[0]);
					int duration = NumberUtil.toInt(effDat[1]);
					Effect eff = Effect.valueOf(effDat[2].toUpperCase());
					EffectType effect = new EffectType(plugin, duration, radius, eff);
					gun.setReleaseEffect(effect);
				}
				else if (effDat.length == 4)
				{
					double radius = NumberUtil.toDouble(effDat[0]);
					int duration = NumberUtil.toInt(effDat[1]);
					Effect eff = Effect.valueOf(effDat[2].toUpperCase());
					byte specialDat = Byte.parseByte(effDat[3]);
					EffectType effect = new EffectType(plugin, duration, radius, eff);
					effect.setSpecialDat(specialDat);
					gun.setReleaseEffect(effect);
				}
			}

			if (var.equals("outofammomessage"))
			{
				// The old out of ammo message was hideous
				if (! val.startsWith("This gun needs"))
					gun.setOutOfAmmoMessage(val);
			}
		}
	}
}