/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.client.render.tile;

import blusunrize.immersiveengineering.api.IEProperties.IEObjState;
import blusunrize.immersiveengineering.api.IEProperties.Model;
import blusunrize.immersiveengineering.api.IEProperties.VisibilityList;
import blusunrize.immersiveengineering.api.crafting.BlueprintCraftingRecipe;
import blusunrize.immersiveengineering.api.crafting.MultiblockRecipe;
import blusunrize.immersiveengineering.api.utils.client.SinglePropertyModelData;
import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.utils.IERenderTypes;
import blusunrize.immersiveengineering.client.utils.TransformingVertexBuilder;
import blusunrize.immersiveengineering.common.blocks.generic.PoweredMultiblockBlockEntity;
import blusunrize.immersiveengineering.common.blocks.generic.PoweredMultiblockBlockEntity.MultiblockProcess;
import blusunrize.immersiveengineering.common.blocks.generic.PoweredMultiblockBlockEntity.MultiblockProcessInWorld;
import blusunrize.immersiveengineering.common.blocks.metal.AutoWorkbenchBlockEntity;
import blusunrize.immersiveengineering.common.register.IEBlocks.Multiblocks;
import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import com.google.common.collect.HashMultimap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;

public class AutoWorkbenchRenderer extends IEBlockEntityRenderer<AutoWorkbenchBlockEntity>
{
	public static DynamicModel<Direction> DYNAMIC;

	@Override
	public void render(AutoWorkbenchBlockEntity te, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn)
	{
		if(!te.formed||te.isDummy()||!te.getLevelNonnull().hasChunkAt(te.getBlockPos()))
			return;

		//Grab model + correct eextended state
		final BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
		BlockPos blockPos = te.getBlockPos();
		BlockState state = te.getLevelNonnull().getBlockState(blockPos);
		if(state.getBlock()!=Multiblocks.autoWorkbench.get())
			return;
		BakedModel model = DYNAMIC.get(te.getFacing());

		//Outer GL Wrapping, initial translation
		matrixStack.pushPose();
		matrixStack.translate(0.5, 0.5, 0.5);
		bufferIn = BERenderUtils.mirror(te, matrixStack, bufferIn);

		//Item Displacement
		float[][] itemDisplays = new float[te.processQueue.size()][];
		//Animations
		float drill = 0;
		float lift = 0;
		float press = 0;
		float liftPress = 0;

		for(int i = 0; i < itemDisplays.length; i++)
		{
			MultiblockProcess<MultiblockRecipe> process = te.processQueue.get(i);
			if(process==null||process.processTick <= 0||process.processTick==process.maxTicks)
				continue;
			//+partialTicks
			float processTimer = ((float)process.processTick)/process.maxTicks*180;
			if(processTimer <= 9)
				continue;

			float itemX = -1;
			float itemY = -.34375f;
			float itemZ = -.9375f;
			float itemAngle = 90f;

			if(processTimer <= 24)//slide
			{
				itemAngle = 67.5f;
				if(processTimer <= 19)
				{
					itemZ += .25+(19-processTimer)/10f*.5f;
					itemY += .25+(19-processTimer)/10f*.21875f;
				}
				else
				{
					itemZ += (24-processTimer)/5f*.25f;
					itemY += (24-processTimer)/5f*.25f;
				}
			}
			else if(processTimer <= 40)
			{
				itemX += (processTimer-24)/16f;
			}
			else if(processTimer <= 100)
			{
				itemX += 1;
				float drillStep = 0;
				if(processTimer <= 60)
				{
					lift = (processTimer-40)/20f*.3125f;
					drillStep = 4+(60-processTimer)*4;
				}
				else if(processTimer <= 80)
				{
					lift = .3125f;
					drillStep = 4;
				}
				else
				{
					lift = (100-processTimer)/20f*.3125f;
					drillStep = 4+(processTimer-80)*4;
				}
				if(drillStep > 0)
					drill = processTimer%drillStep/drillStep*360;
				itemY += Math.max(0, lift-.0625);
			}
			else if(processTimer <= 116)
			{
				itemX += 1;
				itemZ += (processTimer-100)/16f;
			}
			else if(processTimer <= 132)
			{
				itemX += 1+(processTimer-116)/16f;
				itemZ += 1;
			}
			else if(processTimer <= 172)
			{
				itemX += 2;
				itemZ += 1;
				if(processTimer <= 142)
					press = (processTimer-132)/10f;
				else if(processTimer <= 162)
					press = 1;
				else
					press = (172-processTimer)/10f;
				liftPress = press*.0625f;
				itemY += liftPress;
			}
			else if(processTimer <= 180)
			{
				itemX += 2+(processTimer-172)/16f;
				itemZ += 1;
			}
			itemDisplays[i] = new float[]{processTimer, itemX, itemY, itemZ, itemAngle};

		}

		matrixStack.pushPose();
		ItemStack blueprintStack = te.inventory.get(0);
		if(!blueprintStack.isEmpty())
			renderModelPart(matrixStack, blockRenderer, bufferIn, state, model, combinedLightIn, combinedOverlayIn, "blueprint");


		matrixStack.translate(0, lift, 0);
		renderModelPart(matrixStack, blockRenderer, bufferIn, state, model, combinedLightIn, combinedOverlayIn, "lift");
		matrixStack.translate(0, -lift, 0);

		Direction f = te.getFacing();
		float tx = f==Direction.WEST?-.9375f: f==Direction.EAST?.9375f: 0;
		float tz = f==Direction.NORTH?-.9375f: f==Direction.SOUTH?.9375f: 0;
		matrixStack.pushPose();
		matrixStack.translate(tx, 0, tz);
		matrixStack.mulPose(new Quaternion(new Vector3f(0, 1, 0), drill, true));
		renderModelPart(matrixStack, blockRenderer, bufferIn, state, model, combinedLightIn, combinedOverlayIn, "drill");
		matrixStack.popPose();

		tx = f==Direction.WEST?-.59375f: f==Direction.EAST?.59375f: 0;
		tz = f==Direction.NORTH?-.59375f: f==Direction.SOUTH?.59375f: 0;
		matrixStack.pushPose();
		matrixStack.translate(tx, -.21875, tz);
		matrixStack.mulPose(new Quaternion(new Vector3f(-f.getStepZ(), 0, f.getStepX()), press*90, true));
		renderModelPart(matrixStack, blockRenderer, bufferIn, state, model, combinedLightIn, combinedOverlayIn, "press");
		matrixStack.popPose();

		matrixStack.translate(0, liftPress, 0);
		renderModelPart(matrixStack, blockRenderer, bufferIn, state, model, combinedLightIn, combinedOverlayIn, "pressLift");

		matrixStack.popPose();

		switch(f)
		{
			case NORTH:
				break;
			case SOUTH:
				matrixStack.mulPose(new Quaternion(new Vector3f(0, 1, 0), 180, true));
				break;
			case WEST:
				matrixStack.mulPose(new Quaternion(new Vector3f(0, 1, 0), 90, true));
				break;
			case EAST:
				matrixStack.mulPose(new Quaternion(new Vector3f(0, 1, 0), -90, true));
				break;
		}

		//DRAW ITEMS HERE
		for(int i = 0; i < itemDisplays.length; i++)
			if(itemDisplays[i]!=null)
			{
				MultiblockProcess<MultiblockRecipe> process = te.processQueue.get(i);
				if(!(process instanceof PoweredMultiblockBlockEntity.MultiblockProcessInWorld))
					continue;

				float scale = .3125f;
				List<ItemStack> dList = ((MultiblockProcessInWorld<?>)process).getDisplayItem();
				if(!dList.isEmpty())
					if(dList.size() < 2)
					{
						matrixStack.pushPose();
						matrixStack.translate(itemDisplays[i][1], itemDisplays[i][2], itemDisplays[i][3]);
						matrixStack.mulPose(new Quaternion(new Vector3f(1, 0, 0), itemDisplays[i][4], true));
						matrixStack.scale(scale, scale, .5f);
						ClientUtils.mc().getItemRenderer().renderStatic(dList.get(0), TransformType.FIXED, combinedLightIn, combinedOverlayIn, matrixStack, bufferIn, 0);
						matrixStack.popPose();
					}
					else
					{
						int size = dList.size();
						int lines = (int)Math.ceil(size/2f);
						float spacer = (lines-1)*.234375f;
						for(int d = 0; d < size; d++)
						{
							float oX = (size > 2?-.3125f: 0)+(lines-d/2)*.0625f+d%2*.3125f;
							float oZ = -spacer/2f+d/2*.234375f;
							float oY = 0;

							float localItemX = itemDisplays[i][1]+oX;
							float localItemY = itemDisplays[i][2]+oY;
							float localItemZ = itemDisplays[i][3]+oZ;
							float subProcess = itemDisplays[i][0]-d/2*4;
							float localAngle = itemDisplays[i][4];
							if(subProcess <= 24)//slide
							{
								localAngle = 67.5f;
								if(subProcess <= 19)
								{
									localItemZ = -1+.25f+(19-subProcess)/10f*.5f;
									localItemY = -.34375f+.25f+(19-subProcess)/10f*.21875f;
								}
								else
								{
									localItemZ = -1+(oZ-(24-subProcess)/5f*oZ);
									localItemY = -.34375f+(24-subProcess)/5f*.25f;
								}
							}
							matrixStack.pushPose();
							matrixStack.translate(localItemX, localItemY, localItemZ);
							matrixStack.mulPose(new Quaternion(new Vector3f(1, 0, 0), localAngle, true));
							matrixStack.scale(scale, scale, .5f);
							ClientUtils.mc().getItemRenderer().renderStatic(dList.get(d), TransformType.FIXED, combinedLightIn, combinedOverlayIn, matrixStack, bufferIn, 0);
							matrixStack.popPose();
						}
					}
			}

		//Blueprint
		double playerDistanceSq = ClientUtils.mc().player.distanceToSqr(Vec3.atCenterOf(blockPos));

		if(!blueprintStack.isEmpty()&&playerDistanceSq < 1000)
		{
			BlueprintCraftingRecipe[] recipes = BlueprintCraftingRecipe.findRecipes(ItemNBTHelper.getString(blueprintStack, "blueprint"));
			BlueprintCraftingRecipe recipe = (te.selectedRecipe < 0||te.selectedRecipe >= recipes.length)?null: recipes[te.selectedRecipe];
			BlueprintLines blueprint = recipe==null?null: getBlueprintDrawable(recipe, te.getLevelNonnull());
			if(blueprint!=null)
			{
				//Width depends on distance
				float lineWidth = playerDistanceSq < 6?3: playerDistanceSq < 25?2: playerDistanceSq < 40?1: .5f;
				matrixStack.pushPose();
				matrixStack.translate(-.195, .125, .97);
				matrixStack.mulPose(new Quaternion(new Vector3f(1, 0, 0), -45, true));
				float scale = .5f/blueprint.textureScale;
				matrixStack.scale(scale, -scale, scale);
				matrixStack.translate(0.5, 0.5, 0.5);
				blueprint.draw(lineWidth, matrixStack, bufferIn);
				matrixStack.popPose();
			}
		}
		matrixStack.popPose();
	}

	public static void renderModelPart(
			PoseStack matrix, final BlockRenderDispatcher blockRenderer, MultiBufferSource buffers, BlockState state,
			BakedModel model, int light, int overlay, String parts
	)
	{
		matrix.pushPose();
		matrix.translate(-0.5, -0.5, -0.5);
		IModelData data = new SinglePropertyModelData<>(new IEObjState(VisibilityList.show(parts)), Model.IE_OBJ_STATE);

		blockRenderer.getModelRenderer().renderModel(matrix.last(), buffers.getBuffer(RenderType.solid()), state, model,
				1, 1, 1,
				light, overlay, data);
		matrix.popPose();
	}

	public static HashMap<BlueprintCraftingRecipe, BlueprintLines> blueprintCache = new HashMap<BlueprintCraftingRecipe, BlueprintLines>();

	public static BlueprintLines getBlueprintDrawable(BlueprintCraftingRecipe recipe, Level world)
	{
		if(recipe==null)
			return null;
		BlueprintLines blueprint = blueprintCache.get(recipe);
		if(blueprint==null)
		{
			blueprint = getBlueprintDrawable(recipe.output, world);
			blueprintCache.put(recipe, blueprint);
		}
		return blueprint;
	}

	public static BlueprintLines getBlueprintDrawable(ItemStack stack, Level world)
	{
		if(stack.isEmpty())
			return null;
		Player player = ClientUtils.mc().player;
		ArrayList<BufferedImage> images = new ArrayList<>();
		try
		{
			BakedModel ibakedmodel = ClientUtils.mc().getItemRenderer().getModel(stack, world, player, 0);
			HashSet<String> textures = new HashSet<>();
			Collection<BakedQuad> quads = ibakedmodel.getQuads(null, null, world.random, EmptyModelData.INSTANCE);
			for(BakedQuad quad : quads)
				if(quad!=null&&quad.getSprite()!=null)
					textures.add(quad.getSprite().getName().toString());
			for(String s : textures)
			{
				ResourceLocation rl = new ResourceLocation(s);
				rl = new ResourceLocation(rl.getNamespace(), String.format("%s/%s%s", "textures", rl.getPath(), ".png"));
				Resource resource = ClientUtils.mc().getResourceManager().getResource(rl);
				BufferedImage bufferedImage = ClientUtils.readBufferedImage(resource.getInputStream());
				if(bufferedImage!=null)
					images.add(bufferedImage);
			}
		} catch(Exception e)
		{
		}
		if(images.isEmpty())
			return null;
		ArrayList<Pair<TexturePoint, TexturePoint>> lines = new ArrayList<>();
		Set<TexturePoint> testSet = new HashSet<>();
		HashMultimap<Integer, TexturePoint> area = HashMultimap.create();
		int wMax = 0;
		for(BufferedImage bufferedImage : images)
		{
			Set<Pair<TexturePoint, TexturePoint>> temp_lines = new HashSet<>();

			int w = bufferedImage.getWidth();
			int h = bufferedImage.getHeight();

			if(h > w)
				h = w;
			if(w > wMax)
				wMax = w;
			for(int hh = 0; hh < h; hh++)
				for(int ww = 0; ww < w; ww++)
				{
					int argb = bufferedImage.getRGB(ww, hh);
					float r = (argb >> 16&255)/255f;
					float g = (argb >> 8&255)/255f;
					float b = (argb&255)/255f;
					float intesity = (r+b+g)/3f;
					int alpha = (argb >> 24)&255;
					if(alpha > 0)
					{
						boolean added = false;
						//Check colour sets for similar colour to shade it later
						TexturePoint tp = new TexturePoint(ww, hh, w);
						if(!testSet.contains(tp))
						{
							for(Integer key : area.keySet())
							{
								for(TexturePoint p : area.get(key))
								{
									float mod = w/(float)p.scale;
									int pColour = bufferedImage.getRGB((int)(p.x*mod), (int)(p.y*mod));
									float dR = (r-(pColour >> 16&255)/255f);
									float dG = (g-(pColour >> 8&255)/255f);
									float dB = (b-(pColour&255)/255f);
									double delta = Math.sqrt(dR*dR+dG*dG+dB*dB);
									if(delta < .25)
									{
										area.put(key, tp);
										added = true;
										break;
									}
								}
								if(added)
									break;
							}
							if(!added)
								area.put(argb, tp);
							testSet.add(tp);
						}
						//Compare to direct neighbour
						for(int i = 0; i < 4; i++)
						{
							int xx = (i==0?-1: i==1?1: 0);
							int yy = (i==2?-1: i==3?1: 0);
							int u = ww+xx;
							int v = hh+yy;

							int neighbour = 0;
							float delta = 1;
							boolean notTransparent = false;
							if(u >= 0&&u < w&&v >= 0&&v < h)
							{
								neighbour = bufferedImage.getRGB(u, v);
								notTransparent = ((neighbour >> 24)&255) > 0;
								if(notTransparent)
								{
									float neighbourIntesity = ((neighbour >> 16&255)+(neighbour >> 8&255)+(neighbour&255))/765f;
									float intesityDelta = Math.max(0, Math.min(1, Math.abs(intesity-neighbourIntesity)));
									float rDelta = Math.max(0, Math.min(1, Math.abs(r-(neighbour >> 16&255)/255f)));
									float gDelta = Math.max(0, Math.min(1, Math.abs(g-(neighbour >> 8&255)/255f)));
									float bDelta = Math.max(0, Math.min(1, Math.abs(b-(neighbour&255)/255f)));
									delta = Math.max(intesityDelta, Math.max(rDelta, Math.max(gDelta, bDelta)));
									delta = delta < .25?0: delta > .4?1: delta;
								}
							}
							if(delta > 0)
							{
								Pair<TexturePoint, TexturePoint> l = Pair.of(new TexturePoint(ww+(i==0?0: i==1?1: 0), hh+(i==2?0: i==3?1: 0), w), new TexturePoint(ww+(i==0?0: i==1?1: 1), hh+(i==2?0: i==3?1: 1), w));
								temp_lines.add(l);
							}
						}
					}
				}
			lines.addAll(temp_lines);
		}

		ArrayList<Integer> lumiSort = new ArrayList<>(area.keySet());
		lumiSort.sort(Comparator.comparingDouble(AutoWorkbenchRenderer::getLuminance));
		HashMultimap<ShadeStyle, Point> complete_areaMap = HashMultimap.create();
		int lineNumber = 2;
		int lineStyle = 0;
		for(Integer i : lumiSort)
		{
			complete_areaMap.putAll(new ShadeStyle(lineNumber, lineStyle), area.get(i));
			++lineStyle;
			lineStyle %= 3;
			if(lineStyle==0)
				lineNumber += 1;
		}

		Set<Pair<Point, Point>> complete_lines = new HashSet<>();
		for(Pair<TexturePoint, TexturePoint> line : lines)
		{
			TexturePoint p1 = line.getKey();
			TexturePoint p2 = line.getValue();
			complete_lines.add(Pair.of(new Point((int)(p1.x/(float)p1.scale*wMax), (int)(p1.y/(float)p1.scale*wMax)), new Point((int)(p2.x/(float)p2.scale*wMax), (int)(p2.y/(float)p2.scale*wMax))));
		}
		return new BlueprintLines(wMax, complete_lines, complete_areaMap);
	}

	public static class BlueprintLines
	{
		final int textureScale;
		final Set<Pair<Point, Point>> lines;
		final HashMultimap<ShadeStyle, Point> areas;

		BlueprintLines(int textureScale, Set<Pair<Point, Point>> lines, HashMultimap<ShadeStyle, Point> areas)
		{
			this.textureScale = textureScale;
			this.lines = lines;
			this.areas = areas;
		}

		public int getTextureScale()
		{
			return textureScale;
		}

		public void draw(float lineWidth, PoseStack matrixStack, MultiBufferSource buffer)
		{
			//Draw edges
			RenderType type = IERenderTypes.getLines(lineWidth);
			VertexConsumer baseBuilder = buffer.getBuffer(type);
			TransformingVertexBuilder builder = new TransformingVertexBuilder(baseBuilder, matrixStack, type.format());
			builder.defaultColor(255, 255, 255, 255);
			for(Pair<Point, Point> line : lines)
			{
				builder.vertex(line.getKey().x, line.getKey().y, 0)
						.endVertex();
				builder.vertex(line.getValue().x, line.getValue().y, 0)
						.endVertex();
			}

			if(lineWidth >= 1)//Draw shading if player is close enough
				for(ShadeStyle style : areas.keySet())
					for(Point pixel : areas.get(style))
						style.drawShading(pixel, builder);
			builder.unsetDefaultColor();
		}
	}

	private static class ShadeStyle
	{
		int stripeAmount = 1;
		int stripeDirection = 0;

		ShadeStyle(int stripeAmount, int stripeDirection)
		{
			this.stripeAmount = stripeAmount;
			this.stripeDirection = stripeDirection;
		}

		void drawShading(Point pixel, VertexConsumer builder)
		{
			float step = 1/(float)stripeAmount;
			float offset = step/2;
			if(stripeDirection > 1)
			{
				int perSide = stripeAmount/2+(stripeAmount%2==1?1: 0);
				step = 1/(float)(perSide);
				offset = stripeAmount%2==1?step: step/2;
			}
			for(int i = 0; i < stripeAmount; i++)
				if(stripeDirection==0)//vertical
				{
					builder.vertex(pixel.x+offset+step*i, pixel.y, 0).endVertex();
					builder.vertex(pixel.x+offset+step*i, pixel.y+1, 0).endVertex();
				}
				else if(stripeDirection==1)//horizontal
				{
					builder.vertex(pixel.x, pixel.y+offset+step*i, 0).endVertex();
					builder.vertex(pixel.x+1, pixel.y+offset+step*i, 0).endVertex();
				}
				else if(stripeDirection==2)//diagonal
				{
					if(i==stripeAmount-1&&stripeAmount%2==1)
					{
						builder.vertex(pixel.x, pixel.y+1, 0).endVertex();
						builder.vertex(pixel.x+1, pixel.y, 0).endVertex();
					}
					else if(i%2==0)
					{
						builder.vertex(pixel.x, pixel.y+offset+step*(i/2), 0).endVertex();
						builder.vertex(pixel.x+offset+step*(i/2), pixel.y, 0).endVertex();
					}
					else
					{
						builder.vertex(pixel.x+1-offset-step*(i/2), pixel.y+1, 0).endVertex();
						builder.vertex(pixel.x+1, pixel.y+1-offset-step*(i/2), 0).endVertex();
					}
				}
		}
	}

	private static class TexturePoint extends Point
	{
		final int scale;

		public TexturePoint(int x, int y, int scale)
		{
			super(x, y);
			this.scale = scale;
		}

		@Override
		public int hashCode()
		{
			return 31*(31*x+y)+scale;
		}
	}

	private static double getLuminance(int rgb)
	{
		return Math.sqrt(.241*(rgb >> 16&255)+.691*(rgb >> 8&255)+.068*(rgb&255));
	}
}
