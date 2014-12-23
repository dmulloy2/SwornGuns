/**
 * (c) 2014 dmulloy2
 */
package net.dmulloy2.swornguns.types;

import java.io.File;
import java.io.FileFilter;

/**
 * @author dmulloy2
 */

public class MacFileFilter implements FileFilter
{
	private static final MacFileFilter i = new MacFileFilter();
	public static MacFileFilter get() { return i; }
	private MacFileFilter() { }

	@Override
	public boolean accept(File file)
	{
		return ! file.getName().toLowerCase().contains("store");
	}
}