package net.mrconqueso.pswg.item.custom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.UnboundedMapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.CrashReport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.mrconqueso.pswg.PSWG;
import net.mrconqueso.pswg.attributes.AttributeUtil;
import net.mrconqueso.pswg.attributes.ModEntityAttributes;
import net.mrconqueso.pswg.data.blaster.BlasterDatapackDefinition;
import net.mrconqueso.pswg.data.blaster.BlasterDescriptor;
import net.mrconqueso.pswg.entity.ModEntities;
import net.mrconqueso.pswg.entity.custom.BlasterBoltEntity;
import net.mrconqueso.pswg.interaction.IHandAnimationAware;
import net.mrconqueso.pswg.interaction.ILeftClickUsable;
import net.mrconqueso.pswg.interaction.IRecoilEntity;
import net.mrconqueso.pswg.item.IPrimaryActionHandler;
import net.mrconqueso.pswg.item.ModItems;
import net.mrconqueso.pswg.item.blaster.BlasterComponents;
import net.mrconqueso.pswg.math.Combinator;
import net.mrconqueso.pswg.math.GMath;
import net.mrconqueso.pswg.math.RandomHelper;
import net.mrconqueso.pswg.networking.GalaxiesCodecs;
import net.mrconqueso.pswg.networking.GalaxiesPacketCodecs;
import net.mrconqueso.pswg.sound.ModSounds;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

import static net.mrconqueso.pswg.item.blaster.BlasterComponents.*;

public class BlasterItem extends Item implements ILeftClickUsable, IPrimaryActionHandler, IHandAnimationAware
{
    public enum CoolingMode
    {
        PASSIVE(false, false),
        OVERHEAT(true, true),
        REQUESTED_BYPASS(true, false),
        FAILED_OVERCHARGE(true, false);

        private final boolean isCooling;
        private final boolean canBypass;

        CoolingMode(boolean isCooling, boolean canBypass)
        {
            this.isCooling = isCooling;
            this.canBypass = canBypass;
        }

        public boolean isCooling() { return isCooling; }
        public boolean canBypass() { return canBypass; }

        public static final Codec<CoolingMode> CODEC = GalaxiesCodecs.forEnum(CoolingMode.class);
        public static final StreamCodec<ByteBuf, CoolingMode> PACKET_CODEC = GalaxiesPacketCodecs.forEnum(CoolingMode.class);
    }

    public record CoolingStatus(CoolingMode coolingMode, float totalHeat) { }

    public enum CoolingBypass
    {
        PRIMARY,
        SECONDARY
    }

    public record AvailableAttachmentsComponent(
            ResourceLocation hud,
            Map<ResourceLocation, ResourceLocation> defaults,
            Map<ResourceLocation, AttachmentDefinition> options
    )
    {
        public static final UnboundedMapCodec<ResourceLocation, AttachmentDefinition> OPTIONS_CODEC = Codec.unboundedMap(ResourceLocation.CODEC, AttachmentDefinition.CODEC);
        public static final StreamCodec<ByteBuf, Map<ResourceLocation, AttachmentDefinition>> OPTIONS_PACKET_CODEC = ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, AttachmentDefinition.PACKET_CODEC);

        public static final Codec<AvailableAttachmentsComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("hud").forGetter(AvailableAttachmentsComponent::hud),
                GalaxiesCodecs.IDENTIFIER_MAP.fieldOf("defaults").forGetter(AvailableAttachmentsComponent::defaults),
                OPTIONS_CODEC.fieldOf("options").forGetter(AvailableAttachmentsComponent::options)
        ).apply(instance, AvailableAttachmentsComponent::new));

        public static final StreamCodec<ByteBuf, AvailableAttachmentsComponent> PACKET_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, AvailableAttachmentsComponent::hud,
                GalaxiesPacketCodecs.IDENTIFIER_MAP, AvailableAttachmentsComponent::defaults,
                OPTIONS_PACKET_CODEC, AvailableAttachmentsComponent::options,
                AvailableAttachmentsComponent::new
        );
    }

    public record AttachmentsComponent(
            ResourceLocation hud,
            Map<ResourceLocation, ResourceLocation> applied
    )
    {
        public static final AttachmentsComponent DEFAULT = new AttachmentsComponent(PSWG.DEFAULT_HUD, Map.of());

        public static final Codec<AttachmentsComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("hud").forGetter(AttachmentsComponent::hud),
                GalaxiesCodecs.IDENTIFIER_MAP.fieldOf("applied").forGetter(AttachmentsComponent::applied)
        ).apply(instance, AttachmentsComponent::new));

        public static final StreamCodec<ByteBuf, AttachmentsComponent> PACKET_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, AttachmentsComponent::hud,
                GalaxiesPacketCodecs.IDENTIFIER_MAP, AttachmentsComponent::applied,
                AttachmentsComponent::new
        );

        public Optional<AttachmentDefinition> getAttachmentInSlot(Map<ResourceLocation, AttachmentDefinition> options, ResourceLocation slot)
        {
            ResourceLocation appliedEntryId = applied().getOrDefault(slot, null);
            if (appliedEntryId == null) return Optional.empty();
            return Optional.ofNullable(options.getOrDefault(appliedEntryId, null));
        }

        public float getAttachmentsValue(Map<ResourceLocation, AttachmentDefinition> options, AttachmentFunction function)
        {
            float identity = function.getCombinator().getIdentity();
            for (ResourceLocation equipped : applied().values())
            {
                AttachmentDefinition equippedValue = options.getOrDefault(equipped, null);
                if (equippedValue != null && equippedValue.function().equals(function.getId()))
                    identity = function.getCombinator().combine(identity, equippedValue.value());
            }
            return identity;
        }
    }

    public record AttachmentDefinition(
            String translationKey,
            List<ResourceLocation> slots,
            ResourceLocation function,
            ResourceLocation category,
            float value
    )
    {
        public static final Codec<AttachmentDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("translationKey").forGetter(AttachmentDefinition::translationKey),
                GalaxiesCodecs.IDENTIFIER_LIST.fieldOf("slots").forGetter(AttachmentDefinition::slots),
                ResourceLocation.CODEC.fieldOf("function").forGetter(AttachmentDefinition::function),
                ResourceLocation.CODEC.fieldOf("category").forGetter(AttachmentDefinition::category),
                Codec.FLOAT.optionalFieldOf("value", 0f).forGetter(AttachmentDefinition::value)
        ).apply(instance, AttachmentDefinition::new));

        public static final StreamCodec<ByteBuf, AttachmentDefinition> PACKET_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, AttachmentDefinition::translationKey,
                GalaxiesPacketCodecs.IDENTIFIER_LIST, AttachmentDefinition::slots,
                ResourceLocation.STREAM_CODEC, AttachmentDefinition::function,
                ResourceLocation.STREAM_CODEC, AttachmentDefinition::category,
                ByteBufCodecs.FLOAT, AttachmentDefinition::value,
                AttachmentDefinition::new
        );
    }

    public record Heat(
            int capacity, int perRound, float drainSpeed, int overheatPenalty,
            int overheatDrainSpeed, int passiveCooldownDelay, int overchargeBonus
    )
    {
        public static final Heat DEFAULT = new Heat(100, 20, 5, 60, 1, 20, 40);

        public static final Codec<Heat> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("capacity").forGetter(Heat::capacity),
                Codec.INT.fieldOf("perRound").forGetter(Heat::perRound),
                Codec.FLOAT.fieldOf("drainSpeed").forGetter(Heat::drainSpeed),
                Codec.INT.fieldOf("overheatPenalty").forGetter(Heat::overheatPenalty),
                Codec.INT.fieldOf("overheatDrainSpeed").forGetter(Heat::overheatDrainSpeed),
                Codec.INT.fieldOf("passiveCooldownDelay").forGetter(Heat::passiveCooldownDelay),
                Codec.INT.fieldOf("overchargeBonus").forGetter(Heat::overchargeBonus)
        ).apply(instance, Heat::new));

        public static final StreamCodec<ByteBuf, Heat> PACKET_CODEC = new StreamCodec<>() {
            @Override public Heat decode(ByteBuf buf) { return new Heat(buf.readInt(), buf.readInt(), buf.readFloat(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt()); }
            @Override public void encode(ByteBuf buf, Heat value) { buf.writeInt(value.capacity()); buf.writeInt(value.perRound()); buf.writeFloat(value.drainSpeed()); buf.writeInt(value.overheatPenalty()); buf.writeInt(value.overheatDrainSpeed()); buf.writeInt(value.passiveCooldownDelay()); buf.writeInt(value.overchargeBonus()); }
        };
    }

    public record Cooling(float primaryBypassTime, float primaryBypassTolerance, float secondaryBypassTime, float secondaryBypassTolerance)
    {
        public static final Cooling DEFAULT = new Cooling(0.7f, 0.1f, 0.25f, 0.05f);

        public static final Codec<Cooling> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.fieldOf("primaryBypassTime").forGetter(Cooling::primaryBypassTime),
                Codec.FLOAT.fieldOf("primaryBypassTolerance").forGetter(Cooling::primaryBypassTolerance),
                Codec.FLOAT.fieldOf("secondaryBypassTime").forGetter(Cooling::secondaryBypassTime),
                Codec.FLOAT.fieldOf("secondaryBypassTolerance").forGetter(Cooling::secondaryBypassTolerance)
        ).apply(instance, Cooling::new));

        public static final StreamCodec<ByteBuf, Cooling> PACKET_CODEC = new StreamCodec<>() {
            @Override public Cooling decode(ByteBuf buf) { return new Cooling(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat()); }
            @Override public void encode(ByteBuf buf, Cooling value) { buf.writeFloat(value.primaryBypassTime()); buf.writeFloat(value.primaryBypassTolerance()); buf.writeFloat(value.secondaryBypassTime()); buf.writeFloat(value.secondaryBypassTolerance()); }
        };
    }

    public record StatsComponent(
            float damage, int range, int automaticRepeatDelay, ResourceLocation fireSound, Heat heat, Cooling cooling
    )
    {
        public static final StatsComponent DEFAULT = new StatsComponent(8, 48, 4, ResourceLocation.withDefaultNamespace("entity.snowball.throw"), Heat.DEFAULT, Cooling.DEFAULT);

        public static final Codec<StatsComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.fieldOf("damage").forGetter(StatsComponent::damage),
                Codec.INT.fieldOf("range").forGetter(StatsComponent::range),
                Codec.INT.fieldOf("automaticRepeatDelay").forGetter(StatsComponent::automaticRepeatDelay),
                ResourceLocation.CODEC.fieldOf("fireSound").forGetter(StatsComponent::fireSound),
                Heat.CODEC.fieldOf("heat").forGetter(StatsComponent::heat),
                Cooling.CODEC.fieldOf("cooling").forGetter(StatsComponent::cooling)
        ).apply(instance, StatsComponent::new));

        public static final StreamCodec<ByteBuf, StatsComponent> PACKET_CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, StatsComponent::damage,
                ByteBufCodecs.INT, StatsComponent::range,
                ByteBufCodecs.INT, StatsComponent::automaticRepeatDelay,
                ResourceLocation.STREAM_CODEC, StatsComponent::fireSound,
                Heat.PACKET_CODEC, StatsComponent::heat,
                Cooling.PACKET_CODEC, StatsComponent::cooling,
                StatsComponent::new
        );
    }

    public record StateComponent(
            boolean isAiming, long lastFired, long fireCooldown, long cooldownStart,
            float lastTotalHeat, float lastVentingHeat, CoolingMode coolingMode,
            int burstBoltsRemaining, long overchargeStart
    )
    {
        public static final StateComponent DEFAULT = new StateComponent(false, 0, 0, 0, 0, 0, CoolingMode.PASSIVE, 0, 0);

        public static final Codec<StateComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.fieldOf("isAiming").forGetter(StateComponent::isAiming),
                Codec.LONG.fieldOf("lastFired").forGetter(StateComponent::lastFired),
                Codec.LONG.fieldOf("fireCooldown").forGetter(StateComponent::fireCooldown),
                Codec.LONG.fieldOf("cooldownStart").forGetter(StateComponent::cooldownStart),
                Codec.FLOAT.fieldOf("lastTotalHeat").forGetter(StateComponent::lastTotalHeat),
                Codec.FLOAT.fieldOf("lastVentingHeat").forGetter(StateComponent::lastVentingHeat),
                CoolingMode.CODEC.fieldOf("coolingMode").forGetter(StateComponent::coolingMode),
                Codec.INT.fieldOf("burstBoltsRemaining").forGetter(StateComponent::burstBoltsRemaining),
                Codec.LONG.fieldOf("overchargeStart").forGetter(StateComponent::overchargeStart)
        ).apply(instance, StateComponent::new));

        public static final StreamCodec<ByteBuf, StateComponent> PACKET_CODEC = new StreamCodec<>() {
            @Override public StateComponent decode(ByteBuf buf) { return new StateComponent(buf.readBoolean(), buf.readLong(), buf.readLong(), buf.readLong(), buf.readFloat(), buf.readFloat(), CoolingMode.PACKET_CODEC.decode(buf), buf.readInt(), buf.readLong()); }
            @Override public void encode(ByteBuf buf, StateComponent value) { buf.writeBoolean(value.isAiming()); buf.writeLong(value.lastFired()); buf.writeLong(value.fireCooldown()); buf.writeLong(value.cooldownStart()); buf.writeFloat(value.lastTotalHeat()); buf.writeFloat(value.lastVentingHeat()); CoolingMode.PACKET_CODEC.encode(buf, value.coolingMode()); buf.writeInt(value.burstBoltsRemaining()); buf.writeLong(value.overchargeStart()); }
        };

        // Mutable Record helper methods
        public StateComponent withIsAiming(boolean aiming) { return new StateComponent(aiming, lastFired, fireCooldown, cooldownStart, lastTotalHeat, lastVentingHeat, coolingMode, burstBoltsRemaining, overchargeStart); }
        public StateComponent withLastFired(long fired) { return new StateComponent(isAiming, fired, fireCooldown, cooldownStart, lastTotalHeat, lastVentingHeat, coolingMode, burstBoltsRemaining, overchargeStart); }
        public StateComponent withFireCooldown(long cd) { return new StateComponent(isAiming, lastFired, cd, cooldownStart, lastTotalHeat, lastVentingHeat, coolingMode, burstBoltsRemaining, overchargeStart); }
        public StateComponent withCooldownStart(long start) { return new StateComponent(isAiming, lastFired, fireCooldown, start, lastTotalHeat, lastVentingHeat, coolingMode, burstBoltsRemaining, overchargeStart); }
        public StateComponent withLastTotalHeat(float heat) { return new StateComponent(isAiming, lastFired, fireCooldown, cooldownStart, heat, lastVentingHeat, coolingMode, burstBoltsRemaining, overchargeStart); }
        public StateComponent withLastVentingHeat(float heat) { return new StateComponent(isAiming, lastFired, fireCooldown, cooldownStart, lastTotalHeat, heat, coolingMode, burstBoltsRemaining, overchargeStart); }
        public StateComponent withCoolingMode(CoolingMode mode) { return new StateComponent(isAiming, lastFired, fireCooldown, cooldownStart, lastTotalHeat, lastVentingHeat, mode, burstBoltsRemaining, overchargeStart); }
        public StateComponent withBurstBoltsRemaining(int remaining) { return new StateComponent(isAiming, lastFired, fireCooldown, cooldownStart, lastTotalHeat, lastVentingHeat, coolingMode, remaining, overchargeStart); }
        public StateComponent withOverchargeStart(long start) { return new StateComponent(isAiming, lastFired, fireCooldown, cooldownStart, lastTotalHeat, lastVentingHeat, coolingMode, burstBoltsRemaining, start); }

        public StateComponent withCooling(CoolingMode mode, long timestamp)
        {
            return this.withCoolingMode(mode).withCooldownStart(timestamp);
        }
    }

    public enum AttachmentFunction
    {
        ZOOM_MULTIPLIER(PSWG.id("zoom_multiplier"), Combinator.GEOMETRIC),
        RECOIL_MULTIPLIER(PSWG.id("recoil_multiplier"), Combinator.GEOMETRIC),
        SPREAD_MULTIPLIER(PSWG.id("spread_multiplier"), Combinator.GEOMETRIC),
        COOLING_MULTIPLIER(PSWG.id("cooling_multiplier"), Combinator.GEOMETRIC),
        FIRE_RATE_MULTIPLIER(PSWG.id("fire_rate_multiplier"), Combinator.GEOMETRIC);

        private final ResourceLocation id;
        private final Combinator combinator;

        AttachmentFunction(ResourceLocation id, Combinator combinator)
        {
            this.id = id;
            this.combinator = combinator;
        }

        public ResourceLocation getId() { return id; }
        public Combinator getCombinator() { return combinator; }
    }

    public static final ResourceLocation MISSING_ID = PSWG.id("missingno");
    protected static final int TOGGLE_AIMING_USE_TIME_TICKS = 3;

    protected static final AttributeModifier ATTR_MODIFIER_AIMING_SPEED_PENALTY_ENABLED = new AttributeModifier(PSWG.id("aiming_speed_penalty"), -0.5F, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    protected static final AttributeModifier ATTR_MODIFIER_AIMING_FOV_ENABLED = new AttributeModifier(PSWG.id("aiming_zoom"), 2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

    public static Properties createSettings()
    {
        return new Properties()
                .component(ID, MISSING_ID)
                .component(ATTACHMENTS, AttachmentsComponent.DEFAULT)
                .component(STATE, StateComponent.DEFAULT);
    }

    public static ItemStack createStack(ResourceLocation id, BlasterDatapackDefinition definition) {
        ItemStack stack = new ItemStack(ModItems.BLASTER.get());

        // IMPORTANT: Remove the .get() calls!
        // Pass the DeferredHolder directly.
        // This allows NeoForge to resolve the ID whenever the registry is actually ready.
        stack.set(BlasterComponents.ID, id);
        stack.set(BlasterComponents.STATS, definition.stats());
        stack.set(BlasterComponents.AVAILABLE_ATTACHMENTS, definition.attachments());

        return stack;
    }

    private static AttachmentsComponent createAvailableAttachments(AvailableAttachmentsComponent attachments)
    {
        return new AttachmentsComponent(attachments.hud(), attachments.defaults());
    }

    public BlasterItem(Properties settings)
    {
        super(settings);
    }

    public static Optional<StatsComponent> getStats(ItemStack stack)
    {
        return Optional.ofNullable(PSWG.DATAPACK_LOADER.getDefinitions().getOrDefault(stack.get(ID), null))
                .map(BlasterDatapackDefinition::stats);
    }

    public static AttachmentsComponent getAttachments(ItemStack stack)
    {
        return stack.getOrDefault(ATTACHMENTS, AttachmentsComponent.DEFAULT);
    }

    public static Optional<AvailableAttachmentsComponent> getAvailableAttachments(ItemStack stack)
    {
        return Optional.ofNullable(PSWG.DATAPACK_LOADER.getDefinitions().getOrDefault(stack.get(ID), null))
                .map(BlasterDatapackDefinition::attachments);
    }

    public static StateComponent getState(ItemStack stack)
    {
        return stack.getOrDefault(STATE, StateComponent.DEFAULT);
    }

    public static void applyState(ItemStack stack, UnaryOperator<StateComponent> stateOperator)
    {
        stack.update(STATE, StateComponent.DEFAULT, stateOperator);
    }

    public static void setAiming(ItemStack stack, boolean aiming)
    {
        applyState(stack, state -> state.withIsAiming(aiming));

        var attrs = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);

        if (aiming)
        {
            attrs = attrs.withModifierAdded(Attributes.MOVEMENT_SPEED, ATTR_MODIFIER_AIMING_SPEED_PENALTY_ENABLED, EquipmentSlotGroup.HAND);
            attrs = attrs.withModifierAdded(ModEntityAttributes.FIELD_OF_VIEW_ZOOM, ATTR_MODIFIER_AIMING_FOV_ENABLED, EquipmentSlotGroup.HAND);
        }
        else
        {
            attrs = AttributeUtil.without(attrs, Attributes.MOVEMENT_SPEED, ATTR_MODIFIER_AIMING_SPEED_PENALTY_ENABLED);
            attrs = AttributeUtil.without(attrs, ModEntityAttributes.FIELD_OF_VIEW_ZOOM, ATTR_MODIFIER_AIMING_FOV_ENABLED);
        }

        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, attrs);
    }

    public static Optional<Float> getFireCooldownProgress(Level world, ItemStack stack, float tickDelta)
    {
        var state = getState(stack);
        var lastFired = state.lastFired();
        var cooldown = state.fireCooldown();
        var time = world.getGameTime() + tickDelta;

        if (cooldown <= lastFired || cooldown < time) return Optional.empty();
        return Optional.of((time - lastFired) / (cooldown - lastFired));
    }

    public static Optional<CoolingBypass> getCoolingBypass(Level world, ItemStack stack, float tickDelta)
    {
        var state = getState(stack);
        if (!state.coolingMode().canBypass()) return Optional.empty();

        var optionalStats = getStats(stack);
        if (optionalStats.isEmpty()) return Optional.empty();
        var stats = optionalStats.get();

        var potentialVentingHeat = getVentingHeat(world, stack, tickDelta);
        if (potentialVentingHeat.isEmpty()) return Optional.empty();

        var ventingHeat = potentialVentingHeat.get() / state.lastVentingHeat();
        var attachments = getAttachments(stack);

        if (Math.abs(ventingHeat - stats.cooling().primaryBypassTime()) <= getScaledPrimaryBypassTolerance(stats, attachments))
            return Optional.of(CoolingBypass.PRIMARY);

        if (Math.abs(ventingHeat - stats.cooling().secondaryBypassTime()) <= getScaledSecondaryBypassTolerance(stats, attachments))
            return Optional.of(CoolingBypass.SECONDARY);

        return Optional.empty();
    }

    public static boolean canFire(Level world, LivingEntity user, ItemStack stack)
    {
        return getState(stack).fireCooldown() < world.getGameTime();
    }

    public static Optional<Float> getOverchargeTimeRemaining(Level world, ItemStack stack, float tickDelta)
    {
        var state = getState(stack);
        var optionalStats = getStats(stack);
        if (optionalStats.isEmpty()) return Optional.empty();

        var overchargeStart = state.overchargeStart();
        var overchargeLength = optionalStats.get().heat().overchargeBonus();
        var time = world.getGameTime() + tickDelta;

        if (time > overchargeStart + overchargeLength) return Optional.empty();
        if (time < overchargeStart) return Optional.of(1f);
        return Optional.of(1 - ((time - overchargeStart) / overchargeLength));
    }

    public static Optional<Float> getAccumulatedHeat(Level world, ItemStack stack, float tickDelta)
    {
        var state = getState(stack);
        if (state.coolingMode() != CoolingMode.PASSIVE) return Optional.empty();

        var stats = getStats(stack);
        if (stats.isEmpty()) return Optional.empty();

        var time = world.getGameTime() + tickDelta;
        var dissipation = getScaledHeatDrainSpeed(stats.get(), getAttachments(stack)) * (time - state.cooldownStart());

        if (dissipation > state.lastTotalHeat()) return Optional.empty();
        return Optional.of(Math.min(state.lastTotalHeat() - dissipation, state.lastTotalHeat()));
    }

    public static Optional<Float> getVentingHeat(Level world, ItemStack stack, float tickDelta)
    {
        var state = getState(stack);
        if (state.coolingMode() == CoolingMode.PASSIVE) return Optional.empty();

        var stats = getStats(stack);
        if (stats.isEmpty()) return Optional.empty();

        var time = world.getGameTime() + tickDelta;
        var dissipation = getScaledOverheatDrainSpeed(stats.get(), getAttachments(stack)) * (time - state.cooldownStart());

        if (dissipation > state.lastVentingHeat()) return Optional.empty();
        return Optional.of(Math.min(state.lastVentingHeat() - dissipation, state.lastVentingHeat()));
    }

    public static CoolingStatus getCoolingStatus(Level world, ItemStack stack, float tickDelta)
    {
        var state = getState(stack);
        return getVentingHeat(world, stack, tickDelta)
                .map(ventingHeat -> new CoolingStatus(state.coolingMode(), ventingHeat))
                .orElseGet(() -> new CoolingStatus(CoolingMode.PASSIVE, getAccumulatedHeat(world, stack, tickDelta).orElse(0f)));
    }

    private static int getScaledAutoRepeatDelay(StatsComponent stats, AttachmentsComponent attachments) { return stats.automaticRepeatDelay(); }
    private static float getScaledPrimaryBypassTolerance(StatsComponent stats, AttachmentsComponent attachments) { return stats.cooling().primaryBypassTolerance(); }
    private static float getScaledSecondaryBypassTolerance(StatsComponent stats, AttachmentsComponent attachments) { return stats.cooling().secondaryBypassTolerance(); }
    private static int getScaledPassiveCooldownDelay(StatsComponent stats, AttachmentsComponent attachments) { return stats.heat().passiveCooldownDelay(); }
    private static float getScaledHeatDrainSpeed(StatsComponent stats, AttachmentsComponent attachments) { return stats.heat().drainSpeed(); }
    private static float getScaledOverheatDrainSpeed(StatsComponent stats, AttachmentsComponent attachments) { return stats.heat().overheatDrainSpeed(); }


    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (stack.get(SERIAL) == null) stack.set(SERIAL, level.getRandom().nextLong());
    }

    @Override
    public Optional<Boolean> shouldSkipHandAnimationOnSwap(ItemStack from, ItemStack to)
    {
        var serialA = from.get(SERIAL);
        var serialB = to.get(SERIAL);
        if (serialA == null || serialB == null) return Optional.empty();
        return Optional.of(serialA.longValue() == serialB.longValue());
    }

    @Override
    public boolean canAttackBlock(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer)
    {
        return false;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user)
    {
        return getState(stack).isAiming() ? 72000 : super.getUseDuration(stack, user);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user)
    {
        releaseUsing(stack, world, user, 0);
        return stack;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack)
    {
        return UseAnim.NONE;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks)
    {
        if (!world.isClientSide() && user.getTicksUsingItem() > TOGGLE_AIMING_USE_TIME_TICKS && getState(stack).isAiming())
            setAiming(stack, false);
        super.releaseUsing(stack, world, user, remainingUseTicks);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand)
    {
        var stack = user.getItemInHand(hand);
        if (!world.isClientSide())
        {
            setAiming(stack, !getState(stack).isAiming());
            user.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public InteractionResult useLeft(Level world, LivingEntity user, InteractionHand hand, boolean repeatEvent)
    {
        ItemStack itemStack = user.getItemInHand(hand);
        if (!canFire(world, user, itemStack)) return InteractionResult.PASS;

        var state = getState(itemStack);
        var attachments = getAttachments(itemStack);
        var optionalStats = getStats(itemStack);
        var optionalAvailableAttachments = getAvailableAttachments(itemStack);

        if (optionalStats.isEmpty() || optionalAvailableAttachments.isEmpty()) return InteractionResult.FAIL;

        var stats = optionalStats.get();
        var timestamp = world.getGameTime();
        var coolingStatus = getCoolingStatus(world, itemStack, 0);

        if (coolingStatus.coolingMode() == CoolingMode.PASSIVE && state.coolingMode() != CoolingMode.PASSIVE)
            state = state.withCooling(CoolingMode.PASSIVE, timestamp);

        if (state.coolingMode().isCooling())
        {
            if (!state.coolingMode().canBypass() || repeatEvent)
            {
                itemStack.set(STATE, state);
                return InteractionResult.FAIL;
            }

            var bypass = getCoolingBypass(world, itemStack, 0);
            if (bypass.isEmpty())
            {
                world.playSound(null, user.getX(), user.getY(), user.getZ(), ModSounds.BYPASS_FAILED.get(), SoundSource.PLAYERS, 1, RandomHelper.floatBetween(world.getRandom(), 0.9f, 1.1f));
                if (world.isClientSide()) return InteractionResult.FAIL;
                itemStack.set(STATE, state.withCoolingMode(CoolingMode.FAILED_OVERCHARGE));
                return InteractionResult.CONSUME;
            }
            else if (bypass.get() == CoolingBypass.PRIMARY)
            {
                world.playSound(null, user.getX(), user.getY(), user.getZ(), ModSounds.BYPASS_PRIMARY.get(), SoundSource.PLAYERS, 1, RandomHelper.floatBetween(world.getRandom(), 0.9f, 1.1f));
                if (world.isClientSide()) return InteractionResult.FAIL;
                itemStack.set(STATE, state.withLastTotalHeat(0).withCooling(CoolingMode.PASSIVE, timestamp));
                return InteractionResult.CONSUME;
            }
            else if (bypass.get() == CoolingBypass.SECONDARY)
            {
                world.playSound(null, user.getX(), user.getY(), user.getZ(), ModSounds.BYPASS_SECONDARY.get(), SoundSource.PLAYERS, 1, RandomHelper.floatBetween(world.getRandom(), 0.9f, 1.1f));
                if (world.isClientSide()) return InteractionResult.FAIL;
                itemStack.set(STATE, state.withLastTotalHeat(0).withOverchargeStart(timestamp).withCooling(CoolingMode.PASSIVE, timestamp));
                return InteractionResult.CONSUME;
            }
        }

        state = state.withLastFired(timestamp)
                .withCooling(CoolingMode.PASSIVE, timestamp + getScaledPassiveCooldownDelay(stats, attachments))
                .withFireCooldown(timestamp + getScaledAutoRepeatDelay(stats, attachments));

        var totalHeat = coolingStatus.totalHeat();
        if (getOverchargeTimeRemaining(world, itemStack, 0).isEmpty())
            totalHeat += stats.heat().perRound();

        if (world instanceof ServerLevel serverWorld)
        {
            fireBolt(user, serverWorld);
            var recoilScale = attachments.getAttachmentsValue(optionalAvailableAttachments.get().options(), AttachmentFunction.RECOIL_MULTIPLIER);
            var recoil = new Vector3f(-(float)RandomHelper.nextGaussian(world.getRandom(), 3.6, 0.2), -(float)RandomHelper.nextGaussian(world.getRandom(), -0.2, 0.2), 0);
            if (user instanceof IRecoilEntity recoilEntity) recoilEntity.pswg$addRecoilVelocity(recoil.mul(recoilScale));
        }

        if (user instanceof IRecoilEntity recoilEntity) recoilEntity.pswg$setRecoilTime(timestamp);

        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvent.createVariableRangeEvent(stats.fireSound()), SoundSource.NEUTRAL, 1, RandomHelper.floatBetween(world.getRandom(), 0.9f, 1.1f));

        if (totalHeat > stats.heat().capacity())
        {
            world.playSound(null, user.getX(), user.getY(), user.getZ(), ModSounds.OVERHEAT.get(), SoundSource.PLAYERS, 1, RandomHelper.floatBetween(world.getRandom(), 0.9f, 1.1f));
            state = state.withLastVentingHeat(totalHeat + stats.heat().overheatPenalty()).withCooling(CoolingMode.OVERHEAT, timestamp).withBurstBoltsRemaining(0);
            totalHeat = 0;
        }

        state = state.withLastTotalHeat(totalHeat);
        itemStack.set(STATE, state);

        return InteractionResult.CONSUME;
    }

    @Override
    public ItemStack invokePrimaryAction(ItemStack stack, Level world, LivingEntity user)
    {
        var timestamp = world.getGameTime();
        var coolingStatus = getCoolingStatus(world, stack, 0);

        if (coolingStatus.coolingMode().isCooling()) return stack;

        var state = getState(stack);
        stack.set(STATE, state.withLastVentingHeat(coolingStatus.totalHeat()).withCooling(CoolingMode.REQUESTED_BYPASS, timestamp).withBurstBoltsRemaining(0));

        world.playSound(null, user.getX(), user.getY(), user.getZ(), ModSounds.VENT.get(), SoundSource.PLAYERS, 1, RandomHelper.floatBetween(world.getRandom(), 0.9f, 1.1f));

        return stack;
    }

    private static void fireBolt(LivingEntity user, ServerLevel serverWorld)
    {
        var projectile = new BlasterBoltEntity(ModEntities.BLASTER_BOLT.get(), serverWorld);
        projectile.setPos(user.getX(), user.getY() + user.getEyeHeight(user.getPose()), user.getZ());

        var pitch = user.getXRot();
        var yaw = user.getYHeadRot();

        projectile.setDeltaMovement(GMath.getForwardVector(yaw, pitch).scale(5));

        // Removed absSnapRotationTo due to unresolved reference.
        // 1.21.1 correct entity rotation application:
        projectile.setYRot(yaw);
        projectile.setXRot(pitch);
        projectile.yRotO = yaw;
        projectile.xRotO = pitch;

        serverWorld.addFreshEntity(projectile);
    }

    public static BlasterDescriptor getBlasterDescriptor(ItemStack stack)
    {
        return getBlasterDescriptor(stack, false);
    }

    public static BlasterDescriptor getBlasterDescriptor(ItemStack stack, boolean allowNull)
    {
        if (stack.getItem() instanceof BlasterItem bi)
            return bi.descriptor;
        if (allowNull)
            return null;

        var j = CrashReport.create(new NullPointerException("Cannot get blaster descriptor for unknown stack " + stack.toString()), "Getting blaster descriptor");
        throw new CrashException(j);
    }
}