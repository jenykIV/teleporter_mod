package net.jenyjek.simple_teleporters;

import net.fabricmc.api.ModInitializer;

import net.jenyjek.simple_teleporters.block.ModBlocks;
import net.jenyjek.simple_teleporters.block.entity.ModBlockEntities;
import net.jenyjek.simple_teleporters.item.ModItemGroups;
import net.jenyjek.simple_teleporters.item.ModItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.GeckoLib;

public class SimpleTeleporters implements ModInitializer {

	public static final String MOD_ID = "simple_teleporters";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("loading mod: " + MOD_ID);
		ModItemGroups.registerItemGroups();
		ModItems.registerModItems();
		ModBlocks.registerModBlocks();
		ModBlockEntities.registerAllBlockEntities();
		LOGGER.info("successfully loaded mod: " + MOD_ID);
		LOGGER.info("Loading Gecko...");
		GeckoLib.initialize();
		LOGGER.info("Gecko initialised");
	}
}