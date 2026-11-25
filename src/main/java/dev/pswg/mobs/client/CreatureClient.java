package dev.pswg.mobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.pswg.Galaxies;
import dev.pswg.mobs.Astromech;
import dev.pswg.mobs.Bantha;
import dev.pswg.mobs.Creatures;
import dev.pswg.mobs.GalaxiesFish;
import dev.pswg.mobs.SandSkitter;
import dev.pswg.mobs.Worrt;
import dev.pswg.rendering.nem.NemModels;
import dev.pswg.rendering.p3d.P3dRenderer;
import dev.pswg.rendering.p3d.P3dResources;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = Galaxies.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class CreatureClient
{
	private CreatureClient() {}

	@SubscribeEvent
	public static void renderers(EntityRenderersEvent.RegisterRenderers event)
	{
		event.registerEntityRenderer(Creatures.WORRT.get(), context -> new CreatureRenderer<>(context, "amphibian/worrt", 0.3F));
		event.registerEntityRenderer(Creatures.BANTHA.get(), context -> new CreatureRenderer<>(context, "mammal/bantha", 1.1F));
		event.registerEntityRenderer(Creatures.SAND_SKITTER.get(), context -> new CreatureRenderer<>(context, "rodent/sand_skitter", 0.2F));
		event.registerEntityRenderer(Creatures.FAA.get(), context -> new CreatureRenderer<>(context, "fish/faa", 0.2F));
		event.registerEntityRenderer(Creatures.LAA.get(), context -> new CreatureRenderer<>(context, "fish/laa", 0.2F));
		Creatures.DROIDS.forEach(droid -> event.registerEntityRenderer(droid.type().get(), DroidRenderer::new));
	}

	@SubscribeEvent
	public static void menus(RegisterMenuScreensEvent event) { event.register(Creatures.DROID_MENU.get(), DroidScreen::new); }

	@SubscribeEvent
	public static void reload(RegisterClientReloadListenersEvent event)
	{
		event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) resources -> AstromechPaint.clear());
	}

	private static final class CreatureRenderer<T extends Mob> extends MobRenderer<T, CreatureModel<T>>
	{
		private final ResourceLocation texture;

		CreatureRenderer(EntityRendererProvider.Context context, String path, float shadow)
		{
			super(context, new CreatureModel<>(Galaxies.id("mob/" + path)), shadow);
			texture = Galaxies.id("textures/entity/" + path + ".png");
		}

		@Override public ResourceLocation getTextureLocation(T mob) { return texture; }

		@Override
		protected void setupRotations(T mob, PoseStack pose, float age, float yaw, float partial, float scale)
		{
			super.setupRotations(mob, pose, age, yaw, partial, scale);
			if (mob instanceof GalaxiesFish && !mob.isNoAi())
			{
				pose.mulPose(Axis.YP.rotationDegrees(4.3F * Mth.sin(age * 0.6F)));
				if (!mob.isInWater())
				{
					pose.translate(0.1F, 0.1F, -0.1F);
					pose.mulPose(Axis.ZP.rotationDegrees(90));
				}
			}
		}

	}

	private static final class CreatureModel<T extends Mob> extends HierarchicalModel<T>
	{
		private final ResourceLocation id;
		private ModelPart mesh = new ModelPart(List.of(), Map.of());
		private int version = -1;

		CreatureModel(ResourceLocation id) { this.id = id; }

		@Override
		public ModelPart root()
		{
			if (version != NemModels.INSTANCE.version())
			{
				version = NemModels.INSTANCE.version();
				var loaded = NemModels.INSTANCE.bake(id);
				if (loaded != null) mesh = loaded;
			}
			return mesh;
		}

		@Override
		public void setupAnim(T mob, float walk, float amount, float age, float yaw, float pitch)
		{
			root().getAllParts().forEach(ModelPart::resetPose);
			if (mob instanceof Bantha bantha)
			{
				if (!bantha.forage.isStarted() && !bantha.call.isStarted())
					animateWalk(BanthaAnimations.BANTHA_WALK, walk, amount, 3, 1);
				update(bantha.forage, BanthaAnimations.BANTHA_FORAGE, age);
				update(bantha.call, BanthaAnimations.BANTHA_CALL, age);
			}
			else if (mob instanceof SandSkitter skitter)
			{
				if (!skitter.nibble.isStarted())
					animateWalk(SandSkitterAnimations.SANDSKITTER_SKITTER, walk, amount, 3, 1);
				update(skitter.nibble, SandSkitterAnimations.SANDSKITTER_NIBBLE, age);
			}
			else if (root().hasChild("body"))
			{
				var body = root().getChild("body");
				if (mob instanceof Worrt worrt)
					WorrtPose.apply(body, worrt, age - worrt.tickCount, age, yaw, pitch);
				else if (mob instanceof GalaxiesFish fish)
					fish(body, fish, fish.isNoAi() ? 0 : age);
			}
		}

		private void update(net.minecraft.world.entity.AnimationState state, net.minecraft.client.animation.AnimationDefinition animation, float age)
		{
			animate(state, animation, age);
			if (state.isStarted() && state.getAccumulatedTime() >= animation.lengthInSeconds() * 1000)
				state.stop();
		}

		private void fish(ModelPart body, GalaxiesFish fish, float age)
		{
			float movement = fish.isInWater() ? 1 : 1.5F;
			float tail = movement * Mth.sin(0.6F * age);
			float fin = movement * Mth.sin(0.1F * age);
			if (fish.getType() == Creatures.FAA.get() && body.hasChild("tailBase"))
			{
				var base = body.getChild("tailBase");
				var tip = base.getChild("tail");
				base.yRot = tip.yRot = -0.15F * tail;
				tip.getChild("caudalFin").yRot = -0.25F * tail;
				body.getChild("pectoralFinRight").yRot = 1 + 0.15F * fin;
			}
			else if (body.hasChild("tail"))
			{
				var base = body.getChild("tail");
				base.yRot = -0.1F * tail;
				base.getChild("tail_1").yRot = -0.3F * tail;
				body.getChild("pectoralFinLeft_1").yRot = 1 + 0.15F * fin;
				body.getChild("flailTop").xRot = 0.05F * fin;
				body.getChild("flailBottom").xRot = -0.349F - 0.05F * fin;
			}
			if (body.hasChild("pectoralFinLeft"))
				body.getChild("pectoralFinLeft").yRot = -1 - 0.15F * fin;
		}
	}

	private static final class DroidRenderer extends EntityRenderer<Astromech>
	{
		DroidRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius = 0.4F; }
		@Override public ResourceLocation getTextureLocation(Astromech droid) { return AstromechPaint.resolve(droid.paint()); }

		@Override
		public void render(Astromech droid, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light)
		{
			var model = P3dResources.MODELS.get(Galaxies.id("droid/r2"));
			if (model != null)
			{
				pose.pushPose();
				pose.scale(0.625F, 0.625F, 0.625F);
				pose.mulPose(Axis.YP.rotationDegrees(180 - yaw));
				var texture = getTextureLocation(droid);
				P3dRenderer.render(model, pose, buffers, part -> texture, part -> joint(droid, part, yaw, partial),
						light, OverlayTexture.NO_OVERLAY);
				pose.popPose();
			}
			super.render(droid, yaw, partial, pose, buffers, light);
		}

		private static Matrix4f joint(Astromech droid, String name, float yaw, float partial)
		{
			float extension = droid.extension(partial);
			float ease = 1 - (1 - extension) * (1 - extension) * (1 - extension);
			float headYaw = Mth.rotLerp(partial, droid.yHeadRotO, droid.yHeadRot) - yaw;
			float headPitch = -droid.getViewXRot(partial) * 0.2F * Mth.cos(headYaw * Mth.DEG_TO_RAD);
			float push = extension * extension * extension * 15;
			float lean = Mth.lerp(ease, headPitch, 23 + push);
			float leg = ease * 0.4535F;
			float foot = Mth.clamp((leg - 0.05F) / 0.4035F, 0, 1);
			var matrix = new Matrix4f();
			return switch (name)
			{
				case "left_leg" -> matrix.rotateX(-push * Mth.DEG_TO_RAD);
				case "right_foot" -> matrix.rotateX(push * Mth.DEG_TO_RAD);
				case "right_shoulder" -> matrix.rotateX(-lean * Mth.DEG_TO_RAD);
				case "center_leg" -> matrix.translate(0, -leg, 0);
				case "center_foot" -> matrix.rotateX(foot * foot * foot * (push - lean) * Mth.DEG_TO_RAD);
				case "body" -> matrix.rotateX(lean * Mth.DEG_TO_RAD);
				case "head" -> matrix.rotateY(-headYaw * Mth.DEG_TO_RAD);
				default -> matrix;
			};
		}
	}

	private static final class DroidScreen extends AbstractContainerScreen<Astromech.Menu>
	{
		private static final ResourceLocation TEXTURE = Galaxies.id("textures/gui/container/astromech.png");
		DroidScreen(Astromech.Menu menu, Inventory slots, Component title) { super(menu, slots, title); }
		@Override
		protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY)
		{
			graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
		}
		@Override
		public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial)
		{
			super.render(graphics, mouseX, mouseY, partial);
			renderTooltip(graphics, mouseX, mouseY);
		}
	}
}
