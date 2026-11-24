package dev.pswg.gadgets.client;

import dev.pswg.gadgets.brewing.BrewingMap;
import dev.pswg.gadgets.brewing.Mixer;
import dev.pswg.gadgets.brewing.MixerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.joml.Vector2i;

import java.util.ArrayDeque;
import java.util.Deque;

public final class MixerScreen extends AbstractContainerScreen<MixerMenu>
{
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("pswg_gadgets", "textures/gui/container/mixer.png");
	private static final ResourceLocation MAP = ResourceLocation.fromNamespaceAndPath("pswg_gadgets", "textures/gui/misc/brewing_map.png");
	private static final int VIEW = 128;
	private final Deque<Vector2i> trail = new ArrayDeque<>();
	private boolean pumping;

	public MixerScreen(MixerMenu menu, Inventory inventory, Component title)
	{
		super(menu, inventory, title);
		imageWidth = 175;
		imageHeight = 255;
		inventoryLabelY = 160;
	}

	@Override
	protected void containerTick()
	{
		super.containerTick();
		double x = minecraft.mouseHandler.xpos() * width / minecraft.getWindow().getScreenWidth();
		double y = minecraft.mouseHandler.ypos() * height / minecraft.getWindow().getScreenHeight();
		if (pumping && over(x, y, 149, 92, 10, 10) && menu.buttonEnabled(0))
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);

		var point = new Vector2i((int) menu.mapX(), (int) menu.mapY());
		if (point.x == 256 && point.y == 256)
			trail.clear();
		else if (trail.isEmpty() || trail.getLast().gridDistance(point) > 3)
		{
			trail.addLast(point);
			if (trail.size() > 60)
				trail.removeFirst();
		}
	}

	@Override
	public boolean mouseClicked(double x, double y, int button)
	{
		if (button == 1 && over(x, y, 149, 92, 10, 10))
		{
			pumping = true;
			return true;
		}
		if (button == 0)
			for (int action = 1; action <= 3; action++)
				if (over(x, y, 138 + (action - 1) * 12, 53, 8, 8) && menu.buttonEnabled(action))
				{
					minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
					return true;
				}
		return super.mouseClicked(x, y, button);
	}

	@Override
	public boolean mouseReleased(double x, double y, int button)
	{
		if (button == 1)
			pumping = false;
		return super.mouseReleased(x, y, button);
	}

	private boolean over(double mouseX, double mouseY, int x, int y, int w, int h)
	{
		return mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h;
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
		int viewX = Mth.clamp((int) menu.mapX() - VIEW / 2, 0, BrewingMap.SIZE - VIEW);
		int viewY = Mth.clamp((int) menu.mapY() - VIEW / 2, 0, BrewingMap.SIZE - VIEW);
		graphics.blit(MAP, leftPos + 7, topPos + 19, viewX, viewY, VIEW - 1, VIEW - 1, BrewingMap.SIZE, BrewingMap.SIZE);
		graphics.enableScissor(leftPos + 7, topPos + 19, leftPos + 134, topPos + 146);
		for (var point : trail)
		{
			int x = leftPos + 7 + point.x - viewX;
			int y = topPos + 19 + point.y - viewY;
			graphics.fill(x, y, x + 1, y + 1, 0xFFFFFFFF);
		}
		int markerX = Mth.clamp((int) menu.mapX() - viewX, 0, VIEW - 2);
		int markerY = Mth.clamp((int) menu.mapY() - viewY, 0, VIEW - 2);
		texture(graphics, 5 + markerX, 17 + markerY, 177, 69, 5, 5);
		graphics.disableScissor();

		int[] barStarts = { 97, 9, 53 };
		for (int group = 0; group < barStarts.length; group++)
			for (int entry = 0; entry < 3; entry++)
			{
				int color = menu.swatch(group, entry);
				if (color == 0)
					continue;
				graphics.setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1);
				int x = barStarts[group] + entry * 11;
				if (entry == 0)
					texture(graphics, x - 1, 151, 176, 146, 11, 5);
				texture(graphics, x, 150, 177, 145, 11, 5);
				if (entry == 2)
					texture(graphics, x + 11, 151, 188, 146, 11, 5);
				graphics.setColor(1, 1, 1, 1);
			}

		for (int action = 1; action <= 3; action++)
			if (menu.buttonEnabled(action))
			{
				int x = 138 + (action - 1) * 12;
				int v = 83 + (action - 1) * 16;
				texture(graphics, x, 53, 177, over(mouseX, mouseY, x, 53, 8, 8) ? v - 8 : v, 8, 8);
			}
		boolean overPump = over(mouseX, mouseY, 149, 92, 10, 10);
		if (menu.buttonEnabled(0))
			texture(graphics, 149, 92, 177, overPump ? 27 : 47, 10, 10);
		texture(graphics, 149, 92, 177, overPump ? 37 : 57, 10, menu.pressure() * 10 / Mixer.MAX_PRESSURE);
		int flame = menu.fuel() * 14 / 1000;
		texture(graphics, 146, 120 - flame, 177, 26 - flame, 14, flame);
	}

	private void texture(GuiGraphics graphics, int x, int y, int u, int v, int w, int h)
	{
		if (h > 0)
			graphics.blit(TEXTURE, leftPos + x, topPos + y, u, v, w, h);
	}
}
