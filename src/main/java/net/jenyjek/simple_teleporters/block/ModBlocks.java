package net.jenyjek.simple_teleporters.block;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.jenyjek.simple_teleporters.SimpleTeleporters;
import net.jenyjek.simple_teleporters.block.custom.TeleporterBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class ModBlocks {

    public static final Block arcstoneBlock = registerBlock("arcstone_block", new Block(FabricBlockSettings.copyOf(Blocks.STONE_BRICKS)));
    public static final Block teleporterBlock = Registry.register(Registries.BLOCK, new Identifier(SimpleTeleporters.MOD_ID, "teleporter"), new TeleporterBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque().luminance(5)));

    public static final Block chargedArcstoneBlock = registerBlock("charged_arcstone_block", new Block(FabricBlockSettings.copyOf(Blocks.STONE_BRICKS).luminance(12).emissiveLighting(Blocks::always)));

    private  static  Block registerBlock(String name, Block block){
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, new Identifier(SimpleTeleporters.MOD_ID, name), block);
    }

    private static Item registerBlockItem(String name, Block block){
        return Registry.register(Registries.ITEM, new Identifier(SimpleTeleporters.MOD_ID, name), new BlockItem(block, new FabricItemSettings()));
    }

    public static void registerModBlocks(){
        SimpleTeleporters.LOGGER.info("Registering blocks for " + SimpleTeleporters.MOD_ID);
    }
}
