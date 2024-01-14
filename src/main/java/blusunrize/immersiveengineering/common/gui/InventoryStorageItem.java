package blusunrize.immersiveengineering.common.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import blusunrize.immersiveengineering.api.tool.IInternalStorageItem;
import blusunrize.immersiveengineering.api.tool.IUpgradeableTool;

public class InventoryStorageItem implements IInventory
{
	private ItemStack itemStack;
	private Container container;
	public ItemStack[] stackList;
	private String name;

	public InventoryStorageItem(Container par1Container, ItemStack stack)
	{
		this.container = par1Container;
		if(stack!=null && stack.getItem() instanceof IInternalStorageItem)
		{
			this.itemStack=stack;
			int slots = ((IInternalStorageItem)stack.getItem()).getInternalSlots(stack);
			this.stackList = new ItemStack[slots];
			this.name = stack.getDisplayName();
		}
	}


	@Override
	public int getSizeInventory()
	{
		return this.stackList.length;
	}

	@Override
	public ItemStack getStackInSlot(int i)
	{
		if(i >= this.getSizeInventory())return null;
		return this.stackList[i];
	}	

	@Override
	public ItemStack getStackInSlotOnClosing(int i)
	{
		if (this.stackList[i] != null)
		{
			ItemStack itemstack = this.stackList[i];
			this.stackList[i] = null;
			return itemstack;
		}
		return null;
	}

	@Override
	public ItemStack decrStackSize(int i, int j)
	{

		if (this.stackList[i] != null)
		{
			ItemStack itemstack;

			if (this.stackList[i].stackSize <= j)
			{
				itemstack = this.stackList[i];
				this.stackList[i] = null;
				this.markDirty();
				this.container.onCraftMatrixChanged(this);
				return itemstack;
			}
			itemstack = this.stackList[i].splitStack(j);

			if (this.stackList[i].stackSize == 0)
			{
				this.stackList[i] = null;
			}

			this.container.onCraftMatrixChanged(this);
			return itemstack;
		}
		return null;
	}


	@Override
	public void setInventorySlotContents(int i, ItemStack stack)
	{
		this.stackList[i] = stack;

		if (stack != null && stack.stackSize > this.getInventoryStackLimit())
		{
			stack.stackSize = this.getInventoryStackLimit();
		}

		this.container.onCraftMatrixChanged(this);
	}

	@Override
	public String getInventoryName() {
		return "container."+name;
	}

	@Override
	public boolean hasCustomInventoryName() {
		return false;
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public void markDirty()
	{
		if(itemStack!=null)
		{
			((IInternalStorageItem)this.itemStack.getItem()).setContainedItems(itemStack, stackList);
			if(this.itemStack.getItem() instanceof IUpgradeableTool)
				((IUpgradeableTool)this.itemStack.getItem()).recalculateUpgrades(itemStack);
		}
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer entityplayer) {
		return true;
	}

	@Override
	public void openInventory() {}

	@Override
	public void closeInventory() {}

	@Override
	public boolean isItemValidForSlot(int i, ItemStack itemstack) {
		return true;
	}

}
