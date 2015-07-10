/**
 * SwornGuns - Guns in Minecraft
 * Copyright (C) 2012 - 2015 MineSworn
 * Copyright (C) 2013 - 2015 dmulloy2
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.dmulloy2.swornguns;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import lombok.Getter;
import net.dmulloy2.SwornPlugin;
import net.dmulloy2.commands.CmdHelp;
import net.dmulloy2.handlers.CommandHandler;
import net.dmulloy2.handlers.LogHandler;
import net.dmulloy2.handlers.PermissionHandler;
import net.dmulloy2.swornguns.api.SwornGunsAPI;
import net.dmulloy2.swornguns.commands.CmdList;
import net.dmulloy2.swornguns.commands.CmdReload;
import net.dmulloy2.swornguns.commands.CmdToggle;
import net.dmulloy2.swornguns.commands.CmdVersion;
import net.dmulloy2.swornguns.integration.SwornNationsHandler;
import net.dmulloy2.swornguns.integration.SwornRPGHandler;
import net.dmulloy2.swornguns.integration.UltimateArenaHandler;
import net.dmulloy2.swornguns.io.WeaponReader;
import net.dmulloy2.swornguns.listeners.EntityListener;
import net.dmulloy2.swornguns.listeners.PlayerListener;
import net.dmulloy2.swornguns.types.Bullet;
import net.dmulloy2.swornguns.types.EffectType;
import net.dmulloy2.swornguns.types.Gun;
import net.dmulloy2.swornguns.types.GunPlayer;
import net.dmulloy2.swornguns.types.MacFileFilter;
import net.dmulloy2.types.MyMaterial;
import net.dmulloy2.util.FormatUtil;
import net.dmulloy2.util.Util;

import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * @author dmulloy2
 */

public class SwornGuns extends SwornPlugin implements SwornGunsAPI
{
	// Integration
	private @Getter UltimateArenaHandler ultimateArenaHandler;
	private @Getter SwornNationsHandler swornNationsHandler;
	private @Getter SwornRPGHandler swornRPGHandler;

	// Maps
	private @Getter Map<String, Gun> loadedGuns;
	private @Getter Map<Integer, Bullet> bullets;
	private @Getter Map<String, GunPlayer> players;
	private @Getter Map<Integer, EffectType> effects;

	private @Getter List<String> disabledWorlds;
	private @Getter String prefix = FormatUtil.format("&3[&eSwornGuns&3]&e ");

	@Override
	public void onEnable()
	{
		long start = System.currentTimeMillis();

		// Configuration
		saveDefaultConfig();
		reloadConfig();

		// Initialize variables
		loadedGuns = new HashMap<>();
		bullets = new ConcurrentHashMap<>();
		players = new ConcurrentHashMap<>();
		effects = new ConcurrentHashMap<>();

		disabledWorlds = getConfig().getStringList("disabledWorlds");

		// Handlers
		logHandler = new LogHandler(this);
		commandHandler = new CommandHandler(this);
		permissionHandler = new PermissionHandler(this);

		// Integration
		setupIntegration();

		// Register commands
		commandHandler.setCommandPrefix("swornguns");

		CmdHelp cmdHelp = new CmdHelp(this);
		cmdHelp.setHeader("&3 ---- &eSwornGuns Help &3-- &e{1}&3/&e{2} &3----");
		commandHandler.registerPrefixedCommand(cmdHelp);

		commandHandler.registerPrefixedCommand(new CmdList(this));
		commandHandler.registerPrefixedCommand(new CmdReload(this));
		commandHandler.registerPrefixedCommand(new CmdToggle(this));
		commandHandler.registerPrefixedCommand(new CmdVersion(this));

		// Register events
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(new PlayerListener(this), this);
		pm.registerEvents(new EntityListener(this), this);

		// Files
		File guns = new File(getDataFolder(), "guns");
		if (! guns.exists())
		{
			guns.mkdir();
		}

		File projectile = new File(getDataFolder(), "projectile");
		if (! projectile.exists())
		{
			projectile.mkdir();
		}

		// Load guns
		loadGuns();
		loadProjectiles();

		getOnlinePlayers();

		// Update timer
		new UpdateTimer().runTaskTimer(this, 20L, 1L);

		logHandler.log("{0} has been enabled. Took {1} ms.", getDescription().getFullName(), System.currentTimeMillis() - start);
	}

	@Override
	public void onDisable()
	{
		long start = System.currentTimeMillis();

		getServer().getScheduler().cancelTasks(this);
		getServer().getServicesManager().unregisterAll(this);

		if (! bullets.isEmpty())
		{
			for (Bullet bullet : bullets.values())
			{
				bullet.destroy();
			}
		}

		if (! players.isEmpty())
		{
			for (GunPlayer player : players.values())
			{
				player.unload();
			}
		}

		clearMemory();

		logHandler.log("{0} has been disabled. Took {1} ms.", getDescription().getFullName(), System.currentTimeMillis() - start);
	}

	private final void clearMemory()
	{
		loadedGuns.clear();
		loadedGuns = null;
		effects.clear();
		effects = null;
		bullets.clear();
		bullets = null;
		players.clear();
		players = null;
	}

	private final void setupIntegration()
	{
		try
		{
			swornNationsHandler = new SwornNationsHandler(this);
		} catch (Throwable ex) { }

		try
		{
			swornRPGHandler = new SwornRPGHandler(this);
		} catch (Throwable ex) { }

		try
		{
			ultimateArenaHandler = new UltimateArenaHandler(this);
		} catch (Throwable ex) { }
	}

	public final boolean isSwornNationsEnabled()
	{
		return swornNationsHandler != null && swornNationsHandler.isEnabled();
	}

	public final boolean isSwornRPGEnabled()
	{
		return swornRPGHandler != null && swornRPGHandler.isEnabled();
	}

	public final boolean isUltimateArenaEnabled()
	{
		return ultimateArenaHandler != null && ultimateArenaHandler.isEnabled();
	}

	private static final List<String> stockProjectiles = Arrays.asList("flashbang", "grenade", "molotov", "smokegrenade");

	private void loadProjectiles()
	{
		int loaded = 0;

		File dir = new File(getDataFolder(), "projectile");

		File[] children = dir.listFiles(MacFileFilter.get());
		if (children == null || children.length == 0)
		{
			for (String s : stockProjectiles)
			{
				saveResource("projectile" + File.separator + s, false);
			}

			children = dir.listFiles(MacFileFilter.get());
		}

		for (File child : children)
		{
			WeaponReader reader = new WeaponReader(this, child);
			if (reader.isLoaded())
			{
				Gun gun = reader.getGun();
				gun.setThrowable(true);
				loadedGuns.put(child.getName(), gun);
				if (gun.isNeedsPermission())
				{
					PluginManager pm = getServer().getPluginManager();
					String node = "swornguns.fire." + child.getName();
					Permission permission = pm.getPermission(node);
					if (permission == null)
					{
						permission = new Permission(node, PermissionDefault.OP);
						pm.addPermission(permission);
					}
				}

				loaded++;
			}
			else
			{
				logHandler.log(Level.WARNING, "Could not load projectile: {0}", child.getName());
			}
		}

		logHandler.log("Loaded {0} projectiles!", loaded);
	}

	private static final List<String> stockGuns = Arrays.asList("AutoShotgun", "DoubleBarrel", "Flamethrower", "Pistol", "Rifle",
			"RocketLauncher", "Shotgun", "Sniper");

	private void loadGuns()
	{
		int loaded = 0;

		File dir = new File(getDataFolder(), "guns");
		if (! dir.exists())
			dir.mkdirs();

		File[] children = dir.listFiles(MacFileFilter.get());
		if (children == null || children.length == 0)
		{
			for (String s : stockGuns)
			{
				saveResource("guns" + File.separator + s, false);
			}

			children = dir.listFiles(MacFileFilter.get());
		}

		for (File child : children)
		{
			WeaponReader reader = new WeaponReader(this, child);
			if (reader.isLoaded())
			{
				Gun gun = reader.getGun();
				loadedGuns.put(child.getName(), gun);
				if (gun.isNeedsPermission())
				{
					PluginManager pm = getServer().getPluginManager();
					String node = "swornguns.fire." + child.getName();
					Permission permission = pm.getPermission(node);
					if (permission == null)
					{
						permission = new Permission(node, PermissionDefault.OP);
						pm.addPermission(permission);
					}
				}

				loaded++;
			}
			else
			{
				logHandler.log(Level.WARNING, "Could not load gun: {0}", child.getName());
			}
		}

		logHandler.log("Loaded {0} guns!", loaded);
	}

	private void getOnlinePlayers()
	{
		for (Player player : Util.getOnlinePlayers())
		{
			if (! players.containsKey(player.getName()))
				players.put(player.getName(), new GunPlayer(this, player));
		}
	}

	@Override
	public GunPlayer getGunPlayer(Player player)
	{
		GunPlayer gp = players.get(player.getName());
		if (gp == null)
			players.put(player.getName(), gp = new GunPlayer(this, player));

		return gp;
	}

	@Override
	@Deprecated
	public Gun getGun(ItemStack item)
	{
		for (Gun gun : loadedGuns.values())
		{
			if (gun.getMaterial().matches(item))
				return gun;
		}

		return null;
	}

	@Override
	@Deprecated
	public Gun getGun(MyMaterial material)
	{
		for (Gun gun : loadedGuns.values())
		{
			if (gun.getMaterial().equals(material))
				return gun;
		}

		return null;
	}

	@Override
	public Gun getGun(String gunName)
	{
		return loadedGuns.get(gunName);
	}

	public void onQuit(Player player)
	{
		GunPlayer gp = players.get(player.getName());
		if (gp != null)
		{
			gp.unload();
			players.remove(player.getName());
		}
	}

	@Override
	public void removeBullet(Bullet bullet)
	{
		bullets.remove(bullet.getId());
	}

	@Override
	public void addBullet(Bullet bullet)
	{
		bullets.put(bullet.getId(), bullet);
	}

	@Override
	public Bullet getBullet(Entity proj)
	{
		return bullets.get(proj.getEntityId());
	}

	@Override
	public List<Gun> getGunsByType(MyMaterial material)
	{
		List<Gun> ret = new ArrayList<>();

		for (Gun gun : loadedGuns.values())
		{
			if (gun.getMaterial().equals(material))
				ret.add(gun);
		}

		return ret;
	}

	@Override
	public List<Gun> getGunsByItem(ItemStack item)
	{
		List<Gun> ret = new ArrayList<>();

		for (Gun gun : loadedGuns.values())
		{
			if (gun.getMaterial().matches(item))
				ret.add(gun);
		}

		return ret;
	}

	@Override
	public void removeEffect(EffectType effectType)
	{
		effects.remove(effectType.getId());
	}

	@Override
	public void addEffect(EffectType effectType)
	{
		effects.put(effectType.getId(), effectType);
	}

	public static final Sound getSound(String sound)
	{
		try
		{
			return Sound.valueOf(sound.toUpperCase().replaceAll(" ", "_"));
		} catch (Throwable ex) { }
		return null;
	}

	public class UpdateTimer extends BukkitRunnable
	{
		@Override
		public void run()
		{
			if (! players.isEmpty())
			{
				for (GunPlayer player : players.values())
				{
					try
					{
						player.tick();
					}
					catch (Throwable ex)
					{
						logHandler.log(Level.WARNING, Util.getUsefulStack(ex, "ticking player " + player.getName()));
					}
				}
			}

			if (! bullets.isEmpty())
			{
				for (Entry<Integer, Bullet> entry : bullets.entrySet())
				{
					Bullet bullet = entry.getValue();

					try
					{
						bullet.tick();
					}
					catch (Throwable ex)
					{
						logHandler.log(Level.WARNING, Util.getUsefulStack(ex, "ticking bullet " + bullet));

						try
						{
							bullets.remove(entry.getKey());
							bullet.remove();
						} catch (Throwable ex1) { }
					}
				}
			}

			if (! effects.isEmpty())
			{
				for (Entry<Integer, EffectType> entry : effects.entrySet())
				{
					EffectType effect = entry.getValue();

					try
					{
						effect.tick();
					}
					catch (Throwable ex)
					{
						logHandler.log(Level.WARNING, Util.getUsefulStack(ex, "ticking effect " + effect));
						effects.remove(entry.getKey());
					}
				}
			}
		}
	}

	@Override
	public void reload()
	{
		// Config
		reloadConfig();

		// Reload guns
		loadedGuns.clear();
		loadGuns();
		loadProjectiles();

		// Refresh players
		if (! players.isEmpty())
		{
			for (GunPlayer player : players.values())
			{
				player.reload();
			}
		}

		// Get any players we may have missed
		getOnlinePlayers();
	}
}