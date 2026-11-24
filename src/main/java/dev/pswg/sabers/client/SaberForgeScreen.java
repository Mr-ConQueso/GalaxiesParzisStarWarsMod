package dev.pswg.sabers.client;

import dev.pswg.Galaxies;
import dev.pswg.sabers.SaberForge;
import dev.pswg.sabers.SaberItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

final class SaberForgeScreen extends AbstractContainerScreen<SaberForge.Menu>
{
	private static final ResourceLocation TEXTURE = Galaxies.id("textures/gui/container/lightsaber_forge.png");
	private final Channel[] channels = new Channel[3];
	private ItemStack last = ItemStack.EMPTY;
	private boolean unstable;
	private Button unstableButton;
	private Button apply;

	SaberForgeScreen(SaberForge.Menu menu, Inventory player, Component title)
	{
		super(menu, player, title);
		imageWidth = 256;
		imageHeight = 241;
		inventoryLabelX = 48;
		inventoryLabelY = 147;
	}

	@Override
	protected void init()
	{
		super.init();
		titleLabelX = (imageWidth - font.width(title)) / 2;
		for (int index = 0; index < channels.length; index++)
			channels[index] = addRenderableWidget(new Channel(index));
		unstableButton = addRenderableWidget(Button.builder(unstableLabel(), button -> {
			unstable = !unstable;
			button.setMessage(unstableLabel());
		}).bounds(leftPos + 159, topPos + 65, 85, 20).build());
		apply = addRenderableWidget(Button.builder(Component.translatable("screen.pswg.saber.apply"), button -> {
			int rgb = Mth.hsvToRgb(channels[0].value(), channels[1].value(), channels[2].value());
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, SaberForge.EDIT_FLAG | rgb | (unstable ? SaberForge.UNSTABLE_FLAG : 0));
		}).bounds(leftPos + 173, topPos + 90, 57, 20).build());
		refresh();
	}

	private Component unstableLabel()
	{
		return Component.translatable(unstable ? "screen.pswg.saber.unstable" : "screen.pswg.saber.stable");
	}

	@Override
	protected void containerTick()
	{
		super.containerTick();
		if (!ItemStack.matches(last, menu.saber()))
			refresh();
	}

	private void refresh()
	{
		var stack = menu.saber();
		boolean present = stack.getItem() instanceof SaberItem;
		last = stack.copy();
		float[] hsv = new float[3];
		unstable = false;
		if (present)
		{
			var state = SaberItem.state(stack);
			int color = state.color();
			java.awt.Color.RGBtoHSB(color >> 16 & 255, color >> 8 & 255, color & 255, hsv);
			unstable = state.unstable();
		}
		for (int i = 0; i < channels.length; i++)
		{
			channels[i].set(hsv[i]);
			channels[i].active = present;
		}
		unstableButton.active = apply.active = present;
		unstableButton.setMessage(unstableLabel());
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
		if (!menu.saber().isEmpty())
		{
			int color = Mth.hsvToRgb(channels[0].value(), channels[1].value(), channels[2].value());
			graphics.renderItem(menu.saber(), leftPos + 65, topPos + 28);
			graphics.fill(leftPos + 82, topPos + 33, leftPos + 181, topPos + 40, 0x70000000 | color);
			graphics.fill(leftPos + 82, topPos + 35, leftPos + 178, topPos + 38, 0xFF000000 | color);
			graphics.fill(leftPos + 82, topPos + 36, leftPos + 176, topPos + 37, -1);
		}
	}

	private final class Channel extends AbstractSliderButton
	{
		private final int index;

		private Channel(int index)
		{
			super(leftPos + 41, topPos + 59 + index * 23, 100, 20, Component.empty(), 0);
			this.index = index;
			updateMessage();
		}

		private void set(float value)
		{
			this.value = value;
			updateMessage();
		}

		private float value() { return (float) value; }

		@Override
		protected void updateMessage()
		{
			String key = switch (index)
			{
				case 0 -> "hue";
				case 1 -> "saturation";
				default -> "value";
			};
			setMessage(Component.translatable("screen.pswg.saber." + key, Math.round(value * 100)));
		}

		@Override
		protected void applyValue() {}
	}
}
