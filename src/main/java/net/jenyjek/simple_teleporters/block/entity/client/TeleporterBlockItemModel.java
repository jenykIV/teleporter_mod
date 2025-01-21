package net.jenyjek.simple_teleporters.block.entity.client;

import net.jenyjek.simple_teleporters.SimpleTeleporters;
import net.jenyjek.simple_teleporters.item.custom.TeleporterBlockItem;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class TeleporterBlockItemModel extends GeoModel<TeleporterBlockItem> {
    @Override
    public Identifier getModelResource(TeleporterBlockItem teleporterBlockItem) {
        return new Identifier(SimpleTeleporters.MOD_ID, "geo/teleporter.geo.json");
    }

    @Override
    public Identifier getTextureResource(TeleporterBlockItem teleporterBlockItem) {
        return new Identifier(SimpleTeleporters.MOD_ID, "textures/block/teleporter.png");
    }

    @Override
    public Identifier getAnimationResource(TeleporterBlockItem teleporterBlockItem) {
        return new Identifier(SimpleTeleporters.MOD_ID, "animations/teleporter.animation.json");
    }
}
