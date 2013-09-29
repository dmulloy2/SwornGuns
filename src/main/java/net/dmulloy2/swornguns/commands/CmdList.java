package net.dmulloy2.swornguns.commands;

import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.types.Gun;
import net.dmulloy2.swornguns.util.FormatUtil;

/**
 * @author dmulloy2
 */

public class CmdList extends SwornGunsCommand
{
	public CmdList(SwornGuns plugin)
	{
		super(plugin);
		this.name = "list";
		this.description = "Displays all available guns";
	}

	@Override
	public void perform()
	{
		sendMessage("&3====[ &eSwornGuns &3]====");

		for (Gun gun : plugin.getLoadedGuns())
		{
			sendMessage(" &b- &e{0}  &bType: &e{1}  &bAmmo: &e{2} x {3}", gun.getName(), FormatUtil.getFriendlyName(gun.getGunMaterial()),
					FormatUtil.getFriendlyName(gun.getAmmoMaterial()), gun.getAmmoAmtNeeded());
		}
	}
}