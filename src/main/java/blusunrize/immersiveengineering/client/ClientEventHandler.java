package blusunrize.immersiveengineering.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.lwjgl.opengl.GL11;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.api.AdvancedAABB;
import blusunrize.immersiveengineering.api.crafting.BlastFurnaceRecipe;
import blusunrize.immersiveengineering.api.energy.IImmersiveConnectable;
import blusunrize.immersiveengineering.api.energy.ImmersiveNetHandler;
import blusunrize.immersiveengineering.api.energy.ImmersiveNetHandler.Connection;
import blusunrize.immersiveengineering.api.energy.WireType;
import blusunrize.immersiveengineering.api.shader.ShaderCase;
import blusunrize.immersiveengineering.api.shader.ShaderRegistry;
import blusunrize.immersiveengineering.api.tool.IDrillHead;
import blusunrize.immersiveengineering.api.tool.ZoomHandler;
import blusunrize.immersiveengineering.api.tool.ZoomHandler.IZoomTool;
import blusunrize.immersiveengineering.client.fx.ParticleRenderer;
import blusunrize.immersiveengineering.client.gui.GuiBlastFurnace;
import blusunrize.immersiveengineering.client.models.ModelIEObj;
import blusunrize.immersiveengineering.client.render.TileRenderArcFurnace;
import blusunrize.immersiveengineering.common.Config;
import blusunrize.immersiveengineering.common.IEContent;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IBlockOverlayText;
import blusunrize.immersiveengineering.common.gui.ContainerRevolver;
import blusunrize.immersiveengineering.common.items.ItemChemthrower;
import blusunrize.immersiveengineering.common.items.ItemDrill;
import blusunrize.immersiveengineering.common.items.ItemRevolver;
import blusunrize.immersiveengineering.common.items.ItemSkyhook;
import blusunrize.immersiveengineering.common.util.IELogger;
import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import blusunrize.immersiveengineering.common.util.Lib;
import blusunrize.immersiveengineering.common.util.SkylineHelper;
import blusunrize.immersiveengineering.common.util.Utils;
import blusunrize.immersiveengineering.common.util.compat.GregTechHelper;
import blusunrize.immersiveengineering.common.util.network.MessageRequestBlockUpdate;
import cofh.api.energy.IEnergyReceiver;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelVillager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.obj.Face;
import net.minecraftforge.client.model.obj.GroupObject;
import net.minecraftforge.client.model.obj.TextureCoordinate;
import net.minecraftforge.client.model.obj.WavefrontObject;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;
import net.minecraftforge.oredict.OreDictionary;

public class ClientEventHandler
{
	public static IIcon iconItemBlank;
	public static int itemSheetWidth;
	public static int itemSheetHeight;

	@SubscribeEvent()
	public void textureStich(TextureStitchEvent.Pre event)
	{
		if(event.map.getTextureType()==0)
		{
			if(IEContent.IECreosote)
				IEContent.fluidCreosote.setIcons(event.map.registerIcon("immersiveengineering:fluid/creosote_still"), event.map.registerIcon("immersiveengineering:fluid/creosote_flow"));
			if(IEContent.IEPlantoil)
				IEContent.fluidPlantoil.setIcons(event.map.registerIcon("immersiveengineering:fluid/plantoil_still"), event.map.registerIcon("immersiveengineering:fluid/plantoil_flow"));
			if(IEContent.IEEthanol)
				IEContent.fluidEthanol.setIcons(event.map.registerIcon("immersiveengineering:fluid/ethanol_still"), event.map.registerIcon("immersiveengineering:fluid/ethanol_flow"));
			if(IEContent.IEBiodiesel)
				IEContent.fluidBiodiesel.setIcons(event.map.registerIcon("immersiveengineering:fluid/biodiesel_still"), event.map.registerIcon("immersiveengineering:fluid/biodiesel_flow"));
			WireType.iconDefaultWire = event.map.registerIcon("immersiveengineering:wire");
			TileRenderArcFurnace.hotMetal_flow = event.map.registerIcon("immersiveengineering:fluid/hotMetal_flow");
			TileRenderArcFurnace.hotMetal_still = event.map.registerIcon("immersiveengineering:fluid/hotMetal_still");
		}
		if(event.map.getTextureType()==1)
		{
			iconItemBlank = event.map.registerIcon("immersiveengineering:white");
		}
		if(event.map.getTextureType()==Config.getInt("revolverSheetID"))
		{
			IELogger.info("Stitching Revolver Textures!");
			((ItemRevolver)IEContent.itemRevolver).stichRevolverTextures(event.map);
		}
		for(ShaderRegistry.ShaderRegistryEntry entry : ShaderRegistry.shaderRegistry.values())
			for(ShaderCase sCase : entry.getCases())
				sCase.stichTextures(event.map, event.map.getTextureType());
	}
	@SubscribeEvent()
	public void textureStich(TextureStitchEvent.Post event)
	{
		if(event.map.getTextureType()==0)
			for(ModelIEObj modelIE : ModelIEObj.existingStaticRenders)
			{
				WavefrontObject model = modelIE.rebindModel();
				rebindUVsToIcon(model, modelIE);
			}
		if(event.map.getTextureType()==1)
		{
			itemSheetWidth = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
			itemSheetHeight = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
		}
		//
		//		if(event.map.getTextureType()==Config.getInt("revolverSheetID"))
		//		{
		//			try {
		//				IELogger.debug("TEST-udafe");
		//				TextureAtlasSprite tal = (TextureAtlasSprite)event.map.registerIcon("immersiveengineering:revolver");
		//				URL url = new URL("http://i.imgur.com/bU3bEDe.png");
		//				BufferedImage img = ImageIO.read(url);
		//				IELogger.debug("url = "+url);
		//				IELogger.debug("img = "+img);
		//				IELogger.debug("Loading sprite");
		//				tal.loadSprite(new BufferedImage[]{img}, null, false);
		//				IELogger.debug("sprite loaded");
		//				((ItemRevolver)IEContent.itemRevolver).revolverDefaultTexture=tal;
		//			} catch (Exception e) {
		//				e.printStackTrace();
		//			}
		//		}
	}

	void rebindUVsToIcon(WavefrontObject model, ModelIEObj modelIE)
	{
		for(GroupObject groupObject : model.groupObjects)
		{
			IIcon icon = modelIE.getBlockIcon(groupObject.name);
			if(icon==null)
				continue;
			float minU = icon.getInterpolatedU(0);
			float sizeU = icon.getInterpolatedU(16) - minU;
			float minV = icon.getInterpolatedV(0);
			float sizeV = icon.getInterpolatedV(16) - minV;
			float baseOffsetU = (16f/icon.getIconWidth())*.0005F;
			float baseOffsetV = (16f/icon.getIconHeight())*.0005F;
			for(Face face : groupObject.faces)
			{
				float averageU = 0F;
				float averageV = 0F;
				if(face.textureCoordinates!=null && face.textureCoordinates.length>0)
				{
					for(int i=0; i<face.textureCoordinates.length; ++i)
					{
						averageU += face.textureCoordinates[i].u;
						averageV += face.textureCoordinates[i].v;
					}
					averageU = averageU / face.textureCoordinates.length;
					averageV = averageV / face.textureCoordinates.length;
				}

				for (int i=0; i<face.vertices.length; ++i)
				{
					float offsetU, offsetV;
					TextureCoordinate textureCoordinate = face.textureCoordinates[i];
					offsetU = baseOffsetU;
					offsetV = baseOffsetV;
					if (face.textureCoordinates[i].u > averageU)
						offsetU = -offsetU;
					if (face.textureCoordinates[i].v > averageV)
						offsetV = -offsetV;

					face.textureCoordinates[i] = new TextureCoordinate(
							minU + sizeU * (textureCoordinate.u+offsetU),
							minV + sizeV * (textureCoordinate.v+offsetV)
							);
				}
			}
		}
	}

	public static Set<Connection> skyhookGrabableConnections = new HashSet();
	@SubscribeEvent
	public void onPlayerTick(TickEvent.PlayerTickEvent event)
	{
		if(event.side.isClient() && event.phase==TickEvent.Phase.START && event.player==ClientUtils.mc().renderViewEntity)
		{
			skyhookGrabableConnections.clear();
			EntityPlayer player = event.player;
			ItemStack stack = player.getCurrentEquippedItem();
			if(stack!=null && stack.getItem() instanceof ItemSkyhook)
			{
				TileEntity connector = null;
				double lastDist = 0;
				Connection line = null;
				double py = player.posY+player.getEyeHeight();
				for(int xx=-2; xx<=2; xx++)
					for(int zz=-2; zz<=2; zz++)
						for(int yy=0; yy<=3; yy++)
						{
							TileEntity tile = player.worldObj.getTileEntity((int)player.posX+xx, (int)py+yy, (int)player.posZ+zz);
							if(tile!=null)
							{
								Connection con = SkylineHelper.getTargetConnection(player.worldObj, tile.xCoord,tile.yCoord,tile.zCoord, player, null);
								if(con!=null)
								{
									double d = tile.getDistanceFrom(player.posX,py,player.posZ);
									if(connector==null || d<lastDist)
									{
										connector=tile;
										lastDist=d;
										line=con;
									}
								}
							}
						}
				if(line!=null&&connector!=null)
					skyhookGrabableConnections.add(line);
			}
		}
		if(event.side.isClient() && event.phase == TickEvent.Phase.END && event.player!=null)
		{
			EntityPlayer player = event.player;
			ItemStack stack = player.getCurrentEquippedItem();
			boolean twohanded = stack!=null && (stack.getItem() instanceof ItemDrill);
			if(twohanded && (player!=ClientUtils.mc().renderViewEntity||ClientUtils.mc().gameSettings.thirdPersonView!=0))
			{
				if(player.getItemInUseCount() <= 0)
				{
					player.clearItemInUse();
					player.setItemInUse(stack, Integer.MAX_VALUE);
				}
			}
			if (!Minecraft.getMinecraft().isIntegratedServerRunning()&&ItemDrill.animationTimer!=null&&ItemDrill.animationTimer.containsKey(player.getCommandSenderName()))
			{
				synchronized (ItemDrill.animationTimer)
				{
					Integer timer = ItemDrill.animationTimer.get(player.getCommandSenderName());
					timer--;
					if (timer < 18&&timer>15)
						timer = 20;
					if (timer>0)
						ItemDrill.animationTimer.put(player.getCommandSenderName(), timer);
					else
						ItemDrill.animationTimer.remove(player.getCommandSenderName());
				}
			}
		}
	}

	@SubscribeEvent
	public void onItemTooltip(ItemTooltipEvent event)
	{
		if(FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT
				&& ClientUtils.mc().currentScreen != null
				&& ClientUtils.mc().currentScreen instanceof GuiBlastFurnace
				&& BlastFurnaceRecipe.isValidBlastFuel(event.itemStack))
			event.toolTip.add(EnumChatFormatting.GRAY+StatCollector.translateToLocalFormatted("desc.ImmersiveEngineering.info.blastFuelTime", BlastFurnaceRecipe.getBlastFuelTime(event.itemStack)));
		//		for(int oid : OreDictionary.getOreIDs(event.itemStack))
		//			event.toolTip.add(OreDictionary.getOreName(oid));
	}

	@SubscribeEvent()
	public void lastWorldRender(RenderWorldLastEvent event)
	{
		connectionsRendered = false;
		ParticleRenderer.dispatch();
	}
	static boolean connectionsRendered = false;
	public static void renderAllIEConnections(float partial)
	{
		if(connectionsRendered)
			return;
		GL11.glPushMatrix();

		GL11.glDisable(GL11.GL_CULL_FACE);
		//		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		OpenGlHelper.glBlendFunc(770, 771, 1, 0);
		GL11.glShadeModel(GL11.GL_SMOOTH);
		RenderHelper.enableStandardItemLighting();

		Tessellator.instance.startDrawing(GL11.GL_QUADS);

		EntityLivingBase viewer = ClientUtils.mc().renderViewEntity;
		double dx = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * partial;//(double)event.partialTicks;
		double dy = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * partial;//(double)event.partialTicks;
		double dz = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * partial;//(double)event.partialTicks;

		for(Object o : ClientUtils.mc().renderGlobal.tileEntities)
			if(o instanceof IImmersiveConnectable)
			{
				TileEntity tile = (TileEntity)o;
				//				int lb = tile.getWorldObj().getLightBrightnessForSkyBlocks(tile.xCoord, tile.yCoord, tile.zCoord, 0);
				//				int lb_j = lb % 65536;
				//				int lb_k = lb / 65536;
				//				OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float)lb_j / 1.0F, (float)lb_k / 1.0F);


				Tessellator.instance.setTranslation(tile.xCoord-dx, tile.yCoord-dy, tile.zCoord-dz);
				//				GL11.glTranslated((tile.xCoord+.5-dx), (tile.yCoord+.5-dy), (tile.zCoord+.5-dz));
				ClientUtils.renderAttachedConnections((TileEntity)tile);
				//				GL11.glTranslated(-(tile.xCoord+.5-dx), -(tile.yCoord+.5-dy), -(tile.zCoord+.5-dz));

			}

		Iterator<ImmersiveNetHandler.Connection> it = skyhookGrabableConnections.iterator();
		World world = viewer.worldObj;
		while(it.hasNext())
		{
			ImmersiveNetHandler.Connection con = it.next();
			Tessellator.instance.setTranslation(con.start.posX-dx, con.start.posY-dy, con.start.posZ-dz);
			double r = con.cableType.getRenderDiameter()/2;
			ClientUtils.drawConnection(con, Utils.toIIC(con.start, world), Utils.toIIC(con.end, world),   0x00ff99,128,r*1.75, con.cableType.getIcon(con));
		}

		Tessellator.instance.setTranslation(0,0,0);
		Tessellator.instance.draw();

		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_CULL_FACE);

		GL11.glPopMatrix();
		connectionsRendered = true;
	}

	@SubscribeEvent()
	public void onRenderOverlayPre(RenderGameOverlayEvent.Pre event)
	{
		if(ZoomHandler.isZooming && event.type==RenderGameOverlayEvent.ElementType.CROSSHAIRS)
		{
			event.setCanceled(true);
			if(ZoomHandler.isZooming)
			{
				ClientUtils.bindTexture("immersiveengineering:textures/gui/scope.png");
				int width = event.resolution.getScaledWidth();
				int height = event.resolution.getScaledHeight();
				int resMin = Math.min(width,height);
				float offsetX = (width-resMin)/2f;
				float offsetY = (height-resMin)/2f;

				if(resMin==width)
				{
					ClientUtils.drawColouredRect(0,0, width,(int)offsetY+1, 0xff000000);
					ClientUtils.drawColouredRect(0,(int)offsetY+resMin, width,(int)offsetY+1, 0xff000000);
				}
				else
				{
					ClientUtils.drawColouredRect(0,0, (int)offsetX+1,height, 0xff000000);
					ClientUtils.drawColouredRect((int)offsetX+resMin,0, (int)offsetX+1,height, 0xff000000);
				}
				GL11.glEnable(GL11.GL_BLEND);
				OpenGlHelper.glBlendFunc(770, 771, 1, 0);

				GL11.glTranslatef(offsetX,offsetY,0);
				ClientUtils.drawTexturedRect(0,0,resMin,resMin, 0f,1f,0f,1f);

				ClientUtils.bindTexture("immersiveengineering:textures/gui/hudElements.png");
				ClientUtils.drawTexturedRect(218/256f*resMin,64/256f*resMin, 24/256f*resMin,128/256f*resMin, 64/256f,88/256f,96/256f,224/256f);
				ItemStack equipped = ClientUtils.mc().thePlayer.getCurrentEquippedItem();
				if(equipped!=null && equipped.getItem() instanceof IZoomTool)
				{
					IZoomTool tool = (IZoomTool)equipped.getItem();
					float[] steps = tool.getZoomSteps(equipped, ClientUtils.mc().thePlayer);
					if(steps!=null && steps.length>1)
					{
						int curStep = -1;
						float dist=0;

						float totalOffset = 0;
						float stepLength = 118/(float)steps.length;
						float stepOffset = (stepLength-7)/2f;
						GL11.glTranslatef(223/256f*resMin,64/256f*resMin, 0);
						GL11.glTranslatef(0,(5+stepOffset)/256*resMin,0);
						for(int i=0; i<steps.length; i++)
						{
							ClientUtils.drawTexturedRect(0,0, 8/256f*resMin,7/256f*resMin, 88/256f,96/256f,96/256f,103/256f);
							GL11.glTranslatef(0,stepLength/256*resMin,0);
							totalOffset += stepLength;

							if(curStep==-1 || Math.abs(steps[i]-ZoomHandler.fovZoom)<dist)
							{
								curStep = i;
								dist = Math.abs(steps[i]-ZoomHandler.fovZoom);
							}
						}
						GL11.glTranslatef(0,-totalOffset/256*resMin,0);

						if(curStep>=0 && curStep<steps.length)
						{
							GL11.glTranslatef(6/256f*resMin,curStep*stepLength/256*resMin,0);
							ClientUtils.drawTexturedRect(0,0, 8/256f*resMin,7/256f*resMin, 88/256f,98/256f,103/256f,110/256f);
							ClientUtils.font().drawString((1/steps[curStep])+"x", (int)(16/256f*resMin),0, 0xffffff);
							GL11.glTranslatef(-6/256f*resMin,-curStep*stepLength/256*resMin,0);
						}
						GL11.glTranslatef(0,-((5+stepOffset)/256*resMin),0);
						GL11.glTranslatef(-223/256f*resMin,-64/256f*resMin, 0);
					}
				}	

				GL11.glTranslatef(-offsetX,-offsetY,0);
			}
		}
	}

	@SubscribeEvent()
	public void onRenderOverlayPost(RenderGameOverlayEvent.Post event)
	{
		if(ClientUtils.mc().thePlayer!=null && event.type == RenderGameOverlayEvent.ElementType.TEXT)
		{
			EntityPlayer player = ClientUtils.mc().thePlayer;
			if(player.getCurrentEquippedItem()!=null)
			{
				ItemStack equipped = player.getCurrentEquippedItem();
				if(OreDictionary.itemMatches(new ItemStack(IEContent.itemTool,1,2), equipped, false) || OreDictionary.itemMatches(new ItemStack(IEContent.itemWireCoil,1,OreDictionary.WILDCARD_VALUE), equipped, false) )
				{
					if(ItemNBTHelper.hasKey(equipped, "linkingPos"))
					{
						int[] link = ItemNBTHelper.getIntArray(equipped, "linkingPos");
						if(link!=null&&link.length>3)
						{
							String s = StatCollector.translateToLocalFormatted(Lib.DESC_INFO+"attachedTo", link[1],link[2],link[3]);
							ClientUtils.font().drawString(s, event.resolution.getScaledWidth()/2 - ClientUtils.font().getStringWidth(s)/2, event.resolution.getScaledHeight()-GuiIngameForge.left_height-10, WireType.ELECTRUM.getColour(null), true);
						}
					}
				}
				else if(equipped.getItem() instanceof ItemRevolver && equipped.getItemDamage()!=2)
				{
					ClientUtils.bindTexture("immersiveengineering:textures/gui/revolver.png");
					ItemStack[] bullets = ((ItemRevolver)equipped.getItem()).getBullets(equipped);
					int bulletAmount = bullets.length;
					float dx = event.resolution.getScaledWidth()-32-48;
					float dy = event.resolution.getScaledHeight()-64;
					GL11.glPushMatrix();
					GL11.glEnable(GL11.GL_BLEND);
					GL11.glTranslated(dx, dy, 0);
					GL11.glScalef(.5f, .5f, 1);

					ClientUtils.drawTexturedRect(0,1,74,74, 0/256f,74/256f, 51/256f,125/256f);
					if(bulletAmount>=18)
						ClientUtils.drawTexturedRect(47,1,103,74, 74/256f,177/256f, 51/256f,125/256f);
					else if(bulletAmount>8)
						ClientUtils.drawTexturedRect(57,1,79,39, 57/256f,136/256f, 12/256f,51/256f);

					RenderItem ir = RenderItem.getInstance();
					int[][] slots = ContainerRevolver.slotPositions[bulletAmount>=18?2: bulletAmount>8?1: 0];
					for(int i=0; i<bulletAmount; i++)
					{
						if(bullets[i]!=null)
						{
							int x = 0; 
							int y = 0;
							if(i==0)
							{
								x = 29;
								y = 3;
							}
							else if(i-1<slots.length)
							{
								x = slots[i-1][0];
								y = slots[i-1][1];
							}
							else
							{
								int ii = i-(slots.length+1);
								x = ii==0?48: ii==1?29: ii==3?2: 10;
								y = ii==1?57: ii==3?30: ii==4?11: 49;
							}

							ir.renderItemIntoGUI(ClientUtils.mc().fontRenderer, ClientUtils.mc().renderEngine, bullets[i], x,y);
						}
					}
					RenderHelper.disableStandardItemLighting();
					GL11.glDisable(GL11.GL_BLEND);
					GL11.glPopMatrix();
				}
				else if((equipped.getItem() instanceof ItemDrill && equipped.getItemDamage()==0)
						||equipped.getItem() instanceof ItemChemthrower)
				{
					boolean drill = equipped.getItem() instanceof ItemDrill;
					ClientUtils.bindTexture("immersiveengineering:textures/gui/hudElements.png");
					GL11.glColor4f(1, 1, 1, 1);
					float dx = event.resolution.getScaledWidth()-16;
					float dy = event.resolution.getScaledHeight();
					GL11.glPushMatrix();
					GL11.glTranslated(dx, dy, 0);
					int w = 31;
					int h = 62;
					double uMin = 179/256f;
					double uMax = 210/256f;
					double vMin = 9/256f;
					double vMax = 71/256f;
					ClientUtils.drawTexturedRect(-24,-68, w,h, uMin,uMax,vMin,vMax);

					GL11.glTranslated(-23,-37,0);
					FluidStack fuel = ((IFluidContainerItem)equipped.getItem()).getFluid(equipped);
					int amount = fuel!=null?fuel.amount:0;
					if(!drill && player.isUsingItem())
						amount -= player.getItemInUseDuration()*Config.getInt("chemthrower_consumption");
					float cap = (float)((IFluidContainerItem)equipped.getItem()).getCapacity(equipped);
					float angle = 83-(166* amount/cap);
					GL11.glRotatef(angle, 0, 0, 1);
					ClientUtils.drawTexturedRect(6,-2, 24,4, 91/256f,123/256f, 80/256f,87/256f);
					GL11.glRotatef(-angle, 0, 0, 1);
					//					for(int i=0; i<=8; i++)
					//					{
					//						float angle = 83-(166/8f)*i;
					//						GL11.glRotatef(angle, 0, 0, 1);
					//						ClientUtils.drawTexturedRect(6,-2, 24,4, 91/256f,123/256f, 80/96f,87/96f);
					//						GL11.glRotatef(-angle, 0, 0, 1);
					//					}
					GL11.glTranslated(23,37,0);
					if(drill)
					{
						ClientUtils.drawTexturedRect(-54,-73, 66,72, 108/256f,174/256f, 4/256f,76/256f);
						RenderItem ir = RenderItem.getInstance();
						ItemStack head = ((ItemDrill)equipped.getItem()).getHead(equipped);
						if(head!=null)
						{
							ir.renderItemIntoGUI(ClientUtils.mc().fontRenderer, ClientUtils.mc().renderEngine, head, -51,-45);
							ir.renderItemOverlayIntoGUI(ClientUtils.font(), ClientUtils.mc().renderEngine, head, -51,-45);
							RenderHelper.disableStandardItemLighting();
						}
					}
					else
					{
						ClientUtils.drawTexturedRect(-41,-73, 53,72, 8/256f,61/256f, 4/256f,76/256f);
						boolean ignite = ItemNBTHelper.getBoolean(equipped, "ignite");
						ClientUtils.drawTexturedRect(-32,-43, 12,12, 66/256f,78/256f, (ignite?21:9)/256f,(ignite?33:21)/256f);

					}
					GL11.glPopMatrix();
				}
				//				else if(equipped.getItem() instanceof ItemRailgun)
				//				{
				//					float dx = event.resolution.getScaledWidth()-32-48;
				//					float dy = event.resolution.getScaledHeight()-40;
				//					ClientUtils.bindTexture("immersiveengineering:textures/gui/hudElements.png");
				//					GL11.glColor4f(1, 1, 1, 1);
				//					GL11.glPushMatrix();
				//					GL11.glEnable(GL11.GL_BLEND);
				//					GL11.glTranslated(dx, dy, 0);
				//
				//					int duration = player.getItemInUseDuration();
				//					int chargeTime = ((ItemRailgun)equipped.getItem()).getChargeTime(equipped);
				//					int chargeLevel = Math.min(99, (int)(duration/(float)chargeTime*100));
				//					//					ClientUtils.drawTexturedRect(0,0, 64,32, 0/256f,64/256f, 96/256f,128/256f);
				//
				//					GL11.glScalef(1.5f,1.5f,1.5f);
				//					int col = Config.getBoolean("nixietubeFont")?Lib.colour_nixieTubeText:0xffffff;
				//					ClientProxy.nixieFont.setDrawTubeFlag(false);
				//					//					ClientProxy.nixieFont.drawString((chargeLevel<10?"0"+chargeLevel:""+chargeLevel), 19,3, col);
				//					ClientProxy.nixieFont.setDrawTubeFlag(true);
				//
				//					GL11.glPopMatrix();
				//				}

				MovingObjectPosition mop = ClientUtils.mc().objectMouseOver;
				if(mop!=null)
				{
					TileEntity tileEntity = player.worldObj.getTileEntity(mop.blockX, mop.blockY, mop.blockZ);
					if(OreDictionary.itemMatches(new ItemStack(IEContent.itemTool,1,2), equipped, true))
					{
						int col = Config.getBoolean("nixietubeFont")?Lib.colour_nixieTubeText:0xffffff;
						String[] text = null;
						if(tileEntity instanceof IEnergyReceiver)
						{
							ForgeDirection fd = ForgeDirection.getOrientation(mop.sideHit);
							int maxStorage = ((IEnergyReceiver)tileEntity).getMaxEnergyStored(fd);
							int storage = ((IEnergyReceiver)tileEntity).getEnergyStored(fd);
							if(maxStorage>0)
								text = StatCollector.translateToLocalFormatted(Lib.DESC_INFO+"energyStored","<br>"+Utils.toScientificNotation(storage,"0##",100000)+" / "+Utils.toScientificNotation(maxStorage,"0##",100000)).split("<br>");
						}
						else if(Lib.GREG && GregTechHelper.gregtech_isValidEnergyOutput(tileEntity))
						{
							String gregStored = GregTechHelper.gregtech_getEnergyStored(tileEntity);
							if(gregStored!=null)
								text = StatCollector.translateToLocalFormatted(Lib.DESC_INFO+"energyStored","<br>"+gregStored).split("<br>");
						}
						else if(mop.entityHit instanceof IEnergyReceiver)
						{
							int maxStorage = ((IEnergyReceiver)mop.entityHit).getMaxEnergyStored(ForgeDirection.UNKNOWN);
							int storage = ((IEnergyReceiver)mop.entityHit).getEnergyStored(ForgeDirection.UNKNOWN);
							if(maxStorage>0)
								text = StatCollector.translateToLocalFormatted(Lib.DESC_INFO+"energyStored","<br>"+Utils.toScientificNotation(storage,"0##",100000)+" / "+Utils.toScientificNotation(maxStorage,"0##",100000)).split("<br>");
						}
						if(text!=null)
						{
							if (player.worldObj.getTotalWorldTime()%20==0)
							{
								ImmersiveEngineering.packetHandler.sendToServer(new MessageRequestBlockUpdate(mop.blockX, mop.blockY, mop.blockZ, player.dimension));
							}
							int i = 0;
							for(String s : text)
								if(s!=null)
								{
									int w = ClientProxy.nixieFontOptional.getStringWidth(s);
									ClientProxy.nixieFontOptional.drawString(s, event.resolution.getScaledWidth()/2-w/2, event.resolution.getScaledHeight()/2-4-text.length*(ClientProxy.nixieFontOptional.FONT_HEIGHT+2)+(i++)*(ClientProxy.nixieFontOptional.FONT_HEIGHT+2), col, true);
								}
						}
					}
				}
			}
			if(ClientUtils.mc().objectMouseOver!=null)
			{
				boolean hammer = player.getCurrentEquippedItem()!=null?Utils.isHammer(player.getCurrentEquippedItem()): false;
				MovingObjectPosition mop = ClientUtils.mc().objectMouseOver;
				TileEntity tileEntity = player.worldObj.getTileEntity(mop.blockX, mop.blockY, mop.blockZ);
				if(tileEntity instanceof IBlockOverlayText)
				{
					IBlockOverlayText overlayBlock = (IBlockOverlayText) tileEntity;
					String[] text = overlayBlock.getOverlayText(ClientUtils.mc().thePlayer, mop, hammer);
					boolean useNixie = overlayBlock.useNixieFont(ClientUtils.mc().thePlayer, mop);
					if(text!=null && text.length>0)
					{
						FontRenderer font = useNixie?ClientProxy.nixieFontOptional:ClientUtils.font();
						int col = (useNixie&&Config.getBoolean("nixietubeFont"))?Lib.colour_nixieTubeText:0xffffff;
						int i = 0;
						for(String s : text)
							if(s!=null)
								font.drawString(s, event.resolution.getScaledWidth()/2+8, event.resolution.getScaledHeight()/2+8+(i++)*font.FONT_HEIGHT, col, true);
					}
				}
			}
		}
	}

	@SubscribeEvent()
	public void onFOVUpdate(FOVUpdateEvent event)
	{
		EntityPlayer player = ClientUtils.mc().thePlayer;
		if(player.getCurrentEquippedItem()!=null && player.getCurrentEquippedItem().getItem() instanceof IZoomTool)
		{
			if(player.isSneaking() && player.onGround)
			{
				ItemStack equipped = player.getCurrentEquippedItem();
				IZoomTool tool = (IZoomTool)equipped.getItem();
				if(tool.canZoom(equipped, player))
				{
					if(!ZoomHandler.isZooming)
					{
						float[] steps = tool.getZoomSteps(equipped, player);
						if(steps!=null && steps.length>0)
						{
							int curStep = -1;
							float dist=0;
							for(int i=0; i<steps.length; i++)
								if(curStep==-1 || Math.abs(steps[i]-ZoomHandler.fovZoom)<dist)
								{
									curStep = i;
									dist = Math.abs(steps[i]-ZoomHandler.fovZoom);
								}
							if(curStep!=-1)
								ZoomHandler.fovZoom = steps[curStep];
							else 
								ZoomHandler.fovZoom = event.fov;
						}
						ZoomHandler.isZooming = true;
					}
					event.newfov = ZoomHandler.fovZoom;
				}
				else if(ZoomHandler.isZooming)
					ZoomHandler.isZooming = false;
			}
			else if(ZoomHandler.isZooming)
				ZoomHandler.isZooming = false;
		}
		else if(ZoomHandler.isZooming)
			ZoomHandler.isZooming = false;
	}
	@SubscribeEvent
	public void onMouseEvent(MouseEvent event)
	{
		if(event.dwheel != 0)
		{
			EntityPlayer player = ClientUtils.mc().thePlayer;
			if(player.getCurrentEquippedItem()!=null && player.getCurrentEquippedItem().getItem() instanceof IZoomTool && player.isSneaking())
			{
				ItemStack equipped = player.getCurrentEquippedItem();
				IZoomTool tool = (IZoomTool)equipped.getItem();
				if(tool.canZoom(equipped, player))
				{
					float[] steps = tool.getZoomSteps(equipped, player);
					if(steps!=null && steps.length>0)
					{
						int curStep = -1;
						float dist=0;
						for(int i=0; i<steps.length; i++)
							if(curStep==-1 || Math.abs(steps[i]-ZoomHandler.fovZoom)<dist)
							{
								curStep = i;
								dist = Math.abs(steps[i]-ZoomHandler.fovZoom);
							}
						if(curStep!=-1)
						{
							int newStep = curStep+(event.dwheel>0?-1:1);
							if(newStep>=0 && newStep<steps.length)
								ZoomHandler.fovZoom = steps[newStep];
							event.setCanceled(true);
						}
					}
				}
			}
		}
	}


	@SubscribeEvent()
	public void renderAdditionalBlockBounds(DrawBlockHighlightEvent event)
	{
		if(event.subID==0 && event.target.typeOfHit==MovingObjectPosition.MovingObjectType.BLOCK)
		{	
			float f1 = 0.002F;
			double d0 = event.player.lastTickPosX + (event.player.posX - event.player.lastTickPosX) * (double)event.partialTicks;
			double d1 = event.player.lastTickPosY + (event.player.posY - event.player.lastTickPosY) * (double)event.partialTicks;
			double d2 = event.player.lastTickPosZ + (event.player.posZ - event.player.lastTickPosZ) * (double)event.partialTicks;
			if(event.player.worldObj.getBlock(event.target.blockX,event.target.blockY,event.target.blockZ) instanceof IEBlockInterfaces.ICustomBoundingboxes)
			{
				ChunkCoordinates cc = new ChunkCoordinates(event.target.blockX,event.target.blockY,event.target.blockZ);
				IEBlockInterfaces.ICustomBoundingboxes block = (IEBlockInterfaces.ICustomBoundingboxes) event.player.worldObj.getBlock(event.target.blockX,event.target.blockY,event.target.blockZ);
				ArrayList<AxisAlignedBB> set = block.addCustomSelectionBoxesToList(event.player.worldObj, cc.posX,cc.posY,cc.posZ);
				if(!set.isEmpty())
				{
					GL11.glEnable(GL11.GL_BLEND);
					OpenGlHelper.glBlendFunc(770, 771, 1, 0);
					GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.4F);
					GL11.glLineWidth(2.0F);
					GL11.glDisable(GL11.GL_TEXTURE_2D);
					GL11.glDepthMask(false);
					ArrayList<AxisAlignedBB> specialBoxes = new ArrayList<AxisAlignedBB>();
					AxisAlignedBB overrideBox = null; 
					for(AxisAlignedBB aabb : set)
						if(aabb!=null)
						{
							boolean b = block.addSpecifiedSubBox(event.player.worldObj, cc.posX,cc.posY,cc.posZ, event.player, aabb, event.target.hitVec, specialBoxes);
							if(b)
								overrideBox = specialBoxes.get(specialBoxes.size()-1);
						}

					if(overrideBox!=null)
						renderBoundingBox(overrideBox, cc.posX-d0,cc.posY-d1,cc.posZ-d2, f1);
					else
						for(AxisAlignedBB aabb : specialBoxes.isEmpty()?set:specialBoxes)
							if(aabb!=null)
								renderBoundingBox(aabb, cc.posX-d0,cc.posY-d1,cc.posZ-d2, f1);

					GL11.glDepthMask(true);
					GL11.glEnable(GL11.GL_TEXTURE_2D);
					GL11.glDisable(GL11.GL_BLEND);
					event.setCanceled(true);
				}
			}


			ItemStack stack = event.player.getCurrentEquippedItem();
			World world = event.player.worldObj;
			if(stack!=null && stack.getItem() instanceof ItemDrill && ((ItemDrill)stack.getItem()).isEffective(world.getBlock(event.target.blockX,event.target.blockY,event.target.blockZ).getMaterial()))
			{
				ItemStack head = ((ItemDrill)stack.getItem()).getHead(stack);
				if(head!=null)
				{
					int side = event.target.sideHit;
					int diameter = ((IDrillHead)head.getItem()).getMiningSize(head)+((ItemDrill)stack.getItem()).getUpgrades(stack).getInteger("size");
					int depth = ((IDrillHead)head.getItem()).getMiningDepth(head)+((ItemDrill)stack.getItem()).getUpgrades(stack).getInteger("depth");

					int startX=event.target.blockX;
					int startY=event.target.blockY;
					int startZ=event.target.blockZ;
					if(diameter%2==0)//even numbers
					{
						float hx = (float)event.target.hitVec.xCoord-event.target.blockX;
						float hy = (float)event.target.hitVec.yCoord-event.target.blockY;
						float hz = (float)event.target.hitVec.zCoord-event.target.blockZ;
						if((side<2&&hx<.5)||(side<4&&hx<.5))
							startX-= diameter/2;
						if(side>1&&hy<.5)
							startY-= diameter/2;
						if((side<2&&hz<.5)||(side>3&&hz<.5))
							startZ-= diameter/2;
					}
					else//odd numbers
					{
						startX-=(side==4||side==5?0: diameter/2);
						startY-=(side==0||side==1?0: diameter/2);
						startZ-=(side==2||side==3?0: diameter/2);
					}

					GL11.glColor4f(0.1F, 0.1F, 0.1F, 0.4F);
					GL11.glLineWidth(1F);
					GL11.glDisable(GL11.GL_TEXTURE_2D);

					//					AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(startX,startY,startZ, startX+(side==4||side==5?1:diameter),startY+(side==0||side==1?1:diameter),startZ+(side==2||side==3?1: diameter));
					//					RenderGlobal.drawOutlinedBoundingBox(aabb.expand((double)f1, (double)f1, (double)f1).getOffsetBoundingBox(-d0, -d1, -d2), -1);
					for(int dd=0; dd<depth; dd++)
						for(int dw=0; dw<diameter; dw++)
							for(int dh=0; dh<diameter; dh++)
							{
								int x = startX+ (side==4||side==5?dd: dw);
								int y = startY+ (side==0||side==1?dd: dh);
								int z = startZ+ (side==0||side==1?dh: side==4||side==5?dw: dd);
								Block block = event.player.worldObj.getBlock(x,y,z);
								if(block!=null && !block.isAir(world, x, y, z) && block.getPlayerRelativeBlockHardness(event.player, world, x, y, z) != 0)
								{
									if(!((ItemDrill)stack.getItem()).canBreakExtraBlock(world, block, x, y, z, world.getBlockMetadata(x,y,z), event.player, stack, head, false))
										continue;
									AxisAlignedBB aabb = block.getSelectedBoundingBoxFromPool(event.player.worldObj, x,y,z);
									if(aabb!=null)
									{
										RenderGlobal.drawOutlinedBoundingBox(aabb.expand((double)f1, (double)f1, (double)f1).getOffsetBoundingBox(-d0, -d1, -d2), -1);
									}
								}
							}
					GL11.glDepthMask(true);
					GL11.glEnable(GL11.GL_TEXTURE_2D);
					GL11.glDisable(GL11.GL_BLEND);
				}
			}

		}
	}

	static void renderBoundingBox(AxisAlignedBB aabb, double offsetX, double offsetY, double offsetZ, float expand)
	{
		if(aabb instanceof AdvancedAABB && ((AdvancedAABB)aabb).drawOverride!=null && ((AdvancedAABB)aabb).drawOverride.length>0)
		{
			double midX = aabb.minX+(aabb.maxX-aabb.minX)/2;
			double midY = aabb.minY+(aabb.maxY-aabb.minY)/2;
			double midZ = aabb.minZ+(aabb.maxZ-aabb.minZ)/2;
			ClientUtils.tes().addTranslation((float)offsetX, (float)offsetY, (float)offsetZ);
			for(Vec3[] face : ((AdvancedAABB)aabb).drawOverride)
			{
				ClientUtils.tes().startDrawing(GL11.GL_LINE_LOOP);
				for(Vec3 v : face)
					ClientUtils.tes().addVertex(v.xCoord+(v.xCoord<midX?-expand:expand),v.yCoord+(v.yCoord<midY?-expand:expand),v.zCoord+(v.zCoord<midZ?-expand:expand));
				ClientUtils.tes().draw();
			}
			ClientUtils.tes().addTranslation((float)-offsetX, (float)-offsetY, (float)-offsetZ);
		}
		else
			RenderGlobal.drawOutlinedBoundingBox(aabb.getOffsetBoundingBox(offsetX, offsetY, offsetZ).expand((double)expand, (double)expand, (double)expand), -1);
	}

	@SubscribeEvent()
	public void onClientDeath(LivingDeathEvent event)
	{
	}
	@SubscribeEvent()
	public void onRenderLivingPre(RenderLivingEvent.Pre event)
	{
		if(event.entity.getEntityData().hasKey("headshot"))
		{
			ModelBase model = event.renderer.mainModel;
			if(model instanceof ModelBiped)
				((ModelBiped)model).bipedHead.showModel=false;
			else if(model instanceof ModelVillager)
				((ModelVillager)model).villagerHead.showModel=false;
		}
		//		if(OreDictionary.itemMatches(new ItemStack(IEContent.itemRailgun),event.entity.getEquipmentInSlot(0),true))
		//		{
		//			ModelBase model = event.renderer.mainModel;
		//			if(model instanceof ModelBiped)
		//				((ModelBiped)model).bipedLeftArm.rotateAngleX=.9f;
		//		}
	}
	@SubscribeEvent()
	public void onRenderLivingPost(RenderLivingEvent.Post event)
	{
		if(event.entity.getEntityData().hasKey("headshot"))
		{
			ModelBase model = event.renderer.mainModel;
			if(model instanceof ModelBiped)
				((ModelBiped)model).bipedHead.showModel=true;
			else if(model instanceof ModelVillager)
				((ModelVillager)model).villagerHead.showModel=true;
		}
	}
}
