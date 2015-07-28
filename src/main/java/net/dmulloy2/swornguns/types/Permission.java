package net.dmulloy2.swornguns.types;

import lombok.Getter;
import net.dmulloy2.types.IPermission;

/**
 * @author dmulloy2
 */

@Getter
public enum Permission implements IPermission
{
	RELOAD,
	;

	private final String node;
	private Permission()
	{
		this.node = toString().toLowerCase().replaceAll("_", ".");
	}
}