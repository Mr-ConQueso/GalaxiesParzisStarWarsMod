package dev.pswg.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

public record WetPourstone(Supplier<? extends Block> dry, Supplier<? extends Block> ruined)
{
	private static final int MEAN_DRYING_TICKS = 10;
	private static final Vec3 SLOWDOWN = new Vec3(0.25, 1.5, 0.25);

	private void dryOnRandomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
	{
		if (!level.dimensionType().hasSkyLight())
			return;
		int skyLight = level.getBrightness(LightLayer.SKY, pos) - level.getSkyDarken();
		float angle = level.getSunAngle(1.0F);
		float boundary = angle < Math.PI ? 0 : Mth.TWO_PI;
		angle += (boundary - angle) * 0.2F;
		if (Math.round(skyLight * Mth.cos(angle)) > 0 && random.nextInt(MEAN_DRYING_TICKS) == 0)
			level.setBlock(pos, dry.get().withPropertiesOf(state), Block.UPDATE_CLIENTS);
	}

	private void trample(BlockState state, Level level, BlockPos pos, Entity entity)
	{
		if (ruined == null)
		{
			entity.makeStuckInBlock(state, SLOWDOWN);
			return;
		}

		if (level instanceof ServerLevel server && entity instanceof LivingEntity
				&& (entity instanceof Player || server.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING))
				&& entity.getBbWidth() * entity.getBbWidth() * entity.getBbHeight() > 0.512F)
			level.setBlockAndUpdate(pos, Block.pushEntitiesUp(state, ruined.get().withPropertiesOf(state), level, pos));
	}

	public static final class FullBlock extends ColoredFallingBlock
	{
		private final WetPourstone transition;

		public FullBlock(WetPourstone transition, ColorRGBA dust, Properties properties)
		{
			super(dust, properties.randomTicks());
			this.transition = transition;
		}

		@Override
		protected int getDelayAfterPlace()
		{
			return transition.ruined() == null ? super.getDelayAfterPlace() : 30;
		}

		@Override
		protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
		{
			transition.dryOnRandomTick(state, level, pos, random);
		}

		@Override
		protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity)
		{
			transition.trample(state, level, pos, entity);
			super.entityInside(state, level, pos, entity);
		}
	}

	public static final class Slab extends VerticalSlabBlock
	{
		private final WetPourstone transition;

		public Slab(WetPourstone transition, Properties properties)
		{
			super(properties.randomTicks());
			this.transition = transition;
		}

		@Override
		protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
		{
			transition.dryOnRandomTick(state, level, pos, random);
		}

		@Override
		protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity)
		{
			transition.trample(state, level, pos, entity);
			super.entityInside(state, level, pos, entity);
		}
	}

	public static final class Stairs extends StairBlock
	{
		private final WetPourstone transition;

		public Stairs(WetPourstone transition, BlockState base, Properties properties)
		{
			super(base, properties.randomTicks());
			this.transition = transition;
		}

		@Override
		protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
		{
			transition.dryOnRandomTick(state, level, pos, random);
		}

		@Override
		protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity)
		{
			transition.trample(state, level, pos, entity);
			super.entityInside(state, level, pos, entity);
		}
	}
}
