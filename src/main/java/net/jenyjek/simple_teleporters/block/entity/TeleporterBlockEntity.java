package net.jenyjek.simple_teleporters.block.entity;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.jenyjek.simple_teleporters.SimpleTeleporters;
import net.jenyjek.simple_teleporters.item.ModItems;
import net.jenyjek.simple_teleporters.screen.TeleporterScreenHandler;
import net.jenyjek.simple_teleporters.sound.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
//import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.Stat;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.StringIdentifiable;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class TeleporterBlockEntity extends BlockEntity implements GeoBlockEntity, ExtendedScreenHandlerFactory, ImplementedInventory {

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(6, ItemStack.EMPTY);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected final PropertyDelegate propertyDelegate;
    private int power;
    private int teleportCooldown = 0;
    private final int nominalTeleporterCooldown = 300;

    private int maxPower = 1000;
    private final int nominalMaxPower = 1000;
    private int canTransferItems = 0;
    private PlayerEntity lastKnownPlayer;
    private StatusEffectInstance nauseaGot;
    public String customName = "Teleporter";
    private int timeEntityOnBlock;
    private final int nominalTimeTilEntityTeleports = 120;
    private int soundDuration = 200, currentSoundTime = soundDuration;

    private enum SoundProgress {none, first, second}
    private SoundProgress isPlaying = SoundProgress.none;

    public enum States {idle, active, off, item}
    private enum Upgrades{speed, cost, storage, items, cooldown}
    private EnumSet<Upgrades> selectedUpgrades = EnumSet.noneOf(Upgrades.class);

    private States currentAnimation = States.off;

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
        return currentAnimation;
    }

    private void setStateOfAnimations(States to, BlockPos pos){
        if(!currentAnimation.equals(to)){
            triggerAnim("controller", to.toString());
            currentAnimation = to;
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 20, state -> state.setAndContinue(RawAnimation.begin().then("2", Animation.LoopType.LOOP)))
                .triggerableAnim("off",RawAnimation.begin().then("2", Animation.LoopType.LOOP))
                .triggerableAnim("idle",RawAnimation.begin().then("0", Animation.LoopType.LOOP))
                .triggerableAnim("active",RawAnimation.begin().then("1", Animation.LoopType.HOLD_ON_LAST_FRAME))
                .triggerableAnim("item",RawAnimation.begin().then("3", Animation.LoopType.LOOP))
                /*this::predicate*/
        );
    }

    /*private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> tAnimationState) {
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
    }*/

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
        return Text.literal(customName);
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
                            nauseaGot = new StatusEffectInstance(StatusEffects.NAUSEA, 240, 10, false, false, false);
                            playerEntity.addStatusEffect(nauseaGot, null);
                        }
                        if(lastKnownPlayer != playerEntity) lastKnownPlayer = playerEntity;
                        spawnParticles(ParticleTypes.GLOW, this.getPos(), 15,  0);
                        if(timeEntityOnBlock < 60 && isPlaying == SoundProgress.none){
                            if(Objects.equals(this.getDisplayName().getString(), "mhd")) world.playSound(null, this.getPos(), ModSounds.TELEPORTER_EASTER, SoundCategory.BLOCKS, 1f, 1f);
                            else world.playSound(null, this.getPos(), ModSounds.TELEPORTER_1_TELEPORTING, SoundCategory.BLOCKS, 1f, 1f);
                            isPlaying = SoundProgress.first;
                            //sounds play only 1 and end

                        }else if(timeEntityOnBlock > 60 && isPlaying == SoundProgress.first){
                            //sounds play 1, 2, and end
                            if(!(Objects.equals(this.getDisplayName().getString(), "mhd"))) world.playSound(null, this.getPos(), ModSounds.TELEPORTER_2_TELEPORTING, SoundCategory.BLOCKS, 1f, 1f);
                            isPlaying = SoundProgress.second;
                        }

                    }
                } else if (timeEntityOnBlock != 0) {
                    if(lastKnownPlayer != null) lastKnownPlayer.sendMessage(Text.literal("Teleporting cancelled..."), true);
                    if(nauseaGot != null && lastKnownPlayer != null){
                        lastKnownPlayer.removeStatusEffect(StatusEffects.NAUSEA);
                        nauseaGot = null;
                    }
                    teleportCooldown = 90;
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
                            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 60, 10, false, false, false));
                            spawnParticles(ParticleTypes.GLOW_SQUID_INK, this.getPos(), 20, 0.05f);
                            spawnParticles(ParticleTypes.GLOW_SQUID_INK, collisionEntity.getBlockPos(), 20,  0.05f);
                        }
                        if(collisionEntity instanceof ServerPlayerEntity player && player.currentScreenHandler != null) player.closeHandledScreen();
                        isPlaying = SoundProgress.none;
                        timeEntityOnBlock = 0;
                        if(Objects.equals(this.getDisplayName().getString(), "mhd")){
                            world.playSound(collisionEntity, this.getPos(), ModSounds.TELEPORTER_EASTER3, SoundCategory.BLOCKS, 1f, 1f);
                            world.playSound(null, collisionEntity.getBlockPos(), ModSounds.TELEPORTER_EASTER3, SoundCategory.BLOCKS, 1f, 1f);
                        }
                        else{
                            world.playSound(collisionEntity, this.getPos(), ModSounds.TELEPORTER_END_TELEPORTING, SoundCategory.BLOCKS, 1f, 1f);
                            world.playSound(null, collisionEntity.getBlockPos(), ModSounds.TELEPORTER_END_TELEPORTING, SoundCategory.BLOCKS, 1f, 1f);
                        }
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
            canTransferItems = 0;
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
        Text changingName = null;
        for(int slot : slots){
            if(getStack(slot).hasCustomName()) changingName = getStack(slot).getName();

            if(getStack(slot).isOf(ModItems.teleporterSpeedUpgrade)) localSet.add(Upgrades.speed);
            else if(getStack(slot).isOf(ModItems.teleporterCostUpgrade)) localSet.add(Upgrades.cost);
            else if(getStack(slot).isOf(ModItems.teleporterItemUpgrade)) localSet.add(Upgrades.items);
            else if(getStack(slot).isOf(ModItems.teleporterCapacityUpgrade)) localSet.add(Upgrades.storage);
            else if(getStack(slot).isOf(ModItems.teleporterCooldownUpgrade)) localSet.add(Upgrades.cooldown);
        }
        if(changingName != null) this.customName = changingName.getString();
        else this.customName = "Teleporter";

        return localSet;
    }

    private void spawnParticles(ParticleEffect particleTypes, BlockPos positionClicked,int amount, float speed) {
        if(this.world instanceof ServerWorld serverWorld){
            /*sendPacticlePacket(serverWorld,
                    new Vec3d(positionClicked.getX() + 0.5d, positionClicked.getY() + YRoffset, positionClicked.getZ() + 0.5d),
                    particleTypes,
                    new Vec3d(spread, yspread, spread),
                    speed, amount);*/
            sendPacticlePacket(serverWorld,
                    new Vec3d(positionClicked.getX() + 0.5d, positionClicked.getY() + 0.55d, positionClicked.getZ() + 0.5d),
                    particleTypes,
                    new Vec3d(0.2, 0.1, 0.2),
                    speed, amount);
        }
        else{
            SimpleTeleporters.LOGGER.info("No server nigger");
        }
    }

    private void cooldownParticles(BlockPos ppos){
        if(this.world instanceof ServerWorld serverWorld){
            /*sendPacticlePacket(serverWorld,
                    new Vec3d(ppos.getX() + 0.40d + (world.random.nextDouble() * 0.25d), ppos.getY() + (world.random.nextDouble() * 2.00d), ppos.getZ() + 0.40d +(world.random.nextDouble() * 0.25d)),
                    ParticleTypes.REVERSE_PORTAL,
                    new Vec3d(((world.random.nextDouble() * 2d) - 1)*0.05d, ((world.random.nextDouble() * 2d) - 1)*0.02d, ((world.random.nextDouble() * 2d) - 1)*0.05d),
                    0.01f, 5);*/
            sendPacticlePacket(serverWorld,
                    new Vec3d(ppos.getX()+0.5d, ppos.getY()+1.1d, ppos.getZ()+0.5d),
                    ParticleTypes.REVERSE_PORTAL,
                    new Vec3d(0.1d, 0.45d, 0.1d),
                    0.01f, 10);
        }
        else{
            SimpleTeleporters.LOGGER.info("No server nigger");
        }
    }

    public static void sendPacticlePacket (ServerWorld world, Vec3d pos, ParticleEffect particleTypes, Vec3d velocityVector, float speed, int amount) {
        // Get all players in the world
        for (ServerPlayerEntity player : world.getPlayers()) {
            ServerPlayNetworkHandler handler = player.networkHandler;
            if (handler != null) {
                handler.sendPacket(new ParticleS2CPacket(particleTypes, true, pos.x, pos.y, pos.z, (float)velocityVector.x, (float)velocityVector.y, (float)velocityVector.z, speed, amount));
            }
        }
    }
}


