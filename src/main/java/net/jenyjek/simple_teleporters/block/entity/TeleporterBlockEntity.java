package net.jenyjek.simple_teleporters.block.entity;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.jenyjek.simple_teleporters.SimpleTeleporters;
import net.jenyjek.simple_teleporters.item.ModItems;
import net.jenyjek.simple_teleporters.screen.TeleporterScreenHandler;
import net.jenyjek.simple_teleporters.sound.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Box;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.SimpleVoxelShape;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;


public class TeleporterBlockEntity extends BlockEntity implements GeoBlockEntity, ExtendedScreenHandlerFactory, ImplementedInventory {

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(6, ItemStack.EMPTY);
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    protected final PropertyDelegate propertyDelegate;
    private int power;
    private int teleportCooldown = 0;
    private final int nominalTeleporterCooldown = 300;

    private int maxPower = 1000;
    private final int nominalMaxPower = 1000;
    private int canTransferItems = 0;
    private PlayerEntity lastKnownPlayer;
    private StatusEffectInstance nauseaGot;
    
    private int timeEntityOnBlock;
    private final int nominalTimeTilEntityTeleports = 120;
    private int soundDuration = 200, currentSoundTime = soundDuration;

    private enum SoundProgress {none, first, second}
    private SoundProgress isPlaying = SoundProgress.none;

    private enum States {idle, active, off, item}
    private enum Upgrades{speed, cost, storage, items, cooldown}
    private EnumSet<Upgrades> selectedUpgrades = EnumSet.noneOf(Upgrades.class);

    private static final Map<BlockPos, States> animationStateForBlocks = new HashMap<>();

    public TeleporterBlockEntity( BlockPos pos, BlockState state) {

        super(ModBlockEntities.teleporterBlockEntity, pos, state);
        setStateOfAnimations(States.off, this.getPos());
        this.propertyDelegate = new PropertyDelegate() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> TeleporterBlockEntity.this.power;
                    case 1 -> TeleporterBlockEntity.this.maxPower;
                    case 2 -> TeleporterBlockEntity.this.canTransferItems;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index){
                    case 0: TeleporterBlockEntity.this.power = value; break;
                    case 1: TeleporterBlockEntity.this.maxPower = value; break;
                    case 2: TeleporterBlockEntity.this.canTransferItems = value; break;
                }
            }

            @Override
            public int size() {
                return 3;
            }
        };
    }

    private States getStateOfAnimations(BlockPos pos){
        return animationStateForBlocks.get(pos);
    }

    private void setStateOfAnimations(States to, BlockPos pos){
        animationStateForBlocks.put(pos, to);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 20, this::predicate));
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> tAnimationState) {
        if(getStateOfAnimations(this.getPos()) == States.off){
            tAnimationState.getController().setAnimation(RawAnimation.begin().then("2", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        } else if(getStateOfAnimations(this.getPos())  == States.idle){
            tAnimationState.getController().setAnimation(RawAnimation.begin().then("0", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        } else if (getStateOfAnimations(this.getPos())  == States.active) {
            tAnimationState.getController().setAnimation(RawAnimation.begin().then("1", Animation.LoopType.HOLD_ON_LAST_FRAME));
            return PlayState.CONTINUE;
        } else if (getStateOfAnimations(this.getPos()) == States.item) {
            tAnimationState.getController().setAnimation(RawAnimation.begin().then("3", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
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
        nbt.putInt("teleporter.canTransferItems", canTransferItems);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        Inventories.readNbt(nbt, inventory);
        super.readNbt(nbt);
        power = nbt.getInt("teleporter.power");
        maxPower = nbt.getInt("teleporter.maxPower");
        canTransferItems = nbt.getInt("teleporter.canTransferItems");
    }

    @Override
    public Text getDisplayName() {
        return Text.literal("Teleporter");
    }

    @Override
    public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new TeleporterScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    private Entity EntityOnBlock(BlockPos pos, World world) {
        Box areaOfInterest = new Box(pos);
        return  world.getEntitiesByClass(Entity.class, areaOfInterest, entity -> true).stream().findFirst().orElse(null);
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity serverPlayerEntity, PacketByteBuf packetByteBuf) {
        packetByteBuf.writeBlockPos(this.pos);
    }

    public void tick(World world, BlockPos blockPos) {
        if (world.isClient()) return;
        if(selectedUpgrades.contains(Upgrades.storage) && maxPower != nominalMaxPower * 2){
            maxPower = nominalMaxPower * 2;
        }
        else if (!selectedUpgrades.contains(Upgrades.storage) && maxPower != nominalMaxPower){
            maxPower = nominalMaxPower;
            if(power > maxPower) power = maxPower;
        }

        //handle lapis charging
        if (getStack(1).isOf(Items.LAPIS_LAZULI)) {
            if (power + 100 <= maxPower) {
                getStack(1).decrement(1);
                power += 100;
            }
        } else if (getStack(1).isOf(Items.LAPIS_BLOCK)) {
            if (power + 900 <= maxPower) {
                getStack(1).decrement(1);
                power += 900;
            }
        }

        //sound iambient
        if (getStateOfAnimations(this.getPos()) == States.idle || getStateOfAnimations(this.getPos()) == States.item){
            currentSoundTime += 1;
            if(currentSoundTime >= soundDuration){
                world.playSound(null, this.getPos(), ModSounds.TELEPORTER_IDLE, SoundCategory.BLOCKS, 1f, 1f);
                soundDuration = (int) ((Math.random() * 3600.00) + 2400);
                currentSoundTime = 0;
            }
        }

        //handle upgrades in slots
        selectedUpgrades = CheckUpgrades(new int[]{2, 3, 4});
        //SimpleTeleporters.LOGGER.info("upgrades on" + blockPos.toString() + " : " + selectedUpgrades.toString());


        //cartrige installed?
        if (getStack(0).isOf(ModItems.cartridge)) {
            //handle time calc
            int timeTilEntityOnBlockTeleports = calculateTpTime(selectedUpgrades);
            //item mode?
            if(selectedUpgrades.contains(Upgrades.items)){
                if(getCanTransferItems() == 0) setCanTransferItems(100);
                setStateOfAnimations(States.item, this.getPos());
                ItemStack transferSlotContent = getStack(5);
                if(!transferSlotContent.isEmpty()){
                    BlockPos destinaton = canTeleportItems(transferSlotContent.getCount());
                    if(destinaton != null){
                        timeEntityOnBlock += 1;
                        setCanTransferItems((timeEntityOnBlock - 1) * (99) / (timeTilEntityOnBlockTeleports - 1) + 1);
                    }

                    if(timeEntityOnBlock >= timeTilEntityOnBlockTeleports && destinaton != null){
                        world.spawnEntity(new ItemEntity(world, destinaton.getX()+0.5f, destinaton.getY(), destinaton.getZ()+0.5f, transferSlotContent));
                        removeStack(5);
                        power -= calculatePriceItems(this.getPos(), destinaton.getX(), destinaton.getY(), destinaton.getZ(), transferSlotContent.getCount(), selectedUpgrades);
                        timeEntityOnBlock = 0;
                        setCanTransferItems(100);
                    }
                } else{
                    setCanTransferItems(100);
                    timeEntityOnBlock = 0;
                }
            } else
            {
                //entity mode
                if(getCanTransferItems() != 0) setCanTransferItems(0);
                Entity collisionEntity = EntityOnBlock(blockPos, world);
                if(teleportCooldown > 0){
                    teleportCooldown -= 1;
                    cooldownParticles(this.getPos());
                }
                else if (collisionEntity != null && canTeleport(collisionEntity) != null) {
                    setStateOfAnimations(States.active, this.getPos());
                    timeEntityOnBlock += 1;
                    if (collisionEntity instanceof PlayerEntity playerEntity) {
                        playerEntity.sendMessage(Text.literal("Teleporting in " + (((timeTilEntityOnBlockTeleports - timeEntityOnBlock) / 20) + 1)), true);
                        if(!playerEntity.hasStatusEffect(StatusEffects.NAUSEA)){
                            nauseaGot = new StatusEffectInstance(StatusEffects.NAUSEA, 120, 10, false, false, false);
                            playerEntity.addStatusEffect(nauseaGot, null);
                        }
                        if(lastKnownPlayer != playerEntity) lastKnownPlayer = playerEntity;
                        spawnParticles(ParticleTypes.GLOW, this.getPos(), 4, 0, 0.25d, 1);
                        if(timeEntityOnBlock < 60 && isPlaying == SoundProgress.none){
                            world.playSound(null, this.getPos(), ModSounds.TELEPORTER_1_TELEPORTING, SoundCategory.BLOCKS, 1f, 1f);
                            isPlaying = SoundProgress.first;
                            //sounds play only 1 and end

                        }else if(timeEntityOnBlock > 60 && isPlaying == SoundProgress.first){
                            //sounds play 1, 2, and end
                            world.playSound(null, this.getPos(), ModSounds.TELEPORTER_2_TELEPORTING, SoundCategory.BLOCKS, 1f, 1f);
                            isPlaying = SoundProgress.second;
                        }

                    }
                } else if (timeEntityOnBlock != 0) {
                    if(lastKnownPlayer != null) lastKnownPlayer.sendMessage(Text.literal("Teleporting cancelled..."), true);
                    if(nauseaGot != null && lastKnownPlayer != null){
                        lastKnownPlayer.removeStatusEffect(StatusEffects.NAUSEA);
                        nauseaGot = null;
                    }
                    setStateOfAnimations(States.idle, this.getPos());
                    isPlaying = SoundProgress.none;

                    timeEntityOnBlock = 0;
                } else setStateOfAnimations(States.idle, this.getPos());

                if (timeEntityOnBlock >= timeTilEntityOnBlockTeleports) {
                    BlockPos dest = canTeleport(collisionEntity);
                    if (dest != null) {
                        power -= calculatePrice(collisionEntity, dest.getX(), dest.getY(), dest.getZ(), selectedUpgrades);
                        collisionEntity.teleport(dest.getX() + 0.5f, dest.getY(), dest.getZ() + 0.5f);
                        if(nauseaGot != null && collisionEntity instanceof PlayerEntity player){
                            player.removeStatusEffect(StatusEffects.NAUSEA);
                            nauseaGot = null;
                            player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 60, 10, false, false, false));
                            spawnParticles(ParticleTypes.GLOW_SQUID_INK, this.getPos(), 20, 0.1d, 0.1d, 0);
                            spawnParticles(ParticleTypes.GLOW_SQUID_INK, collisionEntity.getBlockPos(), 20, 0.1d, 0.1d, 0);
                        }
                        if(collisionEntity instanceof ServerPlayerEntity player && player.currentScreenHandler != null) player.closeHandledScreen();
                        isPlaying = SoundProgress.none;
                        timeEntityOnBlock = 0;
                        world.playSound(collisionEntity, this.getPos(), ModSounds.TELEPORTER_END_TELEPORTING, SoundCategory.BLOCKS, 1f, 1f);
                        world.playSound(null, collisionEntity.getBlockPos(), ModSounds.TELEPORTER_END_TELEPORTING, SoundCategory.BLOCKS, 1f, 1f);
                        if(selectedUpgrades.contains(Upgrades.cooldown)) teleportCooldown = nominalTeleporterCooldown / 2;
                        else teleportCooldown = nominalTeleporterCooldown;
                        setStateOfAnimations(States.idle, this.getPos());
                    }
                }
            }
        }
        else {
            setStateOfAnimations(States.off, this.getPos());
            timeEntityOnBlock = 0;
            isPlaying = SoundProgress.none;
        }
    }

    private int calculateTpTime(EnumSet<Upgrades> upgrades) {
        if (upgrades.contains(Upgrades.speed)) return nominalTimeTilEntityTeleports/2;
        else return nominalTimeTilEntityTeleports;
    }

    private BlockPos canTeleportItems(int count) {
        NbtCompound nbtData = getStack(0).getNbt();
        if(nbtData != null && nbtData.getBoolean("Written")){ //is the cartridge full?
            long destX = nbtData.getLong("SavedX");
            long destY = nbtData.getLong("SavedY");
            long destZ = nbtData.getLong("SavedZ");
            if(world.getBlockState(new BlockPos((int)destX, (int)destY, (int)destZ)).getBlock() == Blocks.AIR){
                int price = calculatePriceItems(this.getPos(),destX, destY, destZ, count, selectedUpgrades);
                if (power >= price) { //does the teleporter have enough lapis?
                    return new BlockPos((int) destX, (int) destY, (int) destZ);
                }else return null;
            }else return null;
        }else return null;
    }

    private int calculatePriceItems(BlockPos pos, long destX, long destY, long destZ, int count, EnumSet<Upgrades> upgrades) {
        int sum = (int)Math.round(((float)count/64.00) * ((float)Math.sqrt(pos.getSquaredDistance(destX, destY, destZ)) / 10.00));
        if (upgrades.contains(Upgrades.cost)) sum /= 2;
        if(sum == 0) return 1;
        else return sum;
    }

    private int calculatePrice(Entity player, long destX, long destY, long destZ, EnumSet<Upgrades> upgrades){
        int sum = Math.round(((float)Math.sqrt(player.squaredDistanceTo(destX, destY, destZ))));
        if (upgrades.contains(Upgrades.cost)) sum /= 2;
        if(sum == 0) return 1;
        else return sum;
    }

    private BlockPos canTeleport(Entity collisionEntity){
        NbtCompound nbtData = getStack(0).getNbt();
        if(nbtData != null && nbtData.getBoolean("Written")){ //is the cartridge full?
            long destX = nbtData.getLong("SavedX");
            long destY = nbtData.getLong("SavedY");
            long destZ = nbtData.getLong("SavedZ");
            if(world.getBlockState(new BlockPos((int)destX, (int)destY, (int)destZ)).getBlock() == Blocks.AIR) { //is the destination unoccupied?s
                int price = calculatePrice(collisionEntity, destX, destY, destZ, selectedUpgrades);
                if (power >= price) { //does the teleporter have enough lapis?
                    return new BlockPos((int) destX, (int) destY, (int) destZ);
                }
                else return null;
            }else return null;
        }else return null;
    }

    private int getCanTransferItems(){
        return canTransferItems;
    }

    private void setCanTransferItems(int value){
        canTransferItems = value;
    }

    private EnumSet<Upgrades> CheckUpgrades(int[] slots){
        EnumSet<Upgrades> localSet = EnumSet.noneOf(Upgrades.class);
        for(int slot : slots){
            if(getStack(slot).isOf(ModItems.teleporterSpeedUpgrade)) localSet.add(Upgrades.speed);
            else if(getStack(slot).isOf(ModItems.teleporterCostUpgrade)) localSet.add(Upgrades.cost);
            else if(getStack(slot).isOf(ModItems.teleporterItemUpgrade)) localSet.add(Upgrades.items);
            else if(getStack(slot).isOf(ModItems.teleporterCapacityUpgrade)) localSet.add(Upgrades.storage);
            else if(getStack(slot).isOf(ModItems.teleporterCooldownUpgrade)) localSet.add(Upgrades.cooldown);
        }
        return localSet;
    }

    private void spawnParticles(ParticleEffect particleTypes, BlockPos positionClicked,int amount, double spread, double ring, double YRoffset) {
        MinecraftClient instance = MinecraftClient.getInstance();
        instance.execute(() -> {
            for(int i=0; i < 360; i++) {
                if(i % (360/amount) == 0){
                    double rads = i*(Math.PI/180.0);
                    instance.world.addParticle(particleTypes,
                            positionClicked.getX() + 0.5d + (Math.cos(rads) * ring), positionClicked.getY() + (instance.world.random.nextDouble() * YRoffset), positionClicked.getZ() + 0.5d + (Math.sin(rads) * ring),
                            (Math.cos(rads) * 0.1d) * spread, 0.15d, (Math.sin(rads) * 0.1d) * spread);
                }
            }
        });

    }

    private void cooldownParticles(BlockPos ppos){
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            World world = client.world;
            world.addParticle(ParticleTypes.REVERSE_PORTAL, ppos.getX() + 0.5d + (world.random.nextDouble() * 0.25d), ppos.getY() + (world.random.nextDouble() * 2.00d), ppos.getZ() + 0.5d +(world.random.nextDouble() * 0.25d),
                    world.random.nextDouble() * 1d, world.random.nextDouble() * 1d, world.random.nextDouble() * 1d);
        });
    }
}
