package net.dmulloy2.swornguns.types;

import lombok.Getter;

/**
 * @author dmulloy2
 */

@Getter
public enum Permission
{
	RELOAD;

	private final String node;
	private Permission()
	{
		this.node = toString().toLowerCase().replaceAll("_", ".");
	}
}