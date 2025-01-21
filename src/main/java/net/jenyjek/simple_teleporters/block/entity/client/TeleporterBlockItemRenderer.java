package net.jenyjek.simple_teleporters.block.entity.client;

import net.jenyjek.simple_teleporters.item.custom.TeleporterBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class TeleporterBlockItemRenderer extends GeoItemRenderer<TeleporterBlockItem> {
    public TeleporterBlockItemRenderer() {
        super(new TeleporterBlockItemModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
