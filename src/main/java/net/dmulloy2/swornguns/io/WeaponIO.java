package net.dmulloy2.swornguns.io;

import net.dmulloy2.io.FileSerialization;
import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.types.Gun;
import net.dmulloy2.util.Util;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class WeaponIO
{
	private final SwornGuns plugin;

	public WeaponIO(SwornGuns plugin)
	{
		this.plugin = plugin;
	}

	public List<Gun> loadGuns()
	{
		File gunFolder = new File(plugin.getDataFolder(), "guns");
		return loadWeapons(gunFolder);
	}

	public List<Gun> loadProjectiles()
	{
		File projFolder = new File(plugin.getDataFolder(), "projectile");
		List<Gun> projectiles = loadWeapons(projFolder);
		projectiles.forEach(proj -> proj.setThrowable(true));
		return projectiles;
	}

	private List<Gun> loadWeapons(File folder)
	{
		List<Gun> guns = new ArrayList<>();

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
					Gun gun = new Gun(gunName, plugin);

					YamlConfiguration config = new YamlConfiguration();
					config.load(file);
					gun.loadFromConfig(config.getValues(true));

					guns.add(gun);
				}
				catch (Exception ex)
				{
					plugin.getLogHandler().log(Level.WARNING, Util.getUsefulStack(ex, "Loading gun {0}", fileName));
				}
			}
			else if (!fileName.contains("."))
			{
				// load and convert legacy gun

				try
				{
					LegacyWeaponReader legacyReader = new LegacyWeaponReader(plugin, file);
					Gun gun = legacyReader.load();
					if (gun == null)
						continue;

					guns.add(gun);

					File yamlFile = new File(folder, fileName + ".yml");
					if (!yamlFile.exists())
					{
						FileSerialization.save(gun, yamlFile);
					}

					file.delete();
				}
				catch (Exception ex)
				{
					plugin.getLogHandler().log(Level.WARNING, Util.getUsefulStack(ex, "Loading legacy gun {0}", fileName));
				}
			}
		}

		return guns;
	}
}
