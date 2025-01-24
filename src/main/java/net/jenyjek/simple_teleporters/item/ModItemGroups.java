package net.jenyjek.simple_teleporters.item;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.jenyjek.simple_teleporters.SimpleTeleporters;
import net.jenyjek.simple_teleporters.block.ModBlocks;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {
    public static final ItemGroup SimpleTeleportersGroup = Registry.register(Registries.ITEM_GROUP,
            new Identifier(SimpleTeleporters.MOD_ID, "simple_teleporters_group"),
            FabricItemGroup.builder().displayName(Text.translatable("itemgroup.simple_teleporters_group"))
                    .icon(() -> new ItemStack(ModItems.arcStone)).entries((displayContext, entries) -> {
                        entries.add(ModItems.oxidisedCoppperIngot);
                        entries.add(ModItems.arcStone);
                        entries.add(ModBlocks.arcstoneBlock);
                        entries.add(ModItems.cartridge);
                        entries.add(ModItems.teleporter);
                        entries.add(ModBlocks.chargedArcstoneBlock);
                    }).build());

    public static void registerItemGroups(){
        SimpleTeleporters.LOGGER.info("Registering custom creative tab for " + SimpleTeleporters.MOD_ID);
    }
}
