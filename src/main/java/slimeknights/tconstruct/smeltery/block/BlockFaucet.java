package slimeknights.tconstruct.smeltery.block;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.smeltery.tileentity.TileFaucet;

public class BlockFaucet extends BlockContainer {

  // Facing == input, can be any side except bottom, because down always is output direction
  public static final PropertyDirection FACING = PropertyDirection.create("facing", new Predicate<EnumFacing>() {
    @Override
    public boolean apply(@Nullable EnumFacing input) {
      return input != EnumFacing.DOWN;
    }
  });

  public BlockFaucet() {
    super(Material.ROCK);

    this.setCreativeTab(TinkerRegistry.tabSmeltery);
    this.setHardness(3F);
    this.setResistance(20F);
    this.setSoundType(SoundType.METAL);
  }

  @Override
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING);
  }

  /**
   * Convert the given metadata into a BlockState for this Block
   */
  @Override
  public IBlockState getStateFromMeta(int meta) {
    if(meta >= EnumFacing.values().length) {
      meta = 1;
    }
    EnumFacing face = EnumFacing.values()[meta];
    if(face == EnumFacing.DOWN) {
      face = EnumFacing.UP;
    }

    return this.getDefaultState().withProperty(FACING, face);
  }

  /**
   * Convert the BlockState into the correct metadata value
   */
  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).ordinal();
  }

  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ) {
    if(playerIn.isSneaking()) {
      return false;
    }
    TileEntity te = worldIn.getTileEntity(pos);
    if(te instanceof TileFaucet) {
      ((TileFaucet) te).activate();
      return true;
    }
    return super.onBlockActivated(worldIn, pos, state, playerIn, hand, heldItem, side, hitX, hitY, hitZ);
  }

  /* Redstone */

  @Override
  public boolean canConnectRedstone(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
    return true;
  }

  @Override
  public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock) {
    if(worldIn.isRemote) {
      return;
    }
    TileEntity te = worldIn.getTileEntity(pos);
    if(te instanceof TileFaucet) {
      ((TileFaucet) te).handleRedstone(worldIn.isBlockPowered(pos));
    }
  }

  /* Bounds */

  private static final ImmutableMap<EnumFacing, AxisAlignedBB> BOUNDS;
  static {
    ImmutableMap.Builder<EnumFacing, AxisAlignedBB> builder = ImmutableMap.builder();
    builder.put(EnumFacing.UP,    new AxisAlignedBB(0.25,  0.625, 0.25,    0.75,  1,     0.75));
    builder.put(EnumFacing.NORTH, new AxisAlignedBB(0.25,  0.25,  0,       0.75,  0.625, 0.375));
    builder.put(EnumFacing.SOUTH, new AxisAlignedBB(0.25,  0.25,  0.625,   0.75,  0.625, 1.0));
    builder.put(EnumFacing.EAST,  new AxisAlignedBB(0.625, 0.25,  0.25,    1,     0.625, 0.75));
    builder.put(EnumFacing.WEST,  new AxisAlignedBB(0,     0.25,  0.25,    0.375, 0.625, 0.75));
    builder.put(EnumFacing.DOWN, FULL_BLOCK_AABB);

    BOUNDS = builder.build();
  }

  @Override
  public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOUNDS.get(state.getValue(FACING));
  }

  @Override
  public EnumBlockRenderType getRenderType(IBlockState state) {
    return EnumBlockRenderType.MODEL;
  }

  @Override
  @SideOnly(Side.CLIENT)
  public boolean shouldSideBeRendered(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
    return true;
  }

  @Override
  public boolean isFullCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean isOpaqueCube(IBlockState state) {
    return false;
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileFaucet();
  }

  /**
   * Called by ItemBlocks just before a block is actually set in the world, to allow for adjustments to the
   * IBlockstate
   */
  @Override
  public IBlockState onBlockPlaced(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    EnumFacing enumfacing = facing.getOpposite();

    if(enumfacing == EnumFacing.DOWN) {
      enumfacing = placer.getHorizontalFacing().getOpposite();
    }

    return this.getDefaultState().withProperty(FACING, enumfacing);
  }
}
