package net.mrconqueso.pswg.mixin.client.attributes;

import net.mrconqueso.pswg.attributes.ModEntityAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ItemAttributeModifiers;
//
//// Note the $ usage for the nested class
//@Mixin(targets = "net.minecraft.world.item.component.ItemAttributeModifiers$Display$Default")
//public abstract class AttributeModifiersComponent$Display$DefaultMixin
//{
//	/**
//	 * Replaces the default attribute text for some custom
//	 * attributes with custom formatting
//	 */
//	@Inject(
//			method = "accept(Ljava/util/function/Consumer;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/Holder;Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;)V",
//			at = @At("HEAD"),
//			cancellable = true
//	)
//	public void pswg$addTooltip(Consumer<Component> textConsumer, Player player, Holder<Attribute> attribute, AttributeModifier modifier, CallbackInfo ci)
//	{
//		// Check using the Holder directly
//		if (attribute.is(ModEntityAttributes.FIELD_OF_VIEW_ZOOM))
//		{
//			double amount = modifier.amount();
//
//			// Format: "x1.5 Zoom" or similar based on your translation key
//			textConsumer.accept(Component.translatable(
//					ModEntityAttributes.I18N_ATTR_MULTIPLIER,
//					ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(amount),
//					Component.translatable(attribute.value().getDescriptionId())
//			).withStyle(attribute.value().getStyle(true)));
//
//			ci.cancel();
//		}
//	}
//}