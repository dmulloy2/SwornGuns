package net.dmulloy2.swornguns.types;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.dmulloy2.swornapi.util.Util;

public class EffectTask extends BukkitRunnable
{
	private EffectData data;
	private int duration;
	private Location location;

	public EffectTask(EffectData data, Location location)
	{
		this.data = data;
		this.location = location;
		this.duration = data.maxDuration();
	}

	@Override
	public void run()
	{
		duration--;

		if (duration < 0)
		{
			cancel();
			return;
		}

		double yRad = data.radius();
		if (data.type().equals(Effect.MOBSPAWNER_FLAMES))
		{
			yRad = 0.75D;

			World world = location.getWorld();
			if (world != null)
			{
				world.getNearbyPlayers(location, data.radius());
				for (Player player : world.getPlayers())
				{
					if (location.distance(player.getLocation()) < data.radius())
					{
						player.setFireTicks(20);
					}
				}
			}
		}

		double radius = data.radius();
		for (double i = -radius; i <= radius; i += 1.0D)
		{
			for (double ii = -radius; ii <= radius; ii += 1.0D)
			{
				for (double iii = 0.0D; iii <= yRad * 2.0D; iii += 1.0D)
				{
					int rand = Util.random(8);
					if (rand == 2)
					{
						Location newloc = location.clone().add(i, iii - 1.0D, ii);
						Location testLoc = location.clone().add(0.0D, yRad - 1.0D, 0.0D);
						if (newloc.distance(testLoc) <= radius)
						{
							byte dat = data.specialDat() > -1 ? data.specialDat() : (byte) Util.random(8);
							newloc.getWorld().playEffect(newloc, data.type(), dat);
						}
					}
				}
			}
		}
	}
}
