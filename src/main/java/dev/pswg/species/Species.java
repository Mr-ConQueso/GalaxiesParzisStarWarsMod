package dev.pswg.species;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.pswg.Galaxies;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public final class Species
{
	private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Galaxies.MODID);
	public static final Appearance DEFAULT = new Appearance("none", false, Map.of());
	private static final Supplier<AttachmentType<Appearance>> PLAYER_APPEARANCE = ATTACHMENTS.register("species",
			() -> AttachmentType.builder(() -> DEFAULT).serialize(Appearance.CODEC).copyOnDeath().build());
	public static final Map<String, Profile> PROFILES = loadProfiles();

	private Species() {}

	private static Map<String, Profile> loadProfiles()
	{
		var stream = Objects.requireNonNull(Species.class.getResourceAsStream("/data/pswg/species/legacy.json"));
		try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8))
		{
			Map<String, Profile> profiles = new Gson().fromJson(reader, new TypeToken<Map<String, Profile>>() {}.getType());
			return Collections.unmodifiableMap(profiles);
		}
		catch (java.io.IOException e)
		{
			throw new java.io.UncheckedIOException(e);
		}
	}

	public static void register(IEventBus bus)
	{
		ATTACHMENTS.register(bus);
		bus.addListener(SpeciesNetwork::register);
		var events = NeoForge.EVENT_BUS;
		events.addListener(Species::commands);
		events.addListener(Species::joined);
		events.addListener(Species::respawned);
		events.addListener(Species::changedDimension);
		events.addListener(Species::tracked);
	}

	private static void commands(RegisterCommandsEvent event)
	{
		event.getDispatcher().register(Commands.literal("species")
				.executes(context -> {
					SpeciesNetwork.open(context.getSource().getPlayerOrException(), context.getSource().getPlayerOrException().getId(),
							appearance(context.getSource().getPlayerOrException()));
					return 1;
				})
				.then(Commands.argument("species", StringArgumentType.word())
						.suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
								java.util.stream.Stream.concat(java.util.stream.Stream.of("none"), PROFILES.keySet().stream()), builder))
						.executes(context -> {
							String name = StringArgumentType.getString(context, "species");
							var selected = validate(new Appearance(name, false, Map.of()));
							if (selected.isEmpty())
							{
								context.getSource().sendFailure(Component.translatable("message.pswg.species.unknown", name));
								return 0;
							}
							set(context.getSource().getPlayerOrException(), selected.get());
							return 1;
						})));
	}

	public static Optional<Appearance> validate(Appearance input)
	{
		if (input.species().equals("none"))
			return Optional.of(DEFAULT);
		var profile = PROFILES.get(input.species());
		if (profile == null || input.options().size() > profile.options().size())
			return Optional.empty();
		var normalized = new LinkedHashMap<String, String>();
		for (var option : input.options().entrySet())
		{
			var values = profile.options().get(option.getKey());
			if (values == null || !values.contains(option.getValue()) && !validColor(option.getKey(), option.getValue()))
				return Optional.empty();
			if (!values.getFirst().equals(option.getValue()))
				normalized.put(option.getKey(), option.getValue());
		}
		return Optional.of(new Appearance(input.species(), input.female(), Map.copyOf(normalized)));
	}

	private static boolean validColor(String key, String value)
	{
		return (key.equals("humanoid_eye_color") || key.equals("humanoid_tattoo_color")) && value.matches("[0-9a-f]{6}");
	}

	public static Appearance appearance(Player player)
	{
		return player.getData(PLAYER_APPEARANCE);
	}

	public static void set(Player player, Appearance appearance)
	{
		player.setData(PLAYER_APPEARANCE, appearance);
		if (player instanceof ServerPlayer server && server.connection != null)
			SpeciesNetwork.broadcast(server);
	}

	private static void joined(PlayerEvent.PlayerLoggedInEvent event)
	{
		if (event.getEntity() instanceof ServerPlayer player)
			SpeciesNetwork.broadcast(player);
	}

	private static void respawned(PlayerEvent.PlayerRespawnEvent event)
	{
		if (event.getEntity() instanceof ServerPlayer player)
			SpeciesNetwork.broadcast(player);
	}

	private static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event)
	{
		if (event.getEntity() instanceof ServerPlayer player)
			SpeciesNetwork.broadcast(player);
	}

	private static void tracked(PlayerEvent.StartTracking event)
	{
		if (event.getEntity() instanceof ServerPlayer tracker && event.getTarget() instanceof ServerPlayer player)
			SpeciesNetwork.send(tracker, player);
	}

	public record Profile(String model, Map<String, List<String>> options, List<Layer> layers) {}

	public record Layer(String path, String option, String tint) {}

	public record Appearance(String species, boolean female, Map<String, String> options)
	{
		public static final Codec<Appearance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("species").forGetter(Appearance::species),
				Codec.BOOL.optionalFieldOf("female", false).forGetter(Appearance::female),
				Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("options", Map.of()).forGetter(Appearance::options)
		).apply(instance, Appearance::new));

		public Profile profile() { return PROFILES.get(species); }
		public String gender() { return female ? "f" : "m"; }
		public String value(String option) { return options.getOrDefault(option, profile().options().get(option).getFirst()); }
		public Component name() { return Component.translatable("species.pswg." + species); }
	}
}
