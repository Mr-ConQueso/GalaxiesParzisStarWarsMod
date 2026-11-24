package dev.pswg.utilities.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.pswg.Galaxies;
import dev.pswg.rendering.p3d.P3dRenderer;
import dev.pswg.rendering.p3d.P3dResources;
import dev.pswg.utilities.CreatureCage;
import dev.pswg.utilities.LegacyUtilities;
import dev.pswg.utilities.MoistureVaporator;
import dev.pswg.utilities.PowerCoupling;
import dev.pswg.utilities.SlidingDoor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@EventBusSubscriber(modid = Galaxies.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class UtilityClient
{
	private UtilityClient() {}

	@SubscribeEvent
	public static void renderers(EntityRenderersEvent.RegisterRenderers event)
	{
		event.registerBlockEntityRenderer(LegacyUtilities.COUPLING_ENTITY.get(), context -> new CableRenderer());
		event.registerBlockEntityRenderer(LegacyUtilities.DOOR_ENTITY.get(), context -> new DoorRenderer());
		event.registerBlockEntityRenderer(LegacyUtilities.CAGE_ENTITY.get(), context -> new CageRenderer());
	}

	@SubscribeEvent
	public static void menus(RegisterMenuScreensEvent event)
	{
		event.register(LegacyUtilities.VAPORATOR_MENU.get(), VaporatorScreen::new);
	}

	private static final class VaporatorScreen extends AbstractContainerScreen<MoistureVaporator.Menu>
	{
		private static final ResourceLocation TEXTURE = Galaxies.id("textures/gui/container/moisture_vaporator.png");
		VaporatorScreen(MoistureVaporator.Menu menu, Inventory inventory, Component title) { super(menu, inventory, title); }
		@Override protected void init() { super.init(); titleLabelX = (imageWidth - font.width(title)) / 2; }
		@Override protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY)
		{
			graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
			int fill = (int) (menu.progress() * 30);
			graphics.blit(TEXTURE, leftPos + 103, topPos + 58 - fill, 176, 30 - fill, 9, fill);
		}
		@Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial)
		{
			super.render(graphics, mouseX, mouseY, partial);
			renderTooltip(graphics, mouseX, mouseY);
		}
	}

	private static final class DoorRenderer implements BlockEntityRenderer<SlidingDoor.Animation>
	{
		private static final ResourceLocation MODEL = Galaxies.id("block/tatooine_home_door");
		@Override
		public void render(SlidingDoor.Animation door, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay)
		{
			var state = door.getBlockState();
			if (!state.is(LegacyUtilities.SLIDING_DOOR.get())) return;
			var model = P3dResources.MODELS.get(MODEL);
			var color = SlidingDoor.color(state);
			if (model == null || color == null) return;
			var texture = Galaxies.id("textures/block/model/door/sliding_1x2/door_" + color.getName() + ".png");
			pose.pushPose();
			pose.translate(0.5, 0, 0.5);
			pose.mulPose(Axis.YP.rotationDegrees(switch (state.getValue(SlidingDoor.FACING))
			{
				case NORTH -> 90;
				case SOUTH -> -90;
				case WEST -> 180;
				default -> 0;
			}));
			pose.translate(0, 0, 1.352F * 0.625F * door.openness(partial));
			pose.mulPose(Axis.YN.rotationDegrees(90));
			pose.scale(0.625F, 0.625F, 0.625F);
			P3dRenderer.render(model, pose, buffers, part -> texture, part -> part.equals("frame") ? null : new Matrix4f(), light, overlay);
			pose.popPose();
		}
		@Override public AABB getRenderBoundingBox(SlidingDoor.Animation door) { return new AABB(door.getBlockPos()).inflate(1, 1, 1); }
	}

	private static final class CableRenderer implements BlockEntityRenderer<PowerCoupling.Links>
	{
		private static final ResourceLocation TEXTURE = Galaxies.id("textures/block/model/cable.png");

		@Override
		public void render(PowerCoupling.Links links, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay)
		{
			var level = links.getLevel();
			if (level == null || links.outgoing().isEmpty()) return;
			var pos = links.getBlockPos();
			var origin = Vec3.atLowerCornerOf(pos);
			var from = port(links.getBlockState().getValue(PowerCoupling.FACING)).add(0.5, 0.5, 0.5);
			var vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
			for (var target : links.outgoing())
			{
				if (!level.hasChunkAt(target) || !level.getBlockState(target).is(LegacyUtilities.COUPLING.get())) continue;
				var to = Vec3.atCenterOf(target).subtract(origin).add(port(level.getBlockState(target).getValue(PowerCoupling.FACING)));
				var path = to.subtract(from);
				int steps = Math.max(4, (int) Math.ceil(path.length() * 3));
				double sag = Math.min(3, path.horizontalDistance() * 0.115);
				var last = cableRing(from, path, sag, 0);
				for (int i = 1; i <= steps; i++)
				{
					var next = cableRing(from, path, sag, (double) i / steps);
					for (int side = 0; side < 4; side++)
					{
						int neighbor = (side + 1) % 4;
						var normal = last[neighbor].subtract(last[side]).cross(next[side].subtract(last[side])).normalize().toVector3f();
						vertex(vertices, pose.last(), last[side], normal, -1, side * 0.25F, 0, light, overlay);
						vertex(vertices, pose.last(), last[neighbor], normal, -1, (side + 1) * 0.25F, 0, light, overlay);
						vertex(vertices, pose.last(), next[neighbor], normal, -1, (side + 1) * 0.25F, 1, light, overlay);
						vertex(vertices, pose.last(), next[side], normal, -1, side * 0.25F, 1, light, overlay);
					}
					last = next;
				}
			}
		}

		private static Vec3 port(Direction direction) { return Vec3.atLowerCornerOf(direction.getNormal()).scale(-5 / 16.0); }

		private static Vec3[] cableRing(Vec3 from, Vec3 path, double sag, double t)
		{
			var center = from.add(path.scale(t)).add(0, -4 * sag * t * (1 - t), 0);
			var tangent = path.add(0, -4 * sag * (1 - 2 * t), 0).normalize();
			var lateral = tangent.cross(Math.abs(tangent.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize().scale(0.03125);
			var up = tangent.cross(lateral).normalize().scale(0.03125);
			return new Vec3[] { center.add(lateral).add(up), center.subtract(lateral).add(up), center.subtract(lateral).subtract(up), center.add(lateral).subtract(up) };
		}

		@Override
		public AABB getRenderBoundingBox(PowerCoupling.Links links)
		{
			var bounds = new AABB(links.getBlockPos());
			for (var target : links.outgoing()) bounds = bounds.minmax(new AABB(target));
			return bounds.inflate(0.2, 3.2, 0.2);
		}
	}

	private static final class CageRenderer implements BlockEntityRenderer<CreatureCage.Contents>
	{
		private static final ResourceLocation WATER = ResourceLocation.withDefaultNamespace("block/water_still");

		@Override
		public void render(CreatureCage.Contents cage, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay)
		{
			var minecraft = Minecraft.getInstance();
			var state = cage.getBlockState();
			int water = state.getOptionalValue(CreatureCage.Terrarium.WATER).orElse(0);
			var occupant = cage.preview();
			if (occupant != null)
			{
				pose.pushPose();
				pose.translate(0.5, water == 0 ? 0.06 : water / 36.0, 0.5);
				float scale = 0.56F / Math.max(occupant.getBbWidth(), occupant.getBbHeight());
				pose.scale(scale, scale, scale);
				minecraft.getEntityRenderDispatcher().getRenderer(occupant).render(occupant, 0, partial, pose, buffers, light);
				pose.popPose();
			}
			if (water > 0 && cage.getLevel() != null)
			{
				var sprite = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(WATER);
				var surface = buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
				int color = 0xa0000000 | BiomeColors.getAverageWaterColor(cage.getLevel(), cage.getBlockPos());
				double a = 0.025, b = 0.975, y = water / 10.0;
				waterFace(surface, pose.last(), sprite, new Vector3f(0, 1, 0), color, light, overlay,
						new Vec3(a, y, a), new Vec3(a, y, b), new Vec3(b, y, b), new Vec3(b, y, a));
				waterFace(surface, pose.last(), sprite, new Vector3f(0, 0, -1), color, light, overlay,
						new Vec3(a, 0, a), new Vec3(a, y, a), new Vec3(b, y, a), new Vec3(b, 0, a));
				waterFace(surface, pose.last(), sprite, new Vector3f(0, 0, 1), color, light, overlay,
						new Vec3(b, 0, b), new Vec3(b, y, b), new Vec3(a, y, b), new Vec3(a, 0, b));
				waterFace(surface, pose.last(), sprite, new Vector3f(-1, 0, 0), color, light, overlay,
						new Vec3(a, 0, b), new Vec3(a, y, b), new Vec3(a, y, a), new Vec3(a, 0, a));
				waterFace(surface, pose.last(), sprite, new Vector3f(1, 0, 0), color, light, overlay,
						new Vec3(b, 0, a), new Vec3(b, y, a), new Vec3(b, y, b), new Vec3(b, 0, b));
			}
		}

		private static void waterFace(VertexConsumer out, PoseStack.Pose pose, TextureAtlasSprite sprite, Vector3f normal, int color,
		                              int light, int overlay, Vec3 a, Vec3 b, Vec3 c, Vec3 d)
		{
			vertex(out, pose, a, normal, color, sprite.getU0(), sprite.getV1(), light, overlay);
			vertex(out, pose, b, normal, color, sprite.getU0(), sprite.getV0(), light, overlay);
			vertex(out, pose, c, normal, color, sprite.getU1(), sprite.getV0(), light, overlay);
			vertex(out, pose, d, normal, color, sprite.getU1(), sprite.getV1(), light, overlay);
		}
	}

	private static void vertex(VertexConsumer out, PoseStack.Pose pose, Vec3 point, Vector3f normal, int color, float u, float v, int light, int overlay)
	{
		out.addVertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z).setColor(color).setUv(u, v)
				.setOverlay(overlay).setLight(light).setNormal(pose, normal.x, normal.y, normal.z);
	}
}
