package net.dmulloy2.swornguns.types;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import io.papermc.paper.registry.RegistryKey;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;

import net.dmulloy2.swornapi.handlers.LogHandler;
import net.dmulloy2.swornapi.io.FileSerialization;
import net.dmulloy2.swornapi.util.FormatUtil;
import net.dmulloy2.swornapi.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

@Getter
@Accessors(fluent = true)
@EqualsAndHashCode(of={"gunName", "isThrowable"})
@ToString(of={"gunName", "isThrowable"})
@SuppressWarnings("unchecked")
public class GunData implements ConfigurationSerializable
{
	private boolean canHeadshot;
	private boolean isThrowable;
	private boolean hasSmokeTrail;
	private boolean localGunSound;
	private boolean canAimLeft;
	private boolean canAimRight;
	private boolean canGoPastMaxDistance;
	private boolean needsPermission;
	private boolean canFireRight;
	private boolean canFireLeft;
	private boolean hasClip = true;
	private boolean reloadGunOnDrop = true;
	private boolean unlimitedAmmo;
	private boolean warnIfNoPermission;

	private Material material;
	private Material ammo;

	private int ammoAmtNeeded;
	private int roundsPerBurst;
	private int reloadTime;
	private int maxDistance;
	private int bulletsPerClick;
	private int bulletDelay = 2;
	private int releaseTime = -1;
	private int maxClipSize = 30;
	private int bulletDelayTime = 10;

	private int priority;

	private int clipSize;
	private int initialClip = -1;

	private double gunDamage;
	private double armorPenetration;
	private double explosionDamage = -1.0D;
	private double bulletSpeed;
	private double accuracy;
	private double accuracy_aimed = -1.0D;
	private double accuracy_crouched = -1.0D;
	private double explodeRadius;
	private double fireRadius;
	private double flashRadius;
	private double knockback;
	private double recoil;

	private double gunVolume = 1.0D;
	private double gunPitch = 2.0D;

	private String projType = "snowball";
	private transient Class<? extends Projectile> projectileClass = Snowball.class;

	private String explosionType = "FIREWORK";
	private ReloadType reloadType = ReloadType.NORMAL;

	private transient String gunName;
	private String displayName;
	private String outOfAmmoMessage = "";

	private EffectData releaseEffect;

	private transient List<Sound> gunSound = new ArrayList<>();

	private List<String> lore = new ArrayList<>();
	private transient List<Component> loreComponents = new ArrayList<>();

	public GunData(String gunName, boolean throwable)
	{
		this.gunName = gunName;
		this.isThrowable = throwable;
	}

	public void loadFromConfig(Map<String, Object> config, LogHandler logger)
	{
		// deserialize all the primitive values
		FileSerialization.deserialize(this, config);

		if (displayName == null || displayName.isEmpty())
		{
			this.displayName = FormatUtil.capitalize(gunName);
		}

		readProjectileType(logger);
		readGunSounds(config, logger);
		readLore(config, logger);
	}

	private void readProjectileType(LogHandler logger)
	{
		if (projType == null || projType.isEmpty())
		{
			this.projType = "snowball";
			this.projectileClass = Snowball.class;
			return;
		}

		String newProjType = switch (projType.toLowerCase())
		{
			case "enderpearl" -> "ender_pearl";
			case "fish", "fishhook" -> "fishing_bobber";
			case "largefireball" -> "fireball";
			case "smallfireball" -> "small_fireball";
			case "thrownexpbottle" -> "experience_bottle";
			case "thrownpotion" -> "splash_potion";
			case "witherskull" -> "wither_skull";
			default -> null;
		};

		if (newProjType != null)
		{
			logger.warn("[{0}] Converting legacy projectile type \"{1}\" to \"{2}\"."
				+ "Update the configuration as this conversion will be removed in a future update.",
				gunName, projType, newProjType);
			this.projType = newProjType;
		}

		EntityType bulletType = Util.getRegistryEntry(RegistryKey.ENTITY_TYPE, projType);
		if (bulletType != null && bulletType.getEntityClass() != null && Projectile.class.isAssignableFrom(bulletType.getEntityClass()))
		{
			this.projectileClass = (Class<? extends Projectile>) bulletType.getEntityClass();
		}
		else
		{
			logger.warn("[{0}] \"{1}\" is not a valid projectile type. Defaulting to snowball", gunName, projType);
			this.projType = "snowball";
			this.projectileClass = Snowball.class;
		}
	}

	private void readLore(Map<String, Object> config, LogHandler logger)
	{
		List<String> loreLines = (List<String>) config.get("lore");
		if (loreLines == null)
		{
			return;
		}

		for (String line : loreLines)
		{
			try
			{
				if (line.contains("{"))
				{
					loreComponents.add(GsonComponentSerializer.gson().deserialize(line));
				}
				else if (line.contains("&"))
				{
					loreComponents.add(LegacyComponentSerializer.legacyAmpersand().deserialize(line));
				}
				else
				{
					loreComponents.add(MiniMessage.miniMessage().deserialize(line));
				}
			}
			catch (Exception ex)
			{
				logger.log(Level.WARNING, "Invalid lore line \"{0}\" configured for gun {1}", line, gunName);
			}
		}
	}

	private void readGunSounds(Map<String, Object> config, LogHandler logger)
	{
		List<String> gunSoundNames = (List<String>) config.get("gunSound");
		if (gunSoundNames == null)
		{
			return;
		}

		gunSound.clear();
		for (String name : gunSoundNames)
		{
			try
			{
				Sound sound = Util.getRegistryEntry(RegistryKey.SOUND_EVENT, name);
				if (sound != null)
				{
					gunSound.add(sound);
				}
				else
				{
					logger.warn("Invalid sound \"{0}\" configured for gun {1}", name, gunName);
				}
			}
			catch (Exception ex)
			{
				logger.warn(ex, "Invalid sound \"{0}\" configured for gun {1}", name, gunName);
			}
		}
	}

	@Override
	public Map<String, Object> serialize()
	{
		return FileSerialization.serialize(this);
	}
}
