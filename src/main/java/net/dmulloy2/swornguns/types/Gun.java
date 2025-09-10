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
package net.dmulloy2.swornguns.types;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import net.dmulloy2.swornapi.util.FormatUtil;
import net.dmulloy2.swornapi.util.InventoryUtil;
import net.dmulloy2.swornapi.util.MaterialUtil;
import net.dmulloy2.swornapi.util.Util;
import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.events.SwornGunFireEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * @author dmulloy2
 */

@Getter
@Accessors(fluent = true)
@EqualsAndHashCode(of={"data", "owner"})
@ToString(of={"data", "owner"})
public class Gun
{
	private int roundsFired;
	private int gunReloadTimer;
	private int timer;
	private int lastFired;
	private int ticks;
	private int heldDownTicks;
	private int clipRemaining = -1;
	private int bulletsShot;

	private boolean firing;
	private boolean reloading;
	private boolean changed;

	private @Setter boolean dirty = true;

	private final Player player;
	private final GunData data;
	private final GunPlayer owner;
	private final SwornGuns plugin;

	public Gun(GunData data, GunPlayer owner, SwornGuns plugin)
	{
		this.data = data;
		this.owner = owner;
		this.plugin = plugin;
		this.player = owner.player();
	}

	/**
	 * Handles the actual shooting of the gun
	 */
	public final void shoot()
	{
		if (reloading || owner == null)
			return;

		if (!player.isOnline() || player.getHealth() <= 0.0D)
		{
			return;
		}

		if (data.reloadType() == ReloadType.CLIP && clipRemaining == -1)
		{
			if (data.initialClip() < 0 || data.initialClip() > data.clipSize())
				clipRemaining = data.clipSize();
			else
				clipRemaining = data.initialClip();
		}

		SwornGunFireEvent event = new SwornGunFireEvent(this, data.ammoAmtNeeded());
		plugin.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled())
			return;

		int ammoAmtNeeded = event.getAmmoNeeded();
		if (ammoAmtNeeded != 0 && !owner.checkAmmo(this, ammoAmtNeeded) && clipRemaining <= 0)
		{
			player.playSound(owner.player().getLocation(), Sound.ENTITY_ITEM_BREAK, 20.0F, 20.0F);

			if (data.outOfAmmoMessage().isEmpty())
				player.sendMessage(plugin.getPrefix()
					+ FormatUtil.format("&eThis gun needs &b{0}&e!", MaterialUtil.getName(data.ammo())));
			else
				player.sendMessage(plugin.getPrefix()
					+ FormatUtil.format(data.outOfAmmoMessage(), MaterialUtil.getName(data.ammo())));

			finishShooting();
			return;
		}

		if (data.reloadType() == ReloadType.CLIP)
		{
			clipRemaining--;
			if (clipRemaining <= 0 && owner.checkAmmo(this, 1))
			{
				owner.removeAmmo(this, 1);
				reloadGun();
				return;
			}
		}
		else
		{
			owner.removeAmmo(this, ammoAmtNeeded);

			if (roundsFired >= data.maxClipSize() && data.hasClip())
			{
				reloadGun();
				return;
			}
		}

		doRecoil();

		this.changed = true;
		this.roundsFired++;

		for (Sound sound : data.gunSound())
		{
			if (sound != null)
			{
				if (data.localGunSound())
					player.playSound(player.getLocation(), sound, (float) data.gunVolume(), (float) data.gunPitch());
				else
					player.getWorld().playSound(player.getLocation(), sound, (float) data.gunVolume(), (float) data.gunPitch());
			}
		}

		double accuracy = data.accuracy();
		if (owner.player().isSneaking() && data.accuracy_crouched() > -1.0D)
		{
			accuracy = data.accuracy_crouched();
		}

		if (owner.aimedIn() && data.accuracy_aimed() > -1.0D)
		{
			accuracy = data.accuracy_aimed();
		}

		for (int i = 0; i < data.bulletsPerClick(); i++)
		{
			int acc = (int) (accuracy * 1000.0D);

			if (acc <= 0)
				acc = 1;

			Location ploc = player.getLocation();

			Random rand = new Random();

			double dir = -ploc.getYaw() - 90.0F;
			double pitch = -ploc.getPitch();
			double xwep = (rand.nextInt(acc) - rand.nextInt(acc) + 0.5D) / 1000.0D;
			double ywep = (rand.nextInt(acc) - rand.nextInt(acc) + 0.5D) / 1000.0D;
			double zwep = (rand.nextInt(acc) - rand.nextInt(acc) + 0.5D) / 1000.0D;
			double xd = Math.cos(Math.toRadians(dir)) * Math.cos(Math.toRadians(pitch)) + xwep;
			double yd = Math.sin(Math.toRadians(pitch)) + ywep;
			double zd = -Math.sin(Math.toRadians(dir)) * Math.cos(Math.toRadians(pitch)) + zwep;

			Vector vec = new Vector(xd, yd, zd);
			vec.multiply(data.bulletSpeed());

			Bullet bullet = new Bullet(plugin, owner, this, vec);
			plugin.addBullet(bullet);
			bullet.runTaskTimer(plugin, 1L, 1L);
		}

		if (roundsFired >= data.maxClipSize() && data.hasClip())
			reloadGun();
	}

	/**
	 * Handles bullet movement, cooldowns, etc
	 */
	public final boolean tick()
	{
		boolean changed = dirty;
		this.dirty = false;

		this.ticks++;
		this.lastFired++;
		this.timer--;
		this.gunReloadTimer--;

		if (gunReloadTimer < 0)
		{
			if (reloading)
			{
				finishReloading();
				this.reloading = false;
				changed = true;
			}
		}

		gunSounds();

		if (lastFired > 6)
		{
			this.heldDownTicks = 0;
		}

		if ((heldDownTicks >= 2 && timer <= 0) || firing && ! reloading)
		{
			if (data.roundsPerBurst() > 1)
			{
				if (ticks % data.bulletDelay() == 0)
				{
					this.bulletsShot++;

					if (bulletsShot <= data.roundsPerBurst())
					{
						shoot();
					}
					else
					{
						finishShooting();
					}
					changed = true;
				}
			}
			else
			{
				shoot();
				finishShooting();
				changed = true;
			}
		}

		if (reloading)
		{
			this.firing = false;
			changed = true;
		}

		return changed;
	}

	public double calculateDamage(boolean isHeadshot)
	{
		double mult = isHeadshot && data.canHeadshot() ? 1.5D : 1.0D;
		return data.gunDamage() * mult;
	}

	public void armorPenetration(Damageable hurt, double eventDamage)
	{
		double armorPenetration = data.armorPenetration();
		if (armorPenetration > 0.0D && hurt.getHealth() - eventDamage > 0.0D)
		{
			double newHealth = Math.max(0, hurt.getHealth() - armorPenetration);
			hurt.setHealth(newHealth);
		}
	}

	public boolean tryReload()
	{
		if (!data.hasClip() || !changed || !data.reloadGunOnDrop())
		{
			return false;
		}

		reloadGun();
		return true;
	}

	boolean fire()
	{
		if (timer <= 0)
		{
			this.heldDownTicks++;
			this.lastFired = 0;
			this.firing = true;
			return true;
		}

		return false;
	}

	/**
	 * Reloads the gun
	 */
	public void reloadGun()
	{
		this.reloading = true;
		this.gunReloadTimer = data.reloadTime();

		if (data.reloadType() == ReloadType.CLIP)
			this.clipRemaining = data.clipSize();
	}

	/**
	 * Plays various gun sounds
	 */
	private void gunSounds()
	{
		try
		{
			if (reloading)
			{
				int amtReload = data.reloadTime() - gunReloadTimer;
				if (data.reloadType() == ReloadType.BOLT)
				{
					if (amtReload == 6)
						owner.player().playSound(owner.player().getLocation(), Sound.BLOCK_WOODEN_DOOR_OPEN, 2.0F, 1.5F);
					if (amtReload == data.reloadTime() - 4)
						owner.player().playSound(owner.player().getLocation(), Sound.BLOCK_WOODEN_DOOR_CLOSE, 1.0F, 1.5F);
				}
				else if (data.reloadType() == ReloadType.PUMP || data.reloadType() == ReloadType.INDIVIDUAL_BULLET)
				{
					int rep = (data.reloadTime() - 10) / data.maxClipSize();
					if (amtReload >= 5 && amtReload <= data.reloadTime() - 5 && amtReload % rep == 0)
					{
						owner.player().playSound(owner.player().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F);
						owner.player().playSound(owner.player().getLocation(), Sound.BLOCK_NOTE_BLOCK_SNARE, 1.0F, 2.0F);
					}

					if (amtReload == data.reloadTime() - 3)
						owner.player().playSound(owner.player().getLocation(), Sound.BLOCK_PISTON_EXTEND, 1.0F, 2.0F);
					else if (amtReload == data.reloadTime() - 1)
						owner.player().playSound(owner.player().getLocation(), Sound.BLOCK_PISTON_CONTRACT, 1.0F, 2.0F);
				}
				else
				{
					if (amtReload == 6)
					{
						owner.player().playSound(owner.player().getLocation(), Sound.BLOCK_FIRE_AMBIENT, 2.0F, 2.0F);
						owner.player().playSound(owner.player().getLocation(), Sound.BLOCK_WOODEN_DOOR_OPEN, 1.0F, 2.0F);
					}

					if (amtReload == data.reloadTime() / 2)
						owner.player().playSound(owner.player().getLocation(), Sound.BLOCK_PISTON_CONTRACT, 0.33F, 2.0F);

					if (amtReload == data.reloadTime() - 4)
					{
						owner.player().playSound(owner.player().getLocation(), Sound.BLOCK_FIRE_AMBIENT, 2.0F, 2.0F);
						owner.player().playSound(owner.player().getLocation(), Sound.BLOCK_WOODEN_DOOR_CLOSE, 1.0F, 2.0F);
					}
				}
			}
			else
			{
				if (data.reloadType() == ReloadType.PUMP)
				{
					if (timer == 8)
					{
						owner.player().playSound(owner.player().getLocation(), Sound.BLOCK_PISTON_EXTEND, 1.0F, 2.0F);
					}

					if (timer == 6)
					{
						owner.player().playSound(owner.player().getLocation(), Sound.BLOCK_PISTON_CONTRACT, 1.0F, 2.0F);
					}
				}

				if (data.reloadType() == ReloadType.BOLT)
				{
					if (timer == data.bulletDelayTime() - 4)
						owner.player().playSound(owner.player().getLocation(), Sound.BLOCK_WOODEN_DOOR_OPEN, 2.0F, 1.25F);
					if (timer == 6)
						owner.player().playSound(owner.player().getLocation(), Sound.BLOCK_WOODEN_DOOR_CLOSE, 1.0F, 1.25F);
				}
			}
		}
		catch (Throwable ex)
		{
			plugin.getLogHandler().debug(Level.WARNING, Util.getUsefulStack(ex, "playing gun sounds"));
		}
	}

	private void doRecoil()
	{
		if (data.recoil() == 0.0D)
		{
			return;
		}

		Location ploc = player.getLocation();
		double dir = -ploc.getYaw() - 90.0F;
		double pitch = -ploc.getPitch() - 180.0F;
		double xd = Math.cos(Math.toRadians(dir)) * Math.cos(Math.toRadians(pitch));
		double yd = Math.sin(Math.toRadians(pitch));
		double zd = -Math.sin(Math.toRadians(dir)) * Math.cos(Math.toRadians(pitch));

		Vector vec = new Vector(xd, yd, zd);
		vec.multiply(data.recoil() / 2.0D).setY(0);

		player.setVelocity(player.getVelocity().add(vec));
	}

	/**
	 * Does knockback for an entity
	 *
	 * @param entity {@link Entity} to do knock back for
	 * @param speed Knockback speed
	 */
	public void doKnockback(Entity entity, Vector speed)
	{
		if (data.knockback() > 0.0D)
		{
			speed.normalize().setY(0.6D).multiply(data.knockback() / 4.0D);
			entity.setVelocity(speed);
		}
	}

	/**
	 * Finishes reloading
	 */
	public void finishReloading()
	{
		this.bulletsShot = 0;
		this.roundsFired = 0;
		this.changed = false;
		this.gunReloadTimer = 0;
	}

	/**
	 * Finishes shooting
	 */
	private void finishShooting()
	{
		this.bulletsShot = 0;
		this.timer = data.bulletDelayTime();
		this.firing = false;
	}

	void applyCustomName(ItemStack item)
	{
		ItemMeta meta = item.getItemMeta();
		if (meta == null)
		{
			return;
		}

		if (owner.canFireGun(data))
		{
			meta.customName(buildDisplayName());

			List<Component> lore = data.loreComponents();
			if (lore != null && ! lore.isEmpty())
				meta.lore(data.loreComponents());

			item.setItemMeta(meta);
		}
		else if (meta.hasCustomName() && FormatUtil.componentContains(meta.customName(), "\u00AB"))
		{
			meta.customName(null);
			meta.lore(null);
			item.setItemMeta(meta);
		}
	}

	private Component buildDisplayName()
	{
		String gunName = data.displayName();

		if (!data.hasClip())
		{
			return Component.text(gunName, Style.style(NamedTextColor.YELLOW, TextDecoration.ITALIC.withState(false)));
		}

		Material ammo = data.ammo();
		int maxClip = data.maxClipSize();
		int ammoAmtNeeded = Math.max(1, data.ammoAmtNeeded());
		int amount = (int) Math.floor(InventoryUtil.amount(player.getInventory(), ammo) / (double) ammoAmtNeeded);

		int leftInClip, ammoLeft;
		if (data.reloadType() == ReloadType.CLIP)
		{
			leftInClip = Math.max(0, clipRemaining);
			ammoLeft = (Math.max(1, data.clipSize()) * amount) - leftInClip;
		}
		else
		{
			ammoLeft = Math.max(0, amount - maxClip + roundsFired);
			leftInClip = amount - ammoLeft;
		}

		TextComponent.Builder builder = Component.text(gunName, Style.style(NamedTextColor.YELLOW, TextDecoration.ITALIC.withState(false))).toBuilder();

		if (reloading)
		{
			StringBuilder reload = new StringBuilder();
			int scale = 4;
			int reloadTime = Math.max(1, data.reloadTime());
			int bars = (int) Math.round(scale - (((double)gunReloadTimer * scale) / reloadTime));
			reload.append("\u25AA".repeat(Math.max(0, bars)));

			int left = scale - bars;
			reload.append("\u25AB".repeat(Math.max(0, left)));

			builder.append(Component.text("    " + reload.reverse() + "RELOADING" + reload, NamedTextColor.RED));
		}
		else
		{
			builder.append(Component.text("    \u00AB" + leftInClip + " \uFFE8 " + ammoLeft + "\u00BB", NamedTextColor.RED));
		}

		return builder.build();
	}
}
