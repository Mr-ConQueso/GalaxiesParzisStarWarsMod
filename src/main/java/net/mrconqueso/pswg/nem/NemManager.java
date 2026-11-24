package net.mrconqueso.pswg.nem;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.mrconqueso.pswg.data.KeyedReloadableLoader;
import net.mrconqueso.pswg.nem.mixin.ModelPartAccessor;
import net.mrconqueso.pswg.util.client.render.ModelAngleAnimator;
import net.mrconqueso.pswg.util.client.render.MutableAnimatedModel;
import org.apache.commons.lang3.tuple.Pair;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

// Assuming KeyedReloadableLoader is defined elsewhere in your project
public class NemManager extends KeyedReloadableLoader<LayerDefinition> {
	private final ResourceLocation id;
	private final Map<ResourceLocation, LayerDefinition> modelData;
	private final ArrayList<Pair<ResourceLocation, Consumer<ModelPart>>> models;
	private final HashMap<ResourceLocation, PlayerModel<?>> playerModels;
	private final HashMap<ResourceLocation, HumanoidModel<?>> bipedModels;

	public NemManager(ResourceLocation id) {
		super("models", "nem");
		this.id = id;
		this.modelData = new HashMap<>();
		this.models = new ArrayList<>();
		this.playerModels = new HashMap<>();
		this.bipedModels = new HashMap<>();
	}

	@Override
	public LayerDefinition readResource(ResourceManager resourceManager, ProfilerFiller profiler, InputStream stream) throws IOException {
		return buildModel(NbtIo.read(new DataInputStream(stream), NbtAccounter.unlimitedHeap()));
	}

	public static LayerDefinition buildModel(CompoundTag nbt) {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		addChildren(root, nbt.getCompound("parts"));

		CompoundTag texTag = nbt.getCompound("tex");
		return LayerDefinition.create(mesh, texTag.getInt("w"), texTag.getInt("h"));
	}

	private static void addChildren(PartDefinition root, CompoundTag parts) {
		for (String key : parts.getAllKeys()) {
			addChild(root, key, parts.getCompound(key));
		}
	}

	private static void addChild(PartDefinition parent, String partName, CompoundTag part) {
		CompoundTag tex = part.getCompound("tex");
		int partU = tex.getInt("u");
		int partV = tex.getInt("v");
		boolean mirrored = tex.getBoolean("mirrored");

		CompoundTag pos = part.getCompound("pos");
		float x = pos.getFloat("x");
		float y = pos.getFloat("y");
		float z = pos.getFloat("z");

		CompoundTag rot = part.getCompound("rot");
		float pitch = rot.getFloat("pitch");
		float yaw = rot.getFloat("yaw");
		float roll = rot.getFloat("roll");

		PartPose transform = PartPose.offsetAndRotation(x, y, z, pitch, yaw, roll);
		CubeListBuilder partBuilder = CubeListBuilder.create();

		// 10 is the Tag ID for CompoundTag
		ListTag cuboids = part.getList("cuboids", 10);

		for (int i = 0; i < cuboids.size(); i++) {
			CompoundTag cuboid = cuboids.getCompound(i);

			CompoundTag cPos = cuboid.getCompound("pos");
			float cX = cPos.getFloat("x");
			float cY = cPos.getFloat("y");
			float cZ = cPos.getFloat("z");

			CompoundTag cSize = cuboid.getCompound("size");
			int cSX = cSize.getInt("x");
			int cSY = cSize.getInt("y");
			int cSZ = cSize.getInt("z");

			CompoundTag cExpand = cuboid.getCompound("expand");
			float cEX = cExpand.getFloat("x");
			float cEY = cExpand.getFloat("y");
			float cEZ = cExpand.getFloat("z");

			CompoundTag cTex = cuboid.getCompound("tex");
			int cU = cTex.getInt("u");
			int cV = cTex.getInt("v");
			boolean cMirrored = cTex.getBoolean("mirrored");

			partBuilder = partBuilder.mirror(cMirrored ^ mirrored)
					.texOffs(partU + cU, partV + cV)
					.addBox(cX, cY, cZ, cSX, cSY, cSZ, new CubeDeformation(cEX, cEY, cEZ));
		}

		PartDefinition childPart = parent.addOrReplaceChild(partName, partBuilder, transform);

		if (part.contains("children")) {
			addChildren(childPart, part.getCompound("children"));
		}
	}

	@Override
	protected void apply(Map<ResourceLocation, LayerDefinition> prepared, ResourceManager manager, ProfilerFiller profiler) {
		modelData.clear();
		modelData.putAll(prepared);

		for (var modelInfo : models) {
			ResourceLocation modelId = modelInfo.getLeft();
			Consumer<ModelPart> model = modelInfo.getRight();

			if (modelData.containsKey(modelId)) {
				model.accept(modelData.get(modelId).bakeRoot());
			}
		}
	}

	// Assuming MutableAnimatedModel and ModelAngleAnimator are your custom classes
	public <T extends Entity> MutableAnimatedModel<T> getModel(ResourceLocation modelId, ModelAngleAnimator<T> angleAnimator) {
		MutableAnimatedModel<T> model = new MutableAnimatedModel<>(angleAnimator);
		models.add(new Pair<>(modelId, model::setRoot));
		return model;
	}

	public Supplier<PlayerModel<AbstractClientPlayer>> getPlayerModel(ResourceLocation modelId, boolean thinArms) {
		models.add(new Pair<>(modelId, modelPart -> playerModels.put(modelId, new PlayerModel<>(modelPart, thinArms))));
		return () -> (PlayerModel<AbstractClientPlayer>) playerModels.get(modelId);
	}

	public Supplier<PlayerModel<AbstractClientPlayer>> getOverridingPlayerModel(ResourceLocation overrideModelId, ResourceLocation baseModelId, boolean thinArms) {
		models.add(new Pair<>(overrideModelId, modelPart -> playerModels.put(overrideModelId, combinePlayerModels(baseModelId, modelPart, thinArms))));
		return () -> (PlayerModel<AbstractClientPlayer>) playerModels.get(overrideModelId);
	}

	public Supplier<HumanoidModel<LivingEntity>> getBipedModel(ResourceLocation overrideModelId, ResourceLocation baseModelId, boolean thinArms) {
		models.add(new Pair<>(overrideModelId, modelPart -> bipedModels.put(overrideModelId, combinePlayerModels(baseModelId, modelPart, thinArms))));
		return () -> (HumanoidModel<LivingEntity>) bipedModels.get(overrideModelId);
	}

	public Supplier<HumanoidModel<LivingEntity>> getBipedModel(ResourceLocation overrideModelId) {
		models.add(new Pair<>(overrideModelId, modelPart -> bipedModels.put(overrideModelId, new HumanoidModel<>(ensureBipedParts(modelPart)))));
		return () -> (HumanoidModel<LivingEntity>) bipedModels.get(overrideModelId);
	}

	public Supplier<HumanoidModel<LivingEntity>> getBipedArmorModel(ResourceLocation modelId) {
		// HumanoidArmorModel is functionally equivalent to HumanoidModel in modern mappings
		models.add(new Pair<>(modelId, modelPart -> bipedModels.put(modelId, new HumanoidModel<>(ensureBipedParts(modelPart)))));
		return () -> (HumanoidModel<LivingEntity>) bipedModels.get(modelId);
	}

	private static ModelPartAccessor mpa(Object o) {
		return (ModelPartAccessor) o;
	}

	private <T extends LivingEntity> PlayerModel<T> combinePlayerModels(ResourceLocation baseModelId, ModelPart overrideModel, boolean thinArms) {
		LayerDefinition baseModelData = modelData.get(baseModelId);
		ModelPart baseModel = baseModelData.bakeRoot();

		Map<String, ModelPart> baseChildren = mpa(baseModel).getChildren();
		Map<String, ModelPart> overrideChildren = mpa(overrideModel).getChildren();

		for (Map.Entry<String, ModelPart> entry : overrideChildren.entrySet()) {
			if (entry.getValue().isEmpty() && mpa(entry.getValue()).getChildren().isEmpty()) {
				continue;
			}
			baseChildren.put(entry.getKey(), entry.getValue());
		}

		return new PlayerModel<>(baseModel, thinArms);
	}

	private static ModelPart ensureBipedParts(ModelPart modelPart) {
		Map<String, ModelPart> children = mpa(modelPart).getChildren();

		String[] requiredParts = {"head", "hat", "body", "right_arm", "left_arm", "right_leg", "left_leg"};
		for (String partName : requiredParts) {
			if (!children.containsKey(partName)) {
				children.put(partName, createEmptyModelPart());
			}
		}

		return modelPart;
	}

	private static ModelPart createEmptyModelPart() {
		// Requires a mutable map so that elements can be safely added/modified later
		return new ModelPart(List.of(), new HashMap<>());
	}
}