package net.jenyjek.simple_teleporters.sound;

import net.jenyjek.simple_teleporters.SimpleTeleporters;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

public class ModSounds {
    public static final SoundEvent TELEPORTER_1_TELEPORTING = registerSoundEvent("sound_teleporter_1_teleporting");
    public static final SoundEvent TELEPORTER_2_TELEPORTING = registerSoundEvent("sound_teleporter_2_teleporting");
    public static final SoundEvent TELEPORTER_END_TELEPORTING = registerSoundEvent("sound_teleporter_end_teleporting");
    public static final SoundEvent TELEPORTER_IDLE = registerSoundEvent("sound_teleporter_idle");
    public static final SoundEvent TELEPORTER_EASTER = registerSoundEvent("sound_teleporter_kripl");
    public static final SoundEvent TELEPORTER_EASTER3 = registerSoundEvent("sound_teleporter_kripl3");

    private static SoundEvent registerSoundEvent(String name){
        Identifier id = new Identifier(SimpleTeleporters.MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void registerSounds(){
        SimpleTeleporters.LOGGER.info("Registering mod sounds");
    }
}
