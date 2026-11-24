package dev.pswg.structure;

import com.mojang.serialization.MapCodec;
import dev.pswg.Galaxies;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;
import java.util.function.Supplier;

public final class DerelictContainer extends Structure
{
	public static final ResourceKey<Structure> KEY = ResourceKey.create(Registries.STRUCTURE, Galaxies.id("derelict_imperial_container"));
	public static final MapCodec<DerelictContainer> CODEC = simpleCodec(DerelictContainer::new);
	private static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Galaxies.MODID);
	private static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, "minecraft");
	public static final Supplier<StructureType<DerelictContainer>> TYPE = TYPES.register(KEY.location().getPath(), () -> () -> CODEC);
	static final Supplier<StructurePieceType> PIECE = PIECES.register("derelictimpcontainer",
			() -> (StructurePieceType.StructureTemplateType) DerelictContainerPiece::new);

	private DerelictContainer(StructureSettings settings)
	{
		super(settings);
	}

	public static void register(IEventBus bus)
	{
		TYPES.register(bus);
		PIECES.register(bus);
	}

	@Override
	public StructureType<?> type()
	{
		return TYPE.get();
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context)
	{
		var chunk = context.chunkPos();
		int x = chunk.getMinBlockX();
		int z = chunk.getMinBlockZ();
		int surface = context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
				context.heightAccessor(), context.randomState());
		if (surface <= context.heightAccessor().getMinBuildHeight())
			return Optional.empty();

		var rotation = Rotation.getRandom(context.random());
		var origin = new BlockPos(x, surface + context.random().nextIntBetweenInclusive(-3, 0), z);
		return Optional.of(new GenerationStub(origin, pieces ->
				pieces.addPiece(new DerelictContainerPiece(context.structureTemplateManager(), origin, rotation))));
	}

	public static void placeInTatooine(WorldGenLevel level, ChunkAccess chunk, StructureManager structures, ChunkGenerator generator)
	{
		if (!structures.shouldGenerateStructures())
			return;

		var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(KEY);
		if (structure == null)
			return;

		var pos = chunk.getPos();
		var section = SectionPos.of(pos, level.getMinSection());
		var height = chunk.getHeightAccessorForGeneration();
		var writable = new BoundingBox(pos.getMinBlockX(), height.getMinBuildHeight() + 1, pos.getMinBlockZ(),
				pos.getMaxBlockX(), height.getMaxBuildHeight() - 1, pos.getMaxBlockZ());
		var random = new WorldgenRandom(new XoroshiroRandomSource(level.getSeed()));
		long seed = random.setDecorationSeed(level.getSeed(), pos.getMinBlockX(), pos.getMinBlockZ());
		random.setFeatureSeed(seed, 0, structure.step().ordinal());
		structures.startsForStructure(section, structure)
				.forEach(start -> start.placeInChunk(level, structures, generator, random, writable, pos));
	}
}
