package net.dmulloy2.swornguns.io;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.types.EffectType;
import net.dmulloy2.swornguns.types.Gun;
import net.dmulloy2.swornguns.util.Util;

import org.bukkit.Effect;

/**
 * @author dmulloy2
 */

public class WeaponReader
{
	private boolean loaded;

	private File file;
	private Gun ret;

	private final SwornGuns plugin;
	public WeaponReader(SwornGuns plugin, File file)
	{
		this.plugin = plugin;
		this.file = file;

		this.ret = new Gun(file.getName(), plugin);
		this.ret.setFileName(file.getName().toLowerCase());
		this.load();
	}

	public final void load()
	{
		try
		{
			this.loaded = true;
			List<String> file = new ArrayList<String>();
			FileInputStream fstream = new FileInputStream(this.file.getAbsolutePath());
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			while ((strLine = br.readLine()) != null)
			{
				file.add(strLine);
			}
			br.close();
			in.close();
			fstream.close();

			for (String line : file)
			{
				computeData(line);
			}
		}
		catch (Exception e)
		{
			plugin.getLogHandler().log(Level.SEVERE, Util.getUsefulStack(e, "loading gun: " + file.getName()));
			this.loaded = false;
		}
	}

	private final void computeData(String str) throws Exception
	{
		if (str.indexOf("=") > 0)
		{
			String var = str.substring(0, str.indexOf("=")).toLowerCase();
			String val = str.substring(str.indexOf("=") + 1);
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
			if (var.equals("priority"))
				ret.setPriority(Integer.parseInt(val));
			if (var.equals("lore"))
				ret.setLore(val);
			if (var.equals("outofammomessage"))
				ret.setOutOfAmmoMessage(val);
			if (var.equals("warnifnopermission"))
				ret.setWarnIfNoPermission(Boolean.parseBoolean(val));
			if (var.equals("unlimitedammo"))
				ret.setUnlimitedAmmo(Boolean.parseBoolean(val));
			if (var.equals("explosiontype"))
				ret.setExplosionType(val.toUpperCase());
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