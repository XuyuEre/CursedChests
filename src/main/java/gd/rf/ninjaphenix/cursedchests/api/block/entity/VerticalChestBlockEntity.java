package gd.rf.ninjaphenix.cursedchests.api.block.entity;

import gd.rf.ninjaphenix.cursedchests.api.block.VerticalChestBlock;
import gd.rf.ninjaphenix.cursedchests.api.container.ScrollableContainer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvironmentInterface;
import net.fabricmc.api.EnvironmentInterfaces;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.client.block.ChestAnimationProgress;
import net.minecraft.container.Container;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.DoubleInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.DefaultedList;
import net.minecraft.util.Identifier;
import net.minecraft.util.Tickable;
import net.minecraft.util.math.BoundingBox;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.Iterator;
import java.util.List;

@EnvironmentInterfaces({@EnvironmentInterface(value = EnvType.CLIENT, itf = ChestAnimationProgress.class)})
public abstract class VerticalChestBlockEntity extends LootableContainerBlockEntity implements ChestAnimationProgress, Tickable
{
	private DefaultedList<ItemStack> inventory;
	private float animationAngle;
	private float lastAnimationAngle;
	private int viewerCount;
	private int ticksOpen;

	public VerticalChestBlockEntity(BlockEntityType type)
	{
		super(type);
		inventory = DefaultedList.create(getInvSize(), ItemStack.EMPTY);
	}

	@Override public boolean onBlockAction(int actionId, int value){ if (actionId == 1){ viewerCount = value; return true; } else { return super.onBlockAction(actionId, value); } }
	@Override protected DefaultedList<ItemStack> getInvStackList(){ return inventory; }
	@Override public void setInvStackList(DefaultedList<ItemStack> defaultedList_1){ inventory = defaultedList_1; }
	@Environment(EnvType.CLIENT) @Override public float getAnimationProgress(float float_1){ return MathHelper.lerp(float_1, lastAnimationAngle, animationAngle); }
	@Override protected Container createContainer(int int_1, PlayerInventory playerInventory_1){ return null; }
	public abstract Identifier getTexture(boolean isDouble);

	@Override public boolean isInvEmpty()
	{
		Iterator<ItemStack> inventoryIterator = inventory.iterator();
		ItemStack stack;
		do
		{
			if (!inventoryIterator.hasNext()) return true;
			stack = inventoryIterator.next();
		}
		while (stack.isEmpty());
		return false;
	}

	@Override public void fromTag(CompoundTag tag)
	{
		super.fromTag(tag);
		inventory = DefaultedList.create(getInvSize(), ItemStack.EMPTY);
		if (!deserializeLootTable(tag)) Inventories.fromTag(tag, inventory);
	}

	@Override public CompoundTag toTag(CompoundTag tag)
	{
		super.toTag(tag);
		if (!serializeLootTable(tag)) Inventories.toTag(tag, inventory);
		return tag;
	}

	@Override public void tick()
	{
		++ticksOpen;
		viewerCount = recalculateViewerCountIfNecessary(world, this, ticksOpen, pos.getX(), pos.getY(), pos.getZ(), viewerCount);
		lastAnimationAngle = animationAngle;
		if (viewerCount > 0 && animationAngle == 0.0F) playSound(SoundEvents.BLOCK_CHEST_OPEN);
		if (viewerCount == 0 && animationAngle > 0.0F || viewerCount > 0 && animationAngle < 1.0F)
		{
			float float_2 = animationAngle;
			if (viewerCount > 0) animationAngle += 0.1F; else animationAngle -= 0.1F;
			if (animationAngle > 1.0F) animationAngle = 1.0F;
			else if (animationAngle < 0.5F && float_2 >= 0.5F) playSound(SoundEvents.BLOCK_CHEST_CLOSE);
			else if (animationAngle < 0.0F) animationAngle = 0.0F;
		}
	}

	private static int recalculateViewerCountIfNecessary(World world, LockableContainerBlockEntity instance, int ticksOpen, int x, int y, int z, int viewerCount)
	{
		if (!world.isClient && viewerCount != 0 && (ticksOpen + x + y + z) % 200 == 0)
		{
			viewerCount = 0;
			List<PlayerEntity> playersInRange = world.method_18467(PlayerEntity.class, new BoundingBox(x - 5, y - 5, z - 5, x + 6, y + 6, z + 6));
			Iterator<PlayerEntity> playerIterator = playersInRange.iterator();
			while (true)
			{
				Inventory inventory;
				do
				{
					PlayerEntity player;
					do
					{
						if (!playerIterator.hasNext()) return viewerCount;
						player = playerIterator.next();
					}
					while (!(player.container instanceof ScrollableContainer));
					inventory = ((ScrollableContainer) player.container).getInventory();
				}
				while (inventory != instance && (!(inventory instanceof DoubleInventory) || !((DoubleInventory) inventory).isPart(instance)));
				++viewerCount;
			}
		}
		else return viewerCount;
	}

	private void playSound(SoundEvent soundEvent)
	{
		double z = pos.getZ();
		VerticalChestBlock.VerticalChestType chestType = getCachedState().get(VerticalChestBlock.TYPE);
		if (chestType == VerticalChestBlock.VerticalChestType.SINGLE) z += 0.5D;
		else if (chestType == VerticalChestBlock.VerticalChestType.BOTTOM) z += 1.0D;
		world.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, z, soundEvent, SoundCategory.BLOCK, 0.5F, world.random.nextFloat() * 0.1F + 0.9F);
	}

	@Override public void onInvOpen(PlayerEntity player)
	{
		if (player.isSpectator()) return;
		if (viewerCount < 0) viewerCount = 0;
		++viewerCount;
		onInvOpenOrClose();
	}

	@Override public void onInvClose(PlayerEntity player)
	{
		if (player.isSpectator()) return;
		--viewerCount;
		onInvOpenOrClose();
	}

	private void onInvOpenOrClose()
	{
		Block block = getCachedState().getBlock();
		if (block instanceof VerticalChestBlock)
		{
			world.addBlockAction(pos, block, 1, viewerCount);
			world.updateNeighborsAlways(pos, block);
		}
	}
}