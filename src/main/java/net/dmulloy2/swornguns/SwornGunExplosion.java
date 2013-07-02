package net.dmulloy2.swornguns;

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

public class SwornGunExplosion {
	private final Location location;
	public SwornGunExplosion(final Location location) {
		this.location = location;
	}

	public void explode() {
		World world = location.getWorld();
		Firework firework = world.spawn(location, Firework.class);
    
		FireworkMeta meta = firework.getFireworkMeta();
		meta.addEffect(getEffect());
		meta.setPower(1);
    
		firework.setFireworkMeta(meta);
		
		try {
			playFirework(world, location, firework);
		} catch (Exception e) {
			//
		}
	}

	private FireworkEffect.Type getType() {
		Random rand = new Random();
		FireworkEffect.Type type = FireworkEffect.Type.BALL_LARGE;
		if (rand.nextInt(2) == 0) {
			type = FireworkEffect.Type.BURST;
		}
	  
		return type;
	}
  
	private FireworkEffect getEffect() {
		List<Color> c = getColors();
		FireworkEffect.Type type = getType();
	  
		FireworkEffect e = FireworkEffect.builder()
				.flicker(true)
				.withColor(c)
				.withFade(c)
				.with(type)
				.trail(true)
				.build();
	  
		return e;
	}
  
	private List<Color> getColors() {
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

	/** Created by codename_b **/
	
    // internal references, performance improvements
    private Method world_getHandle = null;
    private Method nms_world_broadcastEntityEffect = null;
    private Method firework_getHandle = null;
    
    /**
     * Play a pretty firework at the location with the FireworkEffect when called
     * @param world
     * @param loc
     * @param fe
     */
    public void playFirework(World world, Location loc, Firework firework) throws Exception {
        // the net.minecraft.server.World
        Object nms_world = null;
        Object nms_firework = null;
        /*
         * The reflection part, this gives us access to funky ways of messing around with things
         */
        if (world_getHandle == null) {
            // get the methods of the craftbukkit objects
            world_getHandle = getMethod(world.getClass(), "getHandle");
            firework_getHandle = getMethod(firework.getClass(), "getHandle");
        }
        // invoke with no arguments
        nms_world = world_getHandle.invoke(world, (Object[]) null);
        nms_firework = firework_getHandle.invoke(firework, (Object[]) null);
        // null checks are fast, so having this seperate is ok
        if (nms_world_broadcastEntityEffect == null) {
            // get the method of the nms_world
            nms_world_broadcastEntityEffect = getMethod(nms_world.getClass(), "broadcastEntityEffect");
        }

        /*
         * Finally, we broadcast the entity effect then kill our fireworks object
         */
        // invoke with arguments
        nms_world_broadcastEntityEffect.invoke(nms_world, new Object[] {nms_firework, (byte) 17});
        // remove from the game
        firework.remove();
    }
    
    /**
     * Internal method, used as shorthand to grab our method in a nice friendly manner
     * @param cl
     * @param method
     * @return Method (or null)
     */
    private Method getMethod(Class<?> cl, String method) {
        for(Method m : cl.getMethods()) {
            if(m.getName().equals(method)) {
                return m;
            }
        }
        return null;
    }

}