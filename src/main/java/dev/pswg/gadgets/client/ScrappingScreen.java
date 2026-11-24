package dev.pswg.gadgets.client;

import dev.pswg.gadgets.scrapping.ScrappingMenu;
import dev.pswg.gadgets.scrapping.ScrappingTable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public final class ScrappingScreen extends AbstractContainerScreen<ScrappingMenu>
{
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("pswg_gadgets", "textures/gui/container/scrapping_table.png");
	private static final int[][] MATERIAL_ICONS = { { 45, 192, 0, 9, 8 }, { 44, 192, 9, 10, 7 },
			{ 43, 203, 0, 12, 8 }, { 47, 205, 9, 6, 8 } };

	public ScrappingScreen(ScrappingMenu menu, Inventory inventory, Component title)
	{
		super(menu, inventory, title);
		imageWidth = 175;
		imageHeight = 213;
		inventoryLabelY = 112;
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
		texture(graphics, 0, 0, 0, 0, imageWidth, imageHeight);
		int hovered = -1;
		for (int tool = 0; tool < 3; tool++)
		{
			int progress = menu.progress(tool);
			if (progress < 0)
				continue;
			boolean over = overTool(tool, mouseX, mouseY);
			if (over)
				hovered = tool;
			texture(graphics, 31, 48 + tool * 22, 177, over ? 29 : 18, 10, 10);
			int fill = Mth.ceil(17F * progress / ScrappingTable.MAX_PROGRESS);
			if (fill > 0)
				texture(graphics, 25, 62 + tool * 22 - fill, 187, 17 - fill, 4, fill);
		}

		for (int material = 0; material < MATERIAL_ICONS.length; material++)
		{
			var component = ScrappingTable.material(material);
			if (!menu.input().has(component))
				continue;
			int amount = Mth.clamp(menu.input().getOrDefault(component, 0), 0, 5);
			int y = 55 + material * 12;
			int[] icon = MATERIAL_ICONS[material];
			texture(graphics, icon[0], y - 2, icon[1], icon[2], icon[3], icon[4]);
			texture(graphics, 56, y, 177, 42 + material * 5, 1 + 13 * amount, 4);
			if (hovered >= 0)
				for (int i = 0; i < Math.min(amount, ScrappingTable.cost(hovered, material)); i++)
					texture(graphics, 56 + 13 * (amount - i - 1), y, 177, 62, 14, 4);
		}
	}

	@Override
	public boolean mouseClicked(double x, double y, int button)
	{
		if (button == 0)
			for (int tool = 0; tool < 3; tool++)
				if (menu.progress(tool) >= 0 && overTool(tool, x, y))
				{
					minecraft.gameMode.handleInventoryButtonClick(menu.containerId, tool);
					return true;
				}
		return super.mouseClicked(x, y, button);
	}

	private boolean overTool(int tool, double x, double y)
	{
		int top = topPos + 48 + tool * 22;
		return x >= leftPos + 31 && x < leftPos + 41 && y >= top && y < top + 10;
	}

	private void texture(GuiGraphics graphics, int x, int y, int u, int v, int width, int height)
	{
		graphics.blit(TEXTURE, leftPos + x, topPos + y, u, v, width, height);
	}
}
