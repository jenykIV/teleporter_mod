package net.jenyjek.simple_teleporters.block.entity.client;

import net.jenyjek.simple_teleporters.block.entity.TeleporterBlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class TeleporterBlockRenderer extends GeoBlockRenderer<TeleporterBlockEntity> {
    public TeleporterBlockRenderer(BlockEntityRendererFactory.Context context) {
        super(new TeleporterBlockModel());
    }
}
