package net.dmulloy2.swornguns.types;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;

/**
 * @author dmulloy2
 */

public class Explosion
{
	private final Location location;
	public Explosion(Location location)
	{
		this.location = location;
	}

	public void explode()
	{
		World world = location.getWorld();
		Firework firework = world.spawn(location, Firework.class);

		FireworkMeta meta = firework.getFireworkMeta();
		meta.addEffect(getEffect());
		meta.setPower(1);

		firework.setFireworkMeta(meta);

		try
		{
			playFirework(world, location, firework);
		}
		catch (Exception e)
		{
			//
		}
	}

	private FireworkEffect.Type getType()
	{
		Random rand = new Random();
		FireworkEffect.Type type = FireworkEffect.Type.BALL_LARGE;
		if (rand.nextInt(2) == 0)
		{
			type = FireworkEffect.Type.BURST;
		}

		return type;
	}

	private FireworkEffect getEffect()
	{
		List<Color> c = getColors();
		FireworkEffect.Type type = getType();

		FireworkEffect e = FireworkEffect.builder().flicker(true).withColor(c).withFade(c).with(type).trail(true).build();

		return e;
	}

	private List<Color> getColors()
	{
		List<Color> c = new ArrayList<Color>();
		c.add(Color.RED);
		c.add(Color.RED);
		c.add(Color.RED);
		c.add(Color.ORANGE);
		c.add(Color.ORANGE);
		c.add(Color.ORANGE);
		c.add(Color.BLACK);
		c.add(Color.GRAY);

		return c;
	}
	
	// ---- Fireworks, created by codename_b ---- //
	private Method world_getHandle = null;
	private Method nms_world_broadcastEntityEffect = null;
	private Method firework_getHandle = null;

	public void playFirework(World world, Location loc, Firework firework) throws Exception
	{
		Object nms_world = null;
		Object nms_firework = null;

		if (world_getHandle == null)
		{
			world_getHandle = getMethod(world.getClass(), "getHandle");
			firework_getHandle = getMethod(firework.getClass(), "getHandle");
		}

		nms_world = world_getHandle.invoke(world, (Object[]) null);
		nms_firework = firework_getHandle.invoke(firework, (Object[]) null);

		if (nms_world_broadcastEntityEffect == null)
		{
			nms_world_broadcastEntityEffect = getMethod(nms_world.getClass(), "broadcastEntityEffect");
		}

		nms_world_broadcastEntityEffect.invoke(nms_world, new Object[] { nms_firework, (byte) 17 });

		firework.remove();
	}

	private Method getMethod(Class<?> cl, String method)
	{
		for (Method m : cl.getMethods())
		{
			if (m.getName().equals(method))
			{
				return m;
			}
		}
		
		return null;
	}
}