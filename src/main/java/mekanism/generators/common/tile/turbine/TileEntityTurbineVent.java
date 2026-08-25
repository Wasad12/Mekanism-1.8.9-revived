package mekanism.generators.common.tile.turbine;

import java.util.ArrayList;

import mekanism.common.util.PipeUtils;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

public class TileEntityTurbineVent extends TileEntityTurbineCasing
{
	public TileEntityTurbineVent()
	{
		super("TurbineVent");
	}
	
	@Override
	public void onUpdate()
	{
		super.onUpdate();
		
		if(!worldObj.isRemote && structure != null && structure.flowRemaining > 0)
		{
			FluidStack fluidStack = new FluidStack(FluidRegistry.WATER, structure.flowRemaining);
			
			structure.flowRemaining -= PipeUtils.emit(new ArrayList(), fluidStack, this);
		}
	}
}
