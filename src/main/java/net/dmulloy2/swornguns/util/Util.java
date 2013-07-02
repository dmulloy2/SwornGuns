package net.dmulloy2.swornguns.util;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class Util {
	public static Sound getSound(String gunSound) {
		String snd = gunSound.toUpperCase().replace(" ", "_");
		Sound sound = Sound.valueOf(snd);
		return sound;
	}
	
	public static void playEffect(Effect e, Location l, int num) {
		for (Player player : Bukkit.getOnlinePlayers()) {
			player.playEffect(l, e, num);
		}
	}
	
	public static int random(int i) {
		Random random = new Random();
		return random.nextInt(i);
	}
}