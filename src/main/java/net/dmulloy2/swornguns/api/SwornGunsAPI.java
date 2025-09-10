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
package net.dmulloy2.swornguns.api;

import java.util.Collection;
import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.dmulloy2.swornapi.types.Reloadable;
import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.types.*;

/**
 * API Implementation for {@link SwornGuns}
 *
 * @author dmulloy2
 */

public interface SwornGunsAPI extends Reloadable
{
	GunPlayer getGunPlayer(Player player);

	GunData getGun(String gunName);

	void removeBullet(Bullet bullet);

	void addBullet(Bullet bullet);

	Bullet getBullet(Entity proj);

	List<GunData> getGunsByItem(ItemStack item);

	Collection<GunData> getGuns();
}
