package net.mrconqueso.pswg.util.client;

import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;

public class TooltipUtil {
	public static MutableComponent note(MutableComponent text) {
		return text.withStyle(ChatFormatting.GRAY);
	}

	public static MutableComponent aside(MutableComponent text) {
		return text.withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
	}

	public static Component getStatus(Item item, Object... args) {
		var reg = BuiltInRegistries.ITEM.getKey(item);
		return note(Component.translatable(getStatusKey(reg), args));
	}

	public static String getStatusKey(Item item) {
		var reg = BuiltInRegistries.ITEM.getKey(item);
		return getStatusKey(reg);
	}

	private static String getStatusKey(ResourceLocation registryId) {
		return "tooltip." + registryId.getNamespace() + ".status." + registryId.getPath();
	}

	public static Component getLore(Item item, Object... args) {
		var reg = BuiltInRegistries.ITEM.getKey(item);
		return aside(Component.translatable(getLoreKey("item", reg), args));
	}

	public static String getLoreKey(Item item) {
		var reg = BuiltInRegistries.ITEM.getKey(item);
		return getLoreKey("item", reg);
	}

	public static String getLoreKey(String domain, ResourceLocation registryId) {
		return "lore." + domain + "." + registryId.getNamespace() + "." + registryId.getPath();
	}
}