package net.dmulloy2.swornguns.gun;

import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.util.Util;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class EffectType {
	private int maxDuration;
	private int duration;
	
	private Effect type;
	private double radius;
	private Location location;
	
	private byte specialDat = -1;
	
	private final SwornGuns plugin;

	public EffectType(final SwornGuns plugin, int duration, double radius, Effect type) {
		this.plugin = plugin;
		this.duration = duration;
		this.maxDuration = duration;
		this.type = type;
		this.radius = radius;
	}

	public void start(Location location) {
		this.location = location;
		this.duration = this.maxDuration;
		plugin.addEffect(this);
	}

	public EffectType clone() {
		return new EffectType(this.plugin, this.maxDuration, this.radius, this.type).setSpecialDat(this.specialDat);
	}

	public void tick() {
		this.duration -= 1;

		if (this.duration < 0) {
			plugin.removeEffect(this);
			return;
		}

		double yRad = this.radius;
		if (this.type.equals(Effect.MOBSPAWNER_FLAMES)) {
			yRad = 0.75D;
			Player[] players = Bukkit.getOnlinePlayers();
			for (int i = players.length - 1; i >= 0; i--) {
				if ((players[i].getWorld().equals(this.location.getWorld())) && 
						(this.location.distance(players[i].getLocation()) < this.radius)) {
					players[i].setFireTicks(20);
				}
			}
		}

		for (double i = -this.radius; i <= this.radius; i += 1.0D) {
			for (double ii = -this.radius; ii <= this.radius; ii += 1.0D) {
				for (double iii = 0.0D; iii <= yRad * 2.0D; iii += 1.0D) {
					int rand = Util.random(8);
					if (rand == 2) {
						Location newloc = this.location.clone().add(i, iii - 1.0D, ii);
						Location testLoc = this.location.clone().add(0.0D, yRad - 1.0D, 0.0D);
						if (newloc.distance(testLoc) <= this.radius) {
							byte dat = (byte)Util.random(8);
							if (this.specialDat > -1)
								dat = this.specialDat;
							newloc.getWorld().playEffect(newloc, this.type, dat);
						}
					}
				}
			}
		}
	}

	public EffectType setSpecialDat(byte specialDat) {
		this.specialDat = specialDat;
		return this;
	}
}