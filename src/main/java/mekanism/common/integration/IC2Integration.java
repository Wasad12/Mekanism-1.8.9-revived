package mekanism.common.integration;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.energy.tile.IEnergyTile;
import ic2.api.tile.IEnergyStorage;
import mekanism.api.MekanismConfig.general;
import mekanism.common.base.IEnergyWrapper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.Optional.Method;

public class IC2Integration 
{
	@Method(modid = "IC2")
	public static boolean isOutputter(TileEntity tileEntity, EnumFacing side)
	{
		IEnergyTile tile = EnergyNet.instance.getSubTile(tileEntity.getWorld(), tileEntity.getPos());
		
		if(tile instanceof IEnergySource && ((IEnergySource)tile).emitsEnergyTo(null, side.getOpposite()))
		{
			return true;
		}
		
		return false;
	}
	
	@Method(modid = "IC2")
	public static boolean isAcceptor(TileEntity orig, TileEntity tileEntity, EnumFacing side)
	{
		IEnergyTile tile = EnergyNet.instance.getSubTile(tileEntity.getWorld(), tileEntity.getPos());
		
		if(tile instanceof IEnergySink)
		{
			if(((IEnergySink)tile).acceptsEnergyFrom(null, side.getOpposite()))
			{
				return true;
			}
		}
		
		return false;
	}

	@Method(modid = "IC2")
	public static boolean isEnergyAcceptor(TileEntity tileEntity)
	{
		IEnergyTile tile = EnergyNet.instance.getSubTile(tileEntity.getWorld(), tileEntity.getPos());
		return tile instanceof IEnergySink || tile instanceof IEnergyStorage;
	}
	
	@Method(modid = "IC2")
	public static double emitEnergy(IEnergyWrapper from, TileEntity tileEntity, EnumFacing side, double currentSending)
	{
		IEnergyTile tile = EnergyNet.instance.getSubTile(tileEntity.getWorld(), tileEntity.getPos());
		
		if(tile instanceof IEnergySink && ((IEnergySink)tile).acceptsEnergyFrom(from, side.getOpposite()))
		{
			double toSend = Math.min(currentSending*general.TO_IC2, EnergyNet.instance.getPowerFromTier(((IEnergySink)tile).getSinkTier()));
			toSend = Math.min(Math.min(toSend, ((IEnergySink)tile).getDemandedEnergy()), Integer.MAX_VALUE);
			return (toSend - (((IEnergySink)tile).injectEnergy(side.getOpposite(), toSend, 0)))*general.FROM_IC2;
		}
		else if(tile instanceof IEnergyStorage)
		{
			IEnergyStorage storage = (IEnergyStorage)tile;
			int maxReceive = (int)Math.floor(currentSending*general.TO_IC2);
			int stored = storage.getStored();
			int capacity = storage.getCapacity();
			int toSend = Math.min(maxReceive, capacity - stored);
			if(toSend > 0)
			{
				storage.addEnergy(toSend);
				int after = storage.getStored();
				return (after - stored)*general.FROM_IC2;
			}
		}
		
		return 0;
	}
}
