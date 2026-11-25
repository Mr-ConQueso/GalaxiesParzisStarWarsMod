package dev.pswg.container;

import dev.pswg.Galaxies;
import dev.pswg.block.CorrugatedCrateBlock;
import dev.pswg.block.ImperialLighting;
import dev.pswg.block.KyberCrateBlock;
import dev.pswg.block.LargePipeBlock;
import dev.pswg.block.MetalScaffoldBlock;
import dev.pswg.block.VerticalSlabBlock;
import dev.pswg.block.WaterloggableBlock;
import dev.pswg.block.WideBlock;
import dev.pswg.util.VoxelShapeUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class LegacyConstruction
{
	private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Galaxies.MODID);
	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Galaxies.MODID);

	public static final List<DeferredBlock<KyberCrateBlock>> KYBER_CRATES = List.of("orange", "gray", "black").stream()
			.map(color -> block(color + "_kyber_crate", () -> new KyberCrateBlock(crate()))).toList();
	public static final List<DeferredBlock<?>> SEGMENTED_CRATES = Arrays.stream(DyeColor.values())
			.<DeferredBlock<?>>map(color -> color == DyeColor.GRAY ? releasedCrate("gray_segmented_crate") : block(color.getName() + "_segmented_crate", () ->
					new WideBlock<>(crate(), Crates.SEGMENTED, Block.box(1, 0, 0, 15, 16, 14), Block.box(1, 0, 2, 15, 16, 16)))).toList();
	public static final DeferredBlock<CorrugatedCrateBlock> RELEASED_BROWN_CRATE = releasedCrate("segmented_crate");
	public static final DeferredBlock<CorrugatedCrateBlock> GRAY_PANEL_CRATE = releasedCrate("gray_panel_crate");
	public static final DeferredBlock<CorrugatedCrateBlock> TOOLBOX = block("toolbox",
			() -> new CorrugatedCrateBlock(crate(), Shapes.block(), Crates.TOOLBOX));
	public static final DeferredBlock<LargePipeBlock> LARGE_PIPE = block("large_pipe", () -> new LargePipeBlock(copper(3.5F).noOcclusion()));
	public static final DeferredBlock<MetalScaffoldBlock> SCAFFOLD = BLOCKS.register("scaffold", () -> new MetalScaffoldBlock(copper(3.5F).noOcclusion()));
	public static final DeferredBlock<?> SCAFFOLD_STAIRS = block("scaffold_stairs", () -> new Decoration(copper(3.5F).noOcclusion(),
			Shapes.or(Block.box(0, 0, 0, 16, 8, 16), Block.box(0, 8, 0, 8, 16, 16))));
	public static final DeferredBlock<ImperialLighting.Fixture> LIGHT_FIXTURE = block("light_fixture",
			() -> new ImperialLighting.Fixture(BlockBehaviour.Properties.of().strength(0.3F).sound(SoundType.GLASS)
					.lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? state.getValue(ImperialLighting.Fixture.BRIGHTNESS) * 5 : 0)));

	static
	{
		ITEMS.register("scaffold", () -> new MetalScaffoldBlock.Item(SCAFFOLD.get(), new Item.Properties()));
		block("desh_barrel", () -> new DeshBarrel(copper(2.5F).noOcclusion().dynamicShape().offsetType(BlockBehaviour.OffsetType.XZ)));
		block("spoked_machine", () -> new Decoration(copper(5).noOcclusion(), VoxelShapeUtil.getCenteredCube(10, 20)));
		block("starship_fuel_tank", () -> new WideBlock<>(copper(3.5F).noOcclusion(), null,
				Block.box(0, 0, 0, 16, 13.5, 16), Block.box(0, 0, 0, 16, 13.5, 16)));
		for (String color : List.of("red", "blue"))
			block(color + "_hangar_light", () -> new Decoration(BlockBehaviour.Properties.of().sound(SoundType.METAL)
					.strength(0.5F).lightLevel(state -> 15).noCollission().noOcclusion(), Block.box(1, 0, 2, 15, 5, 14)));

		for (String suffix : List.of("", "_caged", "_pipes", "_caged_pipes"))
			block("imperial_cutout" + suffix, () -> new RotatedPillarBlock(copper(3.5F).noOcclusion()));
		for (int variant = 1; variant <= 3; variant++)
			block("imperial_opaque_grate_" + variant, () -> new Block(copper(3.5F).noOcclusion()));
		for (int variant = 1; variant <= 5; variant++)
			block("imperial_grate_" + variant, () -> new MetalTrapdoor(copper(3.5F).noOcclusion()));
		for (String name : List.of("imperial_vent", "imperial_grated_vent"))
			block(name, () -> new MetalTrapdoor(copper(3.5F).noOcclusion().isValidSpawn((state, level, pos, type) -> false)));

		block("air_vent", () -> new Block(copper(3.5F).noOcclusion()));
		for (int variant = 1; variant <= 2; variant++)
			block("gray_imperial_panel_pattern_" + variant, () -> new RotatedPillarBlock(copper(1.5F)
					.mapColor(state -> state.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y ? MapColor.COLOR_GRAY : MapColor.COLOR_LIGHT_GRAY)));
		block("rusted_gray_imperial_panel_pattern_3_slab", () -> new VerticalSlabBlock(copper(1.5F)));
		block("rusted_gray_imperial_panel_pattern_3_stairs", () -> new StairBlock(GalaxiesBlocks.RUSTED_PANEL.get().defaultBlockState(), copper(1.5F)));
	}

	private LegacyConstruction() {}

	private static BlockBehaviour.Properties copper(float strength)
	{
		return BlockBehaviour.Properties.of().sound(SoundType.COPPER).strength(strength).requiresCorrectToolForDrops();
	}

	private static BlockBehaviour.Properties crate()
	{
		return BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(2.5F).noOcclusion();
	}

	private static DeferredBlock<CorrugatedCrateBlock> releasedCrate(String name)
	{
		return block(name, () -> new CorrugatedCrateBlock(crate().dynamicShape(), VoxelShapeUtil.getCentered(14, 28, 14), Crates.SEGMENTED));
	}

	static Block[] segmentedCrateBlocks()
	{
		return Stream.concat(SEGMENTED_CRATES.stream(), Stream.of(RELEASED_BROWN_CRATE, GRAY_PANEL_CRATE))
				.map(DeferredHolder::get).toArray(Block[]::new);
	}

	private static <T extends Block> DeferredBlock<T> block(String name, Supplier<T> factory)
	{
		var block = BLOCKS.register(name, factory);
		ITEMS.registerSimpleBlockItem(name, block);
		return block;
	}

	public static Collection<DeferredHolder<Block, ? extends Block>> entries()
	{
		return BLOCKS.getEntries();
	}

	public static void register(IEventBus bus)
	{
		BLOCKS.register(bus);
		ITEMS.register(bus);
		bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
			if (event.getTabKey().equals(GalaxiesBlocks.CONSTRUCTION_TAB))
				ITEMS.getEntries().forEach(item -> event.accept(item.get()));
		});
	}

	private static final class MetalTrapdoor extends TrapDoorBlock
	{
		MetalTrapdoor(Properties properties) { super(BlockSetType.IRON, properties); }

		@Override
		protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
		{
			var updated = state.cycle(OPEN);
			level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
			if (updated.getValue(WATERLOGGED))
				level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
			playSound(player, level, pos, updated.getValue(OPEN));
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
	}

	private static final class Decoration extends WaterloggableBlock
	{
		private final VoxelShape[] shapes = new VoxelShape[4];

		Decoration(Properties properties, VoxelShape shape)
		{
			super(properties);
			for (var direction : Direction.Plane.HORIZONTAL)
				shapes[direction.get2DDataValue()] = VoxelShapeUtil.rotateToFace(shape, direction);
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
		{
			super.createBlockStateDefinition(builder);
			builder.add(BlockStateProperties.HORIZONTAL_FACING);
		}

		@Override
		public BlockState getStateForPlacement(BlockPlaceContext context)
		{
			return super.getStateForPlacement(context).setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
		}

		@Override
		protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
		{
			return shapes[state.getValue(BlockStateProperties.HORIZONTAL_FACING).get2DDataValue()];
		}

		@Override protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(BlockStateProperties.HORIZONTAL_FACING, rotation.rotate(state.getValue(BlockStateProperties.HORIZONTAL_FACING))); }
		@Override protected BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(BlockStateProperties.HORIZONTAL_FACING))); }
	}

	private static final class DeshBarrel extends Block
	{
		private static final VoxelShape SHAPE = Shapes.or(box(3.5, 12, 3.5, 12.5, 15, 12.5), box(3.5, 5, 3.5, 12.5, 11, 12.5),
				box(3.5, 1, 3.5, 12.5, 4, 12.5), box(4.5, 0, 4.5, 11.5, 16, 11.5), box(4, 4, 4, 12, 12, 12));

		DeshBarrel(Properties properties) { super(properties); }

		@Override public float getMaxHorizontalOffset() { return 0.2F; }

		@Override
		protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
		{
			var offset = state.getOffset(level, pos);
			return SHAPE.move(offset.x, 0, offset.z);
		}
	}
}
