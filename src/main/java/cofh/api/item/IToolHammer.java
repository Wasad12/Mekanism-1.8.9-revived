package cofh.api.item;

import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.entity.EntityLivingBase;

public interface IToolHammer
{
	boolean isUsable(ItemStack item, EntityLivingBase user, BlockPos pos);

	void toolUsed(ItemStack item, EntityLivingBase user, BlockPos pos);
}
