package blusunrize.immersiveengineering.api.shader;

import net.minecraft.item.ItemStack;

public interface IShaderItem
{
	public ShaderCase getShaderCase(ItemStack shader, ItemStack item, String shaderType);
}