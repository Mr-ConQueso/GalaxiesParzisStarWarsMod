package dev.pswg.structure;

import dev.pswg.Galaxies;
import dev.pswg.blockEntity.CorrugatedCrateBlockEntity;
import dev.pswg.container.GalaxiesBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.LootTable;

public final class DerelictContainerPiece extends TemplateStructurePiece
{
	private static final DyeColor[] RANDOM_DYES = {
			DyeColor.BLACK, DyeColor.BLUE, DyeColor.BROWN, DyeColor.CYAN, DyeColor.GRAY, DyeColor.GREEN,
			DyeColor.LIGHT_BLUE, DyeColor.LIGHT_GRAY, DyeColor.LIME, DyeColor.MAGENTA, DyeColor.ORANGE,
			DyeColor.PINK, DyeColor.PURPLE, DyeColor.RED, DyeColor.WHITE, DyeColor.YELLOW
	};

	public DerelictContainerPiece(StructureTemplateManager manager, BlockPos pos, Rotation rotation)
	{
		super(DerelictContainer.PIECE.get(), 0, manager, DerelictContainer.KEY.location(),
				DerelictContainer.KEY.location().toString(), settings(rotation), pos);
	}

	DerelictContainerPiece(StructureTemplateManager manager, CompoundTag tag)
	{
		super(DerelictContainer.PIECE.get(), tag, manager, id -> settings(Rotation.valueOf(tag.getString("Rot"))));
	}

	private static StructurePlaceSettings settings(Rotation rotation)
	{
		return new StructurePlaceSettings().setRotation(rotation).addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag)
	{
		super.addAdditionalSaveData(context, tag);
		tag.putString("Rot", getRotation().name());
	}

	@Override
	protected void handleDataMarker(String metadata, BlockPos pos, ServerLevelAccessor world, RandomSource random, BoundingBox bounds)
	{
		if (!"crate".equals(metadata) || random.nextFloat() <= 0.33F)
			return;

		int roll = random.nextIntBetweenInclusive(1, 20);
		Block block = switch (roll)
		{
			case 1, 20 -> GalaxiesBlocks.IMPERIAL_CORRUGATED_CRATE.get();
			case 2 -> GalaxiesBlocks.MEDICAL_CORRUGATED_CRATE.get();
			case 3 -> GalaxiesBlocks.MINING_CORRUGATED_CRATE.get();
			default -> BuiltInRegistries.BLOCK.get(Galaxies.id(RANDOM_DYES[roll - 4].getName() + "_corrugated_crate"));
		};
		world.setBlock(pos, block.defaultBlockState(), Block.UPDATE_CLIENTS);
		if (world.getBlockEntity(pos) instanceof CorrugatedCrateBlockEntity crate)
		{
			String table = switch (roll)
			{
				case 1, 20 -> "imperial_crate";
				case 2 -> "medical_crate";
				case 3 -> "mining_crate";
				default -> switch (random.nextIntBetweenInclusive(1, 7))
				{
					case 1, 2, 3 -> "generic_food_crate";
					case 4, 5, 6 -> "generic_tech_crate";
					default -> "generic_weapons_crate";
				};
			};
			ResourceKey<LootTable> loot = ResourceKey.create(Registries.LOOT_TABLE, Galaxies.id("container/" + table));
			crate.setLootTable(loot, random.nextLong());
			crate.setChanged();
		}
	}
}
