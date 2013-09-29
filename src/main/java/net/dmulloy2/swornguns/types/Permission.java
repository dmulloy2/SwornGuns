package net.dmulloy2.swornguns.types;

/**
 * @author dmulloy2
 */

public enum Permission
{
	RELOAD;

	public final String node;

	Permission()
	{
		this.node = toString().toLowerCase().replaceAll("_", ".");
	}

	public String getNode()
	{
		return this.node;
	}
}