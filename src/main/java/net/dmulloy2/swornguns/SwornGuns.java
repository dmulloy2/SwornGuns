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

import lombok.Getter;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitRunnable;

import net.dmulloy2.swornapi.SwornPlugin;
import net.dmulloy2.swornapi.commands.CmdHelp;
import net.dmulloy2.swornapi.config.ConfigParser;
import net.dmulloy2.swornapi.handlers.CommandHandler;
import net.dmulloy2.swornapi.handlers.LogHandler;
import net.dmulloy2.swornapi.handlers.PermissionHandler;
import net.dmulloy2.swornapi.util.FormatUtil;
import net.dmulloy2.swornapi.util.Util;
import net.dmulloy2.swornguns.api.SwornGunsAPI;
import net.dmulloy2.swornguns.commands.CmdList;
import net.dmulloy2.swornguns.commands.CmdReload;
import net.dmulloy2.swornguns.commands.CmdToggle;
import net.dmulloy2.swornguns.commands.CmdVersion;
import net.dmulloy2.swornguns.io.WeaponIO;
import net.dmulloy2.swornguns.listeners.EntityListener;
import net.dmulloy2.swornguns.listeners.InventoryListener;
import net.dmulloy2.swornguns.listeners.PlayerListener;
import net.dmulloy2.swornguns.types.*;

/**
 * @author dmulloy2
 */

public class SwornGuns extends SwornPlugin implements SwornGunsAPI
{
	// Maps
	private @Getter Map<String, GunData> loadedGuns;
	private @Getter Map<UUID, Bullet> bullets;
	private @Getter Map<UUID, GunPlayer> players;

	private final @Getter String prefix = FormatUtil.format("&3[&eSwornGuns&3]&e ");

	@Override
	public void onEnable()
	{
		long start = System.currentTimeMillis();

		// Handlers
		logHandler = new LogHandler(this);
		commandHandler = new CommandHandler(this);
		permissionHandler = new PermissionHandler(this);

		// Configuration
		saveDefaultConfig();
		reloadConfig();
		ConfigParser.parse(this, Config.class);

		// Initialize variables
		loadedGuns = new HashMap<>();
		bullets = new ConcurrentHashMap<>();
		players = new ConcurrentHashMap<>();

		// Register commands
		commandHandler.setCommandPrefix("swornguns", "gun", "guns");

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
		pm.registerEvents(new InventoryListener(this), this);

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

		logHandler.log("SwornGuns has been enabled. Took {0} ms.", System.currentTimeMillis() - start);
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

		logHandler.log("SwornGuns has been disabled. Took {0} ms.", System.currentTimeMillis() - start);
	}

	@Override
	public boolean isPaperPlugin()
	{
		return true;
	}

	private void clearMemory()
	{
		loadedGuns.clear();
		loadedGuns = null;
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

		List<GunData> guns = weaponIO.loadGuns();
		if (guns.isEmpty())
		{
			for (String stock : stockGuns)
			{
				saveResource("guns" + File.separator + stock + ".yml", false);
			}

			guns = weaponIO.loadGuns();
		}

		for (GunData gun : guns)
		{
			setupPermission(gun);
			loadedGuns.put(gun.gunName(), gun);
		}

		List<GunData> projectiles = weaponIO.loadProjectiles();
		if (projectiles.isEmpty())
		{
			for (String stock : stockProjectiles)
			{
				saveResource("projectile" + File.separator + stock + ".yml", false);
			}

			projectiles = weaponIO.loadProjectiles();
		}

		for (GunData gun : projectiles)
		{
			setupPermission(gun);
			loadedGuns.put(gun.gunName(), gun);
		}
	}

	private void setupPermission(GunData gun)
	{
		if (gun.needsPermission())
		{
			PluginManager pm = getServer().getPluginManager();
			String node = "swornguns.fire." + gun.gunName();
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
		for (Player player : Bukkit.getOnlinePlayers())
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
	public GunData getGun(String gunName)
	{
		return loadedGuns.get(gunName);
	}

	@Override
	public Collection<GunData> getGuns()
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
		bullets.remove(bullet.id());
	}

	@Override
	public void addBullet(Bullet bullet)
	{
		bullets.put(bullet.id(), bullet);
	}

	@Override
	public Bullet getBullet(Entity proj)
	{
		return bullets.get(proj.getUniqueId());
	}

	@Override
	public List<GunData> getGunsByItem(ItemStack item)
	{
		List<GunData> ret = new ArrayList<>();

		for (GunData gun : loadedGuns.values())
		{
			if (gun.material() == item.getType())
				ret.add(gun);
		}

		return ret;
	}

	public class UpdateTimer extends BukkitRunnable
	{
		@Override
		public void run()
		{
			Iterator<GunPlayer> playerIter = players.values().iterator();
			while (playerIter.hasNext())
			{
				GunPlayer player = playerIter.next();

				try
				{
					if (!player.tick())
					{
						playerIter.remove();
					}
				}
				catch (Throwable ex)
				{
					if (! player.errorReported())
					{
						logHandler.log(Level.WARNING, Util.getUsefulStack(ex, "ticking player " + player.getName()));
						player.errorReported(true);
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
