package dev.pswg.sabers;

import dev.pswg.gadgets.WorkstationMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class SaberForge extends BlockEntity implements MenuProvider
{
	public static final int EDIT_FLAG = 0x2000000;
	public static final int UNSTABLE_FLAG = 0x1000000;
	public static final int COLOR_MASK = 0xFFFFFF;

	public SaberForge(BlockPos pos, BlockState state)
	{
		super(Sabers.FORGE_ENTITY.get(), pos, state);
	}

	@Override
	public Component getDisplayName()
	{
		return Component.translatable("container.pswg.lightsaber_forge");
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player)
	{
		return new Menu(id, inventory, ContainerLevelAccess.create(player.level(), worldPosition));
	}

	public static final class Menu extends WorkstationMenu
	{
		private final ContainerLevelAccess access;

		public Menu(int id, Inventory player)
		{
			this(id, player, ContainerLevelAccess.NULL);
		}

		private Menu(int id, Inventory player, ContainerLevelAccess access)
		{
			super(Sabers.FORGE_MENU.get(), id, new SimpleContainer(1));
			this.access = access;
			addMachineSlot(0, 14, 63, 1, stack -> stack.getItem() instanceof SaberItem);
			addPlayerSlots(player, 48, 159);
		}

		public ItemStack saber()
		{
			return inventory.getItem(0);
		}

		@Override
		public boolean stillValid(Player player)
		{
			return stillValid(access, player, Sabers.FORGE.get());
		}

		@Override
		public boolean clickMenuButton(Player player, int id)
		{
			var stack = saber();
			if (player.level().isClientSide || player.containerMenu != this || player.isSpectator() || !stillValid(player)
					|| !(stack.getItem() instanceof SaberItem) || (id & ~ (COLOR_MASK | UNSTABLE_FLAG)) != EDIT_FLAG)
				return false;
			var blade = SaberItem.state(stack);
			stack.set(Sabers.BLADE.get(), new Sabers.BladeState(id & COLOR_MASK, (id & UNSTABLE_FLAG) != 0, blade.active(), blade.changed()));
			inventory.setChanged();
			broadcastChanges();
			return true;
		}

		@Override
		public void removed(Player player)
		{
			super.removed(player);
			access.execute((level, pos) -> clearContainer(player, inventory));
		}
	}
}
