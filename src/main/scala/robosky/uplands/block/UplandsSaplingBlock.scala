package robosky.uplands.block

import java.util.Random

import net.minecraft.block.{Block, BlockState, Fertilizable, PlantBlock}
import net.minecraft.entity.EntityContext
import net.minecraft.server.world.ServerWorld
import net.minecraft.state.StateManager
import net.minecraft.state.property.{IntProperty, Properties}
import net.minecraft.tag.BlockTags
import net.minecraft.util.math.BlockPos
import net.minecraft.util.shape.VoxelShape
import net.minecraft.world.{BlockView, IWorld, World}
import robosky.uplands.UplandsBlockTags
import robosky.uplands.world.feature.plants.UplandsSaplingGenerator

object UplandsSaplingBlock {
  val STAGE: IntProperty = Properties.STAGE
  val SHAPE: VoxelShape =
    Block.createCuboidShape(2.0D, 0.0D, 2.0D, 14.0D, 12.0D, 14.0D)
}

class UplandsSaplingBlock(val generator: UplandsSaplingGenerator, val settings: Block.Settings) extends PlantBlock(settings)
  with Fertilizable {
  this.setDefaultState(this.stateManager.getDefaultState.`with`[Integer, Integer](UplandsSaplingBlock.STAGE, 0))

  override def canPlantOnTop(blockState_1: BlockState, blockView_1: BlockView, blockPos_1: BlockPos): Boolean =
    blockState_1.matches(UplandsBlockTags.PlantableOn);

  override def getOutlineShape(blockState_1: BlockState, blockView_1: BlockView, blockPos_1: BlockPos,
    entityContext_1: EntityContext): VoxelShape = UplandsSaplingBlock.SHAPE

  override def scheduledTick(blockState_1: BlockState, world_1: ServerWorld, blockPos_1: BlockPos, random_1: Random): Unit = {
    if (world_1.getLightLevel(blockPos_1.up) >= 9 && random_1.nextInt(7) == 0)
      this.generate(world_1, blockPos_1, blockState_1, random_1)
  }

  override def isFertilizable(blockView_1: BlockView, blockPos_1: BlockPos, blockState_1: BlockState,
    boolean_1: Boolean) = true

  override def canGrow(world_1: World, random_1: Random, blockPos_1: BlockPos, blockState_1: BlockState): Boolean =
    world_1.random.nextFloat.toDouble < 0.45D

  override def grow(world_1: ServerWorld, random_1: Random, blockPos_1: BlockPos, blockState_1: BlockState): Unit = {
    this.generate(world_1, blockPos_1, blockState_1, random_1)
  }

  def generate(iWorld_1: IWorld, blockPos_1: BlockPos, blockState_1: BlockState, random_1: Random): Unit = {
    if (blockState_1.get(UplandsSaplingBlock.STAGE).intValue() == 0)
      iWorld_1.setBlockState(blockPos_1, blockState_1.cycle(UplandsSaplingBlock.STAGE), 4)
    else this.generator.generate(iWorld_1, blockPos_1, blockState_1, random_1)
  }

  override protected def appendProperties(builder: StateManager.Builder[Block, BlockState]): Unit =
    builder.add(UplandsSaplingBlock.STAGE)

}
