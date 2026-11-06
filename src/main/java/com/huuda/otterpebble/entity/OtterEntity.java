package com.huuda.otterpebble.entity;

import com.huuda.otterpebble.config.OtterConfig;
import com.huuda.otterpebble.entity.ai.OtterAdultDiveGoal;
import com.huuda.otterpebble.entity.ai.OtterBellySlideGoal;
import com.huuda.otterpebble.entity.ai.OtterBondGoal;
import com.huuda.otterpebble.entity.ai.OtterFollowFamilyGoal;
import com.huuda.otterpebble.entity.ai.OtterFollowOwnerGoal;
import com.huuda.otterpebble.entity.ai.OtterGroomGoal;
import com.huuda.otterpebble.entity.ai.OtterMoveControl;
import com.huuda.otterpebble.entity.ai.OtterPairFloatGoal;
import com.huuda.otterpebble.entity.ai.OtterPanicGoal;
import com.huuda.otterpebble.entity.ai.OtterPebblePlayGoal;
import com.huuda.otterpebble.entity.ai.OtterRescueGoal;
import com.huuda.otterpebble.entity.ai.OtterWaitAnchorGoal;
import com.huuda.otterpebble.entity.ai.OtterWanderGoal;
import com.huuda.otterpebble.entity.ai.OtterWatchAndLearnGoal;
import com.huuda.otterpebble.entity.ai.WaterHelper;
import com.huuda.otterpebble.registry.ModEntities;
import com.huuda.otterpebble.registry.ModSounds;
import com.huuda.otterpebble.registry.ModTags;
import com.huuda.otterpebble.rescue.RescueTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * The river companion. Implements the GDD v04 state model:
 * <ul>
 *     <li>Relationship: Unbonded -> Curious -> Inspecting -> Bonded (fish offer, one owner).</li>
 *     <li>Player order: Follow / Wait (empty-hand use). Owner absent: the order is preserved.</li>
 *     <li>Current action: Move, Rescue, Social idle. Wait blocks rescue and travel actions.</li>
 * </ul>
 * Animations never change an order; ownership persists through growth, naming and reload.
 */
public class OtterEntity extends TamableAnimal implements GeoEntity {
    // ---------------------------------------------------------------- synced data
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(OtterEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_BOND_STATE = SynchedEntityData.defineId(OtterEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(OtterEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_WAITING = SynchedEntityData.defineId(OtterEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HAS_PEBBLE = SynchedEntityData.defineId(OtterEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<ItemStack> DATA_CARRIED = SynchedEntityData.defineId(OtterEntity.class, EntityDataSerializers.ITEM_STACK);

    // ---------------------------------------------------------------- animations
    public static final String CTRL_MOVEMENT = "movement";
    public static final String CTRL_ACTION = "action";
    public static final String CTRL_MOUTH = "mouth";

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.otter.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.otter.walk");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.otter.swim");
    private static final RawAnimation FLOAT = RawAnimation.begin().thenLoop("animation.otter.float");
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.otter.sit");
    private static final RawAnimation SLIDE = RawAnimation.begin().thenLoop("animation.otter.slide");
    private static final RawAnimation CURIOUS = RawAnimation.begin().thenLoop("animation.otter.curious");
    private static final RawAnimation INSPECT = RawAnimation.begin().thenLoop("animation.otter.inspect");
    private static final RawAnimation GROOM = RawAnimation.begin().thenPlay("animation.otter.groom");
    private static final RawAnimation DIVE = RawAnimation.begin().thenPlay("animation.otter.dive");
    private static final RawAnimation AWKWARD_DIVE = RawAnimation.begin().thenPlay("animation.otter.awkward_dive");
    private static final RawAnimation PEBBLE_PLAY = RawAnimation.begin().thenPlay("animation.otter.pebble_play").thenLoop("animation.otter.pebble_hold");
    private static final RawAnimation PAIR_LEFT_PAW = RawAnimation.begin().thenLoop("animation.otter.float_pair_left_paw");
    private static final RawAnimation PAIR_RIGHT_PAW = RawAnimation.begin().thenLoop("animation.otter.float_pair_right_paw");
    private static final RawAnimation CARRY = RawAnimation.begin().thenLoop("animation.otter.carry");
    private static final RawAnimation TRIG_CHIRP = RawAnimation.begin().thenPlay("animation.otter.chirp");
    private static final RawAnimation TRIG_NOD = RawAnimation.begin().thenPlay("animation.otter.nod");
    private static final RawAnimation TRIG_DELIVER = RawAnimation.begin().thenPlay("animation.otter.deliver");
    private static final RawAnimation TRIG_ACCEPT = RawAnimation.begin().thenPlay("animation.otter.accept");
    private static final RawAnimation TRIG_SPLASH = RawAnimation.begin().thenPlay("animation.otter.splash");
    private static final RawAnimation TRIG_STARTLE = RawAnimation.begin().thenPlay("animation.otter.startle");

    /** Feet this far below the water surface while floating at rest. */
    private static final double FLOAT_DEPTH = 0.15D;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    // ---------------------------------------------------------------- persistent state (saved)
    @Nullable
    private BlockPos waitAnchor;
    private ItemStack carried = ItemStack.EMPTY;
    private int carryLifetime;
    private boolean carryUnlimited;
    private int carryTicks;
    private int chirpCooldown;
    private int slideCooldown;
    private int groomCooldown;
    private int diveCooldown;
    private int pebbleCooldown;
    private int pairCooldown;
    private int fishingCooldown;
    private int splashCooldown;

    // ---------------------------------------------------------------- transient state (rebuilt after reload)
    @Nullable
    private UUID curiousPlayer;
    @Nullable
    private UUID inspector;
    @Nullable
    private InteractionHand inspectHand;
    @Nullable
    private Vec3 lastPlayerPos;
    private int calmTicks;
    private int curiousTicks;
    private int inspectTicks;
    private int startledTicks;
    private int bondRestTicks;
    private int gazeTicks;
    private boolean gazeLatched;
    private int lookAtOwnerTicks;
    private int ticksInWater;
    private int ticksOutOfWater;
    private int lastSwimTicks;
    private int ownerMissingTicks;
    @Nullable
    private UUID partnerId;
    private boolean pairLeader;
    @Nullable
    private Vec3 pairAnchor;
    private float pairYaw;
    @Nullable
    private OtterEntity watchedAdult;
    private int watchedAdultTick;
    private boolean rescueActive;
    private boolean diveActive;

    // client-side smoothing for the swim pitch
    public float swimPitch;
    public float swimPitchO;

    public OtterEntity(EntityType<? extends OtterEntity> type, Level level) {
        super(type, level);
        this.moveControl = new OtterMoveControl(this);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 0.0F);
        this.setMaxUpStep(1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 20.0D);
    }

    /** Temperate river banks and the water surface, in daylight, like other animals. */
    public static boolean checkOtterSpawnRules(EntityType<OtterEntity> type, LevelAccessor level, MobSpawnType reason,
                                               BlockPos pos, RandomSource random) {
        boolean onWater = WaterHelper.isWater(level, pos.below()) && WaterHelper.isPassable(level, pos);
        boolean onBank = WaterHelper.isStandable(level, pos) && WaterHelper.hasWaterNearby(level, pos, 4);
        return (onWater || onBank) && level.getRawBrightness(pos, 0) > 8;
    }

    // ================================================================ setup
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_VARIANT, 0);
        this.entityData.define(DATA_BOND_STATE, (byte) 0);
        this.entityData.define(DATA_ACTION, (byte) 0);
        this.entityData.define(DATA_WAITING, false);
        this.entityData.define(DATA_HAS_PEBBLE, false);
        this.entityData.define(DATA_CARRIED, ItemStack.EMPTY);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

    /** Goal priorities follow Appendix B (selection priority). */
    @Override
    protected void registerGoals() {
        // 02 Immediate danger
        this.goalSelector.addGoal(1, new OtterPanicGoal(this, 1.5D));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Monster.class, 6.0F, 1.1D, 1.4D));
        // 03 Atomic commit (bond inspection keeps the pup still while the offer is inspected)
        this.goalSelector.addGoal(2, new OtterBondGoal(this));
        // 04 Owner absence and commands (Wait anchor)
        this.goalSelector.addGoal(3, new OtterWaitAnchorGoal(this));
        // 05 Active utility
        this.goalSelector.addGoal(4, new OtterRescueGoal(this));
        // 07 Follow or family movement
        this.goalSelector.addGoal(6, new OtterFollowOwnerGoal(this, 1.15D));
        this.goalSelector.addGoal(6, new OtterFollowFamilyGoal(this, 1.1D));
        // 08 Social and ambient actions
        this.goalSelector.addGoal(7, new OtterGroomGoal(this));
        this.goalSelector.addGoal(8, new OtterPairFloatGoal(this));
        this.goalSelector.addGoal(8, new OtterBellySlideGoal(this));
        this.goalSelector.addGoal(8, new OtterAdultDiveGoal(this));
        this.goalSelector.addGoal(8, new OtterWatchAndLearnGoal(this));
        this.goalSelector.addGoal(9, new OtterPebblePlayGoal(this));
        this.goalSelector.addGoal(10, new OtterWanderGoal(this, 1.0D));
        this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(12, new RandomLookAroundGoal(this));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        OtterVariant variant;
        if (spawnData instanceof OtterGroupData group) {
            variant = group.variant;
        } else {
            variant = OtterConfig.pickVariant(level.getRandom());
            spawnData = new OtterGroupData(variant);
        }
        setVariant(variant);
        return super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
    }

    /** Families: the first otter is an adult, the others are usually pups, all sharing one palette. */
    public static class OtterGroupData extends AgeableMob.AgeableMobGroupData {
        public final OtterVariant variant;

        public OtterGroupData(OtterVariant variant) {
            super(0.75F);
            this.variant = variant;
        }
    }

    // ================================================================ save / load
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", getVariant().ordinal());
        tag.putBoolean("Waiting", isWaiting());
        if (waitAnchor != null) {
            tag.putLong("WaitAnchor", waitAnchor.asLong());
        }
        tag.putBoolean("HasPebble", hasPebble());
        if (!carried.isEmpty()) {
            CompoundTag carry = new CompoundTag();
            carry.put("Item", carried.save(new CompoundTag()));
            carry.putInt("Lifetime", carryLifetime);
            carry.putBoolean("Unlimited", carryUnlimited);
            carry.putInt("Ticks", carryTicks);
            tag.put("PendingSettlement", carry);
        }
        CompoundTag cooldowns = new CompoundTag();
        cooldowns.putInt("Chirp", chirpCooldown);
        cooldowns.putInt("Slide", slideCooldown);
        cooldowns.putInt("Groom", groomCooldown);
        cooldowns.putInt("Dive", diveCooldown);
        cooldowns.putInt("Pebble", pebbleCooldown);
        cooldowns.putInt("Pair", pairCooldown);
        cooldowns.putInt("Fishing", fishingCooldown);
        cooldowns.putInt("Splash", splashCooldown);
        tag.put("Cooldowns", cooldowns);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setVariant(OtterVariant.byId(tag.getInt("Variant")));
        setWaiting(tag.getBoolean("Waiting"));
        waitAnchor = tag.contains("WaitAnchor", Tag.TAG_LONG) ? BlockPos.of(tag.getLong("WaitAnchor")) : null;
        setHasPebble(tag.getBoolean("HasPebble") || isTame());
        if (tag.contains("PendingSettlement", Tag.TAG_COMPOUND)) {
            CompoundTag carry = tag.getCompound("PendingSettlement");
            setCarriedInternal(ItemStack.of(carry.getCompound("Item")));
            carryLifetime = carry.getInt("Lifetime");
            carryUnlimited = carry.getBoolean("Unlimited");
            carryTicks = carry.getInt("Ticks");
        }
        CompoundTag cooldowns = tag.getCompound("Cooldowns");
        chirpCooldown = cooldowns.getInt("Chirp");
        slideCooldown = cooldowns.getInt("Slide");
        groomCooldown = cooldowns.getInt("Groom");
        diveCooldown = cooldowns.getInt("Dive");
        pebbleCooldown = cooldowns.getInt("Pebble");
        pairCooldown = cooldowns.getInt("Pair");
        fishingCooldown = cooldowns.getInt("Fishing");
        splashCooldown = cooldowns.getInt("Splash");
        // Rebuild: transient relationship states and decorative actions never survive a reload.
        setBondState(isTame() ? BondState.BONDED : BondState.UNBONDED);
        setAction(OtterAction.NONE);
    }

    // ================================================================ synced accessors
    public OtterVariant getVariant() {
        return OtterVariant.byId(this.entityData.get(DATA_VARIANT));
    }

    public void setVariant(OtterVariant variant) {
        this.entityData.set(DATA_VARIANT, variant.ordinal());
    }

    public BondState getBondState() {
        return BondState.byId(this.entityData.get(DATA_BOND_STATE));
    }

    private void setBondState(BondState state) {
        this.entityData.set(DATA_BOND_STATE, (byte) state.ordinal());
    }

    public OtterAction getAction() {
        return OtterAction.byId(this.entityData.get(DATA_ACTION));
    }

    public void setAction(OtterAction action) {
        this.entityData.set(DATA_ACTION, (byte) action.ordinal());
    }

    public boolean isWaiting() {
        return this.entityData.get(DATA_WAITING);
    }

    private void setWaiting(boolean waiting) {
        this.entityData.set(DATA_WAITING, waiting);
    }

    public boolean hasPebble() {
        return this.entityData.get(DATA_HAS_PEBBLE);
    }

    private void setHasPebble(boolean value) {
        this.entityData.set(DATA_HAS_PEBBLE, value);
    }

    /** The carried tool as seen by the renderer (client copy). */
    public ItemStack getCarriedForRender() {
        return this.entityData.get(DATA_CARRIED);
    }

    public boolean isCarrying() {
        return level().isClientSide ? !getCarriedForRender().isEmpty() : !carried.isEmpty();
    }

    /** One visual prop: the pebble is only shown while playing, never while carrying a tool. */
    public boolean isPebbleVisible() {
        return hasPebble() && getAction() == OtterAction.PEBBLE_PLAY && getCarriedForRender().isEmpty();
    }

    public boolean allowsHeadLook() {
        OtterAction action = getAction();
        return !action.isOnBack() && action != OtterAction.SLIDE && action != OtterAction.GROOM
                && action != OtterAction.DIVE && action != OtterAction.AWKWARD_DIVE;
    }

    // ================================================================ GeckoLib
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CTRL_MOVEMENT, 4, this::movementState));
        controllers.add(new AnimationController<>(this, CTRL_ACTION, 2, state -> PlayState.STOP)
                .triggerableAnim("chirp", TRIG_CHIRP)
                .triggerableAnim("nod", TRIG_NOD)
                .triggerableAnim("deliver", TRIG_DELIVER)
                .triggerableAnim("accept", TRIG_ACCEPT)
                .triggerableAnim("splash", TRIG_SPLASH)
                .triggerableAnim("startle", TRIG_STARTLE));
        controllers.add(new AnimationController<>(this, CTRL_MOUTH, 3, this::mouthState));
    }

    private PlayState movementState(AnimationState<OtterEntity> state) {
        state.getController().setAnimationSpeed(1.0D);
        switch (getAction()) {
            case SLIDE:
                return state.setAndContinue(SLIDE);
            case GROOM:
                return state.setAndContinue(GROOM);
            case DIVE:
                return state.setAndContinue(DIVE);
            case AWKWARD_DIVE:
                return state.setAndContinue(AWKWARD_DIVE);
            case WATCH:
                return state.setAndContinue(SIT);
            case PEBBLE_PLAY:
                return state.setAndContinue(PEBBLE_PLAY);
            case PAIR_LEFT_PAW:
                return state.setAndContinue(PAIR_LEFT_PAW);
            case PAIR_RIGHT_PAW:
                return state.setAndContinue(PAIR_RIGHT_PAW);
            default:
                break;
        }
        boolean moving = state.isMoving();
        if (isInWater()) {
            return state.setAndContinue(moving ? SWIM : FLOAT);
        }
        if (moving) {
            state.getController().setAnimationSpeed(Mth.clamp(0.8D + state.getLimbSwingAmount() * 0.8D, 0.8D, 2.0D));
            return state.setAndContinue(WALK);
        }
        BondState bond = getBondState();
        if (bond == BondState.INSPECTING) {
            return state.setAndContinue(INSPECT);
        }
        if (bond == BondState.CURIOUS) {
            return state.setAndContinue(CURIOUS);
        }
        if (isInSittingPose()) {
            return state.setAndContinue(SIT);
        }
        return state.setAndContinue(IDLE);
    }

    private PlayState mouthState(AnimationState<OtterEntity> state) {
        return getCarriedForRender().isEmpty() ? PlayState.STOP : state.setAndContinue(CARRY);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    // ================================================================ physics
    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return !getAction().isPaired() && super.isPushable();
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isControlledByLocalInstance() && this.isInWater()) {
            this.moveRelative(this.getSpeed(), travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            Vec3 motion = this.getDeltaMovement().scale(0.9D);
            if (shouldFloatAtSurface()) {
                // Float with the belly at the surface instead of sinking: safe, bounded drift.
                double depth = waterDepthAtFeet();
                double vy = Mth.clamp(motion.y + (depth - FLOAT_DEPTH) * 0.04D, -0.08D, 0.08D);
                motion = new Vec3(motion.x, vy, motion.z);
            }
            this.setDeltaMovement(motion);
        } else {
            super.travel(travelVector);
        }
    }

    /**
     * How far the feet are below the water surface, measured from the blocks themselves
     * (independent of Forge's fluid-type bookkeeping). 0 when the feet are not in water.
     */
    public double waterDepthAtFeet() {
        Level level = level();
        BlockPos pos = BlockPos.containing(getX(), getY(), getZ());
        if (!level.getFluidState(pos).is(FluidTags.WATER)) {
            return 0.0D;
        }
        BlockPos top = pos;
        for (int i = 0; i < 32 && level.getFluidState(top.above()).is(FluidTags.WATER); i++) {
            top = top.above();
        }
        double surface = top.getY() + level.getFluidState(top).getHeight(level, top);
        return Math.max(0.0D, surface - getY());
    }

    private boolean shouldFloatAtSurface() {
        return !diveActive && this.getNavigation().isDone();
    }

    public void setDiveActive(boolean value) {
        this.diveActive = value;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            swimPitchO = swimPitch;
            double dx = getX() - xo;
            double dy = getY() - yo;
            double dz = getZ() - zo;
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            float target = 0.0F;
            if (isInWater() && horizontal + Math.abs(dy) > 0.02D && !getAction().isOnBack()) {
                target = (float) (-Mth.atan2(dy, horizontal) * Mth.RAD_TO_DEG);
            }
            swimPitch += (Mth.clamp(target, -60.0F, 60.0F) - swimPitch) * 0.2F;
        }
    }

    // ================================================================ server AI step
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        tickCooldowns();
        tickWaterTracking();
        tickBonding();
        tickGreeting();
        tickCarry();
        tickWaitState();
        if (lookAtOwnerTicks > 0) {
            lookAtOwnerTicks--;
            LivingEntity owner = getOwner();
            if (owner != null) {
                getLookControl().setLookAt(owner, 30.0F, 30.0F);
            }
        }
    }

    private void tickCooldowns() {
        if (chirpCooldown > 0) chirpCooldown--;
        if (slideCooldown > 0) slideCooldown--;
        if (groomCooldown > 0) groomCooldown--;
        if (diveCooldown > 0) diveCooldown--;
        if (pebbleCooldown > 0) pebbleCooldown--;
        if (pairCooldown > 0) pairCooldown--;
        if (fishingCooldown > 0) fishingCooldown--;
        if (splashCooldown > 0) splashCooldown--;
        if (startledTicks > 0) startledTicks--;
        if (bondRestTicks > 0) bondRestTicks--;
    }

    private void tickWaterTracking() {
        if (isInWater()) {
            ticksInWater++;
            ticksOutOfWater = 0;
            lastSwimTicks = ticksInWater;
        } else {
            ticksInWater = 0;
            ticksOutOfWater++;
        }
    }

    // ================================================================ bond: Approach -> Inspect -> Accept
    private void tickBonding() {
        if (isTame()) {
            if (getBondState() != BondState.BONDED) {
                setBondState(BondState.BONDED);
            }
            return;
        }
        if (!isBaby()) {
            if (getBondState() != BondState.UNBONDED) {
                resetBond();
            }
            return;
        }
        switch (getBondState()) {
            case UNBONDED -> tickUnbonded();
            case CURIOUS -> tickCurious();
            case INSPECTING -> tickInspecting();
            default -> setBondState(BondState.UNBONDED);
        }
    }

    private void tickUnbonded() {
        if (bondRestTicks > 0 || startledTicks > 0) {
            calmTicks = 0;
            return;
        }
        Player candidate = findPlayerWithOffering(OtterConfig.CURIOUS_RANGE.get());
        if (candidate == null) {
            calmTicks = 0;
            curiousPlayer = null;
            lastPlayerPos = null;
            return;
        }
        if (!candidate.getUUID().equals(curiousPlayer)) {
            curiousPlayer = candidate.getUUID();
            calmTicks = 0;
            lastPlayerPos = null;
        }
        if (isCalm(candidate)) {
            // Give the pup space: the calm counter only runs while the player keeps a little distance.
            if (distanceToSqr(candidate) > 2.0D * 2.0D && ++calmTicks >= OtterConfig.CALM_TICKS.get()) {
                setBondState(BondState.CURIOUS);
                curiousTicks = 0;
            }
        } else {
            calmTicks = 0;
            if (candidate.isSprinting() && distanceToSqr(candidate) < 5.0D * 5.0D) {
                startle(candidate);
            }
        }
    }

    private void tickCurious() {
        Player player = getPlayer(curiousPlayer);
        if (player == null || distanceToSqr(player) > 10.0D * 10.0D || findOfferingHand(player, null) == null || !isCalm(player)) {
            resetBond();
            return;
        }
        if (++curiousTicks > 600) {
            resetBond(); // lost interest
            bondRestTicks = 200;
        }
    }

    private void tickInspecting() {
        Player player = getPlayer(inspector);
        if (player == null || !player.isAlive() || distanceToSqr(player) > 4.0D * 4.0D || player.isSprinting()
                || this.hurtTime > 0 || findOfferingHand(player, inspectHand) == null) {
            interruptInspection(player);
            return;
        }
        if (++inspectTicks >= OtterConfig.INSPECT_TICKS.get()) {
            commitBond(player);
        }
    }

    /** Bond commit: consume exactly one fish and persist one owner together (single server tick). */
    private void commitBond(Player player) {
        InteractionHand hand = findOfferingHand(player, inspectHand);
        if (hand == null || isTame()) {
            interruptInspection(player);
            return;
        }
        if (ForgeEventFactory.onAnimalTame(this, player)) {
            interruptInspection(player);
            return;
        }
        ItemStack offer = player.getItemInHand(hand);
        if (!player.getAbilities().instabuild) {
            offer.shrink(1);
        }
        this.tame(player);
        this.setPersistenceRequired();
        setHasPebble(true);
        setWaiting(false);
        waitAnchor = null;
        setBondState(BondState.BONDED);
        inspector = null;
        inspectHand = null;
        curiousPlayer = null;
        calmTicks = 0;
        getNavigation().stop();
        level().broadcastEntityEvent(this, (byte) 7); // vanilla taming hearts
        triggerAnim(CTRL_ACTION, "accept");
        playSound(ModSounds.OTTER_CHIRP.get(), 1.0F, 1.4F);
        player.displayClientMessage(Component.translatable("message.otterpebble.bonded", getDisplayName()), true);
    }

    private void interruptInspection(@Nullable Player player) {
        resetBond();
        bondRestTicks = 60;
        if (player != null) {
            player.displayClientMessage(Component.translatable("message.otterpebble.interrupted"), true);
        }
    }

    private void resetBond() {
        setBondState(BondState.UNBONDED);
        curiousPlayer = null;
        inspector = null;
        inspectHand = null;
        calmTicks = 0;
        curiousTicks = 0;
        inspectTicks = 0;
        lastPlayerPos = null;
    }

    private InteractionResult handleOffer(Player player, InteractionHand hand) {
        if (!isBaby()) {
            return InteractionResult.PASS; // only a pup chooses the offer
        }
        switch (getBondState()) {
            case CURIOUS:
                if (player.getUUID().equals(curiousPlayer)) {
                    inspector = player.getUUID();
                    inspectHand = hand;
                    inspectTicks = 0;
                    setBondState(BondState.INSPECTING);
                    getNavigation().stop();
                } else {
                    player.displayClientMessage(Component.translatable("message.otterpebble.busy"), true);
                }
                return InteractionResult.SUCCESS;
            case INSPECTING:
                if (!player.getUUID().equals(inspector)) {
                    player.displayClientMessage(Component.translatable("message.otterpebble.busy"), true);
                }
                return InteractionResult.SUCCESS; // spam or a second player: nothing is consumed
            default:
                // Too soon: the pup flinches back and the player keeps the item.
                startle(player);
                player.displayClientMessage(Component.translatable("message.otterpebble.too_soon"), true);
                return InteractionResult.SUCCESS;
        }
    }

    private boolean isCalm(Player player) {
        Vec3 now = player.position();
        double moved = lastPlayerPos == null ? 0.0D : now.distanceTo(lastPlayerPos);
        lastPlayerPos = now;
        boolean hostile = getLastHurtByMob() == player;
        return !player.isSprinting() && moved < 0.25D && !hostile && !player.isSpectator();
    }

    @Nullable
    private Player findPlayerWithOffering(double range) {
        List<Player> players = level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(range),
                p -> p.isAlive() && !p.isSpectator() && findOfferingHand(p, null) != null);
        Player best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Player p : players) {
            double d = distanceToSqr(p);
            if (d < bestDistance) {
                bestDistance = d;
                best = p;
            }
        }
        return best;
    }

    @Nullable
    private static InteractionHand findOfferingHand(Player player, @Nullable InteractionHand preferred) {
        if (preferred != null && player.getItemInHand(preferred).is(ModTags.BOND_OFFERINGS)) {
            return preferred;
        }
        if (player.getMainHandItem().is(ModTags.BOND_OFFERINGS)) {
            return InteractionHand.MAIN_HAND;
        }
        if (player.getOffhandItem().is(ModTags.BOND_OFFERINGS)) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    @Nullable
    private Player getPlayer(@Nullable UUID id) {
        return id == null ? null : level().getPlayerByUUID(id);
    }

    /** The player the pup is paying attention to (curious or inspecting). */
    @Nullable
    public Player getBondFocusPlayer() {
        return getPlayer(getBondState() == BondState.INSPECTING ? inspector : curiousPlayer);
    }

    public boolean isStartled() {
        return startledTicks > 0;
    }

    private void startle(@Nullable Player cause) {
        startledTicks = 60;
        calmTicks = 0;
        if (!isTame() && getBondState() != BondState.UNBONDED) {
            resetBond();
        }
        triggerAnim(CTRL_ACTION, "startle");
        if (cause != null) {
            Vec3 away = position().subtract(cause.position());
            if (away.lengthSqr() > 1.0E-4D) {
                away = away.normalize().scale(0.3D);
                setDeltaMovement(getDeltaMovement().add(away.x, 0.25D, away.z));
            }
        }
    }

    // ================================================================ interaction: orders, care, offers
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level().isClientSide) {
            boolean owner = isTame() && isOwnedBy(player);
            boolean relevant = (owner && hand == InteractionHand.MAIN_HAND && stack.isEmpty())
                    || (owner && stack.is(ModTags.CARE_FOOD) && (getHealth() < getMaxHealth() || isBaby()))
                    || (!isTame() && isBaby() && stack.is(ModTags.BOND_OFFERINGS));
            return relevant ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (isTame()) {
            if (!isOwnedBy(player)) {
                return InteractionResult.PASS;
            }
            if (hand == InteractionHand.MAIN_HAND && stack.isEmpty()) {
                toggleOrder(player);
                return InteractionResult.SUCCESS;
            }
            if (stack.is(ModTags.CARE_FOOD) && (getHealth() < getMaxHealth() || isBaby())) {
                care(player, stack);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }
        if (stack.is(ModTags.BOND_OFFERINGS)) {
            return handleOffer(player, hand);
        }
        return super.mobInteract(player, hand);
    }

    /** Empty-hand use toggles Follow / Wait. Animations never change an order; this is the only place it changes. */
    private void toggleOrder(Player player) {
        boolean waitNow = !isWaiting();
        setWaiting(waitNow);
        releasePair();
        if (waitNow) {
            waitAnchor = findSafeAnchor(blockPosition());
            getNavigation().stop();
            if (!carried.isEmpty()) {
                settleCarried(true); // Wait suppresses rescue: the tool is settled right away
            }
        } else {
            waitAnchor = null;
            setInSittingPose(false);
        }
        triggerAnim(CTRL_ACTION, "nod");
        player.displayClientMessage(Component.translatable(waitNow ? "message.otterpebble.order.wait"
                : "message.otterpebble.order.follow", getDisplayName()), true);
    }

    private void care(Player player, ItemStack food) {
        if (!player.getAbilities().instabuild) {
            food.shrink(1);
        }
        heal(3.0F);
        if (isBaby()) {
            ageUp(AgeableMob.getSpeedUpSecondsWhenFeeding(-getAge()), true);
        }
        triggerAnim(CTRL_ACTION, "accept");
        playSound(SoundEvents.GENERIC_EAT, 0.6F, 1.3F);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.2D, getZ(), 2, 0.2D, 0.1D, 0.2D, 0.0D);
        }
    }

    // ================================================================ wait anchor
    public boolean isOrderedToWait() {
        return isTame() && isWaiting();
    }

    @Nullable
    public BlockPos getWaitAnchor() {
        return waitAnchor;
    }

    public void setWaitAnchor(@Nullable BlockPos anchor) {
        this.waitAnchor = anchor;
    }

    /** A legal place to wait: water or standable land, away from hazards. */
    public BlockPos findSafeAnchor(BlockPos start) {
        if (isLegalAnchor(start)) {
            return start.immutable();
        }
        return BlockPos.findClosestMatch(start, 4, 3, this::isLegalAnchor).map(BlockPos::immutable).orElse(start.immutable());
    }

    public boolean isLegalAnchor(BlockPos pos) {
        LevelReader level = level();
        if (!WaterHelper.isAreaSafe(level, pos, 1)) {
            return false;
        }
        return WaterHelper.isWater(level, pos) ? WaterHelper.isPassable(level, pos.above()) || WaterHelper.isWater(level, pos.above())
                : WaterHelper.isStandable(level, pos);
    }

    private void tickWaitState() {
        boolean waiting = isOrderedToWait();
        if (waiting && waitAnchor == null) {
            waitAnchor = findSafeAnchor(blockPosition());
        }
        if (waiting && tickCount % 40 == 0 && waitAnchor != null && !isLegalAnchor(waitAnchor)) {
            waitAnchor = findSafeAnchor(waitAnchor); // the anchor became unsafe: return to a legal state
        }
        boolean sit = waiting && onGround() && !isInWater() && getNavigation().isDone()
                && getAction() == OtterAction.NONE && carried.isEmpty();
        if (isInSittingPose() != sit) {
            setInSittingPose(sit);
        }
    }

    // ================================================================ chirp & answer
    private void tickGreeting() {
        if (!isTame() || tickCount % 2 != 0) {
            return;
        }
        LivingEntity ownerEntity = getOwner();
        if (!(ownerEntity instanceof Player owner) || owner.level() != level()) {
            gazeTicks = 0;
            gazeLatched = false;
            return;
        }
        boolean looking = owner.getMainHandItem().isEmpty() && isLookedAtBy(owner);
        if (!looking) {
            gazeTicks = 0;
            gazeLatched = false; // the owner must look away before the next greeting: no call loops
            return;
        }
        if (gazeLatched || chirpCooldown > 0 || isCarrying()) {
            return;
        }
        gazeTicks += 2;
        if (gazeTicks >= OtterConfig.GAZE_TICKS.get()) {
            chirp();
            gazeLatched = true;
            chirpCooldown = OtterConfig.CHIRP_COOLDOWN_SECONDS.get() * 20;
        }
    }

    private boolean isLookedAtBy(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 target = new Vec3(getX(), getY(0.5D), getZ());
        Vec3 toOtter = target.subtract(eye);
        double distance = toOtter.length();
        if (distance > 12.0D || distance < 0.5D) {
            return false;
        }
        double dot = player.getViewVector(1.0F).normalize().dot(toOtter.normalize());
        return dot > 1.0D - 0.06D / distance && player.hasLineOfSight(this);
    }

    /** A brief chirp with a head turn. The current order stays the same. */
    public void chirp() {
        triggerAnim(CTRL_ACTION, "chirp");
        playSound(ModSounds.OTTER_CHIRP.get(), 1.0F, getVoicePitch());
        lookAtOwnerTicks = 30;
        if (level() instanceof ServerLevel server) {
            // visible cue that works without sound
            server.sendParticles(ParticleTypes.NOTE, getX(), getY() + getBbHeight() + 0.35D, getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    // ================================================================ rescue: carry record and settlement
    public boolean canStartRescue() {
        return OtterConfig.RESCUE_ENABLED.get() && isTame() && !isWaiting() && carried.isEmpty() && isAlive()
                && getBondState() == BondState.BONDED;
    }

    public void setRescueActive(boolean active) {
        this.rescueActive = active;
        if (active) {
            releasePair();
        }
    }

    public boolean isRescueActive() {
        return rescueActive;
    }

    /** Claim &amp; dive ends here: the item entity is removed and the record is created in the same tick. */
    public boolean pickUpRescue(ItemEntity item) {
        if (!item.isAlive() || item.getItem().isEmpty() || !carried.isEmpty() || !RescueTracker.isClaimedBy(item, getUUID())) {
            return false;
        }
        ItemStack stack = item.getItem().copy(); // same type, count, condition and metadata
        int age = item.getAge();
        boolean unlimited = age < 0 && age <= -32000;
        int lifetime = item.lifespan - age;
        item.discard();
        setCarriedInternal(stack);
        carryLifetime = Math.max(1, lifetime);
        carryUnlimited = unlimited;
        carryTicks = 0;
        ownerMissingTicks = 0;
        playSound(SoundEvents.ITEM_PICKUP, 0.4F, 1.4F);
        return true;
    }

    /** One safe pickup: straight into the owner's inventory; a full inventory leaves one safe world drop. */
    public void deliverCarried(Player owner) {
        if (carried.isEmpty()) {
            return;
        }
        ItemStack stack = carried;
        Component name = stack.getHoverName();
        int lifetime = carryLifetime;
        boolean unlimited = carryUnlimited;
        setCarriedInternal(ItemStack.EMPTY); // resolve the record before anything else can settle it
        owner.getInventory().add(stack);
        if (!stack.isEmpty()) {
            spawnSettledDrop(stack, owner.position(), lifetime, unlimited, owner);
        }
        level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.3F, 1.2F);
        triggerAnim(CTRL_ACTION, "deliver");
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, owner.getX(), owner.getY() + 1.0D, owner.getZ(), 5, 0.3D, 0.3D, 0.3D, 0.0D);
        }
        owner.displayClientMessage(Component.translatable("message.otterpebble.delivered", getDisplayName(), name), true);
    }

    /**
     * Settlement for failure, expiry, orders or death: exactly once, the record is cleared first.
     * If the owner is right here the tool goes to them, otherwise it is dropped where the otter is.
     */
    public void settleCarried(boolean allowGive) {
        if (carried.isEmpty() || level().isClientSide) {
            return;
        }
        ItemStack stack = carried;
        int lifetime = carryLifetime;
        boolean unlimited = carryUnlimited;
        setCarriedInternal(ItemStack.EMPTY);
        LivingEntity ownerEntity = getOwner();
        if (allowGive && ownerEntity instanceof Player owner && owner.isAlive() && owner.level() == level()
                && distanceToSqr(owner) < 3.0D * 3.0D) {
            owner.getInventory().add(stack);
            if (stack.isEmpty()) {
                return;
            }
            spawnSettledDrop(stack, owner.position(), lifetime, unlimited, owner);
            return;
        }
        spawnSettledDrop(stack, position().add(0.0D, 0.3D, 0.0D), lifetime, unlimited, null);
    }

    private void spawnSettledDrop(ItemStack stack, Vec3 pos, int lifetime, boolean unlimited, @Nullable Player onlyFor) {
        if (stack.isEmpty()) {
            return;
        }
        ItemEntity drop = new ItemEntity(level(), pos.x, pos.y, pos.z, stack);
        drop.setDeltaMovement(Vec3.ZERO);
        if (unlimited) {
            drop.setUnlimitedLifetime();
        } else {
            drop.lifespan = Math.max(1, lifetime); // remaining lifetime is preserved, never reset
        }
        UUID ownerId = getOwnerUUID();
        if (ownerId != null) {
            drop.setThrower(ownerId);
        }
        if (onlyFor != null) {
            drop.setTarget(onlyFor.getUUID());
            drop.setNoPickUpDelay();
        } else {
            drop.setPickUpDelay(10);
        }
        RescueTracker.markSettled(drop);
        level().addFreshEntity(drop);
    }

    private void setCarriedInternal(ItemStack stack) {
        this.carried = stack;
        this.entityData.set(DATA_CARRIED, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
    }

    private void tickCarry() {
        if (carried.isEmpty()) {
            ownerMissingTicks = 0;
            return;
        }
        carryTicks++;
        if (!carryUnlimited && --carryLifetime <= 0) {
            settleCarried(false); // expiry: the tool's own lifetime ran out
            return;
        }
        LivingEntity owner = getOwner();
        boolean ownerPresent = owner != null && owner.isAlive() && owner.level() == level() && distanceToSqr(owner) < 64.0D * 64.0D;
        ownerMissingTicks = ownerPresent ? 0 : ownerMissingTicks + 1;
        if (ownerMissingTicks > 200 || carryTicks > OtterConfig.MAX_CARRY_SECONDS.get() * 20 || isOrderedToWait()) {
            settleCarried(true);
        }
    }

    // ================================================================ owner helpers
    @Nullable
    public Player getOwnerPlayer() {
        LivingEntity owner = getOwner();
        return owner instanceof Player player && player.isAlive() && !player.isSpectator() && player.level() == level() ? player : null;
    }

    // ================================================================ social: pair float
    @Nullable
    public OtterEntity getPartner() {
        if (partnerId == null || !(level() instanceof ServerLevel server)) {
            return null;
        }
        Entity entity = server.getEntity(partnerId);
        return entity instanceof OtterEntity otter && otter.isAlive() ? otter : null;
    }

    public boolean hasPartner() {
        return partnerId != null;
    }

    public boolean isPairLeader() {
        return pairLeader;
    }

    public boolean isPartnerOf(OtterEntity other) {
        return getUUID().equals(other.partnerId) && other.getUUID().equals(partnerId);
    }

    /** One mutual partner lock: both sides are written in the same tick or not at all. */
    public boolean tryLockPartner(OtterEntity other) {
        if (other == this || this.partnerId != null || other.partnerId != null) {
            return false;
        }
        this.partnerId = other.getUUID();
        other.partnerId = this.getUUID();
        this.pairLeader = true;
        other.pairLeader = false;
        return true;
    }

    public void releasePair() {
        OtterEntity partner = getPartner();
        if (partner != null && getUUID().equals(partner.partnerId)) {
            partner.partnerId = null;
            partner.pairLeader = false;
            partner.pairAnchor = null;
            partner.pairCooldown = Math.max(partner.pairCooldown, 1200);
        }
        if (partnerId != null) {
            pairCooldown = Math.max(pairCooldown, 1200);
        }
        partnerId = null;
        pairLeader = false;
        pairAnchor = null;
    }

    /** Shared anchor and facing of a floating pair (kept by the leader). */
    public void setPairAnchor(@Nullable Vec3 anchor, float yaw) {
        this.pairAnchor = anchor;
        this.pairYaw = yaw;
    }

    @Nullable
    public Vec3 getPairAnchor() {
        return pairAnchor;
    }

    public float getPairYaw() {
        return pairYaw;
    }

    public boolean canJoinPair() {
        return OtterConfig.PAIRED_FLOAT.get() && !isBaby() && isInWater() && getAction() == OtterAction.NONE
                && carried.isEmpty() && !isWaiting() && pairCooldown == 0 && partnerId == null && !rescueActive
                && getBondState() != BondState.INSPECTING;
    }

    // ================================================================ learning by watching
    public void watchAdultDive(OtterEntity adult) {
        if (isBaby() && OtterConfig.LEARNING_BY_WATCHING.get() && getAction() == OtterAction.NONE && carried.isEmpty()) {
            this.watchedAdult = adult;
            this.watchedAdultTick = tickCount;
        }
    }

    /** The adult whose dive the pup just saw; the moment passes after a few seconds. */
    @Nullable
    public OtterEntity getWatchedAdult() {
        if (watchedAdult != null && tickCount - watchedAdultTick > 60) {
            watchedAdult = null;
        }
        return watchedAdult;
    }

    public void clearWatchedAdult() {
        watchedAdult = null;
    }

    // ================================================================ water tracking for grooming
    public int getTicksOutOfWater() {
        return ticksOutOfWater;
    }

    public int getLastSwimTicks() {
        return lastSwimTicks;
    }

    public void markGroomed() {
        lastSwimTicks = 0;
    }

    // ================================================================ cooldown access for goals
    public boolean isSlideReady() {
        return slideCooldown == 0;
    }

    public void startSlideCooldown() {
        slideCooldown = 1200 + getRandom().nextInt(1200);
    }

    public boolean isGroomReady() {
        return groomCooldown == 0;
    }

    public void startGroomCooldown() {
        groomCooldown = 900 + getRandom().nextInt(600);
    }

    public boolean isDiveReady() {
        return diveCooldown == 0;
    }

    public void startDiveCooldown() {
        diveCooldown = 900 + getRandom().nextInt(900);
    }

    public boolean isPebbleReady() {
        return pebbleCooldown == 0;
    }

    public void startPebbleCooldown() {
        pebbleCooldown = 900 + getRandom().nextInt(900);
    }

    public void startPairCooldown() {
        pairCooldown = 2400 + getRandom().nextInt(1200);
    }

    // ================================================================ Appendix D (off by default)
    public boolean canHelpFishing() {
        return isTame() && fishingCooldown == 0 && carried.isEmpty();
    }

    /** One extra ordinary fish after the owner's rod catch; the original catch is untouched. */
    public void helpWithFishing(Player owner, Vec3 hookPos) {
        fishingCooldown = OtterConfig.FISHING_COOLDOWN_SECONDS.get() * 20;
        ItemStack fish = new ItemStack(getRandom().nextFloat() < 0.7F ? Items.COD : Items.SALMON);
        ItemEntity item = new ItemEntity(level(), hookPos.x, hookPos.y, hookPos.z, fish);
        double dx = owner.getX() - hookPos.x;
        double dy = owner.getY() - hookPos.y;
        double dz = owner.getZ() - hookPos.z;
        item.setDeltaMovement(dx * 0.1D, dy * 0.1D + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.08D, dz * 0.1D);
        level().addFreshEntity(item);
        chirp();
    }

    public boolean canSplash() {
        return isTame() && splashCooldown == 0 && isAlive();
    }

    /** Drowned escape splash: one brief pushback away from the owner. No damage, pursuit or combat command. */
    public void splashAway(Drowned drowned, Player owner) {
        splashCooldown = OtterConfig.DROWNED_SPLASH_COOLDOWN_SECONDS.get() * 20;
        drowned.knockback(0.9D, owner.getX() - drowned.getX(), owner.getZ() - drowned.getZ());
        drowned.hurtMarked = true;
        triggerAnim(CTRL_ACTION, "splash");
        level().playSound(null, drowned.getX(), drowned.getY(), drowned.getZ(), SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL, 0.8F, 1.2F);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SPLASH, drowned.getX(), drowned.getY() + 0.5D, drowned.getZ(), 20, 0.4D, 0.3D, 0.4D, 0.1D);
        }
    }

    // ================================================================ danger, death and removal
    public void onDanger() {
        releasePair();
        if (getAction() != OtterAction.NONE) {
            setAction(OtterAction.NONE);
        }
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) {
            settleCarried(false);
            releasePair();
        }
        super.die(source);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide && reason.shouldDestroy()) {
            settleCarried(false);
            releasePair();
        }
        super.remove(reason);
    }

    // ================================================================ breeding (spawn egg on an otter makes a pup)
    @Override
    public boolean isFood(ItemStack stack) {
        return false; // no breeding in the concept; fish is used for bonding and care instead
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob parent) {
        OtterEntity pup = ModEntities.OTTER.get().create(level);
        if (pup != null) {
            pup.setVariant(parent instanceof OtterEntity otter ? otter.getVariant() : getVariant());
        }
        return pup;
    }

    // ================================================================ misc vanilla hooks
    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        if (WaterHelper.isWater(level, pos)) {
            return 10.0F;
        }
        return WaterHelper.hasWaterNearby(level, pos, 3) ? 8.0F : level.getPathfindingCostFromLightLevels(pos);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.OTTER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.OTTER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.OTTER_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.6F;
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return !getAction().isPaired() && super.canBeLeashed(player);
    }

    /** Small helper used by goals to face a direction instantly. */
    public void faceDirection(Direction direction) {
        float yaw = direction.toYRot();
        setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }
}
