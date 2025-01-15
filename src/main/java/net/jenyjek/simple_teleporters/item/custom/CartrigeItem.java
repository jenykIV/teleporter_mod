package net.jenyjek.simple_teleporters.item.custom;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CartrigeItem extends Item {
    public CartrigeItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);

        NbtCompound nbts = stack.getNbt();

        if(stack.hasNbt() && nbts.getBoolean("Written")){
            tooltip.add(Text.literal("Saved: X: " + nbts.getLong("SavedX") + " Y: " + nbts.getLong("SavedY") + " Z: " + nbts.getLong("SavedZ")).formatted(Formatting.GREEN));
            tooltip.add(Text.literal("Clear cartridge by crouch-clicking a block").formatted(Formatting.ITALIC));
        }
        else{
            tooltip.add(Text.literal("Nothing saved yet").formatted(Formatting.DARK_GRAY));
            tooltip.add(Text.literal("Save coordinates by clicking a block").formatted(Formatting.ITALIC));
        }
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {

        if(!context.getWorld().isClient()){
            PlayerEntity player = context.getPlayer();
            ItemStack activeItem = context.getStack();
            if(activeItem.hasNbt() && activeItem.getNbt().getBoolean("Written")){
                NbtCompound storedNbts = new NbtCompound();
                if(player.isSneaking()){
                    activeItem.setNbt(storedNbts);
                    player.sendMessage(Text.literal("Cartrige cleared"), true);
                }
                else{
                    storedNbts.putBoolean("Written", true);
                    storedNbts.putLong("SavedX", context.getBlockPos().getX());
                    storedNbts.putLong("SavedY", context.getBlockPos().getY() + 1);
                    storedNbts.putLong("SavedZ", context.getBlockPos().getZ());
                    activeItem.setNbt(storedNbts);
                    player.sendMessage(Text.literal("Saved coordinates were rewritten"), true);
                }
                //item has already written coordinat
            }
            else{
                NbtCompound storedNbts = new NbtCompound();
                storedNbts.putBoolean("Written", true);
                storedNbts.putLong("SavedX", context.getBlockPos().getX());
                storedNbts.putLong("SavedY", context.getBlockPos().getY() + 1);
                storedNbts.putLong("SavedZ", context.getBlockPos().getZ());
                activeItem.setNbt(storedNbts);
                player.sendMessage(Text.literal("Coordinates saved to cartridge"), true);
            }
        }

        return ActionResult.SUCCESS;
    }

}
