package blusunrize.immersiveengineering.common.blocks.stone;

import blusunrize.immersiveengineering.api.crafting.BlastFurnaceRecipe;
import blusunrize.immersiveengineering.common.IEContent;
import blusunrize.immersiveengineering.common.blocks.metal.TileEntityMultiblockPart;
import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.oredict.OreDictionary;

public class TileEntityBlastFurnace extends TileEntityMultiblockPart implements ISidedInventory
{
	ItemStack[] inventory = new ItemStack[4];
	public int facing = 2;
	public int process = 0;
	public int processMax = 0;
	public boolean active = false;
	public int burnTime = 0;
	public int lastBurnTime = 0;

	@Override
	public TileEntityBlastFurnace master()
	{
		if(offset[0]==0&&offset[1]==0&&offset[2]==0)
			return null;
		TileEntity te = worldObj.getTileEntity(xCoord-offset[0], yCoord-offset[1], zCoord-offset[2]);
		return te instanceof TileEntityBlastFurnace?(TileEntityBlastFurnace)te : null;
	}

	public static boolean _Immovable()
	{
		return true;
	}

	@Override
	public float[] getBlockBounds()
	{
		return new float[]{0,0,0,1,1,1};
	}

	@Override
	public ItemStack getOriginalBlock()
	{
		return new ItemStack(IEContent.blockStoneDecoration,1,2);
	}

	@Override
	public void updateEntity()
	{
		if(!worldObj.isRemote&&formed&&master()==null)
		{
			boolean a = active;

			if(burnTime>0)
			{			
				if(process>0)
				{
					int processSpeed = getProcessSpeed();
					if(inventory[0]==null)
					{
						process=0;
						processMax=0;
					}
					else
					{
						process-=processSpeed;
						if(!active)
							active=true;
					}
					burnTime-=processSpeed;
					worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
				}

				if(process<=0)
				{
					if(active)
					{
						BlastFurnaceRecipe recipe = getRecipe();
						if(recipe!=null)
						{
							this.decrStackSize(0, recipe.input instanceof ItemStack?((ItemStack)recipe.input).stackSize:1);
							if(inventory[2]!=null)
								inventory[2].stackSize+=recipe.output.copy().stackSize;
							else
								inventory[2] = recipe.output.copy();
							if (recipe.slag!=null)
							{
								if(inventory[3]!=null)
									inventory[3].stackSize+=recipe.slag.copy().stackSize;
								else
									inventory[3] = recipe.slag.copy();
							}
						}
						processMax=0;
						active=false;
					}
					BlastFurnaceRecipe recipe = getRecipe();
					if(recipe!=null)
					{
						this.process=recipe.time;
						this.processMax=process;
						this.active=true;
					}
				}
			}
			else
			{
				if(active)
					active=false;
			}

			if(burnTime<=10 && getRecipe()!=null)
			{
				if(BlastFurnaceRecipe.isValidBlastFuel(inventory[1]))
				{
					burnTime += BlastFurnaceRecipe.getBlastFuelTime(inventory[1]);
					lastBurnTime = BlastFurnaceRecipe.getBlastFuelTime(inventory[1]);
					this.decrStackSize(1, 1);
					worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
				}
			}

			if(a!=active)
			{
				if (!active)
					turnOff();
				this.markDirty();
				int xMin= facing==5?-2: facing==4?0:-1;
				int xMax= facing==5? 0: facing==4?2: 1;
				int zMin= facing==3?-2: facing==2?0:-1;
				int zMax= facing==3? 0: facing==2?2: 1;
				TileEntity tileEntity;
				for(int yy=-1;yy<=1;yy++)
					for(int xx=xMin;xx<=xMax;xx++)
						for(int zz=zMin;zz<=zMax;zz++)
						{
							tileEntity = worldObj.getTileEntity(xCoord+xx, yCoord+yy, zCoord+zz);
							if(tileEntity!=null)
								tileEntity.markDirty();
							worldObj.markBlockForUpdate(xCoord+xx, yCoord+yy, zCoord+zz);
							worldObj.addBlockEvent(xCoord+xx, yCoord+yy, zCoord+zz, IEContent.blockStoneDevice, 1,active?1:0);
						}
			}
		}
	}
	public BlastFurnaceRecipe getRecipe()
	{
		BlastFurnaceRecipe recipe = BlastFurnaceRecipe.findRecipe(inventory[0]);
		if(recipe==null)
			return null;
		if (recipe.input instanceof ItemStack&&((ItemStack)recipe.input).stackSize>inventory[0].stackSize)
			return null;
		if(inventory[2]!=null && (!OreDictionary.itemMatches(inventory[2],recipe.output,true) || inventory[2].stackSize+recipe.output.stackSize>getInventoryStackLimit()) )
			return null;
		if(inventory[3]!=null && recipe.slag!=null && (!OreDictionary.itemMatches(inventory[3],recipe.slag,true) || inventory[3].stackSize+recipe.slag.stackSize>getInventoryStackLimit()) )
			return null;
		return recipe;
	}

	protected int getProcessSpeed()
	{
		return 1;
	}

	protected void turnOff()
	{}

	@Override
	public boolean receiveClientEvent(int id, int arg)
	{
		if(id==0)
			this.formed = arg==1;
		else if(id==1)
			this.active = arg==1;
		markDirty();
		worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
		return true;
	}

	@Override
	public int getSizeInventory()
	{
		if(!formed)
			return 0;
		return inventory.length;
	}
	@Override
	public ItemStack getStackInSlot(int slot)
	{
		if(!formed)
			return null;
		TileEntityBlastFurnace master = master();
		if(master!=null)
			return master.getStackInSlot(slot);
		if(slot<inventory.length)
			return inventory[slot];
		return null;
	}

	@Override
	public ItemStack decrStackSize(int slot, int amount)
	{
		if(!formed)
			return null;
		TileEntityBlastFurnace master = master();
		if(master!=null)
			return master.decrStackSize(slot,amount);
		ItemStack stack = getStackInSlot(slot);
		if(stack != null)
			if(stack.stackSize <= amount)
				setInventorySlotContents(slot, null);
			else
			{
				stack = stack.splitStack(amount);
				if(stack.stackSize == 0)
					setInventorySlotContents(slot, null);
			}
		return stack;
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int slot)
	{
		if(!formed)
			return null;
		TileEntityBlastFurnace master = master();
		if(master!=null)
			return master.getStackInSlotOnClosing(slot);
		ItemStack stack = getStackInSlot(slot);
		if (stack != null)
			setInventorySlotContents(slot, null);
		return stack;
	}

	@Override
	public void setInventorySlotContents(int slot, ItemStack stack)
	{
		if(!formed)
			return;
		TileEntityBlastFurnace master = master();
		if(master!=null)
		{
			master.setInventorySlotContents(slot,stack);
			return;
		}
		ItemStack old = inventory[slot];
		inventory[slot] = stack;
		if (stack != null && stack.stackSize > getInventoryStackLimit())
			stack.stackSize = getInventoryStackLimit();
		if (slot==0&&(old==null||!Utils.stackMatchesObject(stack, old)))
		{
			BlastFurnaceRecipe recipe = getRecipe();
			if(recipe!=null)
			{
				this.process=recipe.time;
				this.processMax=process;
			}
		}
	}

	@Override
	public String getInventoryName()
	{
		return "IEBlastFurnace";
	}

	@Override
	public boolean hasCustomInventoryName()
	{
		return false;
	}

	@Override
	public int getInventoryStackLimit()
	{
		return 64;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer player)
	{
		return worldObj.getTileEntity(xCoord,yCoord,zCoord)!=this?false:player.getDistanceSq(xCoord+.5D,yCoord+.5D,zCoord+.5D)<=64;
	}

	@Override
	public void openInventory()
	{
	}
	@Override
	public void closeInventory()
	{
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack)
	{
		if(!formed)
			return false;
		TileEntityBlastFurnace master = master();
		if(master!=null)
			return master.isItemValidForSlot(slot,stack);
		if(BlastFurnaceRecipe.isValidBlastFuel(stack))
			return slot==1;
		if(slot==0)
			return BlastFurnaceRecipe.findRecipe(stack)!=null;

		return false;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side)
	{
		//You're no longer automateable, hah!
		//		if(!formed)
		return new int[0];
		//		TileEntityBlastFurnace master = master();
		//		if(master!=null)
		//			return master.getAccessibleSlotsFromSide(side);
		//		return new int[]{0,1,2};
	}

	@Override
	public boolean canInsertItem(int slot, ItemStack stack, int side)
	{
		if(!formed)
			return false;
		TileEntityBlastFurnace master = master();
		if(master!=null)
			return master.canInsertItem(slot,stack,side);
		return (slot==0||slot==1) && isItemValidForSlot(slot,stack);
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack stack, int side)
	{
		if(!formed)
			return false;
		TileEntityBlastFurnace master = master();
		if(master!=null)
			return master.canExtractItem(slot,stack,side);
		return slot==2;
	}



	@Override
	public void readCustomNBT(NBTTagCompound nbt, boolean descPacket)
	{
		super.readCustomNBT(nbt, descPacket);
		facing = nbt.getInteger("facing");
		process = nbt.getInteger("process");
		processMax = nbt.getInteger("processMax");
		active = nbt.getBoolean("active");
		burnTime = nbt.getInteger("burnTime");
		lastBurnTime = nbt.getInteger("lastBurnTime");
		if(!descPacket)
		{
			inventory = Utils.readInventory(nbt.getTagList("inventory", 10), 4);
		}
	}

	@Override
	public void writeCustomNBT(NBTTagCompound nbt, boolean descPacket)
	{
		super.writeCustomNBT(nbt, descPacket);
		nbt.setInteger("facing", facing);
		nbt.setInteger("process", process);
		nbt.setInteger("processMax", processMax);
		nbt.setBoolean("active", active);
		nbt.setInteger("burnTime", burnTime);
		nbt.setInteger("lastBurnTime", lastBurnTime);
		if(!descPacket)
		{
			nbt.setTag("inventory", Utils.writeInventory(inventory));
		}
	}

	@Override
	public void invalidate()
	{
		super.invalidate();
		this.disassemble();
	}

	protected void disassemble()
	{
		if(formed && !worldObj.isRemote)
		{
			int startX = xCoord - offset[0];
			int startY = yCoord - offset[1];
			int startZ = zCoord - offset[2];
			if(!(offset[0]==0&&offset[1]==0&&offset[2]==0) && !(worldObj.getTileEntity(startX, startY, startZ) instanceof TileEntityBlastFurnace))
				return;

			int xMin= facing==5?-2: facing==4?0:-1;
			int xMax= facing==5? 0: facing==4?2: 1;
			int zMin= facing==3?-2: facing==2?0:-1;
			int zMax= facing==3? 0: facing==2?2: 1;
			for(int yy=-1;yy<=1;yy++)
				for(int xx=xMin;xx<=xMax;xx++)
					for(int zz=zMin;zz<=zMax;zz++)
					{
						ItemStack s = null;
						TileEntity te = worldObj.getTileEntity(startX+xx,startY+yy,startZ+zz);
						if(te instanceof TileEntityBlastFurnace)
						{
							s = ((TileEntityBlastFurnace)te).getOriginalBlock();
							((TileEntityBlastFurnace)te).formed=false;
						}
						if(startX+xx==xCoord && startY+yy==yCoord && startZ+zz==zCoord)
							s = this.getOriginalBlock();
						if(s!=null && Block.getBlockFromItem(s.getItem())!=null)
						{
							if(startX+xx==xCoord && startY+yy==yCoord && startZ+zz==zCoord)
								worldObj.spawnEntityInWorld(new EntityItem(worldObj, xCoord+.5,yCoord+.5,zCoord+.5, s));
							else
							{
								if(Block.getBlockFromItem(s.getItem())==IEContent.blockStoneDevice)
									worldObj.setBlockToAir(startX+xx,startY+yy,startZ+zz);
								worldObj.setBlock(startX+xx,startY+yy,startZ+zz, Block.getBlockFromItem(s.getItem()), s.getItemDamage(), 0x3);
							}
						}
					}
		}
	}
}