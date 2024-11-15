package blusunrize.immersiveengineering.api.shader;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

public class ShaderCaseBalloon extends ShaderCase
{
	public String additionalTexture = null;

	public ShaderCaseBalloon(String overlayType, int[] colourUnderlying, int[] colourPrimary, int[] colourSecondary, String additionalTexture)
	{
		super(overlayType, colourUnderlying,colourPrimary,colourSecondary, "immersiveengineering:shaders/balloon_");
		this.additionalTexture = additionalTexture;
	}

	@Override
	public String getShaderType()
	{
		return "balloon";
	}

	@Override
	public int getPasses(ItemStack shader, ItemStack item, String modelPart)
	{
		return additionalTexture!=null?3:2;
	}

	@Override
	public IIcon getReplacementIcon(ItemStack shader, ItemStack item, String modelPart, int pass)
	{
		return pass==2?i_balloonAdditional: pass==1?i_balloonOverlay: i_balloonBase;
	}

	@Override
	public int[] getRGBAColourModifier(ItemStack shader, ItemStack item, String modelPart, int pass)
	{
		if(pass==2 && additionalTexture!=null)
			return colourOverlay;
		return pass==1?colourSecondary : colourPrimary;
	}

	public IIcon i_balloonBase;
	public IIcon i_balloonOverlay;
	public IIcon i_balloonAdditional;
	@Override
	public void stichTextures(IIconRegister ir, int sheetID)
	{
		if(sheetID==0)
		{
			i_balloonBase = ir.registerIcon("immersiveengineering:shaders/balloon_0");
			i_balloonOverlay = ir.registerIcon(this.baseTexturePath+"1_"+this.overlayType);
			if(this.additionalTexture!=null)
				i_balloonAdditional = ir.registerIcon(this.baseTexturePath+additionalTexture);
		}
	}

	@Override
	public void modifyRender(ItemStack shader, ItemStack item, String modelPart, int pass, boolean pre, boolean inventory)
	{
	}
}