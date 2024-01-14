package blusunrize.immersiveengineering.common.blocks.metal;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import blusunrize.immersiveengineering.common.Config;
import blusunrize.immersiveengineering.common.IEContent;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IBlockOverlayText;
import blusunrize.immersiveengineering.common.util.Lib;
import blusunrize.immersiveengineering.common.util.Utils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TileEntitySheetmetalTank extends TileEntityMultiblockPart implements IFluidHandler, IBlockOverlayText
{
	public FluidTank tank = new FluidTank(512000);
	private int[] oldComps = new int[4];
	private int masterCompOld;

	@Override
	public TileEntitySheetmetalTank master()
	{
		if(offset[0]==0&&offset[1]==0&&offset[2]==0)
			return null;
		TileEntity te = worldObj.getTileEntity(xCoord-offset[0], yCoord-offset[1], zCoord-offset[2]);
		return te instanceof TileEntitySheetmetalTank?(TileEntitySheetmetalTank)te : null;
	}

	@Override
	public String[] getOverlayText(EntityPlayer player, MovingObjectPosition mop, boolean hammer)
	{
		if(Utils.isFluidRelatedItemStack(player.getCurrentEquippedItem()))
		{
			FluidStack fs = master()!=null?master().tank.getFluid():this.tank.getFluid();
			String s = null;
			if(fs!=null)
				s = fs.getLocalizedName()+": "+fs.amount+"mB";
			else
				s = StatCollector.translateToLocal(Lib.GUI+"empty");
			return new String[]{s};
		}
		return null;
	}
	@Override
	public boolean useNixieFont(EntityPlayer player, MovingObjectPosition mop)
	{
		return false;
	}

	@Override
	public boolean canUpdate()
	{
		return true;
	}

	@Override
	public void updateEntity()
	{
		if(pos==4 && !worldObj.isRemote && worldObj.isBlockIndirectlyGettingPowered(xCoord,yCoord,zCoord))
			for(int i=0; i<6; i++)
				if(i!=1 && tank.getFluidAmount()>0)
				{
					ForgeDirection f = ForgeDirection.getOrientation(i);
					int out = Math.min(144,tank.getFluidAmount());
					TileEntity te = this.worldObj.getTileEntity(xCoord+(i==4?-1:i==5?1:0),yCoord+(i==0?-1:0),zCoord+(i==2?-1:i==3?1:0));
					if(te!=null && te instanceof IFluidHandler && ((IFluidHandler)te).canFill(f.getOpposite(), tank.getFluid().getFluid()))
					{
						updateComparatorValuesPart1();
						int accepted = ((IFluidHandler)te).fill(f.getOpposite(), new FluidStack(tank.getFluid().getFluid(),out), false);
						FluidStack drained = this.tank.drain(accepted, true);
						((IFluidHandler)te).fill(f.getOpposite(), drained, true);
						worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
						updateComparatorValuesPart2();
					}
				}
	}

	@Override
	public void readCustomNBT(NBTTagCompound nbt, boolean descPacket)
	{
		super.readCustomNBT(nbt, descPacket);
		tank.readFromNBT(nbt.getCompoundTag("tank"));
	}

	@Override
	public void writeCustomNBT(NBTTagCompound nbt, boolean descPacket)
	{
		super.writeCustomNBT(nbt, descPacket);
		NBTTagCompound tankTag = tank.writeToNBT(new NBTTagCompound());
		nbt.setTag("tank", tankTag);
	}

	@Override
	public float[] getBlockBounds()
	{
		if(pos==0||pos==2||pos==6||pos==8)
			return new float[]{.375f,0,.375f,.625f,1,.625f};
		return new float[]{0,0,0,1,1,1};
	}

	@Override
	public ItemStack getOriginalBlock()
	{
		return pos==0||pos==2||pos==6||pos==8?new ItemStack(IEContent.blockWoodenDecoration,1,1):new ItemStack(IEContent.blockMetalDecoration,1,BlockMetalDecoration.META_sheetMetal);
	}

	@Override
	public void invalidate()
	{
		super.invalidate();
		if(formed && !worldObj.isRemote)
		{
			int startX = xCoord - offset[0];
			int startY = yCoord - offset[1];
			int startZ = zCoord - offset[2];
			if(!(offset[0]==0&&offset[1]==0&&offset[2]==0) && !(worldObj.getTileEntity(startX, startY, startZ) instanceof TileEntitySheetmetalTank))
				return;

			for(int yy=0;yy<=4;yy++)
				for(int xx=-1;xx<=1;xx++)
					for(int zz=-1;zz<=1;zz++)
					{
						ItemStack s = null;
						TileEntity te = worldObj.getTileEntity(startX+xx,startY+yy,startZ+zz);
						if(te instanceof TileEntitySheetmetalTank)
						{
							s = ((TileEntitySheetmetalTank)te).getOriginalBlock();
							((TileEntitySheetmetalTank)te).formed=false;
						}
						if(startX+xx==xCoord && startY+yy==yCoord && startZ+zz==zCoord)
							s = this.getOriginalBlock();
						if(s!=null && Block.getBlockFromItem(s.getItem())!=null)
						{
							if(startX+xx==xCoord && startY+yy==yCoord && startZ+zz==zCoord)
								worldObj.spawnEntityInWorld(new EntityItem(worldObj, xCoord+.5,yCoord+.5,zCoord+.5, s));
							else
							{
								if(Block.getBlockFromItem(s.getItem())==IEContent.blockMetalMultiblocks)
									worldObj.setBlockToAir(startX+xx,startY+yy,startZ+zz);
								worldObj.setBlock(startX+xx,startY+yy,startZ+zz, Block.getBlockFromItem(s.getItem()), s.getItemDamage(), 0x3);
							}
						}
					}
		}
	}

	@Override
	public int fill(ForgeDirection from, FluidStack resource, boolean doFill)
	{
		if(!canFill(from, resource!=null?resource.getFluid():null))
			return 0;
		TileEntitySheetmetalTank master = master();
		if(master!=null)
			return master.fill(from,resource,doFill);
		updateComparatorValuesPart1();
		int f = tank.fill(resource, doFill);
		if(f>0 && doFill)
		{
			markDirty();
			worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
			updateComparatorValuesPart2();
		}
		return f;
	}
	@Override
	public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain)
	{
		if(!canDrain(from, resource!=null?resource.getFluid():null))
			return null;
		TileEntitySheetmetalTank master = master();
		if(master!=null)
			return master.drain(from,resource,doDrain);
		if(resource!=null)
			return drain(from, resource.amount, doDrain);
		return null;
	}
	@Override
	public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain)
	{
		if(!canDrain(from, null))
			return null;
		TileEntitySheetmetalTank master = master();
		if(master!=null)
			return master.drain(from,maxDrain,doDrain);
		updateComparatorValuesPart1();
		FluidStack fs = tank.drain(maxDrain, doDrain);
		if(fs!=null && fs.amount>0 && doDrain)
		{
			markDirty();
			worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
			//Block updates for comparators
			updateComparatorValuesPart2();
		}
		return fs;
	}
	@Override
	public boolean canFill(ForgeDirection from, Fluid fluid)
	{
		return formed&&(pos==4||pos==40);
	}
	@Override
	public boolean canDrain(ForgeDirection from, Fluid fluid)
	{
		return formed&&pos==4;
	}
	@Override
	public FluidTankInfo[] getTankInfo(ForgeDirection from)
	{
		if (pos==4||pos==40) {
			if (!formed)
				return new FluidTankInfo[] {};
			TileEntitySheetmetalTank master = master();
			if (master != null)
				return master.getTankInfo(from);
			return new FluidTankInfo[] { tank.getInfo() };
		} else {
			return new FluidTankInfo[0];
		}
	}


	@SideOnly(Side.CLIENT)
	private AxisAlignedBB renderAABB;
	@Override
	@SideOnly(Side.CLIENT)
	public AxisAlignedBB getRenderBoundingBox()
	{
		if(renderAABB==null)
			if(pos==4)
				renderAABB = AxisAlignedBB.getBoundingBox(xCoord-1,yCoord,zCoord-1, xCoord+2,yCoord+5,zCoord+2);
			else
				renderAABB = AxisAlignedBB.getBoundingBox(xCoord,yCoord,zCoord, xCoord,yCoord,zCoord);
		return renderAABB;
	}
	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared()
	{
		return super.getMaxRenderDistanceSquared()*Config.getDouble("increasedTileRenderdistance");
	}

	public int getComparatorOutput() {
		if (pos==4)
		{
			return (15*tank.getFluidAmount())/tank.getCapacity();
		}
		TileEntitySheetmetalTank master = master();
		if (offset[1]>=1&&offset[1]<=4&&master!=null) { //4 layers of storage
			FluidTank t = master.tank;
			int layer = offset[1]-1;
			int vol = t.getCapacity()/4;
			int filled = t.getFluidAmount()-layer*vol;
			int ret = Math.min(15, Math.max(0, (15*filled)/vol));
			return ret;
		}
		return 0;
	}
	
	private void updateComparatorValuesPart1() {
//		oldComps = new int[4];
		int vol = tank.getCapacity() / 4;
		for (int i = 0; i < 4; i++)
		{
			int filled = tank.getFluidAmount() - i * vol;
			oldComps[i] = Math.min(15, Math.max((15*filled)/vol, 0));
		}
		masterCompOld = (15*tank.getFluidAmount())/tank.getCapacity();
	}
	
	private void updateComparatorValuesPart2() {
		int vol = tank.getCapacity() / 6;
		if ((15*tank.getFluidAmount())/tank.getCapacity()!=masterCompOld)
			worldObj.func_147453_f(xCoord, yCoord, zCoord, getBlockType());
		for (int i = 0; i < 4; i++)
		{
			int filled = tank.getFluidAmount() - i * vol;
			int now = Math.min(15, Math.max((15*filled)/vol, 0));
			if (now!=oldComps[i])
			{
				int y = yCoord-offset[1]+i+1;
				for (int x = -1;x<2;x++)
					for (int z = -1;z<2;z++)
						worldObj.func_147453_f(xCoord-offset[0]+x, y, zCoord-offset[2]+z, worldObj.getBlock(xCoord-offset[0]+x, y, zCoord-offset[2]+z));
			}
		}
//		oldComps = null;
	}
}