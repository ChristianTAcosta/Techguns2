package techguns.blocks;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.Mirror;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.fml.common.registry.GameRegistry;
import techguns.Techguns;
import techguns.events.TechgunsGuiHandler;
import techguns.items.ItemTGDoor3x3;
import techguns.tileentities.Door3x3TileEntity;

public class BlockTGDoor3x3<T extends Enum<T> & IStringSerializable> extends GenericBlock {

	public static final PropertyBool MASTER = PropertyBool.create("master");
	public static final PropertyBool ZPLANE = PropertyBool.create("zplane");
	public static final PropertyBool OPENED = PropertyBool.create("open");
	
	protected Class<T> clazz;
	protected BlockStateContainer blockStateOverride;
	
	protected ItemTGDoor3x3<T> placer;
	
	protected static final double size = 6/16D;//0.3125D;
	protected static final double size2 = 4/16D;
	
	protected static final AxisAlignedBB BB_X = new AxisAlignedBB(size, 0, 0, 1-size, 1, 1);
	protected static final AxisAlignedBB BB_Z = new AxisAlignedBB(0, 0, size, 1, 1, 1-size);
	
	protected static final AxisAlignedBB BB_X_openTop = new AxisAlignedBB(0,1-size2,size,1,1,1-size);
	protected static final AxisAlignedBB BB_Z_openTop = new AxisAlignedBB(size,1-size2,0,1-size,1,1);
	
	protected static final AxisAlignedBB BB_Z_openLeft = new AxisAlignedBB(size, 0, 0, 1-size, 1, size2);
	protected static final AxisAlignedBB BB_Z_openRight = new AxisAlignedBB(size, 0, 1-size2, 1-size, 1, 1);
	
	protected static final AxisAlignedBB BB_X_openLeft = new AxisAlignedBB(0, 0, size, size2, 1, 1-size);
	protected static final AxisAlignedBB BB_X_openRight = new AxisAlignedBB(1-size2, 0, size, 1, 1, 1-size);
	
	public static final int BLOCK_UPDATE_DELAY=5;
	
	public static final int DOOR_OPEN_TIME=1000;
	public static final int DOOR_AUTOCLOSE_DELAY = 5000;
	
	public BlockTGDoor3x3(String name,  Class<T> clazz, ItemTGDoor3x3<T> doorplacer) {
		super(name, Material.IRON);
		this.setSoundType(SoundType.METAL);
		this.clazz=clazz;
		this.blockStateOverride = new BlockStateContainer.Builder(this).add(MASTER).add(ZPLANE).add(OPENED).build();
		this.setDefaultState(this.getBlockState().getBaseState().withProperty(MASTER, false).withProperty(ZPLANE, false).withProperty(OPENED, false));
		this.placer=doorplacer;
		this.placer.setBlock(this);
		
		setHardness(5.0f);
	}
	
    @Override
	public void registerBlock(Register<Block> event) {
		super.registerBlock(event);
		GameRegistry.registerTileEntity(Door3x3TileEntity.class, Techguns.MODID+":"+"door3x3tileent");
	}

	public boolean isOpaqueCube(IBlockState state)
    {
        return false;
    }

    public boolean isFullCube(IBlockState state)
    {
        return false;
    }
	
    @Override
	public EnumPushReaction getMobilityFlag(IBlockState state) {
		return EnumPushReaction.BLOCK;
	}

	public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing side)
    {
        return BlockFaceShape.UNDEFINED;
    }
    
	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
	}

	public boolean hasTileEntity(IBlockState state)
    {
       return state.getValue(MASTER);
    }
	
	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		if(state.getValue(MASTER)) {
			return new Door3x3TileEntity();
		}
		return null;
	}

	protected static Vec3i[] pos_z = {
			new Vec3i(0, 1, 0),
			new Vec3i(0, -1, 0),
			new Vec3i(0, 0, -1),
			new Vec3i(0, 0, 1),
			new Vec3i(0, -1, -1),
			new Vec3i(0, -1, 1),
			new Vec3i(0, 1, -1),
			new Vec3i(0, 1, 1)
	};
	
	protected static Vec3i[] pos_x = {
			new Vec3i(0, 1, 0),
			new Vec3i(0, -1, 0),
			new Vec3i(-1, 0, 0),
			new Vec3i(1, 0, 0),
			new Vec3i(-1, -1, 0),
			new Vec3i(1, -1, 0),
			new Vec3i(-1, 1, 0),
			new Vec3i(1, 1, 0)
	};
	
	protected BlockPos findMaster(IBlockAccess w, BlockPos pos, IBlockState state) {
		if(state.getValue(MASTER)) {
			return pos;
		}
		Vec3i[] offsets = pos_x;
		if(state.getValue(ZPLANE)) {
			offsets=pos_z;
		}
		
		for(int i=0;i<offsets.length; i++) {
			BlockPos p = pos.add(offsets[i]);
			IBlockState s = w.getBlockState(p);
			if(s.getBlock()==this && s.getValue(MASTER)) {
				return p;
			}
		}
		return pos;
	}
	
	public static AxisAlignedBB NO_COLLIDE=new AxisAlignedBB(0, 0, 0, 0, 0, 0);
	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		if(!state.getValue(OPENED)) {
			return getBBforPlane(state);
		} else {
			BlockPos master = findMaster(source, pos, state);
			if(state.getValue(MASTER) || master.getY()>= pos.getY()) {
				
				if(!state.getValue(ZPLANE)) {
					if(pos.getX()<master.getX()) {
						return BB_X_openLeft;
					} else if ( pos.getX()>master.getX()) {
						return BB_X_openRight;
					}
				} else {
					if(pos.getZ()<master.getZ()) {
						return BB_Z_openLeft;
					} else if ( pos.getZ()>master.getZ()) {
						return BB_Z_openRight;
					}
				}
				return NO_COLLIDE;
			} else {
				if(!state.getValue(ZPLANE)) {
					return BB_X_openTop;
				} else {
					return BB_Z_openTop;
				}
				//return getBBforPlane(state);
			}
		}
	}
	
	protected AxisAlignedBB getBBforPlane(IBlockState state) {
		if(state.getValue(ZPLANE)) {
			return BB_X;
		} else {
			return BB_Z;
		}
	}
	
	@Override
	public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos) {
		if(blockState.getValue(OPENED)) {
			return NULL_AABB;
		} else {
			return getBBforPlane(blockState);
		}
	}

	  private int getCloseSound()
	    {
	        return this.blockMaterial == Material.IRON ? 1011 : 1012;
	    }

	    private int getOpenSound()
	    {
	        return this.blockMaterial == Material.IRON ? 1005 : 1006;
	    }
	
	public void toggleState(World w, BlockPos masterPos) {
		IBlockState masterstate = w.getBlockState(masterPos);
		boolean opened = !masterstate.getValue(OPENED);
		
		Vec3i[] offsets = pos_x;
		if(masterstate.getValue(ZPLANE)) {
			offsets=pos_z;
		}
		
		for(int i=0;i<offsets.length; i++) {
			BlockPos p = masterPos.add(offsets[i]);
			this.setOpenedStateForBlock(w, p, opened);
		}
		this.setOpenedStateForBlock(w, masterPos, opened);
		 w.playEvent((EntityPlayer)null, opened ? this.getOpenSound() : this.getCloseSound(), masterPos, 0);
		 
		 TileEntity tile = w.getTileEntity(masterPos);
		 if(tile!=null && tile instanceof Door3x3TileEntity) {
			 Door3x3TileEntity door = (Door3x3TileEntity) tile;
			 if(!w.isRemote) {
				door.changeStateServerSide();
				 
				if(door.isAutoClose()) {
					w.scheduleBlockUpdate(masterPos, this, BLOCK_UPDATE_DELAY, 0);
				}
				door.setLastStateChangeTime(System.currentTimeMillis());
			 } else {
				 door.setLastStateChangeTime(System.currentTimeMillis());
			 }
		 } 
	}
	
	public void setOpenedStateForBlock(World w, BlockPos p, boolean b) {
		IBlockState state = w.getBlockState(p);
		if(state.getBlock()==this) {
			IBlockState newstate = state.withProperty(OPENED,b);
			w.setBlockState(p, newstate, 3);
		}
	}
	
	@Override
	public BlockStateContainer getBlockState() {
		return this.blockStateOverride;
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return this.getDefaultState().withProperty(OPENED, (meta&2)>0).withProperty(ZPLANE, (meta&4)>0).withProperty(MASTER, (meta&8)>0);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		int meta=0;
		if(state.getValue(MASTER)) {
			meta+=8;
		}
		if(state.getValue(ZPLANE)) {
			meta+=4;
		}
		if(state.getValue(OPENED)) {
			meta+=2;
		}
		return meta;
	}

	@Override
	public Item getItemDropped(IBlockState state, Random rand, int fortune) {
		return this.placer;
	}

	@Override
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos,
			EntityPlayer player) {

		BlockPos masterPos = this.findMaster(world, pos, state);
		TileEntity tile = world.getTileEntity(masterPos);
		if(tile!=null && tile instanceof Door3x3TileEntity) {
			Door3x3TileEntity door = (Door3x3TileEntity) tile;
			return new ItemStack(this.placer,1,door.getDoorType());
		}
		
		return super.getPickBlock(state, target, world, pos, player);
	}

	@Override
	public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state,
			int fortune) {
	}

	@Override
	public void onBlockHarvested(World worldIn, BlockPos pos, IBlockState state, EntityPlayer player) {
		if (player.capabilities.isCreativeMode || worldIn.isRemote) return;
		
		ItemStack drop =ItemStack.EMPTY;
		BlockPos masterPos = this.findMaster(worldIn, pos, state);
		TileEntity tile = worldIn.getTileEntity(masterPos);

		if(tile!=null && tile instanceof Door3x3TileEntity) {
			Door3x3TileEntity door = (Door3x3TileEntity) tile;
			drop = new ItemStack(this.placer,1,door.getDoorType());
		}
		
		if (!drop.isEmpty() && !worldIn.restoringBlockSnapshots) // do not drop items while restoring blockstates, prevents item dupe
		{
			NonNullList<ItemStack> drops = NonNullList.<ItemStack>withSize(1, drop);
			float chance = net.minecraftforge.event.ForgeEventFactory.fireBlockHarvesting(drops, worldIn, pos, state, 0, 1.0f, false, player);

			if (chance>=1.0f || worldIn.rand.nextFloat() <= chance) {
				spawnAsEntity(worldIn, pos, drop);
			}

		}
		
		super.onBlockHarvested(worldIn, pos, state, player);
	}

	public boolean canPlaceDoor(World worldIn, BlockPos pos, EnumFacing lookdirection) {
		int xmin = 0;
		int xmax = 1;
		int zmin = 0;
		int zmax = 1;
		
		if (lookdirection == EnumFacing.NORTH || lookdirection == EnumFacing.SOUTH) {
			xmin = -1;
			xmax = 2;
		} else {
			zmin = -1;
			zmax = 2;
		}
		
		for (int x =xmin; x<xmax;x++) for (int y=0;y<3;y++) for (int z=zmin; z<zmax; z++) {
			
			if (!this.canPlaceBlockAt(worldIn, pos.add(x, y, z))) {
				return false;
			}
			
		}
		return true;
	}

	public Class<T> getEnumClazz() {
		return clazz;
	}

	protected void breakSlave(World w, BlockPos p) {
		IBlockState state = w.getBlockState(p);
		if(state.getBlock() == this) {
			w.setBlockToAir(p);
		}
	}
	
	protected void checkBreakMaster(World w, BlockPos p) {
		IBlockState state = w.getBlockState(p);
		if(state.getBlock()==this && state.getValue(MASTER)) {
			w.setBlockToAir(p);
		}
	}
	
	@Override
	public boolean isPassable(IBlockAccess worldIn, BlockPos pos) {
		IBlockState state = worldIn.getBlockState(pos);
		return state.getValue(OPENED);
	}

	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
			EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		
		BlockPos master = this.findMaster(worldIn, pos, state);
		
		TileEntity tile = worldIn.getTileEntity(master);
		if(tile!=null && tile instanceof Door3x3TileEntity) {
			Door3x3TileEntity door = (Door3x3TileEntity) tile;
			if(door.isUseableByPlayer(playerIn)) {
			
				if(playerIn.isSneaking()) {
					boolean wrenchRight = (!playerIn.getHeldItemMainhand().isEmpty()) && playerIn.getHeldItemMainhand().getItem().getToolClasses(playerIn.getHeldItemMainhand()).contains("wrench");
					boolean wrenchLeft = (!playerIn.getHeldItemMainhand().isEmpty()) && playerIn.getHeldItemMainhand().getItem().getToolClasses(playerIn.getHeldItemMainhand()).contains("wrench");
					
					if(wrenchRight || wrenchLeft) {
						if(!worldIn.isRemote) {
							TechgunsGuiHandler.openGuiForPlayer(playerIn, tile);
						}
					} else {
						if(door.isOpenWithRightClick()) {
							this.toggleState(worldIn, master);
						}
					}
				} else {
					if(door.isOpenWithRightClick()) {
						this.toggleState(worldIn, master);
					}
				}
			}
		}
		
		return true;
	}
	
	@Override
	public boolean shouldCheckWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
		return true;
	}

	public boolean isPowered(World w, BlockPos masterPos) {
		IBlockState masterstate = w.getBlockState(masterPos);
		Vec3i[] offsets = pos_x;
		if(masterstate.getValue(ZPLANE)) {
			offsets=pos_z;
		}
		
		for(int i=0;i<offsets.length; i++) {
			BlockPos p = masterPos.add(offsets[i]);
			if(w.isBlockPowered(p)) {
				return true;
			}
		}
		return false;
	}
	
	public void updateRedstoneState(IBlockAccess world, BlockPos masterPos) {
		TileEntity tile = world.getTileEntity(masterPos);
		if(tile!=null && tile instanceof Door3x3TileEntity) {
			Door3x3TileEntity door = (Door3x3TileEntity) tile;
			if(door.isRedstoneMode() && door.getRedstoneBehaviour()!=0) {
				IBlockState masterstate = world.getBlockState(masterPos);
				boolean powered = this.isPowered(door.getWorld(), masterPos);
				//System.out.println("POWERED: "+powered);
				if(door.getRedstoneBehaviour()==1) {
					if((powered && !masterstate.getValue(OPENED)) || (!powered && masterstate.getValue(OPENED))) {
						
						this.toggleState(door.getWorld(), masterPos);
					}
				} else if (door.getRedstoneBehaviour()==2) {
					if((!powered && !masterstate.getValue(OPENED)) || (powered && masterstate.getValue(OPENED))) { 
						
						this.toggleState(door.getWorld(), masterPos);
					}
				}
			}
		}
	}

	@Override
	public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
		if(!state.getValue(MASTER)) {
			BlockPos masterpos = this.findMaster(world, pos, state);
			this.updateRedstoneState(world, masterpos);
		}
	}

	@Override
	public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
		if (state.getValue(MASTER)) {
			breakSlave(worldIn,pos.up());
			breakSlave(worldIn,pos.down());
			
			if(state.getValue(ZPLANE)) {
				breakSlave(worldIn,pos.north());
				breakSlave(worldIn,pos.north().up());
				breakSlave(worldIn,pos.north().down());
				
				breakSlave(worldIn,pos.south());
				breakSlave(worldIn,pos.south().up());
				breakSlave(worldIn,pos.south().down());
			} else {
				breakSlave(worldIn,pos.east());
				breakSlave(worldIn,pos.east().up());
				breakSlave(worldIn,pos.east().down());
				
				breakSlave(worldIn,pos.west());
				breakSlave(worldIn,pos.west().up());
				breakSlave(worldIn,pos.west().down());
			}
		} else {
			checkBreakMaster(worldIn,pos.up());
			checkBreakMaster(worldIn,pos.down());
			
			if(state.getValue(ZPLANE)) {
				checkBreakMaster(worldIn,pos.north());
				checkBreakMaster(worldIn,pos.north().up());
				checkBreakMaster(worldIn,pos.north().down());
				
				checkBreakMaster(worldIn,pos.south());
				checkBreakMaster(worldIn,pos.south().up());
				checkBreakMaster(worldIn,pos.south().down());
			} else {
				checkBreakMaster(worldIn,pos.east());
				checkBreakMaster(worldIn,pos.east().up());
				checkBreakMaster(worldIn,pos.east().down());
				
				checkBreakMaster(worldIn,pos.west());
				checkBreakMaster(worldIn,pos.west().up());
				checkBreakMaster(worldIn,pos.west().down());
			}
		}
		

		super.breakBlock(worldIn, pos, state);
	}

	@Override
	public IBlockState withRotation(IBlockState state, Rotation rot) {
		if (rot == Rotation.COUNTERCLOCKWISE_90 || rot == Rotation.CLOCKWISE_90) {
			return state.withProperty(ZPLANE, !state.getValue(ZPLANE));
		}
		return state;
	}

	@Override
	public IBlockState withMirror(IBlockState state, Mirror mirrorIn) {
		return state;
	}

	@Override
	public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
	}

	@Override
	public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
		if(!worldIn.isRemote && state.getValue(MASTER)) {
			TileEntity tile = worldIn.getTileEntity(pos);
			if(tile!=null && tile instanceof Door3x3TileEntity) {
				Door3x3TileEntity door = (Door3x3TileEntity) tile;
				
				if(door.isPlayerDetector()) {
					worldIn.scheduleBlockUpdate(pos, this, BLOCK_UPDATE_DELAY, 0);
				}
				
			}
			
		}
	}

	@Override
	public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
		if(worldIn.isRemote || (state.getBlock()!=this) || !state.getValue(MASTER)) return;
		
		//System.out.println("Block Update Tick!");
		
		TileEntity tile = worldIn.getTileEntity(pos);
		if(tile!=null && tile instanceof Door3x3TileEntity) {
			Door3x3TileEntity door = (Door3x3TileEntity) tile;

			boolean openstate = state.getValue(OPENED);
			if(door.getDoormode()==0) {
				if ( door.isAutoClose()&&openstate) {
					if (door.checkAutoCloseDelay()) {
						this.toggleState(worldIn, pos);
						openstate=false;
					}
					worldIn.scheduleBlockUpdate(pos, this, BLOCK_UPDATE_DELAY, 0);
				}
				
			} else if (door.getDoormode()==2) {
				
				boolean open=checkOpenForNearPlayers(worldIn,door,pos);
				if(open && !openstate) {
					this.toggleState(worldIn, pos);
					openstate=true;
				} else if (!open && openstate && door.checkPlayerSensorAutoCloseDelay()) {
					this.toggleState(worldIn, pos);
					openstate=false;
				}
				worldIn.scheduleBlockUpdate(pos, this, BLOCK_UPDATE_DELAY, 0);
			}

		}
		
	}	
	
	protected boolean checkOpenForNearPlayers(World w, Door3x3TileEntity door, BlockPos masterPos) {
		
		AxisAlignedBB bb = new AxisAlignedBB(masterPos).grow(2, 1, 2);
		List<EntityPlayer> nearbyPlayers = w.getEntitiesWithinAABB(EntityPlayer.class, bb);
		
		for (EntityPlayer ply : nearbyPlayers) {
			if (door.isUseableByPlayer(ply)) {
				return true;
			}
		}
		
		return false;
	}
	
	
}
