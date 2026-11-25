package dev.pswg.gadgets;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SmokeCloud extends Entity implements IEntityWithComplexSpawn
{
	public static final int MAX_AGE = 900;
	private static final EntityDataAccessor<CompoundTag> VISUAL_CELLS = SynchedEntityData.defineId(SmokeCloud.class, EntityDataSerializers.COMPOUND_TAG);
	private Map<BlockPos, Integer> visualCells = Map.of();
	private final Map<BlockPos, Float> cells = new ConcurrentHashMap<>(1024);
	private final Map<UUID, Float> exposure = new HashMap<>();
	private final boolean nerve;
	private int age;

	public SmokeCloud(EntityType<SmokeCloud> type, Level level)
	{
		this(type, level, false);
	}

	public SmokeCloud(EntityType<SmokeCloud> type, Level level, boolean nerve)
	{
		super(type, level);
		this.nerve = nerve;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(VISUAL_CELLS, new CompoundTag());
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key)
	{
		super.onSyncedDataUpdated(key);
		if (key.equals(VISUAL_CELLS) && level().isClientSide)
		{
			var snapshot = entityData.get(VISUAL_CELLS);
			var positions = snapshot.getLongArray("Positions");
			var counts = snapshot.getByteArray("Counts");
			var updated = new HashMap<BlockPos, Integer>();
			for (int i = 0; i < positions.length; i++)
				updated.put(BlockPos.of(positions[i]), counts[i] & 0xFF);
			visualCells = Map.copyOf(updated);
		}
	}

	public Map<BlockPos, Integer> visualCells() { return visualCells; }
	public int cloudAge() { return age; }
	public boolean isNerveGas() { return nerve; }

	@Override public void writeSpawnData(RegistryFriendlyByteBuf buffer) { buffer.writeVarInt(age); }
	@Override public void readSpawnData(RegistryFriendlyByteBuf buffer) { age = buffer.readVarInt(); }

	@Override
	public void tick()
	{
		super.tick();
		age++;
		if (!(level() instanceof ServerLevel server))
			return;

		if (age > MAX_AGE)
		{
			discard();
			return;
		}
		if (age == 1 && cells.isEmpty())
		{
			var origin = blockPosition().above();
			var state = server.getBlockState(origin);
			if (!state.is(Demolitions.GAS_PASS_THROUGH) && state.isSolid())
				origin = blockPosition();
			cells.put(origin, nerve ? 80F : 240F);
		}
		if (age > 1)
			diffuse(server);
		if (nerve)
			applyExposure(server);
		if (age == 1 || age % 6 == 0)
			syncVisualCells();
	}

	private void diffuse(ServerLevel level)
	{
		int totalPermeability = 0;
		float diffusion = nerve ? 0.7F : 0.9F;
		for (var cell : cells.entrySet())
		{
			var pos = cell.getKey();
			var current = level.getBlockState(pos);
			int openFaces = 0;
			for (var face : Direction.values())
			{
				var adjacent = pos.relative(face);
				if (!level.hasChunkAt(adjacent))
					continue;
				var state = level.getBlockState(adjacent);
				if (!state.isFaceSturdy(level, adjacent, face.getOpposite()) && !current.isFaceSturdy(level, pos, face))
					openFaces |= 1 << face.ordinal();
			}
			// Fabric accumulates permeability across the cloud's whole update and applies each transfer in place.
			totalPermeability += Integer.bitCount(openFaces);
			float concentration = cell.getValue();
			for (var face : Direction.values())
			{
				if ((openFaces & (1 << face.ordinal())) == 0)
					continue;
				var adjacent = pos.relative(face);
				float transferred = diffusion * (concentration - cells.getOrDefault(adjacent, 0F)) / totalPermeability;
				if (transferred > 0.00625F)
				{
					cells.merge(adjacent, transferred, Float::sum);
					concentration -= transferred;
				}
			}
			cells.put(pos, concentration);
		}
		cells.values().removeIf(amount -> amount < 0.01F);
	}

	private void syncVisualCells()
	{
		var positions = new long[cells.size()];
		var counts = new byte[positions.length];
		int index = 0;
		for (var cell : cells.entrySet())
		{
			float concentration = cell.getValue() - 0.025F;
			if (concentration <= 0F)
				continue;
			// Fabric uses successively larger doses for additional sprites in a cell.
			float interval = 0.025F;
			int count = 0;
			while (concentration > 0F && count < 255)
			{
				count++;
				concentration -= interval;
				interval += 1 / 64F;
			}
			positions[index] = cell.getKey().asLong();
			counts[index++] = (byte) count;
		}
		var snapshot = new CompoundTag();
		snapshot.putLongArray("Positions", java.util.Arrays.copyOf(positions, index));
		snapshot.putByteArray("Counts", java.util.Arrays.copyOf(counts, index));
		entityData.set(VISUAL_CELLS, snapshot);
	}

	private void applyExposure(ServerLevel level)
	{
		var affected = new java.util.HashSet<UUID>();
		for (var entity : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(32),
				entity -> entity.isAlive() && !entity.isSpectator()))
		{
			float amount = cells.getOrDefault(entity.blockPosition(), 0F);
			if (amount <= 0.25F)
				continue;
			var id = entity.getUUID();
			affected.add(id);
			float toxicity = exposure.merge(id, 1 + amount / 10, Float::sum);
			if (toxicity > 50 && (age % 10 == 0 || !entity.hasEffect(Demolitions.INTOXICATED)))
				entity.addEffect(new MobEffectInstance(Demolitions.INTOXICATED, 100,
						Math.min(12, (int) (toxicity / 50) - 1), false, false, true), this);
		}
		exposure.replaceAll((id, value) -> affected.contains(id) ? value : value - 5);
		exposure.values().removeIf(value -> value <= 1);
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag)
	{
		tag.putInt("Age", age);
		var contents = new ListTag();
		cells.forEach((pos, amount) -> {
			var cell = new CompoundTag();
			cell.putLong("Pos", pos.asLong());
			cell.putFloat("Amount", amount);
			contents.add(cell);
		});
		tag.put("Cells", contents);
		var savedExposure = new ListTag();
		exposure.forEach((id, amount) -> {
			var entry = new CompoundTag();
			entry.putUUID("Entity", id);
			entry.putFloat("Amount", amount);
			savedExposure.add(entry);
		});
		if (!savedExposure.isEmpty())
			tag.put("Exposure", savedExposure);
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag)
	{
		age = tag.getInt("Age");
		exposure.clear();
		for (var entry : tag.getList("Exposure", Tag.TAG_COMPOUND))
		{
			var saved = (CompoundTag) entry;
			if (saved.hasUUID("Entity"))
				exposure.put(saved.getUUID("Entity"), saved.getFloat("Amount"));
		}
		cells.clear();
		for (var entry : tag.getList("Cells", Tag.TAG_COMPOUND))
		{
			var cell = (CompoundTag) entry;
			cells.put(BlockPos.of(cell.getLong("Pos")), cell.getFloat("Amount"));
		}
	}
}
