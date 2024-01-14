package blusunrize.immersiveengineering.common.util.compat;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import blusunrize.immersiveengineering.api.ApiUtils;
import blusunrize.immersiveengineering.api.tool.ChemthrowerHandler;
import blusunrize.immersiveengineering.api.tool.ChemthrowerHandler.ChemthrowerEffect_Potion;
import blusunrize.immersiveengineering.common.IERecipes;
import cpw.mods.fml.common.registry.GameRegistry;

public class EnderIOHelper extends IECompatModule
{
	@Override
	public void preInit()
	{
	}

	@Override
	public void init()
	{
		IERecipes.addOreDictAlloyingRecipe("ingotElectricalSteel",1, "Iron", 400,512, "dustCoal","itemSilicon");
		IERecipes.addOreDictAlloyingRecipe("ingotEnergeticAlloy",1, "Gold", 200,512, "dustRedstone","dustGlowstone");
		Item itemPowder = GameRegistry.findItem("EnderIO", "itemPowderIngot");
		Object dustEnderPearl = ApiUtils.isExistingOreName("dustEnderPearl")?"dustEnderPearl": new ItemStack(itemPowder,1,5);
		IERecipes.addOreDictAlloyingRecipe("ingotPhasedGold",1, "EnergeticAlloy", 200,512, dustEnderPearl);
		IERecipes.addOreDictAlloyingRecipe("ingotPhasedIron",1, "Iron", 200,512, dustEnderPearl);
		IERecipes.addOreDictAlloyingRecipe("ingotConductiveIron",1, "Iron", 100,512, "dustRedstone");
		IERecipes.addOreDictAlloyingRecipe("ingotDarkSteel",1, "Iron", 400,512, "dustCoal","dustObsidian");

		ChemthrowerHandler.registerEffect("nutrient_distillation", new ChemthrowerEffect_Potion(null,0, Potion.confusion,80,1));
	}

	@Override
	public void postInit()
	{
	}
}