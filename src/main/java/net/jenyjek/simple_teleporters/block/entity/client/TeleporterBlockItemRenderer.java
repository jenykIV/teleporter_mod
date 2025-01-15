package net.jenyjek.simple_teleporters.block.entity.client;

import net.jenyjek.simple_teleporters.item.custom.TeleporterBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class TeleporterBlockItemRenderer extends GeoItemRenderer<TeleporterBlockItem> {
    public TeleporterBlockItemRenderer() {
        super(new TeleporterBlockItemModel());
    }
}
