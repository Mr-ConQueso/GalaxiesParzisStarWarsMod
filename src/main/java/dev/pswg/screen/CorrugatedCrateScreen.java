package dev.pswg.screen;

import dev.pswg.Galaxies;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class CorrugatedCrateScreen extends AbstractContainerScreen<CorrugatedCrateMenu>
{
	private final ResourceLocation texture;

	public CorrugatedCrateScreen(CorrugatedCrateMenu menu, Inventory inventory, Component title)
	{
		super(menu, inventory, title);
		if (menu.columns() == 13)
		{
			imageWidth = 248;
			inventoryLabelX = 44;
		}
		texture = Galaxies.id("textures/gui/container/crate_" + menu.columns() + "x3.png");
	}

	@Override
	protected void init()
	{
		super.init();
		inventoryLabelY = imageHeight - 94;
		titleLabelX = (imageWidth - font.width(title)) / 2;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
	{
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
	{
		graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
	}
}
