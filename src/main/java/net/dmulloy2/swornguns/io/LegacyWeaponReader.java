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
package net.dmulloy2.swornguns.io;

import java.io.File;
import java.io.IOException;
import java.util.List;

import net.dmulloy2.swornapi.io.IOUtil;
import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.types.EffectData;
import net.dmulloy2.swornguns.types.Gun;
import net.dmulloy2.swornguns.types.ReloadType;
import net.dmulloy2.swornapi.util.NumberUtil;
import net.dmulloy2.swornapi.util.Util;

import org.bukkit.Effect;

/**
 * @author dmulloy2
 */

public class LegacyWeaponReader
{
	private final File file;
	private final SwornGuns plugin;

	public LegacyWeaponReader(SwornGuns plugin, File file)
	{
		this.plugin = plugin;
		this.file = file;
	}

	public Gun load() throws IOException
	{
		Gun gun = new Gun(file.getName().toLowerCase(), plugin);

		List<String> lines = IOUtil.readLines(file);
		for (String line : lines)
			computeData(gun, line);

		if (gun.getMaterial() == null)
		{
			plugin.getLogHandler().log("Failed to load gun " + file.getName() + ": null material!");
			return null;
		}

		return gun;
	}

	private void computeData(Gun gun, String str)
	{
		if (str.indexOf("=") > 0)
		{
			String var = str.substring(0, str.indexOf("=")).toLowerCase();
			String val = str.substring(str.indexOf("=") + 1);
			if (var.equals("gunname"))
				gun.setDisplayName(val);
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
				gun.setCanHeadshot(Util.toBoolean(val));

			// Firing
			if (var.equals("canshootleft") || var.equals("canclickleft") || var.equals("canfireleft"))
				gun.setCanFireLeft(Util.toBoolean(val));
			if (var.equals("canshootright") || var.equals("canclickright") || var.equals("canfireright"))
				gun.setCanFireRight(Util.toBoolean(val));

			// Aiming
			if (var.equals("canaimleft") || var.equals("canaim"))
				gun.setCanAimLeft(Util.toBoolean(val));
			if (var.equals("canaimright"))
				gun.setCanAimRight(Util.toBoolean(val));

			if (var.equals("knockback"))
				gun.setKnockback(NumberUtil.toDouble(val));
			if (var.equals("recoil"))
				gun.setRecoil(NumberUtil.toDouble(val));
			if (var.equals("bullettype"))
				gun.setProjType(val);
			if (var.equals("needspermission"))
				gun.setNeedsPermission(Util.toBoolean(val));
			if (var.equals("hassmoketrail"))
				gun.setHasSmokeTrail(Util.toBoolean(val));
			if (var.equals("gunsound"))
				gun.addGunSounds(val);
			if (var.equals("maxclipsize"))
				gun.setMaxClipSize(NumberUtil.toInt(val));
			if (var.equals("hasclip"))
				gun.setHasClip(Util.toBoolean(val));
			if (var.equals("reloadgunondrop"))
				gun.setReloadGunOnDrop(Util.toBoolean(val));
			if (var.equals("localgunsound"))
				gun.setLocalGunSound(Util.toBoolean(val));
			if (var.equalsIgnoreCase("canGoPastMaxDistance"))
				gun.setCanGoPastMaxDistance(Util.toBoolean(val));
			if (var.equals("bulletdelaytime"))
				gun.setBulletDelayTime(NumberUtil.toInt(val));
			if (var.equals("explosiondamage"))
				gun.setExplosionDamage(NumberUtil.toDouble(val));
			if (var.equals("timeuntilrelease"))
				gun.setReleaseTime(NumberUtil.toInt(val));
			if (var.equals("reloadtype"))
				gun.setReloadType(ReloadType.getByName(val));
			if (var.equals("priority"))
				gun.setPriority(NumberUtil.toInt(val));
			if (var.equals("lore"))
				gun.setLore(val);
			if (var.equals("warnifnopermission"))
				gun.setWarnIfNoPermission(Util.toBoolean(val));
			if (var.equals("unlimitedammo"))
				gun.setUnlimitedAmmo(Util.toBoolean(val));
			if (var.equals("explosiontype"))
				gun.setExplosionType(val.toUpperCase());
			if (var.equals("clipsize"))
				gun.setClipSize(NumberUtil.toInt(val));
			if (var.equals("play_effect_on_release"))
			{
				String[] effDat = val.split(",");
				if (effDat.length == 3)
				{
					double radius = NumberUtil.toDouble(effDat[0]);
					int duration = NumberUtil.toInt(effDat[1]);
					Effect eff = Effect.valueOf(effDat[2].toUpperCase());
					EffectData effect = new EffectData(eff, duration, radius, (byte) -1);
					gun.setReleaseEffect(effect);
				}
				else if (effDat.length == 4)
				{
					double radius = NumberUtil.toDouble(effDat[0]);
					int duration = NumberUtil.toInt(effDat[1]);
					Effect eff = Effect.valueOf(effDat[2].toUpperCase());
					byte specialDat = Byte.parseByte(effDat[3]);
					EffectData effect = new EffectData(eff, duration, radius, specialDat);
					gun.setReleaseEffect(effect);
				}
			}

			if (var.equals("outofammomessage"))
			{
				// The old out of ammo message was hideous
				if (! val.startsWith("This gun needs"))
					gun.setOutOfAmmoMessage(val);
			}
			
			if (var.equals("initialclip"))
			{
				gun.setInitialClip(NumberUtil.toInt(val));
			}
		}
	}
}
