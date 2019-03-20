package gd.rf.ninjaphenix.cursedchests.block;

import gd.rf.ninjaphenix.cursedchests.block.entity.WoodVerticalChestBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.world.BlockView;

public class WoodVerticalChestBlock extends VerticalChestBlock
{
	WoodVerticalChestBlock(Settings block$Settings_1) { super(block$Settings_1, "wood_chest"); }
	@Override public BlockEntity createBlockEntity(BlockView blockView) { return new WoodVerticalChestBlockEntity(); }
}