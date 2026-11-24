package dev.pswg.sabers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.pswg.Galaxies;
import dev.pswg.container.GalaxiesBlocks;
import dev.pswg.block.WideBlock;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static dev.pswg.sabers.SaberPreset.classic;

public final class Sabers
{
	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Galaxies.MODID);
	private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Galaxies.MODID);
	private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Galaxies.MODID);
	private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Galaxies.MODID);
	private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Galaxies.MODID);
	private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Galaxies.MODID);
	private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Galaxies.MODID);
	private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Galaxies.MODID);

	public static final Supplier<DataComponentType<BladeState>> BLADE = COMPONENTS.registerComponentType("lightsaber",
			builder -> builder.persistent(BladeState.CODEC).networkSynchronized(ByteBufCodecs.fromCodec(BladeState.CODEC)));
	public static final Map<String, DeferredItem<SaberItem>> ITEMS_BY_HILT = registerPresets();
	public static final Supplier<EntityType<ThrownSaber>> THROWN = ENTITIES.register("thrown_lightsaber",
			() -> EntityType.Builder.<ThrownSaber>of(ThrownSaber::new, MobCategory.MISC).sized(0.5F, 0.5F)
					.clientTrackingRange(6).updateInterval(2).build(Galaxies.id("thrown_lightsaber").toString()));
	public static final Supplier<BlockEntityType<SaberForge>> FORGE_ENTITY = BLOCK_ENTITIES.register("lightsaber_forge",
			() -> BlockEntityType.Builder.of(SaberForge::new, Sabers.FORGE.get()).build(null));
	public static final Supplier<WideBlock<SaberForge>> FORGE = BLOCKS.register("lightsaber_forge",
			() -> new WideBlock<>(BlockBehaviour.Properties.of().sound(SoundType.WOOD).strength(2.5F).noOcclusion(), FORGE_ENTITY));
	public static final Supplier<MenuType<SaberForge.Menu>> FORGE_MENU = MENUS.register("lightsaber_forge",
			() -> new MenuType<>(SaberForge.Menu::new, FeatureFlags.DEFAULT_FLAGS));
	private static final Supplier<BlockItem> FORGE_ITEM = ITEMS.registerSimpleBlockItem("lightsaber_forge", FORGE);

	public static final Supplier<SoundEvent> START = sound("lightsaber.start.classic");
	public static final Supplier<SoundEvent> STOP = sound("lightsaber.stop.classic");
	public static final Supplier<SoundEvent> IDLE = sound("lightsaber.idle.classic");

	static
	{
		TABS.register("lightsabers", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.pswg.lightsabers"))
				.icon(() -> ITEMS_BY_HILT.get("luke_rotj").toStack()).displayItems((parameters, output) ->
						ITEMS_BY_HILT.values().forEach(item -> output.accept(item.get()))).build());
	}

	private Sabers() {}

	public static void register(IEventBus bus)
	{
		ITEMS.register(bus);
		BLOCKS.register(bus);
		TABS.register(bus);
		SOUNDS.register(bus);
		ENTITIES.register(bus);
		BLOCK_ENTITIES.register(bus);
		MENUS.register(bus);
		COMPONENTS.register(bus);
		bus.addListener(SaberNetwork::register);
		bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
			if (event.getTabKey().equals(GalaxiesBlocks.CONSTRUCTION_TAB))
				event.accept(FORGE_ITEM.get());
		});
	}

	private static Supplier<SoundEvent> sound(String name)
	{
		return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Galaxies.id(name)));
	}

	private static Map<String, DeferredItem<SaberItem>> registerPresets()
	{
		var items = new LinkedHashMap<String, DeferredItem<SaberItem>>();
		for (var preset : List.of(classic("anakin", "Anakin Skywalker", 0.62F), classic("ezra_padawan", "Ezra Bridger", 0.33F),
				classic("kenobi", "Obi-Wan Kenobi", 0.62F), classic("luke_rotj", "Luke Skywalker", 0.33F),
				classic("qui_gon", "Qui-Gon Jinn", 0.33F), blade("bob", "Jedi Bob", 0.33F, 1, SaberPreset.Blade.BRICK, false, 1),
				classic("kestis", "Cal Kestis", 0.73F), classic("maul_half", "Darth Maul", 0F), classic("maul", "Darth Maul", 0F),
				blade("darksaber", "", 0, 0, SaberPreset.Blade.DARKSABER, false, 1),
				classic("vader", "Darth Vader", 0F), classic("dooku", "Count Dooku", 0F), classic("revan", "Darth Revan", 0.81F),
				blade("kylo", "Kylo Ren", 0, 1, SaberPreset.Blade.DEFAULT, true, 1),
				classic("dume", "Kanan Jarrus", 0.62F), classic("mace", "Mace Windu", 0.81F), classic("shaagi", "Sha'a Gi", 0.33F),
				classic("starkiller", "Galen Marek", 0.62F), blade("yoda", "Yoda", 0.33F, 1, SaberPreset.Blade.DEFAULT, false, 0.5F),
				classic("ahsoka_cw", "Ahsoka", 0.33F), blade("ahsoka_cw_shoto", "Shoto Ahsoka", 0.23F, 1, SaberPreset.Blade.DEFAULT, false, 0.5F),
				blade("ahsoka_rebel", "Ahsoka", 0, 0, SaberPreset.Blade.DEFAULT, false, 1),
				blade("ahsoka_rebel_shoto", "Shoto Ahsoka", 0, 0, SaberPreset.Blade.DEFAULT, false, 0.75F)))
		{
			var state = new BladeState(preset.color(), preset.unstable(), false, -8);
			items.put(preset.id(), ITEMS.register("lightsaber_" + preset.id(),
					() -> new SaberItem(preset, new Item.Properties().stacksTo(1).component(BLADE.get(), state))));
		}
		return java.util.Collections.unmodifiableMap(items);
	}

	private static SaberPreset blade(String id, String owner, float hue, float sat, SaberPreset.Blade type, boolean unstable, float length)
	{
		float value = id.startsWith("ahsoka_rebel") ? 0.66F : 1;
		return new SaberPreset(id, owner, Mth.hsvToRgb(hue, sat, value), type, unstable, length);
	}

	public record BladeState(int color, boolean unstable, boolean active, long changed)
	{
		public static final Codec<BladeState> CODEC = RecordCodecBuilder.create(builder -> builder.group(
				Codec.intRange(0, 0xFFFFFF).fieldOf("color").forGetter(BladeState::color),
				Codec.BOOL.fieldOf("unstable").forGetter(BladeState::unstable),
				Codec.BOOL.fieldOf("active").forGetter(BladeState::active),
				Codec.LONG.fieldOf("changed").forGetter(BladeState::changed)
		).apply(builder, BladeState::new));

		public float extension(long now, float partial)
		{
			float t = Mth.clamp((now - changed + partial) / 8F, 0, 1);
			return active ? 1 - (1 - t) * (1 - t) * (1 - t) : (1 - t) * (1 - t) * (1 - t);
		}
	}
}
