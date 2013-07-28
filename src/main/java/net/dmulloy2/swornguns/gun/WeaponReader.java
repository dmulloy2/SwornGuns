package net.dmulloy2.swornguns.gun;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import net.dmulloy2.swornguns.SwornGuns;

import org.bukkit.Effect;

public class WeaponReader {
	private final SwornGuns plugin;
	public boolean loaded = false;
	public File file;
	public String weaponType;
	public Gun ret;

	public WeaponReader(final SwornGuns plugin, File file, String string) {
		this.plugin = plugin;
		this.file = file;
		this.weaponType = string;
		this.ret = new Gun(file.getName(), plugin);
		this.ret.setFilename(file.getName().toLowerCase());
		load();
	}

	private void computeData(String str) {
		try {
			if (str.indexOf("=") > 0) {
				String var = str.substring(0, str.indexOf("=")).toLowerCase();
				String val = str.substring(str.indexOf("=") + 1);
				if (var.equals("gunname"))
					this.ret.setName(val);
				if (var.equals("guntype"))
					this.ret.setGunType(val);
				if (var.equals("ammoamtneeded"))
					this.ret.setAmmoAmountNeeded(Integer.parseInt(val));
				if (var.equals("reloadtime"))
					this.ret.setReloadTime(Integer.parseInt(val));
				if (var.equals("gundamage"))
					this.ret.setGunDamage(Integer.parseInt(val));
				if (var.equals("armorpenetration"))
					this.ret.setArmorPenetration(Integer.parseInt(val));
				if (var.equals("ammotype"))
					this.ret.setAmmoType(val);
				if (var.equals("roundsperburst"))
					this.ret.setRoundsPerBurst(Integer.parseInt(val));
				if (var.equals("maxdistance"))
					this.ret.setMaxDistance(Integer.parseInt(val));
				if (var.equals("bulletsperclick"))
					this.ret.setBulletsPerClick(Integer.parseInt(val));
				if (var.equals("bulletspeed"))
					this.ret.setBulletSpeed(Double.parseDouble(val));
				if (var.equals("accuracy"))
					this.ret.setAccuracy(Double.parseDouble(val));
				if (var.equals("accuracy_aimed"))
					this.ret.setAccuracyAimed(Double.parseDouble(val));
				if (var.equals("accuracy_crouched"))
					this.ret.setAccuracyCrouched(Double.parseDouble(val));
				if (var.equals("exploderadius"))
					this.ret.setExplodeRadius(Double.parseDouble(val));
				if (var.equals("gunvolume"))
					this.ret.setGunVolume(Double.parseDouble(val));
				if (var.equals("fireradius"))
					this.ret.setFireRadius(Double.parseDouble(val));
				if (var.equals("flashradius"))
					this.ret.setFlashRadius(Double.parseDouble(val));
				if (var.equals("canheadshot"))
					this.ret.setCanHeadshot(Boolean.parseBoolean(val));
				if (var.equals("canshootleft"))
					this.ret.setCanClickLeft(Boolean.parseBoolean(val));
				if (var.equals("canshootright"))
					this.ret.setCanClickRight(Boolean.parseBoolean(val));
				if (var.equals("canclickleft"))
					this.ret.setCanClickLeft(Boolean.parseBoolean(val));
				if (var.equals("canclickright"))
					this.ret.setCanClickRight(Boolean.parseBoolean(val));
				if (var.equals("knockback"))
					this.ret.setKnockback(Double.parseDouble(val));
				if (var.equals("recoil"))
					this.ret.setRecoil(Double.parseDouble(val));
				if (var.equals("canaim"))
					this.ret.setCanAimLeft(Boolean.parseBoolean(val));
				if (var.equals("canaimleft"))
					this.ret.setCanAimLeft(Boolean.parseBoolean(val));
				if (var.equals("canaimright"))
					this.ret.setCanAimRight(Boolean.parseBoolean(val));
				if (var.equals("outofammomessage"))
					this.ret.setOutOfAmmoMessage(val);
				if (var.equals("permissionmessage"))
					this.ret.setPermissionMessage(val);
				if (var.equals("bullettype"))
					this.ret.projType = val;
				if (var.equals("needspermission"))
					this.ret.needsPermission = Boolean.parseBoolean(val);
				if (var.equals("hassmoketrail"))
					this.ret.setSmokeTrail(Boolean.parseBoolean(val));
				if (var.equals("gunsound"))
					this.ret.addGunSounds(val);
				if (var.equals("maxclipsize"))
					this.ret.maxClipSize = Integer.parseInt(val);
				if (var.equals("hasclip"))
					this.ret.hasClip = Boolean.parseBoolean(val);
				if (var.equals("reloadgunondrop"))
					this.ret.reloadGunOnDrop = Boolean.parseBoolean(val);
				if (var.equals("localgunsound"))
					this.ret.setLocalGunSound(Boolean.parseBoolean(val));
				if (var.equalsIgnoreCase("canGoPastMaxDistance"))
					this.ret.setCanGoPastMaxDistance(Boolean.parseBoolean(val));
				if (var.equalsIgnoreCase("ignoreitemdata"))
					this.ret.ignoreItemData = Boolean.parseBoolean(val);
				if (var.equals("bulletdelaytime"))
					this.ret.bulletDelayTime = Integer.parseInt(val);
				if (var.equals("explosiondamage"))
					this.ret.setExplosionDamage(Integer.parseInt(val));
				if (var.equals("timeuntilrelease"))
					this.ret.setReleaseTime(Integer.parseInt(val));
				if (var.equals("reloadtype"))
					this.ret.reloadType = val;
				if (var.equals("play_effect_on_release")) {
					String[] effDat = val.split(",");
					if (effDat.length == 3) {
						double radius = Double.parseDouble(effDat[0]);
						int duration = Integer.parseInt(effDat[1]);
						Effect eff = Effect.valueOf(effDat[2].toUpperCase());
						EffectType effect = new EffectType(plugin, duration, radius, eff);
						this.ret.releaseEffect = effect;
					} else if (effDat.length == 4) {
						double radius = Double.parseDouble(effDat[0]);
						int duration = Integer.parseInt(effDat[1]);
						Effect eff = Effect.valueOf(effDat[2].toUpperCase());
						byte specialDat = Byte.parseByte(effDat[3]);
						EffectType effect = new EffectType(plugin, duration, radius, eff);
						effect.setSpecialDat(specialDat);
						this.ret.releaseEffect = effect;
					}
				}
			}
		} catch (Exception e) {
			plugin.getLogger().severe("Could not load gun: " + file + ": " + e.getMessage());
			this.loaded = false;
		}
	}

	public void load() {
		this.loaded = true;
		List<String> file = new ArrayList<String>();
		try {
			FileInputStream fstream = new FileInputStream(this.file.getAbsolutePath());
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			while ((strLine = br.readLine()) != null) {
				file.add(strLine);
			}
			br.close();
			in.close();
			fstream.close();
		} catch (Exception e) {
			plugin.getLogger().severe("Error loading gun \"" + this.file.getName() + "\": " + e.getMessage());
		}

		for (String line : file) {
			computeData(line);
		}
	}
}