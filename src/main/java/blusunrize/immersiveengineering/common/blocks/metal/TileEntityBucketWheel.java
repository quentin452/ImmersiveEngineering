package blusunrize.immersiveengineering.common.blocks.metal;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.common.Config;
import blusunrize.immersiveengineering.common.IEContent;
import blusunrize.immersiveengineering.common.blocks.multiblocks.MultiblockBucketWheel;
import blusunrize.immersiveengineering.common.util.Utils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

public class TileEntityBucketWheel extends TileEntityMultiblockPart
{
	public int facing = 2;
	public float rotation = 0;
	public ItemStack[] digStacks = new ItemStack[8];
	public boolean active = false;
	public ItemStack particleStack;

	@Override
	public ItemStack getOriginalBlock()
	{
		if(pos<0)
			return null;
		ItemStack s = pos<0?null: MultiblockBucketWheel.instance.getStructureManual()[pos/7][pos%7][0];
		return s!=null?s.copy():null;
	}

	@Override
	public void readCustomNBT(NBTTagCompound nbt, boolean descPacket)
	{
		super.readCustomNBT(nbt, descPacket);
		facing = nbt.getInteger("facing");
		float nbtRot = nbt.getFloat("rotation");
		rotation = (Math.abs(nbtRot-rotation)>5*(float)Config.getDouble("excavator_speed"))?nbtRot:rotation; // avoid stuttering due to packet delays
		digStacks = Utils.readInventory(nbt.getTagList("digStacks", 10), 8);
		active = nbt.getBoolean("active");
		particleStack = nbt.hasKey("particleStack")?ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("particleStack")):null;
	}
	@Override
	public void writeCustomNBT(NBTTagCompound nbt, boolean descPacket)
	{
		super.writeCustomNBT(nbt, descPacket);
		nbt.setInteger("facing", facing);
		nbt.setFloat("rotation", rotation);
		nbt.setTag("digStacks", Utils.writeInventory(digStacks));
		nbt.setBoolean("active", active);
		if(particleStack!=null)
			nbt.setTag("particleStack", particleStack.writeToNBT(new NBTTagCompound()));
	}


	@Override
	public void updateEntity()
	{
		if(!formed || pos!=24)
			return;

		if(active)
		{
			rotation+=(float)Config.getDouble("excavator_speed");
			rotation%=360;
		}

		if(worldObj.isRemote){
			if(particleStack!=null)
			{
				ImmersiveEngineering.proxy.spawnBucketWheelFX(this, particleStack);
				particleStack = null;
			}
		}
	}

	@Override
	public void invalidate()
	{
		super.invalidate();

		if(formed && !worldObj.isRemote)
		{
			int f = facing;
			int startX = xCoord-offset[0];
			int startY = yCoord-offset[1];
			int startZ = zCoord-offset[2];
			
			for(int w=-3;w<=3;w++)
				for(int h=-3;h<=3;h++)
				{
					int xx = (f==3?-w: f==2?w: 0);
					int yy = h;
					int zz = (f==5?-w: f==4?w: 0);

					ItemStack s = null;
					TileEntity te = worldObj.getTileEntity(startX+xx,startY+yy,startZ+zz);
					if(te instanceof TileEntityBucketWheel)
					{
						s = ((TileEntityBucketWheel)te).getOriginalBlock();
						((TileEntityBucketWheel)te).formed=false;
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
	public boolean receiveClientEvent(int id, int arg)
	{
		if(id==0)
			this.active = (arg==1);
		return true;
	}

	@SideOnly(Side.CLIENT)
	private AxisAlignedBB renderAABB;
	@Override
	@SideOnly(Side.CLIENT)
	public AxisAlignedBB getRenderBoundingBox()
	{
		if(renderAABB==null)
			if(pos==24)
				renderAABB = AxisAlignedBB.getBoundingBox(xCoord-(facing<4?3:0),yCoord-3,zCoord-(facing>3?3:0), xCoord+(facing<4?4:1),yCoord+4,zCoord+(facing>3?4:1));
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
	@Override
	public float[] getBlockBounds()
	{
		if(pos==3||pos==9||pos==11)
			return new float[]{0,.25f,0, 1,1,1};
		else if(pos==45||pos==37||pos==39)
			return new float[]{0,0,0, 1,.75f,1};
		else if(pos==21)
			return new float[]{facing==2?.25f:0,0,facing==4?.25f:0, facing==3?.75f:1,1,facing==5?.75f:1};
		else if(pos==27)
			return new float[]{facing==3?.25f:0,0,facing==5?.25f:0, facing==2?.75f:1,1,facing==4?.75f:1};
		else if(pos==15||pos==29)
			return new float[]{facing==2?.25f:0,0,facing==4?.25f:0, facing==3?.75f:1,1,facing==5?.75f:1};
		else if(pos==19||pos==33)
			return new float[]{facing==3?.25f:0,0,facing==5?.25f:0, facing==2?.75f:1,1,facing==4?.75f:1};
		return new float[]{0,0,0,1,1,1};
	}

	@Override
	public TileEntityBucketWheel master()
	{
		if(offset[0]==0&&offset[1]==0&&offset[2]==0)
			return null;
		TileEntity te = worldObj.getTileEntity(xCoord-offset[0], yCoord-offset[1], zCoord-offset[2]);
		return te instanceof TileEntityBucketWheel?(TileEntityBucketWheel)te : null;
	}

}