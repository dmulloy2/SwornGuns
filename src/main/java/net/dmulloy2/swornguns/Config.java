package net.dmulloy2.swornguns;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.Sound;

import net.dmulloy2.swornapi.config.Key;
import net.dmulloy2.swornapi.config.ValueOptions;
import net.dmulloy2.swornapi.config.ValueOptions.ValueOption;

public class Config
{
	@Key("blood-effect.enabled")
	public static boolean bloodEffectEnabled = true;

	@Key("blood-effect.block-id")
	@ValueOptions(ValueOption.PARSE_MATERIAL)
	public static Material bloodEffectType = Material.REDSTONE_BLOCK;

	@Key("blood-effect.guns-only")
	public static boolean bloodEffectGunsOnly = false;

	@Key("smoke-effect")
	public static boolean smokeEffect = true;

	@Key("bullet-sound.enabled")
	public static boolean bulletSoundEnabled = true;

	@Key("bullet-sound.sound")
	@ValueOptions(ValueOption.PARSE_ENUM)
	public static Sound bulletSound = Sound.ENTITY_MAGMA_CUBE_JUMP;

	@Key("block-shatter.enabled")
	public static boolean blockShatterEnabled = true;

	@Key("block-shatter.blocks")
	public static List<Material> blockShatterBlocks = Arrays.asList(
		Material.OAK_LEAVES,
		Material.GLASS,
		Material.GLASS_PANE,
		Material.SAND,
		Material.GRAVEL
	);

	@Key("block-crack")
	public static boolean blockCrack = true;

	@Key("debug")
	public static boolean debug = false;

	@Key("disabledWorlds")
	public static List<String> disabledWorlds = Collections.emptyList();

	@Key("updateGunsOnWorldChange")
	public static boolean updateGunsOnWorldChange = false;
}
