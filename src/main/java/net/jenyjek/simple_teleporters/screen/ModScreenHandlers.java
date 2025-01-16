package net.jenyjek.simple_teleporters.screen;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.jenyjek.simple_teleporters.SimpleTeleporters;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class ModScreenHandlers {
    public static final ScreenHandlerType<TeleporterScreenHandler> teleporterScreen =
            Registry.register(Registries.SCREEN_HANDLER, new Identifier(SimpleTeleporters.MOD_ID, "teleporter_screen"),
                    new ExtendedScreenHandlerType<>(TeleporterScreenHandler::new));

    public static void registerAllScreenHandlers(){
    }
}
