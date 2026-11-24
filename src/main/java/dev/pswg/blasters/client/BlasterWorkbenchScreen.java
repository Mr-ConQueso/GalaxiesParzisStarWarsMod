package dev.pswg.blasters.client;

import dev.pswg.Galaxies;
import dev.pswg.blasters.BlasterItem;
import dev.pswg.blasters.BlasterWorkbench;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

final class BlasterWorkbenchScreen extends AbstractContainerScreen<BlasterWorkbench.Menu>
{
	private static final ResourceLocation TEXTURE = Galaxies.id("textures/gui/container/blaster_workbench.png");
	private int scroll;

	BlasterWorkbenchScreen(BlasterWorkbench.Menu menu, Inventory player, Component title)
	{
		super(menu, player, title);
		imageHeight = 256;
		inventoryLabelY = 164;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial)
	{
		super.render(graphics, mouseX, mouseY, partial);
		renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY)
	{
		graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
		var weapon = menu.weapon();
		var definition = menu.definition();
		if (weapon.isEmpty() || definition == null)
			return;
		graphics.pose().pushPose();
		graphics.pose().translate(leftPos + 88, topPos + 17, 0);
		graphics.pose().scale(2.5F, 2.5F, 1F);
		graphics.renderItem(weapon, 0, 0);
		graphics.pose().popPose();

		var options = menu.options();
		scroll = Mth.clamp(scroll, 0, Math.max(0, options.size() - 3));
		var installed = BlasterItem.attachments(weapon, definition).applied();
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
	}

	@Override
	public boolean mouseClicked(double x, double y, int button)
	{
		if (button == 0 && x >= leftPos + 52 && x < leftPos + 146 && y >= topPos + 70 && y < topPos + 121)
		{
			int index = scroll + (int) (y - topPos - 70) / 17;
			if (index < menu.options().size())
				minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index);
			return true;
		}
		return super.mouseClicked(x, y, button);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double horizontal, double vertical)
	{
		if (x >= leftPos + 52 && x < leftPos + 158 && y >= topPos + 70 && y < topPos + 121)
		{
			scroll = Mth.clamp(scroll - (int) Math.signum(vertical), 0, Math.max(0, menu.options().size() - 3));
			return true;
		}
		return super.mouseScrolled(x, y, horizontal, vertical);
	}
}
