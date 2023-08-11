package blusunrize.immersiveengineering.common.util.compat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import blusunrize.immersiveengineering.common.Config;
import blusunrize.immersiveengineering.common.util.IELogger;
import blusunrize.immersiveengineering.common.util.compat.computercraft.ComputercraftHelper;
import blusunrize.immersiveengineering.common.util.compat.hydcraft.HydCraftHelper;
import blusunrize.immersiveengineering.common.util.compat.mfr.MFRHelper;
import blusunrize.immersiveengineering.common.util.compat.minetweaker.MTHelper;
import blusunrize.immersiveengineering.common.util.compat.opencomputers.OCHelper;
import blusunrize.immersiveengineering.common.util.compat.waila.WailaHelper;
import cpw.mods.fml.common.Loader;

public abstract class IECompatModule
{
	public static HashMap<String, Class<? extends IECompatModule>> moduleClasses = new HashMap<String, Class<? extends IECompatModule>>();
	public static Set<IECompatModule> modules = new HashSet<IECompatModule>();

	static
	{
		moduleClasses.put("appliedenergistics2", AppliedEnergisticsHelper.class);
		moduleClasses.put("MineFactoryReloaded", MFRHelper.class);
		moduleClasses.put("MineTweaker3", MTHelper.class);
		moduleClasses.put("denseores", DenseOresHelper.class);
		moduleClasses.put("EE3", EE3Helper.class);
		moduleClasses.put("ForgeMicroblock", FMPHelper.class);
		moduleClasses.put("Forestry", ForestryHelper.class);
		moduleClasses.put("BackTools", BacktoolsHelper.class);
		moduleClasses.put("Waila", WailaHelper.class);
		moduleClasses.put("gregtech", GregTechHelper.class);
		moduleClasses.put("HydCraft", HydCraftHelper.class);
		moduleClasses.put("ThermalExpansion", ThermalExpansionHelper.class);
		moduleClasses.put("ThermalFoundation", ThermalFoundationHelper.class);
		moduleClasses.put("ThermalDynamics", ThermalDynamicsHelper.class);
		moduleClasses.put("IC2", IC2Helper.class);
		moduleClasses.put("chisel", ChiselHelper.class);
		moduleClasses.put("harvestcraft", HarvestCraftHelper.class);
		moduleClasses.put("CarpentersBlocks", CarpentersHelper.class);
		moduleClasses.put("Botania", BotaniaHelper.class);
		moduleClasses.put("etfuturum", EtFuturumHelper.class);
		moduleClasses.put("EnderIO", EnderIOHelper.class);
		moduleClasses.put("steamcraft2", SteamCraftHelper.class);
		moduleClasses.put("ExtraUtilities", ExtraUtilsHelper.class);
		moduleClasses.put("Thaumcraft", ThaumcraftHelper.class);
		moduleClasses.put("TConstruct", TConstructHelper.class);
		moduleClasses.put("Mekanism", MekanismHelper.class);
		moduleClasses.put("cuttingedge", CuttingEdgeHelper.class);
		moduleClasses.put("Railcraft", RailcraftHelper.class);
		moduleClasses.put("Avaritia", AvaritiaHelper.class);
		moduleClasses.put("ComputerCraft", ComputercraftHelper.class);
		moduleClasses.put("OpenComputers", OCHelper.class);
		moduleClasses.put("MapWriter", MapWriterHelper.class);
	}

	public static void doModulesPreInit()
	{
		for(Entry<String, Class<? extends IECompatModule>> e : moduleClasses.entrySet())
			if(Loader.isModLoaded(e.getKey()) && Config.getBoolean("compat_"+e.getKey()))
				try{
					IECompatModule m = e.getValue().newInstance();
					IELogger.info("Loading compat module: "+m);
					modules.add(m);
					m.preInit();
				}catch (Exception exception){
					IELogger.error("Compat module for "+e.getKey()+" could not be preInitialized. Report this!");
				}
	}
	public static void doModulesInit()
	{
		for(IECompatModule compat : IECompatModule.modules)
			try{
				compat.init();
			}catch (Exception exception){
				IELogger.error("Compat module for "+compat+" could not be initialized");
			}
	}
	public static void doModulesPostInit()
	{
		for(IECompatModule compat : IECompatModule.modules)
			try{
				compat.postInit();
			}catch (Exception exception){
				IELogger.error("Compat module for "+compat+" could not be initialized");
			}
	}
	//We don't want this to happen multiple times after all >_>
	public static boolean serverStartingDone = false;
	public static void doModulesLoadComplete()
	{
		if(!serverStartingDone)
		{
			serverStartingDone = true;
			for(IECompatModule compat : IECompatModule.modules)
				try{
					compat.loadComplete();
				}catch (Exception exception){
					IELogger.error("Compat module for "+compat+" could not be initialized");
				exception.printStackTrace();
				}
		}
	}

	public abstract void preInit();
	public abstract void init();
	public abstract void postInit();
	public void loadComplete(){}
}
