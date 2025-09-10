package net.dmulloy2.swornguns.io;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.configuration.file.YamlConfiguration;

import net.dmulloy2.swornapi.util.Util;
import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.types.GunData;

public class WeaponIO
{
	private final SwornGuns plugin;

	public WeaponIO(SwornGuns plugin)
	{
		this.plugin = plugin;
	}

	public List<GunData> loadGuns()
	{
		File gunFolder = new File(plugin.getDataFolder(), "guns");
		return loadWeapons(gunFolder, false);
	}

	public List<GunData> loadProjectiles()
	{
		File projFolder = new File(plugin.getDataFolder(), "projectile");
		return loadWeapons(projFolder, true);
	}

	private List<GunData> loadWeapons(File folder, boolean isThrowable)
	{
		List<GunData> guns = new ArrayList<>();

		File[] files = folder.listFiles();
		if (files == null || files.length == 0)
			return guns;

		for (File file : files)
		{
			String fileName = file.getName();
			if (fileName.startsWith("."))
				continue;

			if (fileName.endsWith(".yml"))
			{
				// load new style gun
				String gunName = fileName.split("\\.")[0].toLowerCase();

				try
				{
					GunData gun = new GunData(gunName, isThrowable);

					YamlConfiguration config = new YamlConfiguration();
					config.load(file);
					gun.loadFromConfig(config.getValues(true), plugin.getLogHandler());

					guns.add(gun);
				}
				catch (Exception ex)
				{
					plugin.getLogHandler().log(Level.WARNING, Util.getUsefulStack(ex, "Loading gun {0}", fileName));
				}
			}
		}

		return guns;
	}
}
