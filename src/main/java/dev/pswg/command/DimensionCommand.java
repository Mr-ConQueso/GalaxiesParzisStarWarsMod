package dev.pswg.command;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class DimensionCommand
{
	public static void register(RegisterCommandsEvent event)
	{
		event.getDispatcher().register(Commands.literal("cdim")
				.requires(source -> source.hasPermission(2) && source.getEntity() != null)
				.then(Commands.argument("dimension", DimensionArgument.dimension())
						.executes(context -> teleport(
								context.getSource().getEntityOrException(),
								DimensionArgument.getDimension(context, "dimension")))));
	}

	private static int teleport(Entity entity, ServerLevel level)
	{
		int y = level.getChunk(0, 0).getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0) + 1;
		var target = new Vec3(0, y, 0);
		boolean preserveVerticalMotion = entity instanceof LivingEntity living && living.isFallFlying();
		Vec3 velocity = preserveVerticalMotion ? entity.getDeltaMovement() : entity.getDeltaMovement().multiply(1, 0, 1);
		var transition = new DimensionTransition(level, target, velocity, entity.getYRot(), entity.getXRot(),
				DimensionTransition.PLACE_PORTAL_TICKET);

		Entity moved = entity.changeDimension(transition);
		if (moved == null)
			return 0;

		if (!preserveVerticalMotion)
			moved.setOnGround(true);
		if (moved instanceof PathfinderMob mob)
			mob.getNavigation().stop();
		return 1;
	}

	private DimensionCommand()
	{
	}
}
