package net.dmulloy2.swornguns.commands;

import net.dmulloy2.swornguns.SwornGuns;
import net.dmulloy2.swornguns.types.GunPlayer;

/**
 * @author dmulloy2
 */

public class CmdToggle extends SwornGunsCommand
{
	public CmdToggle(SwornGuns plugin)
	{
		super(plugin);
		this.name = "toggle";
		this.description = "Toggle gun firing";
		
		this.mustBePlayer = true;
	}

	@Override
	public void perform()
	{
		GunPlayer gp = plugin.getGunPlayer(player);
		gp.setEnabled(! gp.isEnabled());
		
		sendpMessage("&eYou have turned gun firing {0}&e!", gp.isEnabled() ? "&eon" : "&coff");
	}
}