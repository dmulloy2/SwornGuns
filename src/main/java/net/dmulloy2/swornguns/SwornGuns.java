package net.dmulloy2.swornguns;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.dmulloy2.swornguns.gun.Bullet;
import net.dmulloy2.swornguns.gun.EffectType;
import net.dmulloy2.swornguns.gun.Gun;
import net.dmulloy2.swornguns.gun.GunPlayer;
import net.dmulloy2.swornguns.gun.WeaponReader;
import net.dmulloy2.swornguns.listeners.EntityListener;
import net.dmulloy2.swornguns.listeners.PlayerListener;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SwornGuns extends JavaPlugin {
	private List<Bullet> bullets = new ArrayList<Bullet>();
	private List<Gun> loadedGuns = new ArrayList<Gun>();
	private List<GunPlayer> players = new ArrayList<GunPlayer>();
	private List<EffectType> effects = new ArrayList<EffectType>();

	@Override
	public void onEnable() {
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(new PlayerListener(this), this);
		pm.registerEvents(new EntityListener(this), this);
		
		getCommand("swornguns").setExecutor(new SwornGunsCommand(this));

		startup(true);
    
		getLogger().info(getDescription().getFullName() + " has been enabled");
	}
	
	@Override
	public void onDisable() {
		clearMemory(true);
    
		getLogger().info(getDescription().getFullName() + " has been disabled");
	}

	private void clearMemory(boolean init) {
		getServer().getScheduler().cancelTasks(this);
		
		for (int i=0; i<bullets.size(); i++) {
			Bullet bullet = bullets.get(i);
			bullet.destroy();
		}
		
		for (int i=0; i<players.size(); i++) {
			GunPlayer gp = players.get(i);
			gp.unload();
		}
		
		if (init) {
			loadedGuns.clear();
		}

		effects.clear();
		bullets.clear();
		players.clear();
	}

	private void startup(boolean init) {
		new UpdateTimer().runTaskTimer(this, 20L, 1L);

		File dir = new File(getPluginFolder());
		if (!dir.exists()) {
			dir.mkdir();
		}

		File dir2 = new File(getPluginFolder() + "/guns");
		if (!dir2.exists()) {
			dir2.mkdir();
		}

		dir2 = new File(getPluginFolder() + "/projectile");
		if (!dir2.exists()) {
			dir2.mkdir();
		}

		if (init) {
			loadGuns();
			loadProjectile();
		}

		getOnlinePlayers();
	}

	private final String getPluginFolder() {
		return getDataFolder().getAbsolutePath();
	}

	private void loadProjectile() {
		String path = getPluginFolder() + "/projectile";
		File dir = new File(path);
		String[] children = dir.list();
		if (children != null) {
			for (int i = 0; i < children.length; i++) {
				String filename = children[i];
				WeaponReader f = new WeaponReader(this, new File(path + "/" + filename), "gun");
				if (f.loaded) {
					f.ret.node = ("pvpgunplus." + filename.toLowerCase());
					this.loadedGuns.add(f.ret);
					f.ret.setIsThrowable(true);
					getLogger().info("Loaded Projectile: " + f.ret.getName());
				} else {
					getLogger().info("Failed To Load Projectile: " + f.ret.getName());
				}
			}
		} else {
			getLogger().warning("Could not find any projectiles to load!");
		}
	}

	private void loadGuns() {
		String path = getPluginFolder() + "/guns";
		  File dir = new File(path);
		  String[] children = dir.list();
		  if (children != null) {
			  for (int i = 0; i < children.length; i++) {
				  String filename = children[i];
				  WeaponReader f = new WeaponReader(this, new File(path + "/" + filename), "gun");
				  if (f.loaded) {
					  f.ret.node = ("pvpgunplus." + filename.toLowerCase());
					  this.loadedGuns.add(f.ret);
					  getLogger().info("Loaded Gun: " + f.ret.getName());
				  } else {
					  getLogger().info("Failed To Load Gun: " + f.ret.getName());
				  }
			  }
		  } else {
			  getLogger().warning("Could not find any guns to load!");
		  }
	}

	public void reload(boolean b) {
		clearMemory(b);
		startup(b);
	}

	public void reload() {
		reload(false);
	}

	private void getOnlinePlayers() {
		for (Player player : getServer().getOnlinePlayers()) {
			GunPlayer g = new GunPlayer(this, player);
			players.add(g);
		}
	}

	public GunPlayer getGunPlayer(Player player) {
		for (int i=0; i<players.size(); i++) {
			GunPlayer gp = players.get(i);
			if (gp.getPlayer().getName().equals(player.getName()))
				return gp;
		}
		
		return null;
	}

	public Gun getGun(int materialId) {
		for (int i=0; i<loadedGuns.size(); i++) {
			Gun gun = loadedGuns.get(i);
			if (gun.getGunMaterial() != null) {
				if (gun.getGunMaterial().getId() == materialId)
					return gun;
			}
		}

		return null;
	}

	public Gun getGun(String gunName) {
		for (int i=0; i<loadedGuns.size(); i++) {
			Gun gun = loadedGuns.get(i);
			if (gun.getName().equalsIgnoreCase(gunName) || gun.getFilename().equalsIgnoreCase(gunName))
				return gun;
		}
		
		return null;
	}

	public void onJoin(Player player) {
		if (getGunPlayer(player) == null) {
			GunPlayer gp = new GunPlayer(this, player);
			this.players.add(gp);
		}
	}

	public void onQuit(Player player) {
		for (int i=0; i<players.size(); i++) {
			GunPlayer gp = players.get(i);
			if (gp.getPlayer().getName().equals(player.getName()))
				players.remove(gp);
		}
	}

	public List<Gun> getLoadedGuns() {
		List<Gun> ret = new ArrayList<Gun>();
		for (int i=0; i<loadedGuns.size(); i++) {
			Gun gun = loadedGuns.get(i);
			ret.add(gun.copy());
		}

		return ret;
	}

	public void removeBullet(Bullet bullet) {
		this.bullets.remove(bullet);
	}

	public void addBullet(Bullet bullet) {
		this.bullets.add(bullet);
	}

	public Bullet getBullet(Entity proj) {
		for (int i=0; i<bullets.size(); i++) {
			Bullet bullet = bullets.get(i);
			if (bullet.getProjectile().getUniqueId() == proj.getUniqueId())
				return bullet;
		}

		return null;
	}

	public List<Gun> getGunsByType(ItemStack item) {
		List<Gun> ret = new ArrayList<Gun>();
		for (int i=0; i<loadedGuns.size(); i++) {
			Gun gun = loadedGuns.get(i);
			if (gun.getGunMaterial() == item.getType())
				ret.add(gun);
		}

		return ret;
	}

	public void removeEffect(EffectType effectType) {
		this.effects.remove(effectType);
	}

	public void addEffect(EffectType effectType) {
		this.effects.add(effectType);
	}

	public class UpdateTimer extends BukkitRunnable {
		@Override
		public void run() {
			for (int i=0; i<players.size(); i++) {
				GunPlayer gp = players.get(i);
				if (gp != null) {
					gp.tick();
				}
			}
			
			for (int i=0; i<bullets.size(); i++) {
				Bullet b = bullets.get(i);
				if (b != null) {
					b.tick();
				}
			}
			
			for (int i=0; i<effects.size(); i++) {
				EffectType e = effects.get(i);
				if (e != null) {
					e.tick();
				}
			}
		}
	}
}