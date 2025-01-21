package net.jenyjek.simple_teleporters.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class TeleporterScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public  TeleporterScreenHandler(int syncId, PlayerInventory inventory, PacketByteBuf buf) {
        this(syncId, inventory, (Inventory) inventory.player.getWorld().getBlockEntity(buf.readBlockPos()),
                new ArrayPropertyDelegate(6));
    }
    public TeleporterScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate delegate) {
        super(ModScreenHandlers.teleporterScreen, syncId);
        checkSize(inventory, 6);
        this.inventory = inventory;
        inventory.onOpen(playerInventory.player);
        this.propertyDelegate = delegate;

        this.addSlot(new Slot(inventory, 0, 26, 34));
        this.addSlot(new Slot(inventory, 1, 125, 63));
        this.addSlot(new Slot(inventory, 2, 62, 21));
        this.addSlot(new Slot(inventory, 3, 80, 21));
        this.addSlot(new Slot(inventory, 4, 98, 21));
        this.addSlot(new Slot(inventory, 5, 80, 48));


        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);

        addProperties(delegate);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotId) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotId);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (slotId < this.inventory.size()) {
                if (!this.insertItem(originalStack, this.inventory.size(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(originalStack, 0, this.inventory.size(), false)) {
                return ItemStack.EMPTY;
            }
            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }
        return newStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    private void addPlayerInventory(PlayerInventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 86 + i * 18));
            }
        }
    }
    private void addPlayerHotbar(PlayerInventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 144));
        }
    }

    public float getLapisLeft(){
        return propertyDelegate.get(0)/100f;
    }

    public int powerLevel() {
        if (propertyDelegate.get(1) <= 1){
            propertyDelegate.set(1, 1);
        }
        //return propertyDelegate.get(0) * 72 / propertyDelegate.get(1);
        return propertyDelegate.get(0) * -72 / propertyDelegate.get(1) + 72;
    }

    public int canTransferItems(){
        return propertyDelegate.get(2) * -16 / 100 + 16;
    }
}
