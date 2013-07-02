package net.dmulloy2.swornguns;

import java.util.ArrayList;
import java.util.List;

import net.dmulloy2.swornguns.gun.Gun;
import net.dmulloy2.swornguns.gun.GunPlayer;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SwornGunsCommand implements CommandExecutor {
	private final SwornGuns plugin;
	public SwornGunsCommand(final SwornGuns plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (label.equalsIgnoreCase("swornguns")) {
			if (args.length == 0) {
				displayHelp(sender);
			} else if (args.length == 1) {
				if (args[0].equalsIgnoreCase("reload")) {
					if (sender.hasPermission("swornguns.reload")) {
						plugin.reload(true);
						sender.sendMessage(ChatColor.GREEN + "Reloaded SwornGuns!");
					} else {
						sender.sendMessage(ChatColor.RED + "Permission Denied!");
					}
				} else if (args[0].equalsIgnoreCase("toggle")) {
					if (sender.hasPermission("swornguns.toggle")) {
						if (sender instanceof Player) {
							Player player = (Player)sender;
							GunPlayer gp = plugin.getGunPlayer(player);
							if (gp != null) {
								gp.enabled = !gp.enabled;
								if (gp.enabled) {
									sender.sendMessage(ChatColor.GRAY + "You have turned guns " + ChatColor.GREEN + "on");
								} else {
									sender.sendMessage(ChatColor.GRAY + "You have turned guns " + ChatColor.RED + "off");
								}
							} else {
								sender.sendMessage(ChatColor.RED + "Error finding GunPlayer!");
							}
						} else {
							sender.sendMessage(ChatColor.RED + "You must be a player to do this!");
						}
					} else {
						sender.sendMessage(ChatColor.RED + "Permission Denied!");
					}
				} else if (args[0].equalsIgnoreCase("list")) {
					List<String> lines = new ArrayList<String>();
					
					StringBuilder line = new StringBuilder();
					line.append(ChatColor.DARK_RED + "====[ " + ChatColor.GOLD + "SwornGuns" + ChatColor.DARK_RED + " ]====");
					lines.add(line.toString());
					
					for (Gun gun : plugin.getLoadedGuns()) {
						line = new StringBuilder();
						line.append(" -" + gun.getName() + ChatColor.YELLOW + "(" + Integer.toString(gun.getGunType()) + ")" + ChatColor.GRAY + " AMMO: " 
								+ ChatColor.RED + gun.getAmmoMaterial().toString() + ChatColor.GRAY + "  amt# " + ChatColor.RED + Integer.toString(gun.getAmmoAmtNeeded()));
						lines.add(line.toString());
					}
					
					line = new StringBuilder();
					line.append(ChatColor.DARK_RED + "====================");
					lines.add(line.toString());
					
					for (String s : lines) {
						sender.sendMessage(s);
					}
				} else {
					displayHelp(sender);
				}
			} else {
				displayHelp(sender);
			}
		}
		
		return true;
	}
	
	private void displayHelp(CommandSender sender) {
		sender.sendMessage(ChatColor.DARK_RED + "====[ " + ChatColor.GOLD + "SwornGuns" + ChatColor.DARK_RED + " ]====");
		sender.sendMessage(ChatColor.RED + "/swornguns " + ChatColor.DARK_RED + "help" + ChatColor.YELLOW + " display this help menu");
		sender.sendMessage(ChatColor.RED + "/swornguns " + ChatColor.DARK_RED + "reload" + ChatColor.YELLOW + " reload the plugin");
		sender.sendMessage(ChatColor.RED + "/swornguns " + ChatColor.DARK_RED+ "list" + ChatColor.YELLOW + " list the loaded guns");
		sender.sendMessage(ChatColor.RED + "/swornguns " + ChatColor.DARK_RED + "toggle" + ChatColor.YELLOW + " toggle gun firing");
	}
}