package net.dmulloy2.swornguns.types;

/**
 * @author dmulloy2
 */

public enum Permission
{
	RELOAD;

	private final String node;

	Permission()
	{
		this.node = toString().toLowerCase().replaceAll("_", ".");
	}

	public final String getNode()
	{
		return node;
	}
}