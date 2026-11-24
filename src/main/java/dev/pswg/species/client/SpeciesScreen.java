package dev.pswg.species.client;

import dev.pswg.species.Species;
import dev.pswg.species.Species.Appearance;
import dev.pswg.species.SpeciesNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class SpeciesScreen extends Screen
{
	private static final int WIDTH = 306;
	private static final List<String> SPECIES = new ArrayList<>();
	static
	{
		SPECIES.add("none");
		SPECIES.addAll(Species.PROFILES.keySet());
	}
	private final int target;
	private Appearance selected;
	private int optionsPage;

	SpeciesScreen(int target, Appearance selected)
	{
		super(Component.translatable("screen.pswg.species"));
		this.target = target;
		this.selected = selected;
	}

	@Override
	protected void init()
	{
		int left = (width - WIDTH) / 2;
		int top = height / 2 - 100;
		addRenderableWidget(Button.builder(Component.literal("<"), button -> cycleSpecies(-1)).bounds(left, top, 20, 20).build());
		addRenderableWidget(Button.builder(selected.name(), button -> cycleSpecies(1)).bounds(left + 22, top, 114, 20).build());
		addRenderableWidget(Button.builder(Component.literal(">"), button -> cycleSpecies(1)).bounds(left + 138, top, 20, 20).build());
		if (selected.profile() != null)
		{
			addRenderableWidget(Button.builder(Component.translatable(selected.female() ? "screen.pswg.species.female" : "screen.pswg.species.male"), button -> {
				selected = new Appearance(selected.species(), !selected.female(), selected.options());
				rebuildWidgets();
			}).bounds(left + 166, top, 140, 20).build());
			var options = new ArrayList<>(selected.profile().options().entrySet());
			int start = optionsPage * 6;
			for (int i = start; i < Math.min(start + 6, options.size()); i++)
			{
				var entry = options.get(i);
				option(entry.getKey(), entry.getValue(), left + 158, top + 28 + (i - start) * 20);
			}
			var previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> page(-1)).bounds(left + 159, top + 150, 70, 20).build());
			var next = addRenderableWidget(Button.builder(Component.literal(">"), button -> page(1)).bounds(left + 235, top + 150, 70, 20).build());
			previous.active = optionsPage > 0;
			next.active = start + 6 < options.size();
		}
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> {
			if (minecraft != null && minecraft.getConnection() != null)
				PacketDistributor.sendToServer(new SpeciesNetwork.Select(target, selected));
			onClose();
		}).bounds(left, top + 176, 150, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose()).bounds(left + 156, top + 176, 150, 20).build());
	}

	private void option(String key, List<String> options, int x, int y)
	{
		String name = key.replace("humanoid_", "").replace("clothes_", "").replace('_', ' ');
		if (key.equals("humanoid_eye_color") || key.equals("humanoid_tattoo_color"))
		{
			addRenderableWidget(Button.builder(Component.literal(name), button -> {}).bounds(x, y, 72, 18).build()).active = false;
			var entry = new EditBox(font, x + 76, y, 71, 18, Component.literal(name));
			entry.setMaxLength(6);
			entry.setFilter(value -> value.matches("[0-9a-fA-F]{0,6}"));
			entry.setValue(selected.value(key));
			entry.setResponder(value -> {
				if (value.length() == 6)
					setOption(key, value.toLowerCase(Locale.ROOT));
			});
			addRenderableWidget(entry);
			return;
		}
		addRenderableWidget(Button.builder(Component.literal(name + " · " + selected.value(key).replace('_', ' ')), button -> {
			int index = options.indexOf(selected.value(key));
			setOption(key, options.get(Math.floorMod(index + (hasShiftDown() ? -1 : 1), options.size())));
			button.setMessage(Component.literal(name + " · " + selected.value(key).replace('_', ' ')));
		}).bounds(x, y, 148, 18).build());
	}

	private void setOption(String key, String value)
	{
		var options = new LinkedHashMap<>(selected.options());
		options.put(key, value);
		selected = Species.validate(new Appearance(selected.species(), selected.female(), Map.copyOf(options))).orElse(selected);
	}

	private void page(int direction)
	{
		optionsPage += direction;
		rebuildWidgets();
	}

	private void cycleSpecies(int direction)
	{
		int index = Math.floorMod(SPECIES.indexOf(selected.species()) + direction, SPECIES.size());
		selected = new Appearance(SPECIES.get(index), selected.female(), Map.of());
		optionsPage = 0;
		rebuildWidgets();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial)
	{
		renderBackground(graphics, mouseX, mouseY, partial);
		int left = (width - WIDTH) / 2;
		int top = height / 2 - 100;
		graphics.drawCenteredString(font, title, width / 2, top - 13, 0xffffff);
		if (minecraft != null && minecraft.player != null)
			SpeciesClient.preview(selected, () -> InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
					left + 1, top + 25, left + 151, top + 171, 56, 0.0625F, mouseX, mouseY, minecraft.player));
		super.render(graphics, mouseX, mouseY, partial);
	}

	@Override public boolean isPauseScreen() { return false; }
}
