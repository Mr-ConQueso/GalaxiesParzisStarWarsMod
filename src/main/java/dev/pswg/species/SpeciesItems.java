package dev.pswg.species;

import dev.pswg.Galaxies;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class SpeciesItems
{
	private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Galaxies.MODID);
	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Galaxies.MODID);
	public static final Supplier<EntityType<Mannequin>> TYPE = ENTITIES.register("mannequin", () ->
			EntityType.Builder.of(Mannequin::new, MobCategory.MISC).sized(0.5F, 1.975F).clientTrackingRange(10).build("pswg:mannequin"));
	public static final Supplier<Item> WIZARD = ITEMS.registerSimpleItem("mannequin_wizard", new Item.Properties().stacksTo(1));
	public static final Supplier<Item> MANNEQUIN = ITEMS.register("mannequin", () -> new Item(new Item.Properties())
	{
		@Override
		public InteractionResult useOn(UseOnContext context)
		{
			if (context.getClickedFace() == Direction.DOWN)
				return InteractionResult.FAIL;
			var pos = new BlockPlaceContext(context).getClickedPos();
			var level = context.getLevel();
			var center = Vec3.atBottomCenterOf(pos);
			var box = TYPE.get().getDimensions().makeBoundingBox(center);
			if (!level.noCollision(box) || !level.getEntities(null, box).isEmpty())
				return InteractionResult.FAIL;
			if (level instanceof ServerLevel server)
			{
				var entity = TYPE.get().create(server, EntityType.createDefaultStackConfig(server, context.getItemInHand(), context.getPlayer()),
						pos, MobSpawnType.SPAWN_EGG, true, true);
				if (entity == null)
					return InteractionResult.FAIL;
				float yaw = Mth.floor((Mth.wrapDegrees(context.getRotation() - 180) + 22.5F) / 45) * 45;
				entity.moveTo(entity.getX(), entity.getY(), entity.getZ(), yaw, 0);
				server.addFreshEntityWithPassengers(entity);
				level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 0.75F, 0.8F);
				entity.gameEvent(GameEvent.ENTITY_PLACE, context.getPlayer());
				context.getItemInHand().consume(1, context.getPlayer());
			}
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
	});

	private SpeciesItems() {}

	public static void register(IEventBus bus)
	{
		ITEMS.register(bus);
		ENTITIES.register(bus);
		bus.addListener((EntityAttributeCreationEvent event) -> event.put(TYPE.get(), ArmorStand.createLivingAttributes().build()));
		bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
			if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS))
			{
				event.accept(MANNEQUIN.get());
				event.accept(WIZARD.get());
			}
		});
	}
}
