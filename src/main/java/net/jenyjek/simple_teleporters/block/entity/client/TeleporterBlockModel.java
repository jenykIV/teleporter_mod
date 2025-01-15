package net.jenyjek.simple_teleporters.block.entity.client;

import net.jenyjek.simple_teleporters.SimpleTeleporters;
import net.jenyjek.simple_teleporters.block.entity.TeleporterBlockEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class TeleporterBlockModel extends GeoModel<TeleporterBlockEntity> {
    @Override
    public Identifier getModelResource(TeleporterBlockEntity teleporterBlockEntity) {
        return new Identifier(SimpleTeleporters.MOD_ID, "geo/teleporter.geo.json");
    }

    @Override
    public Identifier getTextureResource(TeleporterBlockEntity teleporterBlockEntity) {
        return new Identifier(SimpleTeleporters.MOD_ID, "textures/block/teleporteur.png");
    }

    @Override
    public Identifier getAnimationResource(TeleporterBlockEntity teleporterBlockEntity) {
        return new Identifier(SimpleTeleporters.MOD_ID, "animations/teleporter.animation.json");
    }
}
