package net.jenyjek.simple_teleporters;

import net.fabricmc.api.ClientModInitializer;
import net.jenyjek.simple_teleporters.block.entity.ModBlockEntities;
import net.jenyjek.simple_teleporters.block.entity.client.TeleporterBlockRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public class SimpleTeleportersClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRendererFactories.register(ModBlockEntities.teleporterBlockEntity, TeleporterBlockRenderer::new);
    }
}
