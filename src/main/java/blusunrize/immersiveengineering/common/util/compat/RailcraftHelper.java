package blusunrize.immersiveengineering.common.util.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import blusunrize.immersiveengineering.api.shader.ShaderCaseMinecart;
import blusunrize.immersiveengineering.api.tool.RailgunHandler;
import blusunrize.immersiveengineering.client.models.ModelShaderMinecart;
import blusunrize.immersiveengineering.common.Config;
import blusunrize.immersiveengineering.common.IEContent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mods.railcraft.api.crafting.IRockCrusherCraftingManager;
import mods.railcraft.api.crafting.RailcraftCraftingManager;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelMinecart;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

public class RailcraftHelper extends IECompatModule
{
	public static final String RAILCRAFT_MODID = "Railcraft";

	@Override
	public void preInit()
	{
	}

	@Override
	public void init()
	{
		Item itemRail = GameRegistry.findItem(RAILCRAFT_MODID, "part.rail");
		RailgunHandler.registerProjectileProperties(new ItemStack(itemRail,1,0), 7,1.25).setColourMap(new int[][]{{0xa4a4a4,0x686868}});
		RailgunHandler.registerProjectileProperties(new ItemStack(itemRail,1,1), 6,1.375).setColourMap(new int[][]{{0xa4a4a4,0xa4a4a4,0x686868, 0xddb82c,0xc9901f}, {0xa4a4a4,0xa4a4a4,0x686868, 0xf5cc2d,0xddb82c},{0xa4a4a4,0xa4a4a4,0x686868, 0xf5cc2d,0xddb82c}, {0xa4a4a4,0xa4a4a4,0x686868, 0xddb82c,0xc9901f}});
		RailgunHandler.registerProjectileProperties(new ItemStack(itemRail,1,3), 7,1).setColourMap(new int[][]{{0x999999,0xa4a4a4,0xa4a4a4, 0xc9901f,0xc9901f,0xba851d}});
		RailgunHandler.registerProjectileProperties(new ItemStack(itemRail,1,4), 8,1.375).setColourMap(new int[][]{{0x686868,0x808080,0x808080, 0x3e2e60,0x3e2e60,0x31254d}});
		RailgunHandler.registerProjectileProperties(new ItemStack(itemRail,1,5), 7,1).setColourMap(new int[][]{{0x999999,0xa4a4a4,0xa4a4a4, 0x9a6033,0x9a6033,0xa86938}});

		Item itemRebar = GameRegistry.findItem(RAILCRAFT_MODID, "part.rebar");
		int[][] rebarColourMap = new int[8*3+1][];
		rebarColourMap[0] = new int[]{0x4a2700, 0x592f00,0x592f00,0x592f00, 0x4a2700};
		rebarColourMap[1] = new int[]{0x572e00, 0x673700,0x673700,0x673700, 0x572e00};
		for(int i=0; i<8; i++)
		{
			rebarColourMap[1+ i*3] = rebarColourMap[1+ i*3+1] = rebarColourMap[1];
			rebarColourMap[1+ i*3+2] = rebarColourMap[0];
		}
		RailgunHandler.registerProjectileProperties(new ItemStack(itemRebar), 7,1.25).setColourMap(rebarColourMap);
		Config.setBoolean("literalRailGun", true);

		FMLInterModComms.sendMessage(RAILCRAFT_MODID, "boiler-fuel-liquid", IEContent.fluidBiodiesel.getName()+"@16000");
	}

	@Override
	public void postInit()
	{
		if(FMLCommonHandler.instance().getEffectiveSide()==Side.CLIENT)
		{
			try{
				Class c_CartModelManager = Class.forName("mods.railcraft.client.render.carts.CartModelManager");
				Field f_modelMinecart = c_CartModelManager.getDeclaredField("modelMinecart");
				f_modelMinecart.setAccessible(true);
				Field modifiersField = Field.class.getDeclaredField("modifiers");
				modifiersField.setAccessible(true);
				modifiersField.set(f_modelMinecart, f_modelMinecart.getModifiers() & ~Modifier.FINAL);
				ModelMinecart wrapped = (ModelMinecart)f_modelMinecart.get(null);
				f_modelMinecart.set(null, new ModelShaderMinecart(wrapped));
				modifiersField.set(f_modelMinecart, f_modelMinecart.getModifiers() | Modifier.FINAL);

				Field f_modelsCore = c_CartModelManager.getDeclaredField("modelsCore");
				Map<Class, ModelBase> modelMap = (Map<Class, ModelBase>)f_modelsCore.get(null);
				for(Map.Entry<Class, ModelBase> e : modelMap.entrySet())
					if(e.getValue().getClass().getName().endsWith("ModelLowSidesMinecart"))
						e.setValue(new ModelShaderLowSidesMinecart());

				ShaderCaseMinecart.invalidMinecartClasses.add((Class<? extends EntityMinecart>)Class.forName("mods.railcraft.common.carts.EntityLocomotive"));
				ShaderCaseMinecart.invalidMinecartClasses.add((Class<? extends EntityMinecart>)Class.forName("mods.railcraft.common.carts.EntityTunnelBore"));
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}

	static Class c_TileSlab;
	static Method c_isDoubleSlab;
	static Method c_getBottomSlab;
	public static boolean isRCSlab(TileEntity tile)
	{
		if(c_TileSlab==null)
		{
			try{
				c_TileSlab = Class.forName("mods.railcraft.common.blocks.aesthetics.slab.TileSlab");
				c_isDoubleSlab = c_TileSlab.getMethod("isDoubleSlab");
				c_getBottomSlab = c_TileSlab.getMethod("getBottomSlab");
			}catch(Exception e){}
		}
		if(c_TileSlab!=null&&c_isDoubleSlab!=null&&c_getBottomSlab!=null)
			return tile!=null && c_TileSlab.isAssignableFrom(tile.getClass());
		return false;
	}
	public static boolean isSteelSlab(TileEntity tile)
	{
		try{
			return (!(boolean)c_isDoubleSlab.invoke(tile) && c_getBottomSlab.invoke(tile).toString()=="STEEL");
		}catch(Exception e){}
		return false;
	}

	public static IRockCrusherCraftingManager getRockCrusherCraftingManager() {
		if (Loader.isModLoaded(RAILCRAFT_MODID)) {
			return RailcraftCraftingManager.rockCrusher;
		}
		return null;
	}

	@SideOnly(Side.CLIENT)
	public class ModelShaderLowSidesMinecart extends ModelShaderMinecart
	{
		public ModelShaderLowSidesMinecart()
		{
			super(new ModelMinecart());
			byte length = 20;
			byte width = 16;
			byte yOffset = 4;
			byte height = 6;
			this.sideModels[3] = new ModelRenderer(this, 0, 0);
			this.sideModels[4] = new ModelRenderer(this, 0, 0);
			this.sideModels[4].mirror = true;
			this.sideModels[3].addBox(-length/2+2, -height-1, -1, length-4, height, 2, 0);
			this.sideModels[3].setRotationPoint(0, yOffset, -width/2+1);
			this.sideModels[4].addBox(-length/2+2, -height-1, -1, length-4, height, 2, 0);
			this.sideModels[4].setRotationPoint(0, yOffset, width/2-1);
			this.sideModels[3].rotateAngleY = (float)Math.PI;
			this.sideModelsMirrored[3] = new ModelRenderer(this, 0, 0);
			this.sideModelsMirrored[4] = new ModelRenderer(this, 0, 0);
			this.sideModelsMirrored[4].mirror = true;
			this.sideModelsMirrored[3].addBox(-length/2+2, -height-1, -1, length-4, height, 2, 0);
			this.sideModelsMirrored[3].setRotationPoint(0, yOffset, -width/2+1);
			this.sideModelsMirrored[4].addBox(-length/2+2, -height-1, -1, length-4, height, 2, 0);
			this.sideModelsMirrored[4].setRotationPoint(0, yOffset, width/2-1);
			this.sideModelsMirrored[3].rotateAngleY = (float)Math.PI;
		}


		@Override
		public void render(Entity entity, float f0, float f1, float f2, float f3, float f4, float f5)
		{
			super.render(entity, f0, f1, f2, f3, f4, f5);
		}
	}
}