package net.jenyjek.simple_teleporters.item.custom;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TeleporterUpgrade extends Item {

    private String tooltipInfo;
    public TeleporterUpgrade(Settings settings, String info) {
        super(settings);
        tooltipInfo = info;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.literal("Upgrades your teleporter").formatted(Formatting.BOLD));
        tooltip.add(Text.literal(tooltipInfo).formatted(Formatting.GREEN));
    }
}
