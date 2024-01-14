package blusunrize.immersiveengineering.common.gui;

import java.util.List;

import blusunrize.immersiveengineering.api.crafting.ArcFurnaceRecipe;
import blusunrize.immersiveengineering.api.crafting.BlastFurnaceRecipe;
import blusunrize.immersiveengineering.api.crafting.BlueprintCraftingRecipe;
import blusunrize.immersiveengineering.api.shader.IShaderEquipableItem;
import blusunrize.immersiveengineering.api.shader.IShaderItem;
import blusunrize.immersiveengineering.api.tool.IBullet;
import blusunrize.immersiveengineering.api.tool.IDrillHead;
import blusunrize.immersiveengineering.api.tool.IUpgrade;
import blusunrize.immersiveengineering.api.tool.IUpgradeableTool;
import blusunrize.immersiveengineering.common.IEContent;
import blusunrize.immersiveengineering.common.items.ItemEngineersBlueprint;
import blusunrize.immersiveengineering.common.util.IEAchievements;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.oredict.OreDictionary;

public abstract class IESlot extends Slot
{
	final Container container;
	public IESlot(Container container, IInventory inv, int id, int x, int y)
	{
		super(inv, id, x, y);
		this.container=container;
	}

	@Override
	public boolean isItemValid(ItemStack itemStack)
	{
		return true;
	}

	public static class Output extends IESlot
	{
		public Output(Container container, IInventory inv, int id, int x, int y)
		{
			super(container, inv, id, x, y);
		}
		@Override
		public boolean isItemValid(ItemStack itemStack)
		{
			return false;
		}
		@Override
		public void onPickupFromSlot(EntityPlayer player, ItemStack stack)
		{
			super.onPickupFromSlot(player, stack);
			if(player!=null && (this.container instanceof ContainerBlastFurnace || this.container instanceof ContainerArcFurnace) && OreDictionary.itemMatches(new ItemStack(IEContent.itemMetal,1,7), stack, true))
				player.triggerAchievement(IEAchievements.makeSteel);
		}
	}
	public static class FluidContainer extends IESlot
	{
		boolean empty;
		public FluidContainer(Container container, IInventory inv, int id, int x, int y, boolean empty)
		{
			super(container, inv, id, x, y);
			this.empty=empty;
		}
		@Override
		public boolean isItemValid(ItemStack itemStack)
		{
			if(empty)
				return FluidContainerRegistry.isEmptyContainer(itemStack);
			else
				return FluidContainerRegistry.isFilledContainer(itemStack);
		}
	}
	public static class BlastFuel extends IESlot
	{
		public BlastFuel(Container container, IInventory inv, int id, int x, int y)
		{
			super(container, inv, id, x, y);
		}
		@Override
		public boolean isItemValid(ItemStack itemStack)
		{
			return BlastFurnaceRecipe.isValidBlastFuel(itemStack);
		}
	}
	public static class Bullet extends IESlot
	{
		int limit;
		public Bullet(Container container, IInventory inv, int id, int x, int y, int limit)
		{
			super(container, inv, id, x, y);
			this.limit=limit;
		}
		@Override
		public boolean isItemValid(ItemStack itemStack)
		{
			//			return true;
			return itemStack!=null && itemStack.getItem() instanceof IBullet;
			//itemStack!=null && itemStack.getItem().equals(IEContent.itemBullet);
		}
		@Override
		public int getSlotStackLimit()
		{
			return limit;
		}
	}
	public static class DrillHead extends IESlot
	{
		public DrillHead(Container container, IInventory inv, int id, int x, int y)
		{
			super(container, inv, id, x, y);
		}
		@Override
		public boolean isItemValid(ItemStack itemStack)
		{
			return itemStack!=null && itemStack.getItem() instanceof IDrillHead;
		}
		@Override
		public int getSlotStackLimit()
		{
			return 1;
		}
	}
	public static class Upgrades extends IESlot
	{
		ItemStack upgradeableTool;
		String type;
		boolean preventDoubles;
		public Upgrades(Container container, IInventory inv, int id, int x, int y, String type, ItemStack upgradeableTool, boolean preventDoubles)
		{
			super(container, inv, id, x, y);
			this.type = type;
			this.upgradeableTool = upgradeableTool;
			this.preventDoubles = preventDoubles;
		}
		@Override
		public boolean isItemValid(ItemStack itemStack)
		{
			if(preventDoubles)
				for(Slot slot : (List<Slot>)container.inventorySlots)
					if(slot instanceof IESlot.Upgrades && ((IESlot.Upgrades)slot).preventDoubles && OreDictionary.itemMatches(slot.getStack(), itemStack, true))
						return false;
			return itemStack!=null && itemStack.getItem() instanceof IUpgrade && ((IUpgrade)itemStack.getItem()).getUpgradeTypes(itemStack).contains(type) && ((IUpgrade)itemStack.getItem()).canApplyUpgrades(upgradeableTool, itemStack);
		}
		@Override
		public int getSlotStackLimit()
		{
			return 64;
		}
	}
	public static class Shader extends IESlot
	{
		ItemStack tool;
		public Shader(Container container, IInventory inv, int id, int x, int y, ItemStack tool)
		{
			super(container, inv, id, x, y);
			this.tool = tool;
			if(FMLCommonHandler.instance().getEffectiveSide()==Side.CLIENT)
				this.setBackgroundIcon(IEContent.itemShader.icons[3]);
		}
		@Override
		public boolean isItemValid(ItemStack itemStack)
		{
			if(itemStack==null || !(itemStack.getItem() instanceof IShaderItem) || tool==null || !(tool.getItem() instanceof IShaderEquipableItem))
				return false;
			return ((IShaderItem)itemStack.getItem()).getShaderCase(itemStack, tool, ((IShaderEquipableItem)tool.getItem()).getShaderType())!=null;
		}
		@Override
		public int getSlotStackLimit()
		{
			return 1;
		}
	}
	public static class UpgradeableItem extends IESlot
	{
		int size;
		public UpgradeableItem(Container container, IInventory inv, int id, int x, int y, int size)
		{
			super(container, inv, id, x, y);
			this.size = size;
		}
		@Override
		public boolean isItemValid(ItemStack itemStack)
		{
			return itemStack!=null && itemStack.getItem() instanceof IUpgradeableTool && ((IUpgradeableTool)itemStack.getItem()).canModify(itemStack);
		}
		@Override
		public int getSlotStackLimit()
		{
			return size;
		}
		@Override
		public void onSlotChanged()
		{
			super.onSlotChanged();
			if(container instanceof ContainerModWorkbench)
				((ContainerModWorkbench)container).rebindSlots();
		}
		@Override
		public boolean canTakeStack(EntityPlayer player)
		{
			if(this.getStack()!=null && getStack().getItem() instanceof IUpgradeableTool && !((IUpgradeableTool)getStack().getItem()).canTakeFromWorkbench(getStack()))
				return false;
			return true;
		}
		@Override
		public void onPickupFromSlot(EntityPlayer player, ItemStack stack)
		{
			super.onPickupFromSlot(player, stack);
			if(stack!=null && stack.getItem() instanceof IUpgradeableTool)
				((IUpgradeableTool)stack.getItem()).removeFromWorkbench(player, stack);

		}
	}
	public static class Ghost extends IESlot
	{
		public Ghost(Container container, IInventory inv, int id, int x, int y)
		{
			super(container, inv, id, x, y);
		}

		@Override
		public void putStack(ItemStack itemStack)
		{
			super.putStack(itemStack);
		}
		@Override
		public boolean canTakeStack(EntityPlayer player)
		{
			return false;
		}
		@Override
		public int getSlotStackLimit()
		{
			return 1;
		}
	}
	public static class ItemDisplay extends IESlot
	{
		public ItemDisplay(Container container, IInventory inv, int id, int x, int y)
		{
			super(container, inv, id, x, y);
		}
		@Override
		public boolean isItemValid(ItemStack itemStack)
		{
			return false;
		}
		@Override
		public boolean canTakeStack(EntityPlayer player)
		{
			return false;
		}
	}

	public static class BlueprintInput extends IESlot
	{
		ItemStack upgradeableTool;
		public BlueprintInput(Container container, IInventory inv, int id, int x, int y, ItemStack upgradeableTool)
		{
			super(container, inv, id, x, y);
			this.upgradeableTool = upgradeableTool;
		}
		@Override
		public void onSlotChanged()
		{
			this.inventory.markDirty();
			if(upgradeableTool!=null && upgradeableTool.getItem() instanceof ItemEngineersBlueprint)
				((ItemEngineersBlueprint)upgradeableTool.getItem()).updateOutputs(upgradeableTool);
			if(container instanceof ContainerModWorkbench)
				((ContainerModWorkbench)container).rebindSlots();
			super.onSlotChanged();
		}
		@Override
		public int getSlotStackLimit()
		{
			return 64;
		}
	}
	public static class BlueprintOutput extends IESlot
	{
		public BlueprintCraftingRecipe recipe;
		ItemStack upgradeableTool;
		public BlueprintOutput(Container container, IInventory inv, int id, int x, int y, ItemStack upgradeableTool, BlueprintCraftingRecipe recipe)
		{
			super(container, inv, id, x, y);
			this.upgradeableTool = upgradeableTool;
			this.recipe = recipe;
		}
		@Override
		public boolean isItemValid(ItemStack itemStack)
		{
			return false;
		}


		@Override
		@SideOnly(Side.CLIENT)
		/**Determines whether to render slot-highlighting*/
		public boolean func_111238_b()
		{
			return this.getHasStack();
		}

		@Override
		public void onPickupFromSlot(EntityPlayer player, ItemStack stack)
		{
			if(upgradeableTool!=null && upgradeableTool.getItem() instanceof ItemEngineersBlueprint)
				((ItemEngineersBlueprint)upgradeableTool.getItem()).reduceInputs(recipe, upgradeableTool, stack, this.container);
			if(container instanceof ContainerModWorkbench)
			{
				((ContainerModWorkbench)container).rebindSlots();
				((ContainerModWorkbench)container).tile.markDirty();
			}
			for(IEAchievements.AchievementIE achievement : IEAchievements.blueprintCraftingAchievements)
			{
				if(achievement.triggerItems!=null && achievement.triggerItems.length>0)
				{
					for(ItemStack trigger : achievement.triggerItems)
						if(OreDictionary.itemMatches(trigger, stack, true))
						{
							player.triggerAchievement(achievement);
							break;
						}
				}
				else if(OreDictionary.itemMatches(achievement.theItemStack, stack, true))
					player.triggerAchievement(achievement);
			}
			this.inventory.markDirty();
			super.onPickupFromSlot(player, stack);
		}
	}

	public static class ArcInput extends IESlot
	{
		public ArcInput(Container container, IInventory inv, int id, int x, int y)
		{
			super(container, inv, id, x, y);
		}
		@Override
		public boolean isItemValid(ItemStack itemStack)
		{
			return itemStack!=null && ArcFurnaceRecipe.isValidRecipeInput(itemStack);
		}
	}
	public static class ArcAdditive extends IESlot
	{
		public ArcAdditive(Container container, IInventory inv, int id, int x, int y)
		{
			super(container, inv, id, x, y);
		}
		@Override
		public boolean isItemValid(ItemStack itemStack)
		{
			return itemStack!=null && ArcFurnaceRecipe.isValidRecipeAdditive(itemStack);
		}
	}
	public static class ArcElectrode extends IESlot
	{
		public ArcElectrode(Container container, IInventory inv, int id, int x, int y)
		{
			super(container, inv, id, x, y);
		}
		@Override
		public boolean isItemValid(ItemStack itemStack)
		{
			return itemStack!=null && IEContent.itemGraphiteElectrode.equals(itemStack.getItem());
		}
	}

	public static class ContainerCallback extends IESlot
	{
		public ContainerCallback(Container container, IInventory inv, int id, int x, int y)
		{
			super(container, inv, id, x, y);
		}
		@Override
		public boolean isItemValid(ItemStack itemStack)
		{
			if(this.container instanceof ICallbackContainer)
				return ((ICallbackContainer)this.container).canInsert(itemStack, slotNumber, this);
			return true;
		}
		@Override
		public boolean canTakeStack(EntityPlayer player)
		{
			if(this.container instanceof ICallbackContainer)
				return ((ICallbackContainer)this.container).canTake(this.getStack(), slotNumber, this);
			return true;
		}
	}
	public static interface ICallbackContainer
	{
		public boolean canInsert(ItemStack stack, int slotNumer, Slot slotObject);
		public boolean canTake(ItemStack stack, int slotNumer, Slot slotObject);
	}
}