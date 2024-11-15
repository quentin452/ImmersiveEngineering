package blusunrize.immersiveengineering.client;

import java.util.LinkedHashSet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import blusunrize.immersiveengineering.api.ManualHelper;
import blusunrize.immersiveengineering.common.Config;
import blusunrize.immersiveengineering.common.util.Utils;
import blusunrize.lib.manual.IManualPage;
import blusunrize.lib.manual.ManualInstance;

public class IEManualInstance extends ManualInstance
{
	public IEManualInstance()
	{
		super(new FontRenderer(ClientUtils.mc().gameSettings, new ResourceLocation("textures/font/ascii.png"), ClientUtils.mc().renderEngine, false), "immersiveengineering:textures/gui/manual.png");
		
		this.fontRenderer.colorCode[0+6] = 0xf78034;
		this.fontRenderer.colorCode[16+6] = 0x3e200d;
		if (Minecraft.getMinecraft().gameSettings.language != null)
		{
			this.fontRenderer.setUnicodeFlag(ClientUtils.mc().getLanguageManager().isCurrentLocaleUnicode());
			this.fontRenderer.setBidiFlag(ClientUtils.mc().getLanguageManager().isCurrentLanguageBidirectional());
		}
		((IReloadableResourceManager)ClientUtils.mc().getResourceManager()).registerReloadListener(this.fontRenderer);
	}

	@Override
	public String formatText(String s)
	{
		String translKey = "ie.manual.entry."+s;
		String translated = StatCollector.translateToLocal(translKey);
		if(translKey!=translated)
			s = translated;
		String splitKey = ";";

		s = s.replaceAll("<br>", "\n");
		int start;
		int overflow=0;
		while( (start=s.indexOf("<config"))>=0 && overflow<50)
		{
			overflow++;
			int end = s.indexOf(">", start);
			String rep = s.substring(start, end+1);
			String[] segment = rep.substring(0,rep.length()-1).split(splitKey);
			if(segment.length<3)
				break;
			String result = "";
			if(segment[1].equalsIgnoreCase("b"))
			{
				if(segment.length>3)
					result = (Config.getBoolean(segment[2])?segment[3]: segment.length>4?segment[4]:"");
				else
					result = ""+Config.getBoolean(segment[2]);
			}
			else if(segment[1].equalsIgnoreCase("i"))
				result = ""+Config.getInt(segment[2]);
			else if(segment[1].equalsIgnoreCase("iA"))
			{
				int[] iA = Config.getIntArray(segment[2]);
				if(segment.length>3)
					try{
						if(segment[3].startsWith("l"))
						{
							int limiter = Integer.parseInt(segment[3].substring(1));
							for(int i=0; i<limiter; i++)
								result += (i>0?", ":"")+iA[i];
						}
						else
						{
							int idx = Integer.parseInt(segment[3]);
							result = ""+iA[idx];
						}
					}catch(Exception ex){
						break;
					}
				else
					for(int i=0; i<iA.length; i++)
						result += (i>0?", ":"")+iA[i];
			}
			else if(segment[1].equalsIgnoreCase("dA"))
			{
				double[] iD = Config.getDoubleArray(segment[2]);
				if(segment.length>3)
					try{
						int idx = Integer.parseInt(segment[3]);
						result = ""+Utils.formatDouble(iD[idx], "#.***");
					}catch(Exception ex){
						break;
					}
				else
					for(int i=0; i<iD.length; i++)
						result += (i>0?", ":"")+Utils.formatDouble(iD[i], "#.***");
			}

			s = s.replaceFirst(rep, result);
		}
		overflow=0;
		while( (start=s.indexOf("<dim"))>=0 && overflow<50)
		{
			overflow++;
			int end = s.indexOf(">", start);
			String rep = s.substring(start, end+1);
			String[] segment = rep.substring(0,rep.length()-1).split(splitKey);
			if(segment.length<2)
				break;
			String result = "";
			try{
				int dim = Integer.parseInt(segment[1]);
				World world = DimensionManager.getWorld(dim);
				if(world!=null && world.provider!=null)
				{
					String name = world.provider.getDimensionName();
					if(name.toLowerCase().startsWith("the "))
						name = name.substring(4);
					result = name;
				}
				else
					result = "Dimension "+dim;
			}catch(Exception ex){
				ex.printStackTrace();
			}
			s = s.replaceFirst(rep, result);
		}


		return s;
	}

	@Override
	public String getManualName()
	{
		return StatCollector.translateToLocal("item.ImmersiveEngineering.tool.manual.name");
	}
	@Override
	public void addEntry(String name, String category, IManualPage... pages)
	{
		super.addEntry(name, category, pages);
		if(!categorySet.contains(category))
			categorySet.add(category);
	}
	LinkedHashSet<String> categorySet = new LinkedHashSet<String>();
	@Override
	public String[] getSortedCategoryList()
	{
		return categorySet.toArray(new String[categorySet.size()]);
	}
	@Override
	public String formatCategoryName(String s)
	{
		return StatCollector.translateToLocal("ie.manual.category."+s+".name");
	}
	@Override
	public String formatEntryName(String s)
	{
		return StatCollector.translateToLocal("ie.manual.entry."+s+".name");
	}
	@Override
	public String formatEntrySubtext(String s)
	{
		return StatCollector.translateToLocal("ie.manual.entry."+s+".subtext");
	}
	@Override
	public boolean showEntryInList(ManualEntry entry)
	{
		if (entry==null||entry.getCategory()==null)
			return false;
		
		if(entry.getCategory().equalsIgnoreCase(ManualHelper.CAT_UPDATE))
			return Config.getBoolean("showUpdateNews");

		return true;
	}
	@Override
	public boolean showCategoryInList(String category)
	{
		return true;
	}

	@Override
	public int getTitleColour()
	{
		return 0xf78034;
	}
	@Override
	public int getSubTitleColour()
	{
		return 0xf78034;
	}
	@Override
	public int getTextColour()
	{
		return 0x555555;
	}
	@Override
	public int getHighlightColour()
	{
		return 0xd4804a;
	}
	@Override
	public int getPagenumberColour()
	{
		return 0x9c917c;
	}

	@Override
	public boolean allowGuiRescale()
	{
		return Config.getBoolean("adjustManualScale");
	}
}