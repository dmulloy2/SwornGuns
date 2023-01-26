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
package net.dmulloy2.swornguns;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import lombok.Getter;
import net.dmulloy2.swornapi.SwornPlugin;
import net.dmulloy2.swornapi.commands.CmdHelp;
import net.dmulloy2.swornapi.config.ConfigParser;
import net.dmulloy2.swornapi.handlers.CommandHandler;
import net.dmulloy2.swornapi.handlers.LogHandler;
import net.dmulloy2.swornapi.handlers.PermissionHandler;
import net.dmulloy2.swornguns.api.SwornGunsAPI;
import net.dmulloy2.swornguns.commands.CmdList;
import net.dmulloy2.swornguns.commands.CmdReload;
import net.dmulloy2.swornguns.commands.CmdToggle;
import net.dmulloy2.swornguns.commands.CmdVersion;
import net.dmulloy2.swornguns.io.WeaponIO;
import net.dmulloy2.swornguns.listeners.EntityListener;
import net.dmulloy2.swornguns.listeners.PlayerListener;
import net.dmulloy2.swornguns.types.Bullet;
import net.dmulloy2.swornguns.types.EffectData;
import net.dmulloy2.swornguns.types.Gun;
import net.dmulloy2.swornguns.types.GunPlayer;
import net.dmulloy2.swornapi.util.FormatUtil;
import net.dmulloy2.swornapi.util.Util;

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
	// Maps
	private @Getter Map<String, Gun> loadedGuns;
	private @Getter Map<Integer, Bullet> bullets;
	private @Getter Map<UUID, GunPlayer> players;
	private @Getter Map<UUID, EffectData> effects;

	private final @Getter String prefix = FormatUtil.format("&3[&eSwornGuns&3]&e ");

	@Override
	public void onEnable()
	{
		long start = System.currentTimeMillis();

		// Configuration
		saveDefaultConfig();
		reloadConfig();
		ConfigParser.parse(this, Config.class);

		// Initialize variables
		loadedGuns = new HashMap<>();
		bullets = new ConcurrentHashMap<>();
		players = new ConcurrentHashMap<>();
		effects = new ConcurrentHashMap<>();

		// Handlers
		logHandler = new LogHandler(this);
		commandHandler = new CommandHandler(this);
		permissionHandler = new PermissionHandler(this);

		// Register commands
		commandHandler.setCommandPrefix("swornguns");

		CmdHelp cmdHelp = new CmdHelp(this);
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
		loadWeapons();

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

	private void clearMemory()
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

	private static final List<String> stockProjectiles = Arrays.asList("flashbang", "grenade", "molotov", "smokegrenade");

	private static final List<String> stockGuns = Arrays.asList("AutoShotgun", "DoubleBarrel", "Flamethrower", "Pistol", "Rifle",
		"RocketLauncher", "Shotgun", "Sniper");

	private void loadWeapons()
	{
		WeaponIO weaponIO = new WeaponIO(this);

		List<Gun> guns = weaponIO.loadGuns();
		if (guns.isEmpty())
		{
			for (String stock : stockGuns)
			{
				saveResource("guns" + File.separator + stock + ".yml", false);
			}

			guns = weaponIO.loadGuns();
		}

		for (Gun gun : guns)
		{
			setupPermission(gun);
			loadedGuns.put(gun.getGunName(), gun);
		}

		List<Gun> projectiles = weaponIO.loadProjectiles();
		if (projectiles.isEmpty())
		{
			for (String stock : stockProjectiles)
			{
				saveResource("projectile" + File.separator + stock + ".yml", false);
			}

			projectiles = weaponIO.loadProjectiles();
		}

		for (Gun gun : projectiles)
		{
			setupPermission(gun);
			loadedGuns.put(gun.getGunName(), gun);
		}
	}

	private void setupPermission(Gun gun)
	{
		if (gun.isNeedsPermission())
		{
			PluginManager pm = getServer().getPluginManager();
			String node = "swornguns.fire." + gun.getGunName();
			Permission permission = pm.getPermission(node);
			if (permission == null)
			{
				permission = new Permission(node, PermissionDefault.OP);
				pm.addPermission(permission);
			}
		}
	}

	private void getOnlinePlayers()
	{
		for (Player player : Util.getOnlinePlayers())
		{
			if (! players.containsKey(player.getUniqueId()))
				players.put(player.getUniqueId(), new GunPlayer(this, player));
		}
	}

	@Override
	public GunPlayer getGunPlayer(Player player)
	{
		GunPlayer gp = players.get(player.getUniqueId());
		if (gp == null)
			players.put(player.getUniqueId(), gp = new GunPlayer(this, player));

		return gp;
	}

	@Override
	public Gun getGun(String gunName)
	{
		return loadedGuns.get(gunName);
	}

	@Override
	public Collection<Gun> getGuns()
	{
		return Collections.unmodifiableCollection(loadedGuns.values());
	}

	public void onQuit(Player player)
	{
		GunPlayer gp = players.get(player.getUniqueId());
		if (gp != null)
		{
			gp.unload();
			players.remove(player.getUniqueId());
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
	public List<Gun> getGunsByItem(ItemStack item)
	{
		List<Gun> ret = new ArrayList<>();

		for (Gun gun : loadedGuns.values())
		{
			if (gun.getMaterial() == item.getType())
				ret.add(gun);
		}

		return ret;
	}

	@Override
	public void removeEffect(EffectData effectType)
	{
		effects.remove(effectType.getId());
	}

	@Override
	public void addEffect(EffectData effectType)
	{
		effects.put(effectType.getId(), effectType);
	}

	public static Sound getSound(String sound)
	{
		try
		{
			return Sound.valueOf(sound.toUpperCase().replaceAll(" ", "_"));
		} catch (Throwable ignored) { }
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
						if (! player.isErrorReported())
						{
							logHandler.log(Level.WARNING, Util.getUsefulStack(ex, "ticking player " + player.getName()));
							player.setErrorReported(true);
						}
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
						} catch (Throwable ignored) { }
					}
				}
			}

			if (! effects.isEmpty())
			{
				Iterator<Entry<UUID, EffectData>> iter = effects.entrySet().iterator();
				while (iter.hasNext())
				{
					EffectData effect = iter.next().getValue();

					boolean finished;

					try
					{
						finished = effect.tick();
					}
					catch (Throwable ex)
					{
						logHandler.log(Level.WARNING, Util.getUsefulStack(ex, "ticking effect " + effect));
						finished = true;
					}

					if (finished)
						iter.remove();
				}
			}
		}
	}

	@Override
	public void reload()
	{
		// Config
		reloadConfig();
		ConfigParser.parse(this, Config.class);

		// Reload guns
		loadedGuns.clear();
		loadWeapons();

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
