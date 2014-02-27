package net.dmulloy2.swornguns.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import lombok.AllArgsConstructor;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;

/**
 * @author dmulloy2
 */

@AllArgsConstructor
public class Explosion
{
	private final Location location;

	/**
	 * Creates the explotion
	 */
	public final void explode()
	{
		World world = location.getWorld();
		Firework firework = world.spawn(location, Firework.class);

		FireworkMeta meta = firework.getFireworkMeta();
		meta.addEffect(getEffect());
		meta.setPower(1);

		firework.setFireworkMeta(meta);

		try
		{
			firework.detonate();
		}
		catch (NoSuchMethodError e)
		{
			// Out of date bukkit
		}
	}

	/**
	 * Firework type
	 */
	private final FireworkEffect getEffect()
	{
		// Colors
		List<Color> c = new ArrayList<Color>();
		c.add(Color.RED);
		c.add(Color.RED);
		c.add(Color.RED);
		c.add(Color.ORANGE);
		c.add(Color.ORANGE);
		c.add(Color.ORANGE);
		c.add(Color.BLACK);
		c.add(Color.GRAY);

		// Type
		Random rand = new Random();
		FireworkEffect.Type type = FireworkEffect.Type.BALL_LARGE;
		if (rand.nextInt(2) == 0)
		{
			type = FireworkEffect.Type.BURST;
		}

		// Build the effect
		FireworkEffect e = FireworkEffect.builder().flicker(true).withColor(c).withFade(c).with(type).trail(true).build();

		return e;
	}
}