package net.jenyjek.simple_teleporters.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.jenyjek.simple_teleporters.SimpleTeleporters;
import net.jenyjek.simple_teleporters.block.ModBlocks;
import net.jenyjek.simple_teleporters.item.custom.CartrigeItem;
import net.jenyjek.simple_teleporters.item.custom.TeleporterBlockItem;
import net.jenyjek.simple_teleporters.item.custom.TeleporterUpgrade;
import net.minecraft.item.Item;

import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
    public static final Item arcStone = registerItem("arcstone", new Item(new FabricItemSettings()));
    public static final Item cartridge = registerItem("cartridge", new CartrigeItem(new FabricItemSettings().maxCount(1)));
    public static final Item teleporter = registerItem("teleporter", new TeleporterBlockItem(ModBlocks.teleporterBlock, new FabricItemSettings().maxCount(16)));
    public static final Item oxidisedCoppperIngot = registerItem("oxidised_copper_ingot", new Item(new FabricItemSettings()));
    public static final Item teleporterSpeedUpgrade = registerItem("teleporter_upgrade_speed", new TeleporterUpgrade(new FabricItemSettings(), "Reduces teleporting time by half."));
    public static final Item teleporterCostUpgrade = registerItem("teleporter_upgrade_cost", new TeleporterUpgrade(new FabricItemSettings(), "Reduces teleporting cost by half."));
    public static final Item teleporterCapacityUpgrade = registerItem("teleporter_upgrade_capacity", new TeleporterUpgrade(new FabricItemSettings(), "Doubles the Lapis capacity."));
    public static final Item teleporterItemUpgrade = registerItem("teleporter_upgrade_item", new TeleporterUpgrade(new FabricItemSettings(), "Turns teleporter into a glorified hopper."));
    public static final Item teleporterCooldownUpgrade = registerItem("teleporter_upgrade_cooldown", new TeleporterUpgrade(new FabricItemSettings(), "Reduces the time it takes to cool down the teleporter by half"));

    private static void addItemsToIngredientItemGroup(FabricItemGroupEntries entries){
        //entries.add(arcStone);
    }

    private static Item registerItem(String name, Item item){
        return Registry.register(Registries.ITEM, new Identifier(SimpleTeleporters.MOD_ID, name), item);
    }

    public static void registerModItems(){
        SimpleTeleporters.LOGGER.info("Registering mod items for" + SimpleTeleporters.MOD_ID);

        //ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(ModItems::addItemsToIngredientItemGroup);
    }
}
