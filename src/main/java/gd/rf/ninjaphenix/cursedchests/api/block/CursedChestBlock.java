package gd.rf.ninjaphenix.cursedchests.api.block;

import gd.rf.ninjaphenix.cursedchests.api.block.entity.CursedChestBlockEntity;
import gd.rf.ninjaphenix.cursedchests.api.inventory.DoubleSidedInventory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.container.ContainerProviderRegistry;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.container.Container;
import net.minecraft.entity.EntityContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateFactory;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BoundingBox;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import java.util.List;

@SuppressWarnings("deprecation")
public class CursedChestBlock extends BlockWithEntity implements Waterloggable, InventoryProvider
{
	@Override public BlockEntity createBlockEntity(BlockView var1)
	{
		Identifier id = Registry.BLOCK.getId(this);
		CursedChestBlockEntity blockEntity = new CursedChestBlockEntity(id);
		return blockEntity;
	}

	interface PropertyRetriever<T>
	{
		T getFromDoubleChest(CursedChestBlockEntity var1, CursedChestBlockEntity var2);

		T getFromSingleChest(CursedChestBlockEntity var1);
	}

	private static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
	public static final DirectionProperty FACING = Properties.FACING_HORIZONTAL;
	public static final EnumProperty<VerticalChestType> TYPE = EnumProperty.create("type", VerticalChestType.class);
	private static final VoxelShape SINGLE_SHAPE = Block.createCuboidShape(1, 0, 1, 15, 14, 15);
	private static final VoxelShape TOP_SHAPE = Block.createCuboidShape(1, -16, 1, 15, 14, 15);
	private static final VoxelShape BOTTOM_SHAPE = Block.createCuboidShape(1, 0, 1, 15, 30, 15);
	protected static String DOUBLE_PREFIX = "container.cursedchests.generic_double";

	private static final PropertyRetriever<SidedInventory> INVENTORY_RETRIEVER = new PropertyRetriever<SidedInventory>()
	{
		@Override public SidedInventory getFromDoubleChest(CursedChestBlockEntity mainBlockEntity, CursedChestBlockEntity secondaryBlockEntity){ return new DoubleSidedInventory(mainBlockEntity, secondaryBlockEntity); }

		@Override public SidedInventory getFromSingleChest(CursedChestBlockEntity mainBlockEntity){ return mainBlockEntity; }
	};

	private static final PropertyRetriever<Component> NAME_RETRIEVER = new PropertyRetriever<Component>()
	{
		@Override public Component getFromDoubleChest(CursedChestBlockEntity mainBlockEntity, CursedChestBlockEntity secondaryBlockEntity)
		{
			if (mainBlockEntity.hasCustomName()) return mainBlockEntity.getDisplayName();
			if (secondaryBlockEntity.hasCustomName()) return secondaryBlockEntity.getDisplayName();
			return new TranslatableComponent(DOUBLE_PREFIX, mainBlockEntity.getDisplayName());
		}

		@Override public Component getFromSingleChest(CursedChestBlockEntity mainBlockEntity){ return mainBlockEntity.getDisplayName(); }
	};

	public CursedChestBlock(Settings settings)
	{
		super(settings);
		setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(WATERLOGGED, false).with(TYPE, VerticalChestType.SINGLE));
	}

	@Environment(EnvType.CLIENT) @Override public boolean hasBlockEntityBreakingRender(BlockState state){ return true; }

	@Override public BlockRenderType getRenderType(BlockState state){ return BlockRenderType.ENTITYBLOCK_ANIMATED; }

	@Override public FluidState getFluidState(BlockState state){ return state.get(WATERLOGGED) ? Fluids.WATER.getDefaultState() : super.getFluidState(state); }

	@Override protected void appendProperties(StateFactory.Builder<Block, BlockState> stateBuilder){ stateBuilder.add(FACING, TYPE, WATERLOGGED); }

	@Override public boolean hasComparatorOutput(BlockState state){ return true; }

	@Override public int getComparatorOutput(BlockState state, World world, BlockPos pos){ return Container.calculateComparatorOutput(getInventory(state, world, pos)); }

	@Override public BlockState rotate(BlockState state, BlockRotation rotation){ return state.with(FACING, rotation.rotate(state.get(FACING))); }

	@Override public BlockState mirror(BlockState state, BlockMirror mirror){ return state.rotate(mirror.getRotation(state.get(FACING))); }

	@Override public SidedInventory getInventory(BlockState state, IWorld world, BlockPos pos){ return retrieve(state, world, pos, INVENTORY_RETRIEVER); }

	private static boolean isChestBlocked(IWorld world, BlockPos pos){ return hasBlockOnTop(world, pos) || hasOcelotOnTop(world, pos); }

	private Stat<Identifier> getOpenStat(){ return Stats.CUSTOM.getOrCreateStat(Stats.OPEN_CHEST); }

	public static SidedInventory getInventoryStatic(BlockState state, IWorld world, BlockPos pos){ return retrieve(state, world, pos, INVENTORY_RETRIEVER); }

	@Override public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, EntityContext verticalEntityPosition)
	{
		switch (state.get(TYPE))
		{
			case TOP: return TOP_SHAPE;
			case BOTTOM: return BOTTOM_SHAPE;
			default: return SINGLE_SHAPE;
		}
	}

	@Override public BlockState getPlacementState(ItemPlacementContext context)
	{
		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		VerticalChestType chestType = VerticalChestType.SINGLE;
		Direction direction_1 = context.getPlayerHorizontalFacing().getOpposite();
		Direction direction_2 = context.getFacing();
		boolean sneaking = context.isPlayerSneaking();
		if (direction_2.getAxis().isVertical() && sneaking)
		{
			BlockState state = world.getBlockState(pos.offset(direction_2.getOpposite()));
			Direction direction_3 = state.getBlock() == this && state.get(TYPE) == VerticalChestType.SINGLE ? state.get(FACING) : null;
			if (direction_3 != null && direction_3.getAxis() != direction_2.getAxis() && direction_3 == direction_1) chestType = direction_2 == Direction.UP ? VerticalChestType.TOP : VerticalChestType.BOTTOM;
		}
		else if (!sneaking)
		{
			BlockState aboveBlockState = world.getBlockState(pos.offset(Direction.UP));
			if (aboveBlockState.getBlock() == this && aboveBlockState.get(TYPE) == VerticalChestType.SINGLE && aboveBlockState.get(FACING) == direction_1) chestType = VerticalChestType.BOTTOM;
			else
			{
				BlockState belowBlockState = world.getBlockState(pos.offset(Direction.DOWN));
				if (belowBlockState.getBlock() == this && belowBlockState.get(TYPE) == VerticalChestType.SINGLE && belowBlockState.get(FACING) == direction_1) chestType = VerticalChestType.TOP;
			}
		}
		return getDefaultState().with(FACING, direction_1).with(TYPE, chestType).with(WATERLOGGED, world.getFluidState(pos).getFluid() == Fluids.WATER);
	}

	@Override public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState otherState, IWorld world, BlockPos pos, BlockPos otherPos)
	{
		if (state.get(WATERLOGGED)) world.getFluidTickScheduler().schedule(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
		if (state.get(TYPE) == VerticalChestType.TOP && world.getBlockState(pos.offset(Direction.DOWN)).getBlock() != this) return state.with(TYPE, VerticalChestType.SINGLE);
		if (state.get(TYPE) == VerticalChestType.BOTTOM && world.getBlockState(pos.offset(Direction.UP)).getBlock() != this) return state.with(TYPE, VerticalChestType.SINGLE);
		if (state.get(TYPE) == VerticalChestType.SINGLE && direction.getAxis().isVertical())
		{
			BlockState realOtherState = world.getBlockState(pos.offset(direction));
			if (!realOtherState.contains(TYPE)) return state.with(TYPE, VerticalChestType.SINGLE);
			if (direction == Direction.UP && realOtherState.get(TYPE) == VerticalChestType.TOP) return state.with(TYPE, VerticalChestType.BOTTOM);
			if (direction == Direction.DOWN && realOtherState.get(TYPE) == VerticalChestType.BOTTOM) return state.with(TYPE, VerticalChestType.TOP);
			return state.with(TYPE, VerticalChestType.SINGLE);
		}
		return super.getStateForNeighborUpdate(state, direction, otherState, world, pos, otherPos);
	}

	@Override public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
	{
		if (stack.hasDisplayName())
		{
			BlockEntity blockEntity = world.getBlockEntity(pos);
			if (blockEntity instanceof CursedChestBlockEntity) ((CursedChestBlockEntity) blockEntity).setCustomName(stack.getDisplayName());
		}
	}

	@Override public void onBlockRemoved(BlockState state_1, World world, BlockPos pos, BlockState state_2, boolean boolean_1)
	{
		if (state_1.getBlock() != state_2.getBlock())
		{
			BlockEntity blockEntity = world.getBlockEntity(pos);
			if (blockEntity instanceof Inventory)
			{
				ItemScatterer.spawn(world, pos, (Inventory) blockEntity);
				world.updateHorizontalAdjacent(pos, this);
			}
			super.onBlockRemoved(state_1, world, pos, state_2, boolean_1);
		}
	}

	@Override public boolean activate(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hitResult)
	{
		if (world.isClient) return true;
		if (player.isSneaking() && hitResult.getSide().getAxis().isVertical() && state.get(TYPE) == VerticalChestType.SINGLE)
		{
			Direction direction = hitResult.getSide();
			BlockState offsetState = world.getBlockState(pos.offset(direction));
			if (offsetState.getBlock() == this && offsetState.get(TYPE) == VerticalChestType.SINGLE && state.get(FACING) == offsetState.get(FACING))
			{
				world.setBlockState(pos, state.with(TYPE, direction == Direction.UP ? VerticalChestType.BOTTOM : VerticalChestType.TOP));
				return true;
			}
		}
		openContainer(state, world, pos, player, hand, hitResult);
		player.incrementStat(getOpenStat());
		return true;
	}

	/*
		This method must be overridden if you are not using cursed chests mod with this api.
	*/
	protected void openContainer(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hitResult)
	{
		Component containerName = retrieve(state, world, pos, NAME_RETRIEVER);
		if (containerName == null) return;
		ContainerProviderRegistry.INSTANCE.openContainer(new Identifier("cursedchests", "scrollcontainer"), player, (packetByteBuf ->
		{
			packetByteBuf.writeBlockPos(pos);
			packetByteBuf.writeTextComponent(containerName);
		}));
	}

	private static <T> T retrieve(BlockState clickedState, IWorld world, BlockPos clickedPos, PropertyRetriever<T> propertyRetriever)
	{
		BlockEntity clickedBlockEntity = world.getBlockEntity(clickedPos);
		if (!(clickedBlockEntity instanceof CursedChestBlockEntity) || isChestBlocked(world, clickedPos)) return null;
		CursedChestBlockEntity clickedChestBlockEntity = (CursedChestBlockEntity) clickedBlockEntity;
		VerticalChestType clickedChestType = clickedState.get(TYPE);
		if (clickedChestType == VerticalChestType.SINGLE) return propertyRetriever.getFromSingleChest(clickedChestBlockEntity);
		BlockPos pairedPos;
		if (clickedChestType == VerticalChestType.TOP) pairedPos = clickedPos.offset(Direction.DOWN);
		else pairedPos = clickedPos.offset(Direction.UP);
		BlockState pairedState = world.getBlockState(pairedPos);
		if (pairedState.getBlock() == clickedState.getBlock())
		{
			VerticalChestType pairedChestType = pairedState.get(TYPE);
			if (pairedChestType != VerticalChestType.SINGLE && clickedChestType != pairedChestType && pairedState.get(FACING) == clickedState.get(FACING))
			{
				if (isChestBlocked(world, pairedPos)) return null;
				BlockEntity pairedBlockEntity = world.getBlockEntity(pairedPos);
				if (pairedBlockEntity instanceof CursedChestBlockEntity)
				{
					CursedChestBlockEntity mainChestBlockEntity = clickedChestType == VerticalChestType.TOP ? (CursedChestBlockEntity) pairedBlockEntity :  clickedChestBlockEntity;
					CursedChestBlockEntity secondaryChestBlockEntity = clickedChestType == VerticalChestType.TOP ? clickedChestBlockEntity : (CursedChestBlockEntity) pairedBlockEntity;
					return propertyRetriever.getFromDoubleChest(mainChestBlockEntity, secondaryChestBlockEntity);
				}
			}
		}
		return propertyRetriever.getFromSingleChest(clickedChestBlockEntity);
	}

	private static boolean hasBlockOnTop(BlockView view, BlockPos pos)
	{
		BlockPos blockPos_2 = pos.up();
		return view.getBlockState(blockPos_2).isSimpleFullBlock(view, blockPos_2);
	}

	private static boolean hasOcelotOnTop(IWorld world, BlockPos pos)
	{
		List<CatEntity> cats = world.getEntities(CatEntity.class, new BoundingBox(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1));
		for (CatEntity catEntity_1 : cats) if (catEntity_1.isSitting()) return true;
		return false;
	}
}
