package blusunrize.immersiveengineering.common.blocks.metal;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.oredict.OreDictionary;
import powercrystals.minefactoryreloaded.api.IDeepStorageUnit;
import blusunrize.immersiveengineering.common.Config;
import blusunrize.immersiveengineering.common.IEContent;
import blusunrize.immersiveengineering.common.util.Utils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TileEntitySilo extends TileEntityMultiblockPart implements ISidedInventory, IDeepStorageUnit
{
	public ItemStack identStack;
	public int storageAmount = 0;
	static int maxStorage = 41472;
	ItemStack inputStack;
	ItemStack outputStack;
	ItemStack prevInputStack;
	ItemStack prevOutputStack;
	boolean lockItem = false;
	private int[] oldComps = new int[6];
	private int masterCompOld;
	private boolean forceUpdate = false;

	@Override
	public TileEntitySilo master()
	{
		if(offset[0]==0&&offset[1]==0&&offset[2]==0)
			return null;
		TileEntity te = worldObj.getTileEntity(xCoord-offset[0], yCoord-offset[1], zCoord-offset[2]);
		return te instanceof TileEntitySilo?(TileEntitySilo)te : null;
	}

	@Override
	public void updateEntity()
	{
		if(pos==4 && !worldObj.isRemote && this.outputStack==null && storageAmount>0 && identStack!=null)
			this.markDirty();

		if(pos==4 && !worldObj.isRemote && this.outputStack!=null && worldObj.isBlockIndirectlyGettingPowered(xCoord,yCoord,zCoord) && worldObj.getTotalWorldTime()%8==0)
		{
			updateComparatorValuesPart1();
			for(int i=0; i<6; i++)
				if(i!=1)
				{
					TileEntity inventory = this.worldObj.getTileEntity(xCoord+(i==4?-1:i==5?1:0),yCoord+(i==0?-1:0),zCoord+(i==2?-1:i==3?1:0));
					ItemStack stack = Utils.copyStackWithAmount(identStack,1);
					if((inventory instanceof ISidedInventory && ((ISidedInventory)inventory).getAccessibleSlotsFromSide(ForgeDirection.OPPOSITES[i]).length>0)
							||(inventory instanceof IInventory && ((IInventory)inventory).getSizeInventory()>0))
						stack = Utils.insertStackIntoInventory((IInventory)inventory, stack, ForgeDirection.OPPOSITES[i]);
					if(stack==null)
					{
						outputStack.stackSize--;
						this.markDirty();
						if(outputStack==null)
							break;
					}
				}
			updateComparatorValuesPart2();
		}
	}

	@Override
	public void readCustomNBT(NBTTagCompound nbt, boolean descPacket)
	{
		super.readCustomNBT(nbt, descPacket);
		if(nbt.hasKey("identStack"))
		{
			NBTTagCompound t = nbt.getCompoundTag("identStack");
			this.identStack = ItemStack.loadItemStackFromNBT(t);
		}
		else
			this.identStack = null;
		storageAmount = nbt.getInteger("storageAmount");
		lockItem = nbt.getBoolean("lockItem");
	}

	@Override
	public void writeCustomNBT(NBTTagCompound nbt, boolean descPacket)
	{
		super.writeCustomNBT(nbt, descPacket);
		if(this.identStack!=null)
		{
			NBTTagCompound t = this.identStack.writeToNBT(new NBTTagCompound());
			nbt.setTag("identStack", t);
		}
		nbt.setInteger("storageAmount", storageAmount);
		nbt.setBoolean("lockItem", lockItem);
	}

	@Override
	public float[] getBlockBounds()
	{
		if(pos==0||pos==2||pos==6||pos==8)
			return new float[]{pos<6?0:.75f,0,pos==0||pos==6?0:.75f, pos>2?1:.25f,1,pos==2||pos==8?1f:.25f};
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
			if(!(offset[0]==0&&offset[1]==0&&offset[2]==0) && !(worldObj.getTileEntity(startX, startY, startZ) instanceof TileEntitySilo))
				return;

			for(int yy=0;yy<=6;yy++)
				for(int xx=-1;xx<=1;xx++)
					for(int zz=-1;zz<=1;zz++)
					{
						ItemStack s = null;
						TileEntity tileEntity = worldObj.getTileEntity(startX+xx,startY+yy,startZ+zz);
						if(tileEntity instanceof TileEntitySilo)
						{
							s = ((TileEntitySilo)tileEntity).getOriginalBlock();
							((TileEntitySilo)tileEntity).formed=false;
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
	public int getSizeInventory()
	{
		if(!formed || !(pos==4||pos==58))
			return 0;
		return 2;
	}
	@Override
	public ItemStack getStackInSlot(int slot)
	{
		if(!formed)
			return null;
		TileEntitySilo master = master();
		if(master!=null)
			return master.getStackInSlot(slot);
		return slot==0?inputStack: outputStack;
	}
	@Override
	public ItemStack decrStackSize(int slot, int amount)
	{
		updateComparatorValuesPart1();
		if(!formed)
			return null;
		TileEntitySilo master = master();
		if(master!=null)
			return master.decrStackSize(slot,amount);

		if(this.outputStack==null)
			return null;

		int rem = Math.min(amount, outputStack.stackSize);
		ItemStack ret = Utils.copyStackWithAmount(outputStack, rem);
		outputStack.stackSize-=rem;
		if(outputStack.stackSize<=0)
			outputStack=null;
		this.markDirty();
		updateComparatorValuesPart2();
		return ret;
	}
	@Override
	public ItemStack getStackInSlotOnClosing(int slot)
	{
		return null;
	}
	@Override
	public void setInventorySlotContents(int slot, ItemStack stack)
	{
		updateComparatorValuesPart1();
		if(!formed)
			return;
		TileEntitySilo master = master();
		if(master!=null)
		{
			master.setInventorySlotContents(slot,stack);
			return;
		}
		if(slot==0)
			this.inputStack = stack;
		else
			this.outputStack = stack;
		this.markDirty();
		updateComparatorValuesPart2();
	}
	@Override
	public String getInventoryName()
	{
		return "IESilo";
	}
	@Override
	public boolean hasCustomInventoryName()
	{
		return false;
	}
	@Override
	public int getInventoryStackLimit()
	{
		return maxStorage;
	}
	@Override
	public boolean isUseableByPlayer(EntityPlayer player)
	{
		return true;
	}
	@Override
	public void openInventory() {}
	@Override
	public void closeInventory(){}
	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack)
	{
		TileEntitySilo master = master();
		if(master!=null)
			return master.isItemValidForSlot(slot,stack);
		return this.identStack==null || (OreDictionary.itemMatches(identStack, stack, true) && ItemStack.areItemStackTagsEqual(stack, identStack));
	}
	@Override
	public int[] getAccessibleSlotsFromSide(int side)
	{
		if(pos==4||pos==58)
			return new int[]{0,1};
		return new int[0];
	}
	@Override
	public boolean canInsertItem(int slot, ItemStack stack, int side)
	{
		if(!formed || pos!=58 || slot!=0 || stack==null)
			return false;
		TileEntitySilo master = master();
		if(master!=null)
			return master.identStack==null || (OreDictionary.itemMatches(master.identStack, stack, true) && ItemStack.areItemStackTagsEqual(stack, master.identStack)&& master.storageAmount<maxStorage); 
		return false; 
	}
	@Override
	public boolean canExtractItem(int slot, ItemStack stack, int side)
	{
		if(!formed || pos!=4 || slot!=1 || stack==null)
			return false;
		TileEntitySilo master = master();
		if(master!=null)
			return master.outputStack!=null && OreDictionary.itemMatches(master.identStack, stack, true) && ItemStack.areItemStackTagsEqual(stack, master.identStack); 
		else
			return this.outputStack!=null && OreDictionary.itemMatches(identStack, stack, true) && ItemStack.areItemStackTagsEqual(stack, identStack);
	}

	@Override
	public void markDirty()
	{
		super.markDirty();

		int oldStorage = storageAmount;
		if(inputStack != null)
		{
			if(this.identStack==null)
				identStack = inputStack;

			if((maxStorage-storageAmount)>0)
			{
				if(prevInputStack==null)//inputStack is new
					storageAmount += inputStack.stackSize;
				else
					storageAmount += inputStack.stackSize - prevInputStack.stackSize;

				if(storageAmount>maxStorage)
					storageAmount = maxStorage;
			}
			//Set new fake inputs
			if((maxStorage-storageAmount)>=identStack.getMaxStackSize())
			{
				inputStack = null;
				prevInputStack = null;
			}
			else
			{
				inputStack = Utils.copyStackWithAmount(identStack, identStack.getMaxStackSize()-(maxStorage-storageAmount));
				prevInputStack = inputStack.copy();
			}
		}

		if(prevOutputStack != null)//Had fake output
		{
			if(outputStack == null)//fully depleted
				storageAmount -= prevOutputStack.stackSize;
			else
				storageAmount -= (prevOutputStack.stackSize-outputStack.stackSize);

			if(storageAmount<0)
				storageAmount = 0;
		}

		// Handle emptying of the barrel
		if(storageAmount==0 && !lockItem)
		{
			identStack = null;
			outputStack = null;
			prevOutputStack = null;
			inputStack = null;
			prevInputStack = null;
			forceUpdate=true;
		}
		else if(identStack!=null)
		{
			if(outputStack==null)
				outputStack = identStack.copy();
			outputStack.stackSize = Math.min(outputStack.getMaxStackSize(), storageAmount);
			prevOutputStack = outputStack.copy();
		}
		if(storageAmount!=oldStorage||forceUpdate)
			worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
		forceUpdate = false;
	}

	@SideOnly(Side.CLIENT)
	private AxisAlignedBB renderAABB;
	@Override
	@SideOnly(Side.CLIENT)
	public AxisAlignedBB getRenderBoundingBox()
	{
		if(renderAABB==null)
			if(pos==4)
				renderAABB = AxisAlignedBB.getBoundingBox(xCoord-1,yCoord,zCoord-1, xCoord+2,yCoord+7,zCoord+2);
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

	//DEEP STORAGE
	@Override
	public ItemStack getStoredItemType()
	{
		TileEntitySilo mast = master();
		if (mast!=null)
			return mast.getStoredItemType();
		if(this.identStack != null)
			return Utils.copyStackWithAmount(identStack, storageAmount);
		return null;
	}

	@Override
	public void setStoredItemCount(int amount)
	{
		TileEntitySilo mast = master();
		if (mast!=null)
		{
			mast.setStoredItemCount(amount);
			return;
		}
		updateComparatorValuesPart1();
		if(amount > maxStorage)
			amount = maxStorage;
		this.storageAmount = amount;
		this.forceUpdate = true;
		this.markDirty();
		updateComparatorValuesPart2();
	}

	@Override
	public void setStoredItemType(ItemStack type, int amount)
	{
		TileEntitySilo mast = master();
		if (mast!=null)
		{
			mast.setStoredItemType(type, amount);
			return;
		}
		updateComparatorValuesPart1();
		this.identStack = Utils.copyStackWithAmount(type, 0);
		if(amount > maxStorage)
			amount = maxStorage;
		this.storageAmount = amount;
		this.forceUpdate = true;
		this.markDirty();
		updateComparatorValuesPart2();
	}

	@Override
	public int getMaxStoredCount()
	{
		return maxStorage;
	}

	public int getComparatorOutput() {
		if (pos==4)
		{
			return (15*storageAmount)/maxStorage;
		}
		TileEntitySilo master = master();
		if (offset[1]>=1&&offset[1]<=6&&master!=null) { //6 layers of storage
			int layer = offset[1]-1;
			int vol = maxStorage/6;
			int filled = master.storageAmount-layer*vol;
			int ret = Math.min(15, Math.max(0, (15*filled)/vol));
			return ret;
		}
		return 0;
	}
	private void updateComparatorValuesPart1() {
		//		oldComps = new int[6];
		int vol = maxStorage / 6;
		for (int i = 0; i < 6; i++)
		{
			int filled = storageAmount - i * vol;
			oldComps[i] = Math.min(15, Math.max((15*filled)/vol, 0));
		}
		masterCompOld = (15*storageAmount)/maxStorage;
	}
	private void updateComparatorValuesPart2() {
		int vol = maxStorage / 6;
		if ((15*storageAmount)/maxStorage!=masterCompOld)
			worldObj.func_147453_f(xCoord, yCoord, zCoord, getBlockType());
		for (int i = 0; i < 6; i++)
		{
			int filled = storageAmount - i * vol;
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
