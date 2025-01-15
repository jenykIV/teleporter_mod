package net.jenyjek.simple_teleporters.block.entity;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.jenyjek.simple_teleporters.SimpleTeleporters;
import net.jenyjek.simple_teleporters.block.ModBlocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {
    public static BlockEntityType<TeleporterBlockEntity> teleporterBlockEntity;

    public static void registerAllBlockEntities(){
        teleporterBlockEntity = Registry.register(Registries.BLOCK_ENTITY_TYPE,
                new Identifier(SimpleTeleporters.MOD_ID, "teleporter_block_entity"),
                FabricBlockEntityTypeBuilder.create(TeleporterBlockEntity::new,
                        ModBlocks.teleporterBlock).build());
    }
}
