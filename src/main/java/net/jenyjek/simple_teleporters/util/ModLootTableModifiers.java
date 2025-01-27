package net.jenyjek.simple_teleporters.util;

import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.jenyjek.simple_teleporters.item.ModItems;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.util.Identifier;

public class ModLootTableModifiers {
    public static final Identifier enderman_loottable_id = new Identifier("minecraft", "entities/enderman");
    public static void modifyLootTables(){
        LootTableEvents.MODIFY.register((resourceManager, lootManager, identifier, builder, lootTableSource) -> {
            if(enderman_loottable_id.equals(identifier)){
                LootPool.Builder poolBuilder = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.1f)) //10% of time
                        .with(ItemEntry.builder(ModItems.arcStone))
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0f, 2.0f)).build());
                builder.pool(poolBuilder.build());
            }
        });
    }
}
