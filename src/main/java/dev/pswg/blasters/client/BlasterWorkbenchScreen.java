package dev.pswg.blasters.client;

import com.mojang.math.Axis;
import dev.pswg.Galaxies;
import dev.pswg.blasters.BlasterDefinition;
import dev.pswg.blasters.BlasterItem;
import dev.pswg.blasters.BlasterNetwork;
import dev.pswg.blasters.BlasterWorkbench;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

final class BlasterWorkbenchScreen extends AbstractContainerScreen<BlasterWorkbench.Menu>
{
	private static final ResourceLocation TEXTURE = Galaxies.id("textures/gui/container/blaster_workbench.png");
	private ItemStack original = ItemStack.EMPTY;
	private ItemStack preview = ItemStack.EMPTY;
	private BlasterDefinition editingDefinition;
	private Button build;
	private Button cancel;
	private int scroll;
	private float yaw;
	private float pitch;
	private boolean rotating;

	BlasterWorkbenchScreen(BlasterWorkbench.Menu menu, Inventory player, Component title)
	{
		super(menu, player, title);
		imageHeight = 256;
		inventoryLabelY = 164;
	}

	@Override
	protected void init()
	{
		super.init();
		build = addRenderableWidget(Button.builder(Component.translatable("screen.pswg.workbench.build"), ignored -> {
			refreshPreview();
			if (editingDefinition != null && !ItemStack.matches(original, preview))
				PacketDistributor.sendToServer(new BlasterNetwork.WorkbenchBuild(menu.containerId, original.copy(),
						BlasterItem.attachments(preview, editingDefinition)));
		}).bounds(leftPos + 51, topPos + 122, 52, 18).build());
		cancel = addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), ignored -> preview = original.copy())
				.bounds(leftPos + 105, topPos + 122, 52, 18).build());
	}

	private void refreshPreview()
	{
		var definition = menu.definition();
		if (definition == editingDefinition && ItemStack.matches(menu.weapon(), original))
			return;
		if (definition != editingDefinition)
		{
			scroll = 0;
			yaw = pitch = 0;
		}
		editingDefinition = definition;
		original = menu.weapon().copy();
		preview = original.copy();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial)
	{
		refreshPreview();
		build.visible = cancel.visible = editingDefinition != null;
		build.active = cancel.active = !ItemStack.matches(original, preview);
		super.render(graphics, mouseX, mouseY, partial);
		renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY)
	{
		graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
		if (editingDefinition == null)
			return;
		graphics.enableScissor(leftPos + 49, topPos + 13, leftPos + 157, topPos + 63);
		graphics.pose().pushPose();
		graphics.pose().translate(leftPos + 103, topPos + 38, 0);
		graphics.pose().mulPose(Axis.XP.rotationDegrees(pitch));
		graphics.pose().mulPose(Axis.YP.rotationDegrees(yaw));
		graphics.pose().scale(2.5F, 2.5F, 2.5F);
		graphics.renderItem(preview, -8, -8);
		graphics.pose().popPose();
		graphics.disableScissor();

		var options = menu.options();
		scroll = Mth.clamp(scroll, 0, Math.max(0, options.size() - 3));
		var installed = BlasterItem.attachments(preview, editingDefinition).applied();
		for (int row = 0; row < 3 && scroll + row < options.size(); row++)
		{
			int y = topPos + 70 + row * 17;
			var option = options.get(scroll + row);
			boolean selected = installed.containsValue(option.getKey());
			boolean hovered = mouseX >= leftPos + 52 && mouseX < leftPos + 146 && mouseY >= y && mouseY < y + 17;
			int color = selected ? 0xFF96CF93 : hovered ? 0xFFEEEECC : 0xFFE5E5E5;
			graphics.fill(leftPos + 52, y, leftPos + 146, y + 16, selected ? 0xC030572F : 0xC0414749);
			var label = Component.translatable(option.getValue().translationKey()).getString();
			if (label.equals(option.getValue().translationKey()))
				label = option.getKey().getPath().substring(option.getKey().getPath().lastIndexOf('/') + 1).replace('_', ' ');
			graphics.drawString(font, font.plainSubstrByWidth(label, 88), leftPos + 55, y + 4, color, false);
		}

		drawStat(graphics, 7, 141, "damage", "damage_multiplier", false);
		drawStat(graphics, 88, 141, "accuracy", "spread_multiplier", true);
		drawStat(graphics, 7, 152, "cooling", "cooling_multiplier", false);
		drawStat(graphics, 88, 152, "rate", "fire_rate_multiplier", false);
	}

	private void drawStat(GuiGraphics graphics, int x, int y, String label, String function, boolean invert)
	{
		float before = BlasterItem.multiplier(original, editingDefinition, function);
		float after = BlasterItem.multiplier(preview, editingDefinition, function);
		if (invert)
		{
			before = 1 / before;
			after = 1 / after;
		}
		var text = Component.translatable("screen.pswg.workbench." + label, Math.round(before * 100), Math.round(after * 100));
		graphics.drawString(font, font.plainSubstrByWidth(text.getString(), 80), leftPos + x, topPos + y, 0xff404040, false);
	}

	@Override
	public boolean mouseClicked(double x, double y, int button)
	{
		if (button == 0 && editingDefinition != null && x >= leftPos + 49 && x < leftPos + 157 && y >= topPos + 13 && y < topPos + 63)
		{
			rotating = true;
			return true;
		}
		if (button == 0 && editingDefinition != null && x >= leftPos + 52 && x < leftPos + 146 && y >= topPos + 70 && y < topPos + 121)
		{
			var options = menu.options();
			int index = scroll + (int) (y - topPos - 70) / 17;
			if (index < options.size())
				BlasterWorkbench.toggleAttachment(preview, editingDefinition, options.get(index).getKey());
			return true;
		}
		return super.mouseClicked(x, y, button);
	}

	@Override
	public boolean mouseDragged(double x, double y, int button, double dx, double dy)
	{
		if (rotating && button == 0)
		{
			yaw = Mth.wrapDegrees(yaw + (float) dx * 2);
			pitch = Mth.clamp(pitch + (float) dy * 2, -70, 70);
			return true;
		}
		return super.mouseDragged(x, y, button, dx, dy);
	}

	@Override
	public boolean mouseReleased(double x, double y, int button)
	{
		if (rotating && button == 0)
		{
			rotating = false;
			return true;
		}
		return super.mouseReleased(x, y, button);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double horizontal, double vertical)
	{
		if (editingDefinition != null && x >= leftPos + 52 && x < leftPos + 158 && y >= topPos + 70 && y < topPos + 121)
		{
			scroll = Mth.clamp(scroll - (int) Math.signum(vertical), 0, Math.max(0, menu.options().size() - 3));
			return true;
		}
		return super.mouseScrolled(x, y, horizontal, vertical);
	}
}
