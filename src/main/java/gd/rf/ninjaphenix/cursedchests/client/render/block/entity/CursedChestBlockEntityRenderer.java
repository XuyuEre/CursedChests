package gd.rf.ninjaphenix.cursedchests.client.render.block.entity;

import com.mojang.blaze3d.platform.GlStateManager;
import gd.rf.ninjaphenix.cursedchests.api.CursedChestRegistry;
import gd.rf.ninjaphenix.cursedchests.api.block.CursedChestBlock;
import gd.rf.ninjaphenix.cursedchests.api.block.CursedChestType;
import gd.rf.ninjaphenix.cursedchests.api.block.entity.CursedChestBlockEntity;
import gd.rf.ninjaphenix.cursedchests.block.ModBlocks;
import gd.rf.ninjaphenix.cursedchests.client.render.entity.model.LongChestEntityModel;
import gd.rf.ninjaphenix.cursedchests.client.render.entity.model.TallChestEntityModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.entity.model.ChestDoubleEntityModel;
import net.minecraft.client.render.entity.model.ChestEntityModel;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;

@Environment(EnvType.CLIENT)
public class CursedChestBlockEntityRenderer extends BlockEntityRenderer<CursedChestBlockEntity>
{
	private final ChestEntityModel modelSingleChest = new ChestEntityModel();
	private final ChestEntityModel modelTallChest = new TallChestEntityModel();
	private final ChestEntityModel modelVanillaChest = new ChestDoubleEntityModel();
	private final ChestEntityModel modelLongChest = new LongChestEntityModel();

	@Override public void render(CursedChestBlockEntity blockEntity, double x, double y, double z, float lidPitch, int breaking_stage)
	{

		BlockState state = blockEntity.hasWorld() ? blockEntity.getCachedState() : ModBlocks.wood_chest.getDefaultState().with(CursedChestBlock.FACING, Direction.SOUTH).with(CursedChestBlock.TYPE, CursedChestType.SINGLE);
		CursedChestType chestType = state.get(CursedChestBlock.TYPE);
		//if (chestType == CursedChestType.TOP && breaking_stage < 0) return;
		Identifier b = blockEntity.getBlock();
		if(b == null) b = Registry.BLOCK.getId(ModBlocks.wood_chest);
		ChestEntityModel chestModel = getChestModelAndBindTexture(b, breaking_stage, chestType);
		if(chestModel == null) return;
		GlStateManager.enableDepthTest();
		GlStateManager.depthFunc(515);
		GlStateManager.depthMask(true);
		if (breaking_stage >= 0)
		{
			GlStateManager.matrixMode(5890);
			GlStateManager.pushMatrix();
			// change this for chest types x, y, z
			GlStateManager.scalef(4, 4, 1);
			GlStateManager.translatef(0.0625F, 0.0625F, 0.0625F);
			GlStateManager.matrixMode(5888);
		}
		else GlStateManager.color4f(1, 1, 1, 1);
		GlStateManager.pushMatrix();
		GlStateManager.enableRescaleNormal();
		GlStateManager.translated(x, y + 1, z + 1);
		GlStateManager.scalef(1, -1, -1);
		float chestYaw = state.get(ChestBlock.FACING).asRotation();
		if (Math.abs(chestYaw) > 1.0E-5D)
		{
			GlStateManager.translated(0.5, 0.5, 0.5);
			GlStateManager.rotatef(chestYaw, 0, 1, 0);
			GlStateManager.translated(-0.5, -0.5, -0.5);
		}
		if (chestType == CursedChestType.TOP) GlStateManager.translatef(0, 1, 0);
		setLidPitch(blockEntity, lidPitch, chestModel);
		chestModel.method_2799();
		GlStateManager.disableRescaleNormal();
		GlStateManager.popMatrix();
		GlStateManager.color4f(1, 1, 1, 1);
		if (breaking_stage >= 0)
		{
			GlStateManager.matrixMode(5890);
			GlStateManager.popMatrix();
			GlStateManager.matrixMode(5888);
		}
	}

	private ChestEntityModel getChestModelAndBindTexture(Identifier block, int breaking_stage, CursedChestType chestType)
	{
		Identifier identifier_5;
		if (breaking_stage >= 0) identifier_5 = DESTROY_STAGE_TEXTURES[breaking_stage];
		else identifier_5 = CursedChestRegistry.getChestTexture(block, chestType);
		bindTexture(identifier_5);
		switch(chestType)
		{
			case BOTTOM:
				return modelTallChest;
			case FRONT:
				return modelLongChest;
			case LEFT:
				return modelVanillaChest;
			case SINGLE:
				return modelSingleChest;
			default:
				return null;
		}
	}

	private void setLidPitch(CursedChestBlockEntity blockEntity, float lidPitch, ChestEntityModel model)
	{
		float newPitch = 1.0F - blockEntity.getAnimationProgress(lidPitch);
		model.method_2798().pitch = -((1.0F - newPitch * newPitch * newPitch) * 1.5707964F);
	}
}