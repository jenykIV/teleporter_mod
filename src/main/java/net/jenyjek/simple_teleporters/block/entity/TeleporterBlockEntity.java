package net.jenyjek.simple_teleporters.block.entity;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.jenyjek.simple_teleporters.SimpleTeleporters;
import net.jenyjek.simple_teleporters.item.ModItems;
import net.jenyjek.simple_teleporters.screen.TeleporterScreenHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Items;
import net.minecraft.item.MinecartItem;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.RenderUtils;

import java.util.List;

import static java.lang.Math.sqrt;

public class TeleporterBlockEntity extends BlockEntity implements GeoBlockEntity, ExtendedScreenHandlerFactory, ImplementedInventory {

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(2, ItemStack.EMPTY);
    private AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    protected final PropertyDelegate propertyDelegate;
    private int power;
    private int maxPower = 1000;
    
    private int timeEntityOnBlock;
    private int timeTilEntityOnBlockTeleports = 100;

    public TeleporterBlockEntity( BlockPos pos, BlockState state) {
        super(ModBlockEntities.teleporterBlockEntity, pos, state);
        this.propertyDelegate = new PropertyDelegate() {
            @Override
            public int get(int index) {
                switch (index){
                    case 0: return TeleporterBlockEntity.this.power;
                    case 1: return TeleporterBlockEntity.this.maxPower;
                    default: return 0;
                }
            }

            @Override
            public void set(int index, int value) {
                switch (index){
                    case 0: TeleporterBlockEntity.this.power = value; break;
                    case 1: TeleporterBlockEntity.this.maxPower = value; break;
                }
            }

            @Override
            public int size() {
                return 2;
            }
        };
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> tAnimationState) {
        tAnimationState.getController().setAnimation(RawAnimation.begin().then("0", Animation.LoopType.LOOP));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object blockEntity) {
        return RenderUtils.getCurrentTick();
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return this.inventory;
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, inventory);
        nbt.putInt("teleporter.power", power);
        nbt.putInt("teleporter.maxPower", maxPower);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        Inventories.readNbt(nbt, inventory);
        super.readNbt(nbt);
        power = nbt.getInt("teleporter.power");
        maxPower = nbt.getInt("teleporter.maxPower");
    }

    @Override
    public Text getDisplayName() {
        return Text.literal("Teleporter");
    }

    @Override
    public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new TeleporterScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    private static Entity EntityOnBlock(BlockPos pos, World world) {
        Box areaOfInterest = new Box(pos);
        return  world.getEntitiesByClass(Entity.class, areaOfInterest, entity -> true).stream().findFirst().orElse(null);
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity serverPlayerEntity, PacketByteBuf packetByteBuf) {
        packetByteBuf.writeBlockPos(this.pos);
    }

    public void tick(World world, BlockPos blockPos, BlockState state) {
        if(world.isClient()) return;

        if(getStack(1).isOf(Items.LAPIS_LAZULI)){
            if(power + 100 <= maxPower){
                getStack(1).decrement(1);
                power += 100;
            }
        }

        if(getStack(0).isOf(ModItems.cartridge)) {
            Entity collisionEntity = EntityOnBlock(blockPos, world);

            if(collisionEntity != null && canTeleport(collisionEntity).equals("yes")){
                timeEntityOnBlock += 1;
                if(collisionEntity instanceof PlayerEntity playerEntity){
                    playerEntity.sendMessage(Text.literal("Teleporting in " + (((timeTilEntityOnBlockTeleports - timeEntityOnBlock) / 20 ) + 1)), true);
                }
            }
            else if (timeEntityOnBlock != 0){
                timeEntityOnBlock = 0;
            }

            if (timeEntityOnBlock >= timeTilEntityOnBlockTeleports && collisionEntity instanceof PlayerEntity player){

                NbtCompound nbtData = getStack(0).getNbt();

                if(nbtData != null && nbtData.getBoolean("Written")){

                    long destX = nbtData.getLong("SavedX");
                    long destY = nbtData.getLong("SavedY");
                    long destZ = nbtData.getLong("SavedZ");

                    if(world.getBlockState(new BlockPos((int)destX, (int)destY, (int)destZ)).getBlock() == Blocks.AIR) {
                        //destination not occupied by blocks
                        int price = (int) calculatePrice(player, destX, destY, destZ);
                        if (power >= price) {
                            player.teleport(destX, destY, destZ);
                            power -= price;
                            player.sendMessage(Text.literal("teleporting ..."), true);
                        }
                        else player.sendMessage(Text.literal("not enough power"), true);
                    }
                    else player.sendMessage(Text.literal("Destination occupied by blocks"),true);
                }
                else player.sendMessage(Text.literal("No destination set"), true);

                timeEntityOnBlock = 0;
            }
        }
    }

    private long calculatePrice(PlayerEntity player, long x, long y, long z){
        return Math.round(Math.sqrt(player.squaredDistanceTo(x, y, z)));
    }

    private String canTeleport(Entity collisionEntity){
        if (collisionEntity instanceof PlayerEntity player){ //is this entity a player?
            NbtCompound nbtData = getStack(0).getNbt();
            if(nbtData != null && nbtData.getBoolean("Written")){ //is the cartridge full?

                long destX = nbtData.getLong("SavedX");
                long destY = nbtData.getLong("SavedY");
                long destZ = nbtData.getLong("SavedZ");

                if(world.getBlockState(new BlockPos((int)destX, (int)destY, (int)destZ)).getBlock() == Blocks.AIR) { //is the destination unoccupied?
                    //destination not occupied by blocks
                    int price = (int) calculatePrice(player, destX, destY, destZ);
                    if (power >= price) { //does the teleporter have enough lapis?
                        return "yes";
                    }
                    else return "not enough power";
                }else return  "occupied destination";
            }else return "no cartridge data";
        }else return "not a player";
    }
}
