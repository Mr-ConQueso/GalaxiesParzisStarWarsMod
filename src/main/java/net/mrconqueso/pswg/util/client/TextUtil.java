package net.mrconqueso.pswg.util.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class TextUtil {
	public static Component stylizeKeybind(Component text) {
		return Component.literal("[").append(text).append("]").withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD));
	}

	public static void drawArea(GuiGraphics guiGraphics, Font font, int x, int y, int width, Component text) {
		List<FormattedCharSequence> lines = font.split(text, width);

		for (int m = 0; m < lines.size(); ++m) {
			guiGraphics.drawString(font, lines.get(m), x, y + m * font.lineHeight, 0, false);
		}
	}
}