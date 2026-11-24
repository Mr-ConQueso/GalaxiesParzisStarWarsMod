package dev.pswg.gadgets;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class SmokeCloud extends Entity
{
	private final Map<BlockPos, Float> cells = new HashMap<>();
	private int age;

	public SmokeCloud(EntityType<SmokeCloud> type, Level level)
	{
		super(type, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
	}

	@Override
	public void tick()
	{
		super.tick();
		if (!(level() instanceof ServerLevel server))
			return;

		if (age++ >= 900)
		{
			discard();
			return;
		}
		if (age == 1 && cells.isEmpty())
		{
			var origin = blockPosition().above();
			if (server.getBlockState(origin).isSolid())
				origin = blockPosition();
			cells.put(origin, 240F);
		}
		if (age % 2 == 0)
			diffuse(server);
		if (age % 4 == 0)
			emitSmoke(server);
	}

	private void diffuse(ServerLevel level)
	{
		var changed = new HashMap<>(cells);
		var neighbors = new ArrayList<BlockPos>(6);
		for (var cell : cells.entrySet())
		{
			var pos = cell.getKey();
			var current = level.getBlockState(pos);
			neighbors.clear();
			for (var face : Direction.values())
			{
				var adjacent = pos.relative(face);
				if (!level.hasChunkAt(adjacent))
					continue;
				var state = level.getBlockState(adjacent);
				if ((state.is(Demolitions.GAS_PASS_THROUGH) || !state.isFaceSturdy(level, adjacent, face.getOpposite()))
						&& (current.is(Demolitions.GAS_PASS_THROUGH) || !current.isFaceSturdy(level, pos, face)))
					neighbors.add(adjacent);
			}
			for (var adjacent : neighbors)
			{
				float transferred = 0.9F * (cell.getValue() - cells.getOrDefault(adjacent, 0F)) / neighbors.size();
				if (transferred > 0.00625F)
				{
					changed.merge(adjacent, transferred, Float::sum);
					changed.merge(pos, -transferred, Float::sum);
				}
			}
		}
		changed.values().removeIf(amount -> amount < 0.01F);
		cells.clear();
		cells.putAll(changed);
	}

	private void emitSmoke(ServerLevel level)
	{
		var visible = new ArrayList<>(cells.keySet());
		if (visible.isEmpty())
			return;
		int count = Math.min(48, visible.size());
		for (int i = 0; i < count; i++)
		{
			var pos = visible.get(random.nextInt(visible.size()));
			level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5,
					cells.get(pos) > 0.2F ? 2 : 1, 0.31, 0.25, 0.31, 0.005);
		}
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
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag)
	{
		age = tag.getInt("Age");
		cells.clear();
		for (var entry : tag.getList("Cells", Tag.TAG_COMPOUND))
		{
			var cell = (CompoundTag) entry;
			cells.put(BlockPos.of(cell.getLong("Pos")), cell.getFloat("Amount"));
		}
	}
}
