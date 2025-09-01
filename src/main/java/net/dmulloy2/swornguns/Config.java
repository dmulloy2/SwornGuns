package net.dmulloy2.swornguns;

import java.util.Collections;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockType;

import net.dmulloy2.swornapi.config.Key;
import net.dmulloy2.swornapi.config.KnownRegistry;
import net.dmulloy2.swornapi.config.TransformRegistry;

public class Config
{
	@Key("blood-effect.enabled")
	public static boolean bloodEffectEnabled = true;

	@Key("blood-effect.block-id")
	@TransformRegistry(KnownRegistry.BLOCK)
	public static BlockType bloodEffectType = BlockType.REDSTONE_BLOCK;

	@Key("blood-effect.guns-only")
	public static boolean bloodEffectGunsOnly = false;

	@Key("smoke-effect")
	public static boolean smokeEffect = true;

	@Key("bullet-sound.enabled")
	public static boolean bulletSoundEnabled = true;

	@Key("bullet-sound.sound")
	@TransformRegistry(KnownRegistry.SOUND)
	public static Sound bulletSound = Sound.ENTITY_MAGMA_CUBE_JUMP;

	@Key("block-shatter.enabled")
	public static boolean blockShatterEnabled = true;

	@Key("block-shatter.blocks")
	public static List<Material> blockShatterBlocks = List.of(
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
