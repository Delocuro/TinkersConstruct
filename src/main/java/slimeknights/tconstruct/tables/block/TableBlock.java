package slimeknights.tconstruct.tables.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.fluid.IFluidState;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import slimeknights.mantle.block.InventoryBlock;
import slimeknights.mantle.tileentity.InventoryTileEntity;
import slimeknights.tconstruct.library.TinkerNBTConstants;
import slimeknights.tconstruct.library.utils.TagUtil;
import slimeknights.tconstruct.tables.tileentity.TableTileEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class TableBlock extends InventoryBlock {

  public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
  public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

  public static final VoxelShape SHAPE = VoxelShapes.or(
    Block.makeCuboidShape(0.0D, 12.0D, 0.0D, 16.0D, 16.0D, 16.0D), //top
    Block.makeCuboidShape(0.0D, 0.0D, 0.0D, 4.0D, 15.0D, 4.0D), //leg
    Block.makeCuboidShape(12.0D, 0.0D, 0.0D, 16.0D, 15.0D, 4.0D), //leg
    Block.makeCuboidShape(12.0D, 0.0D, 12.0D, 16.0D, 15.0D, 16.0D), //leg
    Block.makeCuboidShape(0.0D, 0.0D, 12.0D, 4.0D, 15.0D, 16.0D) //leg
  );

  protected TableBlock(Properties builder) {
    super(builder);

    this.setDefaultState(this.stateContainer.getBaseState().with(FACING, Direction.NORTH).with(WATERLOGGED, false));
  }

  @Nonnull
  public BlockRenderType getRenderType(BlockState state) {
    return BlockRenderType.MODEL;
  }

  @Override
  public boolean isSideInvisible(BlockState state, BlockState adjacentBlockState, Direction side) {
    return false;
  }

  @Override
  public void onBlockPlacedBy(World worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
    super.onBlockPlacedBy(worldIn, pos, state, placer, stack);

    CompoundNBT tag = TagUtil.getTagSafe(stack);
    TileEntity tileEntity = worldIn.getTileEntity(pos);

    if (tileEntity instanceof TableTileEntity) {
      TableTileEntity tableTileEntity = (TableTileEntity) tileEntity;

      if (tag.contains(TinkerNBTConstants.TINKER_DATA, Constants.NBT.TAG_COMPOUND)) {
        CompoundNBT data = tag.getCompound(TinkerNBTConstants.TINKER_DATA);

        if (data.contains(TinkerNBTConstants.ITEMS)) {
          tableTileEntity.readInventoryFromNBT(data);
        }

        if (data.contains(TinkerNBTConstants.LEG_TEXTURE, Constants.NBT.TAG_COMPOUND)) {
          CompoundNBT legTexture = data.getCompound(TinkerNBTConstants.LEG_TEXTURE);
          tableTileEntity.setLegTexture(legTexture);
        } else {
          tableTileEntity.setLegTexture(new CompoundNBT());
        }
      } else {
        tableTileEntity.setLegTexture(new CompoundNBT());
      }

      if (stack.hasDisplayName()) {
        tableTileEntity.setCustomName(stack.getDisplayName());
      }
    }
  }

  @Override
  public boolean removedByPlayer(BlockState state, World world, BlockPos pos, PlayerEntity player, boolean willHarvest, IFluidState fluid) {
    this.onPlayerDestroy(world, pos, state);

    if (willHarvest) {
      this.harvestBlock(world, player, pos, state, world.getTileEntity(pos), player.getHeldItemMainhand());
    }

    if (this.keepInventory(state)) {
      TileEntity te = world.getTileEntity(pos);

      if (te instanceof InventoryTileEntity) {
        ((InventoryTileEntity) te).clear();
      }
    }

    world.removeBlock(pos, false);

    return false;
  }

  @Override
  public void onReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
    if (state.getBlock() != newState.getBlock()) {
      TileEntity tileentity = worldIn.getTileEntity(pos);
      if (tileentity instanceof InventoryTileEntity) {
        InventoryHelper.dropInventoryItems(worldIn, pos, (IInventory) tileentity);
        worldIn.updateComparatorOutputLevel(pos, this);
      }
    }

    super.onReplaced(state, worldIn, pos, newState, isMoving);
  }

  protected boolean keepInventory(BlockState state) {
    return false;
  }

  private void writeDataOntoItemStack(@Nonnull ItemStack item, @Nonnull IBlockReader world, @Nonnull BlockPos pos, @Nonnull BlockState state, boolean inventorySave) {
    TileEntity tileEntity = world.getTileEntity(pos);

    if (tileEntity != null) {
      if (tileEntity instanceof TableTileEntity) {
        TableTileEntity table = (TableTileEntity) tileEntity;
        CompoundNBT tag = TagUtil.getTagSafe(item);

        if (!tag.contains(TinkerNBTConstants.TINKER_DATA, Constants.NBT.TAG_COMPOUND)) {
          tag.put(TinkerNBTConstants.TINKER_DATA, new CompoundNBT());
        }

        // save inventory, if not empty
        if (inventorySave && keepInventory(state)) {
          if (!table.isInventoryEmpty()) {
            CompoundNBT inventoryTag = new CompoundNBT();
            table.writeInventoryToNBT(inventoryTag);
            tag.put(TinkerNBTConstants.TINKER_DATA, inventoryTag);
            table.clear();
          }
        }

        // texture
        CompoundNBT data = table.getLegTexture();

        if (data != null && !data.isEmpty()) {
          tag.getCompound(TinkerNBTConstants.TINKER_DATA).put(TinkerNBTConstants.LEG_TEXTURE, data);
        }

        if (!tag.isEmpty()) {
          item.setTag(tag);
        }
      }
    }
  }

  @Override
  public ItemStack getPickBlock(BlockState state, RayTraceResult target, IBlockReader world, BlockPos pos, PlayerEntity player) {
    ItemStack itemStack = new ItemStack(this);

    this.writeDataOntoItemStack(itemStack, world, pos, state, true);

    return itemStack;
  }

  public static ItemStack createItemStack(TableBlock table, Block block) {
    ItemStack stack = new ItemStack(table, 1);

    if (block != null) {
      ItemStack blockStack = new ItemStack(block, 1);
      CompoundNBT tag = new CompoundNBT();
      CompoundNBT subTag = new CompoundNBT();

      if (!tag.contains(TinkerNBTConstants.TINKER_DATA, Constants.NBT.TAG_COMPOUND)) {
        tag.put(TinkerNBTConstants.TINKER_DATA, new CompoundNBT());
      }

      blockStack.write(subTag);
      tag.getCompound(TinkerNBTConstants.TINKER_DATA).put(TinkerNBTConstants.LEG_TEXTURE, subTag);
      stack.setTag(tag);
    }

    return stack;
  }

  /*@Override
  public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items) {
    items.add(new ItemStack(this));
    items.add(createItemStack(this, Blocks.COAL_BLOCK));
  }*/

  @Override
  public BlockState getStateForPlacement(BlockItemUseContext context) {
    IWorld iworld = context.getWorld();
    BlockPos blockpos = context.getPos();
    boolean flag = iworld.getFluidState(blockpos).getFluid() == Fluids.WATER;
    return this.getDefaultState().with(FACING, context.getPlacementHorizontalFacing().getOpposite()).with(WATERLOGGED, flag);
  }

  /**
   * Returns the blockstate with the given rotation from the passed blockstate. If inapplicable, returns the passed
   * blockstate.
   */
  @Override
  public BlockState rotate(BlockState state, Rotation rot) {
    return state.with(FACING, rot.rotate(state.get(FACING)));
  }

  /**
   * Returns the blockstate with the given mirror of the passed blockstate. If inapplicable, returns the passed
   * blockstate.
   */
  @Override
  public BlockState mirror(BlockState state, Mirror mirrorIn) {
    return state.rotate(mirrorIn.toRotation(state.get(FACING)));
  }

  @Override
  protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
    builder.add(FACING, WATERLOGGED);
  }

  @Override
  @Deprecated
  public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
    return SHAPE;
  }

  @Override
  public BlockState updatePostPlacement(BlockState stateIn, Direction facing, BlockState facingState, IWorld worldIn, BlockPos currentPos, BlockPos facingPos) {
    if (stateIn.get(WATERLOGGED)) {
      worldIn.getPendingFluidTicks().scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickRate(worldIn));
    }

    return super.updatePostPlacement(stateIn, facing, facingState, worldIn, currentPos, facingPos);
  }

  @Override
  public IFluidState getFluidState(BlockState state) {
    return state.get(WATERLOGGED) ? Fluids.WATER.getStillFluidState(false) : super.getFluidState(state);
  }

}
